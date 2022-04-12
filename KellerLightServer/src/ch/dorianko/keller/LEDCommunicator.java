package ch.dorianko.keller;

import ch.dorianko.keller.packets.ArdPacket;
import ch.dorianko.keller.packets.SetBrightnessPacket;
import com.fazecast.jSerialComm.SerialPort;
import ch.dorianko.keller.helpers.AbstractHelper;

import java.util.Optional;
import java.util.function.Supplier;

public class LEDCommunicator {

    private int ledBrightness = 200;
    private SerialPort port;
    private Supplier<SerialPort> portSupplier;
    private Optional<AbstractHelper> currentHelper = Optional.empty();
    private int numBlockingPackets = 0;

    public void supplyPort(Supplier<SerialPort> portSupplier) {
        this.port = portSupplier.get();
        this.portSupplier = portSupplier;
    }

    private SerialPort getPort() {
        if (this.port != null && this.port.isOpen())
            return this.port;

        this.port = this.portSupplier.get();
        while (!this.port.isOpen()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            this.port = this.portSupplier.get();
        }

        return this.port;
    }

    public int getLedBrightness(){
        return this.ledBrightness;
    }

    public void setBrightness(int brightness) {
        if (this.ledBrightness == brightness)
            return;
        this.ledBrightness = brightness;
        this.sendPacket(new SetBrightnessPacket((byte)Util.clip(this.ledBrightness, 0, 255)));
    }

    public synchronized void sendPacket(ArdPacket p) {
        p.encode();
        var data = p.data();
        //System.out.println(MathUtils.printHexBinary(data));
        if(this.getPort().bytesAwaitingWrite() > 0){
            if(this.numBlockingPackets > 5) {
                System.out.println("The last 5 packets have been blocking, throwing away current one... "+this.getPort().bytesAwaitingWrite());
                return;
            }

            this.numBlockingPackets++;
        }else if(this.numBlockingPackets > 0)
            this.numBlockingPackets--;

        this.getPort().writeBytes(data, data.length);
    }

    public synchronized void setHelper(AbstractHelper newHelper) {
        this.setHelper(Optional.of(newHelper));
    }

    public synchronized void setHelper(Optional<AbstractHelper> newHelper) {
        this.currentHelper.ifPresent(AbstractHelper::terminate);
        this.currentHelper = newHelper;
        this.currentHelper.ifPresent(AbstractHelper::start);
    }

    public Optional<AbstractHelper> getHelper() {
        return this.currentHelper;
    }
}
