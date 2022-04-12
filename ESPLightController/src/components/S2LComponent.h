#pragma once
#include "AbstractComponent.h"
#include "StrobeComponent.h"

const int S2L_RESOLUTION = 3;
const int S2L_NUM_WAVE_NODES = NUM_LED * S2L_RESOLUTION;
const int S2L_DOWNSCALE_INTERPOL = 0; // TODO: test this
const bool S2L_LINEAR_MAPPING = false;
const bool S2L_ENABLE_FLASH = false;

const float S2L_WAVE_MOVEPERSECOND = S2L_NUM_WAVE_NODES * 2;

const float S2L_HUE_CIRCLE = 10; // seconds

class S2LComponent : public AbstractComponent{
private:
    uint8_t wave[S2L_NUM_WAVE_NODES];
    float overflow = 0;
    float hueVal = 0;
    int totalNodesReplaced = 0;

    float flashFade = 0;
    float flashMomentum = 0;
    uint8_t hsvSaturation = 255;

    bool forceStrobeActive = false;

    StrobeComponent myStrobe;

public:
    virtual PacketType getMode() {
        return S2L;
    }

    // Take the wave values and downscale them to individual LEDs
    void waveToRgb(float delta)
    {
        this->hueVal += 255 * (delta / S2L_HUE_CIRCLE);
        while (this->hueVal >= 255)
            this->hueVal -= 255;

        for (int i = 0; i < NUM_LED; i++){
            // For every LED `i` figure out the corresponding nodes and luminance
            float lum = 0;
            float sat = this->hsvSaturation;
            // map LED to wave index
            int from, to;
            if(S2L_LINEAR_MAPPING){
                from = max(0, i * S2L_RESOLUTION - S2L_DOWNSCALE_INTERPOL);
                to = min(S2L_NUM_WAVE_NODES, (i + 1) * S2L_RESOLUTION + S2L_DOWNSCALE_INTERPOL);
            }else if(false){
                float t = i / (float)NUM_LED; // 0 - 1
                float a = 2.5f;
                t *= (a * t * t - 1.5f * a * t + 0.5f * a + 1); // apply transformation
                int pos = (int)(t * S2L_NUM_WAVE_NODES);
                from = max(0, pos - S2L_DOWNSCALE_INTERPOL);
                to = min(S2L_NUM_WAVE_NODES, pos + S2L_RESOLUTION + S2L_DOWNSCALE_INTERPOL);
            }else if(false){
                float t = i / (float)NUM_LED; // 0 - 1
                t *= t * (-2 * t + 3); // apply transformation
                int pos = (int)(t * S2L_NUM_WAVE_NODES);
                from = max(0, pos - S2L_DOWNSCALE_INTERPOL);
                to = min(S2L_NUM_WAVE_NODES, pos + S2L_RESOLUTION + S2L_DOWNSCALE_INTERPOL);
            }
            else if(true){
                float t = i / (float)NUM_LED; // 0 - 1
                t = t*t + 0.02f; // apply transformation
                int pos = (int)(t * S2L_NUM_WAVE_NODES);
                from = max(0, pos - S2L_DOWNSCALE_INTERPOL);
                to = min(S2L_NUM_WAVE_NODES, pos + S2L_RESOLUTION + S2L_DOWNSCALE_INTERPOL);
            }
            
            // Add luminance of wave
            for (int s = from; s <= to; s++)
                lum += this->wave[s];
            lum /= max(0, to - from) + 1;
            if (flashFade > 0){
                lum = max(lum, flashFade);
                sat = 255 - flashFade;
            }   
            
            // human eye correction: humans see light logarithmically
            lum = (lum * lum) / 255;
            lum = constrain(lum, 0, 255.f);
            
            if (lum < 20)
                lum = 0;

            CHSV hsv((uint8_t)this->hueVal, (uint8_t)sat, (uint8_t)lum);
            CRGB colOut;
            
            hsv2rgb_rainbow(hsv, colOut);
            setBoth(i, colOut);
        }
    }

