package me.islandscout.hawk.utils;

public class Placeholder {

    private final String key;
    private final Object value;

    public Placeholder(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
