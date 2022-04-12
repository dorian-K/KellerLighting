#pragma once

#include "AbstractComponent.h"
#include <lz4.h>

class RawColorComponent : public AbstractComponent {
public:
    virtual PacketType getMode() {
        return RAW;
    }
    virtual void start(unsigned char* networkData, int pkLen){
        // [byte flags][Array Color]
        if (pkLen < 4)
            return;
        unsigned char flags = networkData[0];
        //bool isMirror = (flags & 1) == 0;
        // isMirror is unused, right now all leds are mirrored
        bool isCompressed = (flags & (1 << 1)) != 0;

        if(isCompressed){
            unsigned char* decompColors = new unsigned char[NUM_LED * 3 + 1];
            int len = LZ4_decompress_safe(reinterpret_cast<char*>(&networkData[1]), reinterpret_cast<char*>(decompColors), pkLen - 1, NUM_LED * 3 + 1);
            if(len <= 0){
                delete[] decompColors;
                return;
            }
               
            int numLedInPacket = len / 3;
            for(int i = 0; i < numLedInPacket; i++){
                if (i >= NUM_LED)
                    break;
                uint8_t r = decompColors[i];
                uint8_t g = decompColors[i + numLedInPacket];
                uint8_t b = decompColors[i + numLedInPacket * 2];
                setBoth(i, CRGB(r, g, b));
            }
            for(int i = numLedInPacket; i < NUM_LED; i++){
                setBoth(i, CRGB(0,0,0));
            }

            delete[] decompColors;
        }else{
            unsigned char* colorData = reinterpret_cast<unsigned char *>(&networkData[1]);
            int i = 0;
            for (; i < pkLen / 3; i++)
            {
                if (i >= NUM_LED)
                    break;
                uint8_t r = colorData[i * 3];
                uint8_t g = colorData[i * 3 + 1];
                uint8_t b = colorData[i * 3 + 2];
                setBoth(i, CRGB(r, g, b));
            }
            int lastI = i - 1;
            if (lastI >= 0 && lastI < NUM_LED)
            { // If the user did not specify all leds, use the last set color to fill the remaining leds
                for (; i < NUM_LED; i++){
                    uint8_t r = colorData[lastI * 3];
                    uint8_t g = colorData[lastI * 3 + 1];
                    uint8_t b = colorData[lastI * 3 + 2];
                    setBoth(i, CRGB(r, g, b));
                }
            }
        }

        
    }
    virtual void loop() {

    }
};