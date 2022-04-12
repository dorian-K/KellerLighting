package ch.dorianko.keller.packets;

public class SetBrightnessPacket extends ArdPacket {

    private final byte brightness;

    public SetBrightnessPacket(byte brightness) {
        super(1);
        this.brightness = brightness;
    }

    @Override
    protected PacketType getID() {
        return PacketType.SET_BRIGHTNESS;
    }

    @Override
    protected void encodeContent() {
        this.contentBuffer.put(this.brightness);
    }
}
