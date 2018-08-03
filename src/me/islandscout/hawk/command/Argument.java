package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public abstract class Argument {
    private final String name;
    private final String description;
    private final String syntax;
    protected static Hawk hawk;

    public Argument(String name, String syntax, String description) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return name + (syntax.length() == 0 ? "" : " " + syntax);
    }

    static void setHawkReference(Hawk plugin) {
        hawk = plugin;
    }

    public abstract void process(CommandSender sender, Command cmd, String label, String[] args);
}
