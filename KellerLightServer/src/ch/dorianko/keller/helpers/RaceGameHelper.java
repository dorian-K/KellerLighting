package ch.dorianko.keller.helpers;

import ch.dorianko.keller.packets.RawPacket;
import ch.dorianko.keller.LEDCommunicator;
import ch.dorianko.keller.Util;
import org.json.JSONObject;

import java.awt.*;

public class RaceGameHelper extends AbstractHelper {

    public RaceGameHelper(LEDCommunicator c) {
        super(c);
    }

    public void processGameData(JSONObject obj) {
        float maxRpm = obj.optFloat("max_rpm");
        float rpm = obj.optFloat("rpm");
        if (maxRpm < rpm || rpm <= 0)
            return;

        int numLed = 121 ; // 121
        float startRpm = maxRpm * 0.66f;
        int startYellow = Math.round(Util.map(maxRpm - 1500, startRpm, maxRpm, 0, numLed));

        int startRed = Math.round(Util.map(maxRpm - 500, startRpm, maxRpm, 0, numLed));

        var target = Util.map(rpm, startRpm, maxRpm, 0, numLed);

        Color[] c = new Color[numLed];
        boolean nowBlink = System.currentTimeMillis() % 100 < 50;
        for (int i = 0; i < c.length; i++) {
            if (i >= (int) target + 1) {
                c[i] = Color.BLACK;
                continue;
            }
            if (i > startRed)
                c[i] = nowBlink ? Color.BLACK : Color.RED;
            else if (i > startYellow)
                c[i] = Color.YELLOW;
            else
                c[i] = Color.BLUE;

            if (i > 0 && i == (int) target && target > i) {
                float scale = target - i;
                float[] hsv = new float[3];
                Color.RGBtoHSB(c[i].getRed(), c[i].getGreen(), c[i].getBlue(), hsv);
                hsv[2] *= scale * scale;
                c[i] = new Color(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
            }
        }

        //var pk = new RawPacket(c);
        var pk = RawPacket.compress(c);
        this.communicator.sendPacket(pk);
    }


    @Override
    public void terminate() {

    }

    @Override
    public void start() {

    }
}
