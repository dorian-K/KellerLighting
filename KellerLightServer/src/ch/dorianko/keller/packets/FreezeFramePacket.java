package ch.dorianko.keller.packets;

public class FreezeFramePacket extends ArdPacket {

    public FreezeFramePacket() {
        super(0);
    }

    @Override
    protected PacketType getID() {
        return PacketType.FREEZE_FRAME;
    }

    @Override
    protected void encodeContent() {

    }
}