    int continueWave(bool fillRemainder, float delta)
    {
        float numMoveFloat = delta * S2L_WAVE_MOVEPERSECOND + this->overflow;
        int numMoveDiscrete = (int)numMoveFloat;
        if (numMoveDiscrete > S2L_NUM_WAVE_NODES / 2)
        {
            numMoveDiscrete = 0; // this should not happen
            numMoveFloat = 0;
            memset(wave, 0, S2L_NUM_WAVE_NODES);
        }
        this->overflow = numMoveFloat - numMoveDiscrete;

        // Move wave by numMoveDiscrete steps
        for (int i = S2L_NUM_WAVE_NODES - 1; i >= numMoveDiscrete; i--)
            this->wave[i] = this->wave[i - numMoveDiscrete];

        // The technically correct term would be velocity
        float momentum = (wave[numMoveDiscrete] - wave[numMoveDiscrete + 1]);
        if(momentum > 0.f)
            momentum = 0.f;

        // Fill up the remaining part of the wave by pretending the value falls back to 0
        for (int i = numMoveDiscrete - 1; i >= 0; i--)
        {
            if (fillRemainder == false)
            {
                wave[i] = wave[i + 1];
                continue;
            }
            momentum -= 2.5f;
            
            wave[i] = (uint8_t)constrain((float)wave[i + 1] + momentum, 0.f, 255.f);
        }

        return numMoveDiscrete;
    }

    void continueFlash(float delta)
    {
        if (flashFade > 0)
        {
            flashMomentum -= 1700 * delta;
            flashFade = max(0.f, flashFade + flashMomentum * delta);
        }
        else
            flashMomentum = 0;
    }

    virtual void start(unsigned char *networkData, int pkLen)
    {
        float delta = max(0uL, micros() - lastUpdate) / 1000000.f;
        if(delta > 1)
            delta = 1;

        // Fill with momentum
        int numReplaced = continueWave(true, delta) + constrain(this->totalNodesReplaced, 0, S2L_NUM_WAVE_NODES);
        this->totalNodesReplaced = 0;

        double total = 0;
        int num = 0;
        unsigned char flags = networkData[0];

        for (int i = 1; i < (pkLen - 1) / 2 /*take only lower quarter*/; i++)
        {
            unsigned char uval = networkData[i + 1];
            float val = uval / 25.5f;
            val = constrain(val, 0, 10);
            total += val;
            num++;
        }
        total /= num;
        bool forceHit = (flags & 1) != 0;
        bool hasTriggeredFlash = false;
        if(forceHit){
            hasTriggeredFlash = true;
            this->flashMomentum = 0;
        }

        this->forceStrobeActive = (flags & (1 << 1)) != 0;
        if (total > 3.5f)
        {
            if (total > 9.3f && this->flashFade < 30 && S2L_ENABLE_FLASH)
            {
                this->flashMomentum = min(flashMomentum, 0.f);
                hasTriggeredFlash = true;
            }
           
            total = map(constrain(total, 4.5f, 6.f), 4.5f, 6.f, 8.5f, 10.f);
            for (int i = 0; i < constrain(numReplaced, S2L_RESOLUTION, S2L_NUM_WAVE_NODES); i++)
                this->wave[i] = max(this->wave[i], (uint8_t)(total * 25.4f));
        }

        if(hasTriggeredFlash){
            this->hueVal += 10;
            this->flashFade = 255;
        }

        continueFlash(delta);

        if (this->forceStrobeActive)
            this->myStrobe.loop();
        else
            waveToRgb(delta);

        this->updateLastUpdate();
    }
    virtual void loop()
    {
        float delta = max(0uL, micros() - lastUpdate) / 1000000.f;
        if(delta > 1)
            delta = 1;
        this->totalNodesReplaced += continueWave(false, delta); // Do lazy fill without momentum
        continueFlash(delta);

        if(this->forceStrobeActive)
            this->myStrobe.loop();
        else
            waveToRgb(delta);

        this->updateLastUpdate();
    }
};