/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.util;

import me.islandscout.hawk.Hawk;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TextFileReader {

    private final Hawk hawk;
    private final File file;
    private final String filename;
    private BufferedReader buffer;

    public TextFileReader(Hawk hawk, String filename) {
        this.hawk = hawk;
        this.file = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + filename);
        this.filename = filename;
    }

    public void load() {
        if (!file.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                hawk.getLogger().severe("Failed to create " + filename);
                e.printStackTrace();
                return;
            }
        }
        try {
            buffer = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            hawk.getLogger().severe("Failed to locate " + filename);
            e.printStackTrace();
        }
    }

    public String readLine() {
        try {
            return buffer.readLine();
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to read " + filename + ". Is its BufferedReader initialized?");
            e.printStackTrace();
            return null;
        }
    }

    public List<String> read() {
        List<String> result = new ArrayList<>();
        String line = readLine();
        while(line != null) {
            result.add(line);
            line = readLine();
        }
        return result;
    }

    public void reset() {
        try {
            buffer.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void overwrite(List<String> data) {
        try (FileWriter fw = new FileWriter(file, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw))
        {
            for (String line : data) {
                out.println(line);
            }
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to write to " + filename);
            e.printStackTrace();
        }
    }
}
