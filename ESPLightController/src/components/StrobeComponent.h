#pragma once
#include "AbstractComponent.h"

class StrobeComponent : public AbstractComponent {
private:
    bool strobeState = false;
public:
    virtual PacketType getMode() {
        return STROBE;
    }
    virtual void start(unsigned char* networkData, int pkLen){
        this->loop();
    }
    virtual void loop() {
        float delta = max(0uL, micros() - lastUpdate) / 1000000.f;

        if (strobeState && delta > 0.01f){
            this->updateLastUpdate();
            strobeState = false;
        }else if (!strobeState && delta > 0.06f){
            this->updateLastUpdate();
            strobeState = true;
        }
        
        setAll(strobeState ? CRGB(255, 255, 255) : CRGB(1u, 1u, 1u));
    }
};
