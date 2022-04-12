package ch.dorianko.keller.packets;

public class StrobePacket extends ArdPacket {

    public StrobePacket() {
        super(0);
    }

    @Override
    protected PacketType getID() {
        return PacketType.STROBE;
    }

    @Override
    protected void encodeContent() {

    }
}
