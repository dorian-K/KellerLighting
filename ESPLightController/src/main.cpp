#include <Arduino.h>
#include <FastLED.h>
#include "ota_util.h"
#include "serialutil.h"
#include "commons.h"
#include "components/AbstractComponent.h"
#include "components/AmbientComponent.h"
#include "components/S2LComponent.h"
#include "components/RawColorComponent.h"
#include <memory>

#if FASTLED_VERSION < 3001000
#error "Requires FastLED 3.1 or later; check github for latest code."
#endif

unsigned char brightness = 100;

unsigned long lastPacket = 0;
unsigned long startup = 0;

std::shared_ptr<AbstractComponent> currentComponent;
SerialUtil serialUtil;

void setup()
{	
	currentComponent = std::make_shared<AmbientComponent>();
	Serial.begin(921600);
	delay(100);

	FastLED.addLeds<WS2812B, 13, GRB>(rawLedLeft, NUM_LED + 1).setCorrection(TypicalLEDStrip);
	FastLED.addLeds<WS2812B, 26, GRB>(rawLedRight, NUM_LED).setCorrection(TypicalLEDStrip);

	FastLED.setBrightness(10);
	setAll(CRGB::Red);
	FastLED.show();

	doOtaSetup();

	startup = millis();
	setAll(CRGB::Green);
	FastLED.show();
	FastLED.setBrightness(brightness);
	lastPacket = millis();
}

void processSetBrightness(int pkLen, unsigned char* networkData)
{
	// [byte brightness]
	if (pkLen < 1)
		return;
	unsigned char newBrightness = networkData[0];
	if (newBrightness != brightness)
	{
		FastLED.setBrightness(newBrightness);
		brightness = newBrightness;
	}
}

void loop()
{
	doOtaLoop(); // comment this out if OTA is not needed

	// Timout: reset to Ambient Mode
	if (millis() - lastPacket > 10 * 1000 && (!currentComponent || currentComponent->getMode() != AMBIENT))
		currentComponent = std::make_shared<AmbientComponent>();

	// disable strobe after 5s if no follow up packet is sent
	if (currentComponent && currentComponent->getMode() == STROBE && millis() - lastPacket > 5000)
		currentComponent.reset(); 

	// Loop current component
	if(currentComponent){
		currentComponent->loop();
		FastLED.show();
	}

	serialUtil.tick();
	if(serialUtil.getHasNewPacket() == false){
		FastLED.delay(1);
		return;
	}

	lastPacket = millis();
	auto pkType = serialUtil.getPacketType();
	auto pkLen = serialUtil.getDataLength();
	auto pkData = serialUtil.getDataPointer();

	if(!currentComponent || currentComponent->getMode() != pkType){
		// switch to relevant component if its not set already
		switch (pkType){
		case S2L:
			currentComponent = std::make_shared<S2LComponent>();
			break;
		case AMBIENT:
			currentComponent = std::make_shared<AmbientComponent>();
			break;
		case STROBE:
			currentComponent = std::make_shared<StrobeComponent>();
			break;
		case RAW:
			currentComponent = std::make_shared<RawColorComponent>();
			break;
		default:
			break;
		}
	}

	// Miscellanous packets 
	switch (pkType)
	{
	case SET_BRIGHTNESS:
		processSetBrightness(pkLen, pkData);
		break;
	case FREEZE_FRAME:
		FastLED.delay(1000);
		break;
	default:
		break;
	}

	// Update component state
	if(currentComponent && currentComponent->getMode() == pkType)
		currentComponent->start(pkData, pkLen);
	FastLED.show();
	FastLED.delay(1);
}
