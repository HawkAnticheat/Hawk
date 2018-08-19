package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.combat.*;
import me.islandscout.hawk.checks.interaction.BlockBreakHitbox;
import me.islandscout.hawk.checks.interaction.BlockBreakSpeed;
import me.islandscout.hawk.checks.interaction.WrongBlock;
import me.islandscout.hawk.checks.movement.*;
import me.islandscout.hawk.events.BlockDigEvent;
import me.islandscout.hawk.events.Event;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.Debug;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private Hawk hawk;

    private ExemptList exemptList;

    private List<Check> checkList = new ArrayList<>();

    public CheckManager(Hawk hawk) {
        this.hawk = hawk;
        Check.hawk = hawk;
        exemptList = new ExemptList();
    }

    //initialize checks, register any Bukkit events they listen to, and save any changes in configs to files.
    //can be used to reload checks
    public void loadChecks() {
        checkList.clear();

        new FightHitbox();
        new FightCriticals();
        new Phase();
        new Fly();
        new BlockBreakSpeed();
        new NoFall();
        new ClockSpeed();
        new Speed();
        new FightSynchronized();
        new Inertia();
        new BlockBreakHitbox();
        new WrongBlock();
        new LiquidExit();
        new GroundSpoof();
        new FightSpeed();
        new FightAccuracy();

        for(Check check : checkList) {
            if(check instanceof Listener)
                Bukkit.getPluginManager().registerEvents((Listener)check, hawk);
        }

        hawk.saveConfigs();
    }

    //iterate through all async checks.
    @SuppressWarnings("unchecked")
    public void dispatchEvent(Event e) {
        for(Check check : checkList) {
            if(check instanceof AsyncCheck) {
                AsyncCheck asyncCheck = (AsyncCheck)check;
                if(check instanceof AsyncCustomCheck) {
                    asyncCheck.checkEvent(e);
                }
                else if(check instanceof AsyncMovementCheck && e instanceof PositionEvent) {
                    asyncCheck.checkEvent(e);
                }
                else if(check instanceof AsyncEntityInteractionCheck && e instanceof InteractEntityEvent) {
                    asyncCheck.checkEvent(e);
                }
                else if(check instanceof AsyncBlockDigCheck && e instanceof BlockDigEvent) {
                    asyncCheck.checkEvent(e);
                }
            }
        }
    }

    public void removeData(Player p) {
        for(Check check : checkList)
            check.removeData(p);
    }

    public List<Check> getCheckList() {
        return checkList;
    }

    public ExemptList getExemptList() {
        return exemptList;
    }
}
