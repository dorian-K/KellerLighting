package ch.dorianko.keller;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Locale;

public class MainClass {

    public static LEDCommunicator led;

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        led = new LEDCommunicator();
        led.supplyPort(() -> {
            var port = SerialPort.getCommPort("/dev/ttyUSB0");
            port.setBaudRate(921600);
            port.openPort();
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 500, 0);
            System.out.println("Port supplied isOpen=" + port.isOpen());
            return port;
        });
        System.out.println("Communicator initialized");

        new HttpApi().start();
        new WSApi(8081).start();
    }


}
