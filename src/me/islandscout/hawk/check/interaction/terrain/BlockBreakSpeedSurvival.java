/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.interaction.terrain;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.itemstack.WrappedItemStack;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakSpeedSurvival extends CustomCheck {

    private final Map<UUID, Long> interactTick;
    private final Map<UUID, Block> targetBlockMap;
    private final Map<UUID, Float> blockDamageCumulativeMap;
    private boolean CHECK_ON_GROUND;
    private final boolean PREVENT_SAME_TICK;

    public BlockBreakSpeedSurvival() {
        super("blockbreakspeedsurvival", true, 5, 10, 0.99, 5000, "%player% failed block break speed. Block: %block%, Speed: %speed%, VL: %vl%", null);
        interactTick = new HashMap<>();
        targetBlockMap = new HashMap<>();
        blockDamageCumulativeMap = new HashMap<>();
        CHECK_ON_GROUND = (boolean) customSetting("checkOnGround", "", false);
        PREVENT_SAME_TICK = (boolean)customSetting("flagSameTick", "", true);
    }

    @Override
    protected void check(Event event) {
        if(event instanceof MoveEvent)
            tickDig(event.getHawkPlayer());
        else if(event instanceof BlockDigEvent)
            checkEvent((BlockDigEvent) event);
    }

    private void tickDig(HawkPlayer pp) {
        Block target = targetBlockMap.get(pp.getUuid());
        if(pp.isDigging() && target != null && pp.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            float accumulatedDamage = blockDamageCumulativeMap.getOrDefault(pp.getUuid(), 0F);
            float damage = getDamage(target, pp);
            blockDamageCumulativeMap.put(pp.getUuid(), accumulatedDamage + damage);
        }
    }

    private void checkEvent(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if(p.getGameMode() == GameMode.CREATIVE)
            return;

        if (e.getDigAction() == BlockDigEvent.DigAction.START) {
            targetBlockMap.put(p.getUniqueId(), e.getBlock());
            blockDamageCumulativeMap.put(p.getUniqueId(), 0F);
            interactTick.put(p.getUniqueId(), pp.getCurrentTick());
            tickDig(pp);
            return;
        }

        if (e.getDigAction() == BlockDigEvent.DigAction.COMPLETE) {
            float totalDamage = blockDamageCumulativeMap.getOrDefault(p.getUniqueId(), 0F);

            if(totalDamage < 1 || (PREVENT_SAME_TICK && pp.getCurrentTick() == interactTick.getOrDefault(p.getUniqueId(), 0L))) {
                double speedFactor = 1 / totalDamage;
                double vl = Math.min((speedFactor - 1) * 10, 10);
                Block b = e.getBlock();
                punish(pp, vl, true, e, new Placeholder("block", b.getType()), new Placeholder("speed", MathPlus.round(speedFactor, 2) + "x"));
                e.resync();
            }
            else {
                reward(pp);
            }

            targetBlockMap.put(p.getUniqueId(), null);
            blockDamageCumulativeMap.put(p.getUniqueId(), 0F);
        }
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        targetBlockMap.remove(uuid);
        blockDamageCumulativeMap.remove(uuid);
        interactTick.remove(uuid);
    }

    //Yes, I skidded this from NMS. plz don't sue me ;-;
    //I'm only trying to make this game more enjoyable for us players.
    //I'm pasting a bunch of methods here because I can't be bothered
    //to abstract AND synchronize all of this with the network thread
    //(and no, I'm not talking about the "synchronized" keyword).

    private float getDamage(Block block, HawkPlayer pp) {
        if(block == null || ServerUtils.getBlockAsync(block.getLocation()) == null)
            return 0;
        WrappedBlock wBlock = WrappedBlock.getWrappedBlock(block);
        float strength = wBlock.getStrength();
        return strength < 0.0F ? 0.0F : (!isDestroyableByHeldItem(wBlock, pp) ? getCurrentPlayerStrVsBlock(wBlock, false, pp) / strength / 100.0F : getCurrentPlayerStrVsBlock(wBlock, true, pp) / strength / 30.0F);
    }

    private boolean isDestroyableByHeldItem(WrappedBlock wBlock, HawkPlayer pp) {
        if (wBlock.isMaterialAlwaysDestroyable()) {
            return true;
        } else {
            WrappedItemStack itemstack = WrappedItemStack.getWrappedItemStack(pp.getHeldItem());
            return itemstack.canDestroySpecialBlock(wBlock.getBukkitBlock());
        }
    }

    private float getCurrentPlayerStrVsBlock(WrappedBlock wBlock, boolean flag, HawkPlayer pp) {
        float destroySpeed = getDestroySpeedOfHeldItem(wBlock, pp);
        if (destroySpeed > 1.0F && pp.getHeldItem() != null) {
            int enchLvl = pp.getHeldItem().getEnchantmentLevel(Enchantment.DIG_SPEED);
            WrappedItemStack itemstack = WrappedItemStack.getWrappedItemStack(pp.getHeldItem());
            if (enchLvl > 0) {
                float f1 = (float)(enchLvl * enchLvl + 1);
                if (!itemstack.canDestroySpecialBlock(wBlock.getBukkitBlock()) && destroySpeed <= 1.0F) {
                    destroySpeed += f1 * 0.08F;
                } else {
                    destroySpeed += f1;
                }
            }
        }

        for(org.bukkit.potion.PotionEffect potionEffect : pp.getPlayer().getActivePotionEffects()) {

            if(potionEffect.getType().equals(PotionEffectType.FAST_DIGGING)) {
                destroySpeed *= 1.0F + (potionEffect.getAmplifier() + 1) * 0.2F;
            }

            if(Hawk.getServerVersion() == 8) {
                if(potionEffect.getType().equals(PotionEffectType.SLOW_DIGGING)) {
                    float f1;
                    switch(potionEffect.getAmplifier()) {
                        case 0:
                            f1 = 0.3F;
                            break;
                        case 1:
                            f1 = 0.09F;
                            break;
                        case 2:
                            f1 = 0.0027F;
                            break;
                        case 3:
                        default:
                            f1 = 8.1E-4F;
                    }

                    destroySpeed *= f1;
                }
            }
            else {
                if(potionEffect.getType().equals(PotionEffectType.SLOW_DIGGING)) {
                    destroySpeed *= 1.0F - (potionEffect.getAmplifier() + 1) * 0.2F;
                }
            }
        }

        boolean waterWorkerEnch = pp.getHeldItem() != null && pp.getHeldItem().getEnchantmentLevel(Enchantment.WATER_WORKER) > 0;
        if (this.a(pp) && !waterWorkerEnch) {
            destroySpeed /= 5.0F;
        }

        if (CHECK_ON_GROUND && !pp.isOnGround()) {
            destroySpeed /= 5.0F;
        }

        return destroySpeed;
    }

    private float getDestroySpeedOfHeldItem(WrappedBlock wBlock, HawkPlayer pp) {
        float f = 1.0F;
        WrappedItemStack itemStack = WrappedItemStack.getWrappedItemStack(pp.getHeldItem());
        f *= itemStack.getDestroySpeed(wBlock.getBukkitBlock());
        return f;
    }

    private boolean a(HawkPlayer pp) {
        Vector position = pp.getPosition();
        double d0 = position.getY() + 1.62;
        int i = floor(position.getX());
        int j = (int)d((float)floor(d0));
        int k = floor(position.getZ());
        Block block = ServerUtils.getBlockAsync(new Location(pp.getWorld(), i, j, k));
        if(block == null)
            return false;
        if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
            float f = b(block.getData()) - 0.11111111F;
            float f1 = (float)(j + 1) - f;
            return d0 < (double)f1;
        } else {
            return false;
        }
    }

    private int floor(double var0) {
        int var2 = (int)var0;
        return var0 < (double)var2 ? var2 - 1 : var2;
    }

    private long d(double var0) {
        long var2 = (long)var0;
        return var0 < (double)var2 ? var2 - 1L : var2;
    }

    private float b(int i) {
        if (i >= 8) {
            i = 0;
        }
        return (i + 1) / 9.0F;
    }

}
