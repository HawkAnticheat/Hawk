package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncBlockDigCheck;
import me.islandscout.hawk.events.DigAction;
import me.islandscout.hawk.events.BlockDigEvent;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.material.Leaves;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakSpeed extends AsyncBlockDigCheck {

    /*
      Conforms to 1.8 block breaking standards

      No need to check if the player is using the appropriate tool, because Spigot already does that.
      Spigot will delay the block destruction if, for example, a player uses a diamond shovel to mine stone in 2x speed.
      Additionally, Hawk's WrongBlock check will cancelThreshold it if the player starts mining another block.
      A player using a diamond shovel to mine stone is able to break 1.4x faster using cheats. Might need to improve this.
    */

    //TODO: Shears and wool

    private Map<UUID, Long> interactTime;
    private Map<Material, Integer> materialTime;
    private final int DURATION_OFFSET;
    private final double CREATIVE_RATE;

    public BlockBreakSpeed() {
        super("blockbreakspeed", "&7%player% failed block break speed. Block: %block%, Time: %time%, VL: %vl%");
        interactTime = new HashMap<>();
        materialTime = new HashMap<>();
        DURATION_OFFSET = 55;
        CREATIVE_RATE = 1/15D;
    }

    public void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if(e.getDigAction() == DigAction.START && p.getGameMode() != GameMode.CREATIVE) {
            interactTime.put(p.getUniqueId(), System.currentTimeMillis());
            return;
        }
        if(e.getDigAction() == DigAction.COMPLETE || p.getGameMode() == GameMode.CREATIVE) {
            Block b = e.getBlock();
            float hardness = BlockNMS.getBlockNMS(b).getStrength();

            boolean harvestable = e.getBlock().getDrops(e.getPlayer().getItemInHand()).size() > 0 || e.getBlock().getDrops().size() == 0 || e.getBlock().getState().getData() instanceof Leaves;
            double expectedTime = harvestable ? hardness * 1.5 : hardness * 5;

            int enchant = p.getItemInHand().getEnchantmentLevel(Enchantment.DIG_SPEED);
            enchant = enchant > 0 ? (enchant * enchant) + 1 : 0;

            String name = p.getItemInHand().toString();
            if(name.contains("SPADE") || name.contains("PICKAXE") || name.contains("AXE")) {
                if(name.contains("WOOD")) expectedTime *= 1D / (2 + enchant);
                else if(name.contains("STONE")) expectedTime *= 1D / (4 + enchant);
                else if(name.contains("IRON")) expectedTime *= 1D / (6 + enchant);
                else if(name.contains("DIAMOND")) expectedTime *= 1D / (8 + enchant);
                else if(name.contains("GOLD")) expectedTime *= 1D / (12 + enchant);
            }
            else expectedTime *= 1 / (enchant > 0 ? enchant : 1);

            expectedTime = potionEffect(expectedTime, p);

            expectedTime = Math.round(expectedTime * 100000) / 100000D;
            double actualTime = (System.currentTimeMillis() - interactTime.getOrDefault(p.getUniqueId(), 0L) + DURATION_OFFSET) / 1000D;

            if(p.getGameMode() == GameMode.CREATIVE)
                expectedTime = CREATIVE_RATE;

            //Debug.broadcastMessage("ACTUAL: " + actualTime + ", EXPECTED: " + expectedTime);
            if(actualTime < expectedTime) {
                punishAndTryCancelAndBlockRespawn(pp, e, new Placeholder("block", b.getType()), new Placeholder("time", actualTime + "s"));
            }
            else {
                reward(pp);
            }

            if(p.getGameMode() == GameMode.CREATIVE)
                interactTime.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    private double potionEffect(double expectedTime, Player p) {
        for(PotionEffect effect : p.getActivePotionEffects()) {
            if(!effect.getType().equals(PotionEffectType.FAST_DIGGING) && !effect.getType().equals(PotionEffectType.SLOW_DIGGING)) continue;
            expectedTime *= 1 / (((effect.getAmplifier() + 1) * 0.2) + 1);
            if(effect.getType().equals(PotionEffectType.SLOW_DIGGING)) {
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
}
