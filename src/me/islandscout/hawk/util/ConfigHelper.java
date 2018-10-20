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

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigHelper {

    /**
     * Will return object from a config with specified path. If the object does not exist in
     * the config, it will add it into the config and return the default object.
     *
     * @param defaultValue default object
     * @param config       FileConfiguration instance
     * @param path         path to object
     */

    //this method is probably the only necessary method in this util
    public static Object getOrSetDefault(Object defaultValue, FileConfiguration config, String path) {
        Object result;
        if (config.isSet(path)) {
            result = config.get(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static List<String> getOrSetDefault(List<String> defaultValue, FileConfiguration config, String path) {
        List<String> result;
        if (config.isSet(path)) {
            result = config.getStringList(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static boolean getOrSetDefault(boolean defaultValue, FileConfiguration config, String path) {
        boolean result;
        if (config.isSet(path)) {
            result = config.getBoolean(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static int getOrSetDefault(int defaultValue, FileConfiguration config, String path) {
        int result;
        if (config.isSet(path)) {
            result = config.getInt(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static long getOrSetDefault(long defaultValue, FileConfiguration config, String path) {
        long result;
        if (config.isSet(path)) {
            result = config.getInt(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static double getOrSetDefault(double defaultValue, FileConfiguration config, String path) {
        double result;
        if (config.isSet(path)) {
            result = config.getDouble(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }

    public static String getOrSetDefault(String defaultValue, FileConfiguration config, String path) {
        String result;
        if (config.isSet(path)) {
            result = config.getString(path);
        } else {
            result = defaultValue;
            config.set(path, defaultValue);
        }
        return result;
    }
}
