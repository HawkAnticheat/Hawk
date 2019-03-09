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

package me.islandscout.hawk.check.movement;

public class BlockGlitch {

    //Just rubberband to the location before touching the cancelled client block

    //Look, if you're going to rubberband to a location like that, you should make a priority system. For example, if
    //you jump on a phantomblock that gets cancelled, and then X moves ahead if you get flagged for speed AND fly (for failing phantomblock)
    //on the same move, and speed is before fly in the process list, you'll get rubberbanded to the speed legit location and not the fly legit
    //location; a fly bypass. The priority system should give priority to older setback locations if a conflict like this
    //should occur.
}
