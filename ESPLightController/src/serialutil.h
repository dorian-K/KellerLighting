
#include <Arduino.h>
#include "commons.h"

#define MAX_NETWORK_LENGTH 1024

unsigned char magicVal[4] = {0x4A, 0xAC, 0xA3, 0x57};

uint32_t adler32(unsigned char *data, int len)
{
	uint32_t a = 1, b = 0;
	size_t index;

	// Process each byte of the data in order
	for (index = 0; index < len; ++index)
	{
		a = (a + data[index]) % 65521;
		b = (b + a) % 65521;
	}

	return (b << 16) | a;
}

class SerialUtil {
    unsigned char netData[MAX_NETWORK_LENGTH + 16];
    unsigned int dataOffset = 0;
    unsigned int packetOffset = 0;
    bool isInDoneState = false;

public:
    SerialUtil() = default;

    bool processByte(unsigned char byte){
        if(isInDoneState){ // Previous packet should have been handled by now
            isInDoneState = false;
            dataOffset = 0;
            packetOffset = 0;
        }
        if(packetOffset < 4){ // magic part
            unsigned char target = magicVal[packetOffset];
            if(byte == target){
                packetOffset++;
            }else{
                packetOffset = 0;
            }
            return false;
        }

        if(dataOffset < 2 + 1 + 4){ // Read all header bytes
            netData[dataOffset] = byte;
            packetOffset++;
            dataOffset++;   
            return false;
        }
        short pkLen = netData[0] | (netData[1] << 8);
        unsigned char pkType = netData[2];
        
        // Sanity checks
        if(pkLen > MAX_NETWORK_LENGTH || pkLen < 0){
            packetOffset = dataOffset = 0;
            return false;
        }
        if(pkType < 1 || pkType > 20){
            // 0 would be INVALID
            packetOffset = dataOffset = 0;
            return false;
        }

        if(dataOffset < pkLen + 2 + 1 + 4){
            netData[dataOffset] = byte; // read the remainder of the packet
            packetOffset++;
            dataOffset++;   
            return false;
        }

        unsigned int checksum = netData[3] | (netData[4] << 8) | (netData[5] << 16) | (netData[6] << 24);
        
        // done reading data, compute the checksum
        auto ourCheck = adler32(&netData[2 + 1 + 4], pkLen);
        if(ourCheck != checksum){
            // Checksum mismatch!
            packetOffset = dataOffset = 0;
            return false;
        }

        // Packet successfully received!
        isInDoneState = true;
        return true;
    }

    bool tick() {
        bool isDone = false;
        while(Serial.available() && !isDone){
            unsigned char byte = Serial.read();
            isDone = this->processByte(byte);
        }

        return isDone;
    }

    bool getHasNewPacket(){
        return this->isInDoneState;
    }

    unsigned char* getDataPointer(){
        return &this->netData[2 + 1 + 4];
    }

    int getDataLength(){
        return this->netData[0] | (this->netData[1] << 8);
    }

    PacketType getPacketType(){
        return static_cast<PacketType>(this->netData[2]);
    }
};
