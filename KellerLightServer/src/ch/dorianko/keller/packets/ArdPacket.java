package ch.dorianko.keller.packets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;

public abstract class ArdPacket {

    enum PacketType {
        INVALID,
        LEGACY,
        S2L,
        AMBIENT,
        STROBE,
        RAW,
        SET_BRIGHTNESS,
        FREEZE_FRAME
    }

    private ByteBuffer bb;
    protected ByteBuffer contentBuffer;
    private boolean hasInsertedContent = false;

    public ArdPacket(int contentSize) {
        this.bb = ByteBuffer.allocate(contentSize + 12);
        this.contentBuffer = ByteBuffer.allocate(contentSize);
    }

    public void reset() {
        //this.bb.clear();
        this.bb.order(ByteOrder.LITTLE_ENDIAN);
        //this.contentBuffer.clear();
        this.contentBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.hasInsertedContent = false;
    }

    protected void header() {
        bb.putInt(0x57a3ac4a);
    }

    public void insertContent() {
        this.bb.putShort((short) this.contentBuffer.position());
        this.bb.put(((byte) this.getID().ordinal()));
        var flipped = this.contentBuffer.flip();
        var a = new Adler32();
        a.update(flipped.array(), flipped.arrayOffset(), flipped.remaining());
        var ch = a.getValue();
        this.bb.put((byte) (ch & 0xFF)).put((byte) ((ch >> 8) & 0xFF)).put((byte) ((ch >> 16) & 0xFF)).put((byte) ((ch >> 24) & 0xFF));
        this.bb.put(flipped);
        this.hasInsertedContent = true;
    }

    public byte[] data() {
        if (!this.hasInsertedContent)
            throw new IllegalStateException("has not inserted content buffer");
        return bb.array();
    }

    public void encode() {
        this.reset();
        this.header();
        this.encodeContent();
        this.insertContent();
    }

    protected abstract PacketType getID();

    protected abstract void encodeContent();

}
