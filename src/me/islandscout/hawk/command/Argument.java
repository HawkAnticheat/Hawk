package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

abstract class Argument implements Comparable<Argument> {
    private final String name;
    private final String description;
    private final String syntax;
    static Hawk hawk;

    Argument(String name, String syntax, String description) {
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

    public abstract boolean process(CommandSender sender, Command cmd, String label, String[] args);

    @Override
    public int compareTo(Argument other) {
        return name.compareTo(other.name);
    }
}
