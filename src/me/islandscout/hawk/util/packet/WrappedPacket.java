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

package me.islandscout.hawk.util.packet;

public abstract class WrappedPacket {

    //I should probably make a nice API for this. Unfortunately, I don't have THAT much
    //time on my hands to spare.
    protected final Object packet;
    private final PacketType type;

    WrappedPacket(Object packet, PacketType type) {
        this.packet = packet;
        this.type = type;
    }

    public abstract Object getPacket();

    public PacketType getType() {
        return type;
    }

    public abstract void setByte(int index, int value);

    public abstract byte[] getBytes();

    public abstract Object readPacket();

    public abstract void overwritePacket(Object packetDataSerializer);

    public enum PacketType {
        FLYING,
        POSITION,
        LOOK,
        POSITION_LOOK,
        USE_ENTITY,
        BLOCK_DIG,
        CUSTOM_PAYLOAD,
        ABILITIES,
        BLOCK_PLACE,
        ARM_ANIMATION,
        HELD_ITEM_SLOT,
        ENTITY_META_DATA,
        ENTITY_ACTION
    }
}
