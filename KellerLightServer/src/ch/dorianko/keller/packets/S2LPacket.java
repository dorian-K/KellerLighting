package ch.dorianko.keller.packets;

import ch.dorianko.keller.Util;

public class S2LPacket extends ArdPacket {

    private final byte[] values;
    private byte flags;

    public static int FORCE_HIT = 1;
    public static int FORCE_STROBE = 1 << 1;

    public S2LPacket(float[] vals, byte flags) {
        super(vals.length + 1);
        this.values = new byte[vals.length];
        for (int i = 0; i < vals.length; i++) {
            var v = vals[i];
            float normed = Util.clip(v, 0, 10);
            this.values[i] = (byte) Math.min(255, (normed * 25.5f));
        }
        this.flags = flags;
    }

    @Override
    protected PacketType getID() {
        return PacketType.S2L;
    }

    @Override
    protected void encodeContent() {
        this.contentBuffer.put(flags);
        this.contentBuffer.put(this.values);
    }
}
