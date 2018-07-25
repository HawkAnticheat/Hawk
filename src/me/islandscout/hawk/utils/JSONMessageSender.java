package me.islandscout.hawk.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * This class is used to prepare and send JSON messages for 1.8 Spigot servers.
 */
public class JSONMessageSender {

    private TextComponent msg;

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
