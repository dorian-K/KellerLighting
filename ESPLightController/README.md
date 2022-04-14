ESPLightController
==================

This component runs on an ESP32 microcontroller. It receives data from the server to send signals to LEDs with the FastLED library.

Data transmission is accomplished via Serial, see [serialutil.h](src/serialutil.h)

Over the air updates are implemented via a WebServer, see [ota_util.h](src/ota_util.h). The code will not compile before you define WLAN_SSID and WLAN_PW.

https://user-images.githubusercontent.com/62394594/163398682-77a8644a-9475-4917-b96e-9ec3c741e50f.mp4


There are 5 components:

[Ambient](src/components/AmbientComponent.h)
-------
Slowly fade through the rainbow spectrum, this is the default mode on startup. Continually updates even if no more data is sent.

[Raw Color](src/components/RawColorComponent.h)
---------
Applies a static color array sent by the server, optionally supports compression.

This is used for the Race Game integration in the server

[S2L](src/components/S2LComponent.h)
---
Abbreviation for Sound to Light.

Receives sound data from the server and uses it to generate a visualisation based on the currently playing music. Continually updates even if no more data is sent.

[Strobe](src/components/StrobeComponent.h)
------
Rapidly turns the leds on and off (flashing). Automatically turns off after some time if no further packet is sent.

Set Brightness
--------------
Sets the Brightness of the LEDs. Can be sent in any mode.
