#pragma once
#include "commons.h"

class AbstractComponent {
protected:
    unsigned long lastUpdate = 0;

    void updateLastUpdate(){
        this->lastUpdate = micros();
    }
public:
    virtual void start(unsigned char* networkData, int pkLen) = 0;
    virtual void loop() = 0;
    virtual PacketType getMode() = 0;

    virtual ~AbstractComponent() = default;
};