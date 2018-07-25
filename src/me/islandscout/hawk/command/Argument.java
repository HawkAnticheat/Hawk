package me.islandscout.hawk.command;

public abstract class Argument {

    private String name;
    private String description;

    public Argument(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
