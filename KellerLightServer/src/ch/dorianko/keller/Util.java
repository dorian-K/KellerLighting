package ch.dorianko.keller;

public class Util {

    public static double clip(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clip(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clip(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float mapClip(float value, float rangeStart, float rangeEnd, float newRangeStart, float newRangeEnd) {
        return map(Util.clip(value, rangeStart, rangeEnd), rangeStart, rangeEnd, newRangeStart, newRangeEnd);
    }

    public static float map(float value, float rangeStart, float rangeEnd, float newRangeStart, float newRangeEnd) {
        value -= rangeStart;
        value /= rangeEnd - rangeStart;
        value *= newRangeEnd - newRangeStart;
        value += newRangeStart;
        return value;
    }
}
