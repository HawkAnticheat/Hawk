/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.ArmSwingEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractAction;
import me.islandscout.hawk.event.InteractEntityEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightNoSwing extends CustomCheck {

    //PASSED (9/11/18)

    private final Map<UUID, Long> lastClientTickSwung;

    public FightNoSwing() {
        super("fightnoswing", "%player% failed noswing. VL: %vl%");
        lastClientTickSwung = new HashMap<>();
    }

    @Override
    protected void check(Event event) {
        if (event instanceof ArmSwingEvent)
            processSwing((ArmSwingEvent) event);
        else if (event instanceof InteractEntityEvent)
            processHit((InteractEntityEvent) event);

    }

    private void processSwing(ArmSwingEvent e) {
        lastClientTickSwung.put(e.getPlayer().getUniqueId(), e.getHawkPlayer().getCurrentTick());
    }

    private void processHit(InteractEntityEvent e) {
        if (e.getInteractAction() != InteractAction.ATTACK)
            return;
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if (!lastClientTickSwung.containsKey(p.getUniqueId()) || pp.getCurrentTick() != lastClientTickSwung.get(p.getUniqueId())) {
            punish(pp, true, e);
        } else {
            reward(pp);
        }
    }

    @Override
    public void removeData(Player p) {
        lastClientTickSwung.remove(p.getUniqueId());
    }
}
