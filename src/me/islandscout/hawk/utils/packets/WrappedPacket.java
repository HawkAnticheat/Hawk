package me.islandscout.hawk.utils.packets;

public abstract class WrappedPacket {

    //I should probably make a nice API for this. Unfortunately, I don't have THAT much
    //time on my hands to spare.
    protected Object packet;
    private PacketType type;

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
    }
}
