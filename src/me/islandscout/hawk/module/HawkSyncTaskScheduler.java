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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

public class HawkSyncTaskScheduler implements Runnable {

    private long currentTick;
    private final Hawk hawk;
    private List<HawkTask> tasks;
    private static int hawkTaskInstances;

    public HawkSyncTaskScheduler(Hawk hawk) {
        this.hawk = hawk;
        this.tasks = new ArrayList<>();
    }

    @Override
    public void run() {
        for (HawkTask hawkTask : tasks) {
            if (currentTick % hawkTask.interval == 0) {
                hawkTask.task.run();
            }
        }
        currentTick++;
    }

    //Don't harass me for reinventing the wheel; I'm trying to make my life easier.
    public int addRepeatingTask(Runnable task, int interval) {
        HawkTask hawkTask = new HawkTask(task, interval);
        tasks.add(hawkTask);
        return hawkTask.id;
    }

    public void cancelTask(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            HawkTask task = tasks.get(i);
            if (id == task.id) {
                tasks.remove(i);
                break;
            }
        }
    }

    private class HawkTask {

        private Runnable task;
        private int interval;
        private int id;

        private HawkTask(Runnable task, int interval) {
            this.task = task;
            this.interval = interval;
            this.id = hawkTaskInstances;
            hawkTaskInstances++;
        }
    }
}
