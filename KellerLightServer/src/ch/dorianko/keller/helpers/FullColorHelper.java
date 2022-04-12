package ch.dorianko.keller.helpers;

import ch.dorianko.keller.LEDCommunicator;
import ch.dorianko.keller.packets.RawPacket;

import java.awt.*;

public class FullColorHelper extends AbstractHelper {

    private Color myColor;

    public FullColorHelper(LEDCommunicator c, Color startCol) {
        super(c);
        this.myColor = startCol;
    }

    public void setColor(Color c) {
        this.myColor = c;
        System.out.println("Setting color " + c.getRed() + " " + c.getGreen() + " " + c.getBlue());
        this.sendUpdate();
    }

    private void sendUpdate() {
        Color[] c = new Color[1];
        c[0] = this.myColor;
        this.communicator.sendPacket(new RawPacket(c));
    }

    @Override
    public void terminate() {
    }

    ;

    @Override
    public void start() {
        this.sendUpdate();
    }
}
