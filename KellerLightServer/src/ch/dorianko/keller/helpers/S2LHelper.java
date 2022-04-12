package ch.dorianko.keller.helpers;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.util.fft.FFT;
import ch.dorianko.keller.LEDCommunicator;
import ch.dorianko.keller.Util;
import ch.dorianko.keller.packets.S2LPacket;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class S2LHelper extends AbstractHelper {

    // FFT Parameters
    private final float sampleRate = 44100;
    private final int bufferSize = 512 * 4;
    private final int overlap = bufferSize / 2;
    // Smoothing of output values
    private final static int stdDevAverage = 3;
    private final static int totalAverage = 2;

    private AudioDispatcher dispatcher = null;
    private Thread dispatcherThread = null;

    public boolean doForceStrobe = false;
    public boolean doForceHit = false;
    private int dontSendTimer = 0;

    private final ArrayBlockingQueue<Float[]> history = new ArrayBlockingQueue<>((int) ((sampleRate / (bufferSize - overlap)) * 2.5f) /*3.5 seconds*/);

    public S2LHelper(LEDCommunicator c) {
        super(c);
    }

    AudioProcessor fftProcessor = new AudioProcessor() {

        final FFT fft = new FFT(bufferSize);
        final float[] amplitudes = new float[bufferSize / 2];

        @Override
        public void processingFinished() {}

        @Override
        public boolean process(AudioEvent audioEvent) {
            float[] audioFloatBuffer = audioEvent.getFloatBuffer();
            float[] transformbuffer = new float[bufferSize * 2];
            System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
            fft.forwardTransform(transformbuffer);
            fft.modulus(transformbuffer, amplitudes);

            int numPots = 30;
            float[] freqPots = new float[numPots];
            int[] numPot = new int[numPots];

            for (int i = 0; i < amplitudes.length; i++) {
                double freq = fft.binToHz(i, sampleRate);
                if (freq >= 1000)
                    break;

                int ind = Math.round(Util.mapClip((float) freq, 0, 500f, 0, numPots - 1));

                freqPots[ind] += amplitudes[i];
                numPot[ind]++;
            }
            for(int i = 0; i < numPots; i++){
                freqPots[i] /= numPot[i];
                if (Float.isNaN(freqPots[i]))
                    freqPots[i] = 0; // numPot[i] is 0?
            }

            Float[] historyEntry = new Float[freqPots.length];
            Float[] historyMean = new Float[freqPots.length];
            Float[] historyDev = new Float[freqPots.length];
            if (history.size() > 2) {
                Arrays.fill(historyMean, 0f);
                Arrays.fill(historyDev, 0f);

                // Average all historic values, take the mean
                history.forEach(ent -> { // For every history entry
                    for (int i = 0; i < historyMean.length; i++)
                        historyMean[i] += ent[i]; // For every bin
                });
                for (int i = 0; i < historyMean.length; i++)
                    historyMean[i] /= history.size();

                // Now figure out the standard deviation over all historic values
                history.forEach(ent -> {
                    for (int i = 0; i < historyDev.length; i++) {
                        float error = ent[i] - historyMean[i];
                        historyDev[i] += error * error;
                    }
                });
                for (int i = 0; i < historyDev.length; i++)
                    historyDev[i] = (float) Math.sqrt(historyDev[i] / history.size());

                // nearest-neighbour interpolation, average nearby deviations
                if (stdDevAverage > 0) {
                    Float[] oldHistoryDev = new Float[historyDev.length];
                    System.arraycopy(historyDev, 0, oldHistoryDev, 0, historyDev.length);

                    for (int i = 0; i < historyDev.length; i++) {
                        historyDev[i] = 0f;
                        int total = 0;
                        for (int off = i - stdDevAverage; off <= i + stdDevAverage; off++) {
                            total++; 
                            historyDev[i] += oldHistoryDev[Util.clip(off, 0, oldHistoryDev.length - 1)];
                        }
                        historyDev[i] /= total;
                    }
                }
            }

            float[] endValues = new float[freqPots.length];
            for (int i = 0; i < freqPots.length; i++) {
                float endVal = freqPots[i];
                historyEntry[i] = endVal;

                if (history.size() > 2) {
                    // MathUtils.remapClip(i, freqPots.length / 4, freqPots.length / 2, 1f, 1.5f)
                    endVal = (endVal - historyMean[i]) / Math.max(0.1f, historyDev[i] * 0.8f);
                    endVal = Math.max(0, endVal * 5);
                }

                endValues[i] = endVal;
            }

            // nearest-neighbour interpolation, averages neighbouring bins together, weighted by the sqrt of their distance
            if (totalAverage > 0) {
                float[] oldEndValues = new float[endValues.length];
                System.arraycopy(endValues, 0, oldEndValues, 0, endValues.length);

                for (int i = 0; i < endValues.length; i++) {
                    endValues[i] = 0f;
                    float total = 0;
                    for (int off = i - totalAverage; off <= i + totalAverage; off++) {
                        float mul = (float) Math.sqrt((totalAverage + 1) - (Math.abs(i - off)));
                        total += mul;

                        endValues[i] += oldEndValues[Util.clip(off, 0, oldEndValues.length - 1)] * mul;
                    }
                    endValues[i] /= total;
                }
            }

            if (history.remainingCapacity() == 0)
                history.remove();
            history.add(historyEntry);

            var pk = new S2LPacket(endValues, S2LHelper.this.makeFlags());
            communicator.sendPacket(pk);

            return true;
        }
    };

    @Override
    public void terminate() {
        if (dispatcher == null)
            return;

        dispatcher.stop();
        if (this.dispatcherThread == null)
            return;

        try {
            this.dispatcherThread.join(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte makeFlags(){
        byte flags = 0;
        if(this.doForceHit){
            this.doForceHit = false;
            flags |= S2LPacket.FORCE_HIT;
        }
        if(this.doForceStrobe){
            flags |= S2LPacket.FORCE_STROBE;
        }
        return flags;
    }

    @Override
    public void start() {
        try {
            this.setAndStartMixer(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void setAndStartMixer(Mixer mixer) throws LineUnavailableException {
        this.terminate();

        final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line;
        if (mixer == null)
            line = AudioSystem.getTargetDataLine(format);
        else
            line = (TargetDataLine) mixer.getLine(dataLineInfo);
        line.open(format, bufferSize);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
        // create a new dispatcher
        dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
        dispatcher.addAudioProcessor(fftProcessor);
        this.dispatcherThread = new Thread(dispatcher, "Audio dispatching");
        this.dispatcherThread.start();
    }

}
