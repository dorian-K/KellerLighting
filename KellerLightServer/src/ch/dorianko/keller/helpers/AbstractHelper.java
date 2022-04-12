package ch.dorianko.keller.helpers;

import ch.dorianko.keller.LEDCommunicator;

public abstract class AbstractHelper {

    protected final LEDCommunicator communicator;

    public AbstractHelper(LEDCommunicator c) {
        this.communicator = c;
    }

    public abstract void terminate();

    public abstract void start();
}
