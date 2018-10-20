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

package me.islandscout.hawk.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * This class is used to prepare and send JSON messages for 1.8 Spigot servers.
 */
public class JSONMessageSender {

    private final TextComponent msg;

    public JSONMessageSender(String message) {
        this.msg = new TextComponent(message);
    }

    public void sendMessage(Player p) {
        p.spigot().sendMessage(msg);
    }

    public void setClickCommand(String cmd) {
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + cmd));
    }

    public void setHoverMsg(String msg) {
        this.msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(msg).create()));
    }
}
