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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.HawkEventListener;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityInteractEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class MouseRecorder {

    private Hawk hawk;
    //private static Map<UUID, MouseRecorder> pending; //probably not necessary
    private static int moves;
    private Pair<Float, Float> origin;
    private List<Pair<Float, Float>> vectors;
    private List<Integer> clicks;

    public MouseRecorder(Hawk hawk) {
        this.hawk = hawk;
        moves = 200;
        //pending = new HashMap<>();
        vectors = new ArrayList<>();
        clicks = new ArrayList<>();
    }

    //to be called from main thread
    public void start(Player target) {
        //pending.put(target.getUniqueId(), this);
        HawkEventListener listener = new MouseRecorderListener(target);
        hawk.getPacketCore().addHawkEventListener(listener);
        Debug.broadcastMessage("RECORD START");
    }

    private void render(Player target) {
        Debug.broadcastMessage("DRAWING MOUSE PATH");
        Bukkit.getScheduler().runTaskAsynchronously(hawk, () -> {
            BufferedImage img = new BufferedImage(360, 180, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setBackground(Color.BLACK);

            origin.setKey(MathPlus.clampDegrees360(origin.getKey()));
            origin.setValue(Math.min(Math.max(origin.getValue(), -90), 90) + 90);

            Pair<Float, Float> currCoord = new Pair<>(origin);

            //how are we going to deal with spaghetti neckers or yaw spammers?
            //remember, we're dealing with raw client input; don't trust it.
            //have something in here to prevent this thread from crashing

            for(Pair<Float, Float> vector : vectors) {
                float distance = (float)MathPlus.distance2d(vector.getKey(), vector.getValue());
                g.setColor(new Color(1F, 1 / (0.3F * distance + 1), 1 / (0.3F * distance + 1), 1 / (0.1F * distance + 1)));

                float x1 = currCoord.getKey();
                float y1 = currCoord.getValue();
                float x2 = x1 + vector.getKey();
                float y2 = y1 + vector.getValue();
                g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);

                currCoord.setKey(x2);
                currCoord.setValue(y2);

                //handle if line goes off the canvas horizontally
                if(x2 >= 360) {
                    float slope = (y2 - y1) / (x2 - x1); //impossible for a div by 0 because this won't run unless motX > 0
                    float intersection = slope * (360 - x1) + y1;
                    g.drawLine(0, (int)intersection, (int)x2 % 360, (int)y2);
                    currCoord.setKey(x2 % 360);
                }
                else if(x2 < 0) {
                    float slope = (y2 - y1) / (x2 - x1);
                    float intersection = slope * (0 - x1) + y1;
                    g.drawLine(360 - 1, (int)intersection, (int)(360 + x2) % 360, (int)y2);
                    currCoord.setKey((360 + x2) % 360);
                }


            }

            try {
                Debug.broadcastMessage("Writing image to disk...");
                File image = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + "recordings" + File.separator + target.getName() + System.currentTimeMillis() + ".png");
                image.mkdirs();
                if (!image.exists()) {
                    image.createNewFile();
                }
                ImageIO.write(img, "PNG", image);
                Debug.broadcastMessage("Complete!");
            }
            catch(Exception e) {
                e.printStackTrace();
                Debug.broadcastMessage("Could not write image.");
            }
        });
    }

    public class MouseRecorderListener implements HawkEventListener {

        private Player target;

        MouseRecorderListener(Player target) {
            this.target = target;
        }

        @Override
        public void onEvent(Event e) {
            if(e.getPlayer().equals(target)) {
                if(e instanceof PositionEvent) {
                    PositionEvent posE = (PositionEvent)e;
                    float deltaYaw = posE.getTo().getYaw() - posE.getFrom().getYaw();
                    float deltaPitch = posE.getTo().getPitch() - posE.getFrom().getPitch();
                    if(vectors.size() < moves) {
                        if (vectors.size() == 0)
                            origin = new Pair<>(posE.getFrom().getYaw(), posE.getFrom().getPitch());
                        vectors.add(new Pair<>(deltaYaw, deltaPitch));
                    }
                    else {
                        //pending.remove(target.getUniqueId());
                        //TODO: Remove from listener list when player disconnects
                        hawk.getPacketCore().removeHawkEventListener(this);
                        render(target);
                    }
                }
                else if(e instanceof InteractEntityEvent) {
                    clicks.add(vectors.size());
                }
            }
        }
    }
}
