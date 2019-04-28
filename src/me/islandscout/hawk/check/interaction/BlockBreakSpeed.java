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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockDigCheck;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.block.BlockNMS;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.material.Leaves;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakSpeed extends BlockDigCheck {

    /*
      Conforms to 1.8 block breaking standards

      No need to check if the player is using the appropriate tool, because Spigot already does that.
      Spigot will delay the block destruction if, for example, a player uses a diamond shovel to mine stone in 2x speed.
      Additionally, Hawk's WrongBlock check will cancelThreshold it if the player starts mining another block.
      A player using a diamond shovel to mine stone is able to break 1.4x faster using cheats. Might need to improve this.
    */

    /*TODO: Rewrite this so that it updates expected block damage every client tick, rather than
        waiting until a COMPLETE status to approximate expected time. Should be more accurate.
        Take a peek at nms.Block#getDamage(...)
    */
    //TODO: Shears and wool

    private final Map<UUID, Long> interactTick;
    private final boolean PREVENT_SAME_TICK;

    public BlockBreakSpeed() {
        super("blockbreakspeed", "%player% failed block break speed. Block: %block%, Time: %time%, VL: %vl%");
        interactTick = new HashMap<>();
        PREVENT_SAME_TICK = (boolean)customSetting("flagSameTick", "", true);
    }

    public void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if (e.getDigAction() == BlockDigEvent.DigAction.START && p.getGameMode() != GameMode.CREATIVE) {
            interactTick.put(p.getUniqueId(), pp.getCurrentTick());
            return;
        }
        if (e.getDigAction() == BlockDigEvent.DigAction.COMPLETE || p.getGameMode() == GameMode.CREATIVE) {
            Block b = e.getBlock();
            float hardness = BlockNMS.getBlockNMS(b).getStrength();

            boolean harvestable = e.getBlock().getDrops(e.getPlayer().getItemInHand()).size() > 0 || e.getBlock().getDrops().size() == 0 || e.getBlock().getState().getData() instanceof Leaves;
            double expectedTime = harvestable ? hardness * 1.5 : hardness * 5;

            int enchant = p.getItemInHand().getEnchantmentLevel(Enchantment.DIG_SPEED);
            enchant = enchant > 0 ? (enchant * enchant) + 1 : 0;

            String name = p.getItemInHand().toString();
            if (name.contains("SPADE") || name.contains("PICKAXE") || name.contains("AXE")) {
                if (name.contains("WOOD")) expectedTime *= 1D / (2 + enchant);
                else if (name.contains("STONE")) expectedTime *= 1D / (4 + enchant);
                else if (name.contains("IRON")) expectedTime *= 1D / (6 + enchant);
                else if (name.contains("DIAMOND")) expectedTime *= 1D / (8 + enchant);
                else if (name.contains("GOLD")) expectedTime *= 1D / (12 + enchant);
            } else expectedTime *= 1D / (enchant > 0 ? enchant : 1);

            expectedTime = potionEffect(expectedTime, p);

            expectedTime = Math.round(expectedTime * 100000) / 100000D;
            double actualTime = (pp.getCurrentTick() - interactTick.getOrDefault(p.getUniqueId(), 0L) + 1) * 0.05;

            if (p.getGameMode() == GameMode.CREATIVE)
                expectedTime = 0.05;

            if (actualTime < expectedTime || (PREVENT_SAME_TICK && pp.getCurrentTick() == interactTick.getOrDefault(p.getUniqueId(), 0L))) {
                punishAndTryCancelAndBlockRespawn(pp, 1, e, new Placeholder("block", b.getType()), new Placeholder("time", MathPlus.round(actualTime, 2) + "s"));
            } else {
                reward(pp);
            }

            if (p.getGameMode() == GameMode.CREATIVE)
                interactTick.put(p.getUniqueId(), pp.getCurrentTick());
        }
    }

    private double potionEffect(double expectedTime, Player p) {
        for (PotionEffect effect : p.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.FAST_DIGGING) && !effect.getType().equals(PotionEffectType.SLOW_DIGGING))
                continue;
            expectedTime *= 1 / (((effect.getAmplifier() + 1) * 0.2) + 1);
            if (effect.getType().equals(PotionEffectType.SLOW_DIGGING)) {
                int poteffect = effect.getAmplifier() + 1;
                switch (poteffect) {
                    case 1:
                        expectedTime *= 3.33333333333333;
                    case 2:
                        expectedTime *= 11.1111111111111;
                    case 3:
                        expectedTime *= 370.037037037037;
                    default:
                        expectedTime *= 1234.56790123456;
                }
            }
            break;
        }
        return expectedTime;
    }

    @Override
    public void removeData(Player p) {
        interactTick.remove(p.getUniqueId());
    }
}
