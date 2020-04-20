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

    private final File file;
    private BufferedReader buffer;

    public TextFileReader(Hawk hawk, String filename) {
        this.file = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + filename);
    }

    public void load() throws IOException {
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        }
        buffer = new BufferedReader(new FileReader(file));
    }

    public String readLine() throws IOException {
        return buffer.readLine();
    }

    public List<String> read() throws IOException {
        List<String> result = new ArrayList<>();
        String line = readLine();
        while (line != null) {
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

    public void overwrite(List<String> data) throws IOException {
        FileWriter fw = new FileWriter(file, false);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        for (String line : data) {
            out.println(line);
        }
        out.close();
    }
}
