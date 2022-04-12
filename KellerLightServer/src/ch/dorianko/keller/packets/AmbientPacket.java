package ch.dorianko.keller.packets;

public class AmbientPacket extends ArdPacket {

    public AmbientPacket() {
        super(0);
    }

    @Override
    protected PacketType getID() {
        return PacketType.AMBIENT;
    }

    @Override
    protected void encodeContent() {

    }
}
