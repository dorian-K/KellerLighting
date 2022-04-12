package ch.dorianko.keller.helpers;

import ch.dorianko.keller.LEDCommunicator;
import ch.dorianko.keller.packets.AmbientPacket;

public class AmbientHelper extends AbstractHelper {

    public AmbientHelper(LEDCommunicator c) {
        super(c);
    }

    @Override
    public void terminate() {
    }

    ;

    @Override
    public void start() {
        this.communicator.sendPacket(new AmbientPacket());
    }
}
