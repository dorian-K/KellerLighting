package ch.dorianko.keller.helpers;

import ch.dorianko.keller.LEDCommunicator;
import ch.dorianko.keller.packets.StrobePacket;

public class StrobeHelper extends AbstractHelper {

    public StrobeHelper(LEDCommunicator c) {
        super(c);
    }

    @Override
    public void terminate() {
    }

    ;

    @Override
    public void start() {
        this.communicator.sendPacket(new StrobePacket());
    }
}
