#pragma once
#include <Arduino.h>
#include <FastLED.h>
#include "AbstractComponent.h"
#include "commons.h"

const int hueCircleAmbient = 30000;

class AmbientComponent : public AbstractComponent {
private:
    float hueVal = 0;
public:
    virtual PacketType getMode() {
        return AMBIENT;
    }
    virtual void start(unsigned char* networkData, int pkLen){
        this->loop();
    }
    virtual void loop(){
        float delta = max(0uL, micros() - lastUpdate) / 1000000.f;
        if(delta > 0.2f)
            delta = 0.2f;

        float hueUpdate = 255 * (delta / (hueCircleAmbient / 1000.f));
        this->hueVal += hueUpdate;
        while (this->hueVal >= 255)
            this->hueVal -= 255;

        CHSV hsv1((uint8_t)this->hueVal, 255, 255);
        CHSV hsv2((uint8_t)this->hueVal + 1, 255, 255);
        CRGB col1, col2, colOut;
        hsv2rgb_rainbow(hsv1, col1);
        hsv2rgb_rainbow(hsv2, col2);
        colOut = col1.lerp16(col2, (this->hueVal - (int)this->hueVal) * 65535);
        for (int i = 0; i < NUM_LED; i++)
            setBoth(i, colOut);

        this->updateLastUpdate();
    }
};