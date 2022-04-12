#ifndef COMMONS_H
#define COMMONS_H
#include <FastLED.h>

enum PacketType
{
	INVALID = 0,
	LEGACY = 1,
	S2L = 2,
	AMBIENT = 3,
	STROBE = 4,
	RAW = 5,
	SET_BRIGHTNESS = 6,
	FREEZE_FRAME = 7,
};

constexpr bool printErrors = false;

constexpr int NUM_LED = 121;

extern CRGB rawLedLeft[NUM_LED + 1];
extern CRGB rawLedRight[NUM_LED];

template <typename T>
inline void setLeft(int i, T c)
{
	rawLedLeft[i + 1] = c;
}

template <typename T>
inline void setRight(int i, T c)
{
	rawLedRight[i] = c;
}

template <typename T>
inline void setBoth(int i, T c)
{
	setLeft(i, c);
	setRight(i, c);
}

template <typename T>
void setAll(T col)
{
	for (int i = 0; i < NUM_LED; i++)
		setBoth(i, col);
}

#endif