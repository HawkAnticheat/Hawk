package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.BukkitCheck;
import me.islandscout.hawk.checks.Cancelless;

public class FightAccuracy extends BukkitCheck implements Cancelless {

    public FightAccuracy(Hawk hawk, String name, String flag) {
        super(hawk, name, flag);
    }


}
