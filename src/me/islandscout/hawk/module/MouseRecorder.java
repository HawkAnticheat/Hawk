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
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class MouseRecorder {

    private Hawk hawk;
    private static int moves;
    private Pair<Float, Float> origin;
    private List<Pair<Float, Float>> vectors;
    private List<Integer> clicks;
    private final float RESOLUTION = 3F;
    private final int WIDTH;
    private final int HEIGHT;
    private final float CLICK_DOT_RADIUS = 0.7F;

    public MouseRecorder(Hawk hawk) {
        this.hawk = hawk;
        moves = 200;
        vectors = new ArrayList<>();
        clicks = new ArrayList<>();
        WIDTH = (int)(360 * RESOLUTION);
        HEIGHT = (int)(180 * RESOLUTION);
    }

    //to be called from main thread
    public void start(CommandSender admin, Player target) {
        HawkEventListener listener = new MouseRecorderListener(admin, target);
        admin.sendMessage(ChatColor.GOLD + "Recording mouse movements and hits of " + target.getName() + "...");
        hawk.getPacketCore().addHawkEventListener(listener);
    }

    private void render(CommandSender admin, Player target) {
        Bukkit.getScheduler().runTaskAsynchronously(hawk, () -> {
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setBackground(Color.BLACK);

            origin.setKey(MathPlus.clampDegrees360(origin.getKey()));
            origin.setValue(Math.min(Math.max(origin.getValue(), -90), 90) + 90);

            //how are we going to deal with spaghetti neckers or yaw spammers?
            //remember, we're dealing with raw client input; don't trust it.
            //have something in here to prevent this thread from crashing

            renderClicks(g);
            renderMovement(g);

            try {
                File image = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + "recordings" + File.separator + target.getName() + "-" + System.currentTimeMillis() + ".png");
                image.mkdirs();
                if (!image.exists()) {
                    image.createNewFile();
                }
                ImageIO.write(img, "PNG", image);
                admin.sendMessage(ChatColor.GOLD + "Complete! Saved image to " + image.getPath());
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    //TODO: I suggest drawing the starting position w/ a blue dot
    private void renderClicks(Graphics2D g) {
        g.setColor(new Color(0F, 1F, 0F, 0.4F));
        Pair<Float, Float> currCoord = new Pair<>(origin);
        for(int i = 0; i < vectors.size(); i++) {
            float x1 = currCoord.getKey();
            float y1 = currCoord.getValue();
            float x2 = x1 + vectors.get(i).getKey();
            float y2 = y1 + vectors.get(i).getValue();

            if(clicks.contains(i)) {
                g.fillOval((int)((x1 - CLICK_DOT_RADIUS) * RESOLUTION), (int)((y1 - CLICK_DOT_RADIUS) * RESOLUTION), (int)(2* CLICK_DOT_RADIUS *RESOLUTION), (int)(2* CLICK_DOT_RADIUS *RESOLUTION));
            }

            currCoord.setKey(x2);
            currCoord.setValue(y2);

            //handle if line goes off the canvas horizontally
            if(x2 >= 360) {
                currCoord.setKey(x2 % 360);
            }
            else if(x2 < 0) {
                currCoord.setKey((360 + x2) % 360);
            }
        }
    }

    private void renderMovement(Graphics2D g) {
        Pair<Float, Float> currCoord = new Pair<>(origin);
        for(Pair<Float, Float> vector : vectors) {
            float distance = (float)MathPlus.distance2d(vector.getKey(), vector.getValue());
            g.setColor(new Color(1F, 1 / (0.3F * distance + 1), 1 / (0.3F * distance + 1), 1 / (0.2F * distance + 1)));

            float x1 = currCoord.getKey();
            float y1 = currCoord.getValue();
            float x2 = x1 + vector.getKey();
            float y2 = y1 + vector.getValue();
            g.drawLine((int)(x1 * RESOLUTION), (int)(y1 * RESOLUTION), (int)(x2 * RESOLUTION), (int)(y2 * RESOLUTION));

            currCoord.setKey(x2);
            currCoord.setValue(y2);

            //handle if line goes off the canvas horizontally
            if(x2 >= 360) {
                float slope = (y2 - y1) / (x2 - x1); //impossible for a div by 0 because this won't run unless motX > 0
                float intersection = slope * (360 - x1) + y1;
                g.drawLine(0, (int)(intersection * RESOLUTION), (int)((x2 % 360) * RESOLUTION), (int)(y2 * RESOLUTION));
                currCoord.setKey(x2 % 360);
            }
            else if(x2 < 0) {
                float slope = (y2 - y1) / (x2 - x1);
                float intersection = slope * (0 - x1) + y1;
                g.drawLine((int)((360 - 1) * RESOLUTION), (int)(intersection * RESOLUTION), (int)(((360 + x2) % 360) * RESOLUTION), (int)(y2 * RESOLUTION));
                currCoord.setKey((360 + x2) % 360);
            }
        }
    }

    public class MouseRecorderListener implements HawkEventListener {

        private Player target;
        private CommandSender admin;

        MouseRecorderListener(CommandSender admin, Player target) {
            this.target = target;
            this.admin = admin;
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
                        //TODO: Remove from listener list when player disconnects
                        admin.sendMessage(ChatColor.GOLD + "Finished recording. Rendering...");
                        hawk.getPacketCore().removeHawkEventListener(this);
                        render(admin, target);
                    }
                }
                else if(e instanceof InteractEntityEvent && ((InteractEntityEvent) e).getInteractAction() == InteractAction.ATTACK) {
                    clicks.add(vectors.size());
                }
            }
        }
    }
}
