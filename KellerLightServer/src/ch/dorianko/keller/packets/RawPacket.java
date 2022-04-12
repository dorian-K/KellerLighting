package ch.dorianko.keller.packets;

import net.jpountz.lz4.LZ4Factory;

import java.awt.*;

public class RawPacket extends ArdPacket {

    private final byte[] colorData;
    private boolean isCompressed = false;

    public RawPacket(Color[] arr) {
        super(arr.length * 3 + 1);
        this.colorData = new byte[arr.length * 3];
        for (int i = 0; i < arr.length; i++) {
            var c = arr[i];
            this.colorData[i * 3] = (byte) c.getRed();
            this.colorData[i * 3 + 1] = (byte) c.getGreen();
            this.colorData[i * 3 + 2] = (byte) c.getBlue();
        }
    }

    public RawPacket(byte[] data, boolean isCompressed) {
        super(data.length + 1);
        this.colorData = new byte[data.length];
        System.arraycopy(data, 0, this.colorData, 0, data.length);
        this.isCompressed = isCompressed;
    }

    public static RawPacket compress(Color[] arr){
        // convert to bytes
        byte[] colorData = new byte[arr.length * 3];
        for (int i = 0; i < arr.length; i++) {
            var c = arr[i];
            colorData[i] = (byte) c.getRed();
            colorData[i + arr.length] = (byte) c.getGreen();
            colorData[i + arr.length * 2] = (byte) c.getBlue();
        }
        // compress
        var comp = LZ4Factory.fastestInstance().fastCompressor().compress(colorData);
        //System.out.println("Compressed "+colorData.length+" to "+comp.length);
        return new RawPacket(comp, true);
    }

    @Override
    protected PacketType getID() {
        return PacketType.RAW;
    }

    @Override
    protected void encodeContent() {
        byte flags = 0;
        if(this.isCompressed)
            flags |= 1 << 1;
        this.contentBuffer.put(flags); // flags
        this.contentBuffer.put(colorData);
    }
}
