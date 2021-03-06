KellerLightServer
================

This component serves the purpose of bridging the gap between the user and the LED controller. Additionally it can process audio data into frequency information which the LED controller can understand.

It communicates to the ESP LED controller via Serial.

It hosts a [Web Server](src/ch/dorianko/keller/HttpApi.java) on port 8080 for easy configuration by the user and exposes a REST api for simple scripts. 

It also hosts a [WebSocket server](src/ch/dorianko/keller/WSApi.java) on port 8081 to allow for more efficient realtime communication, like telemetry from racing games.

All APIs are unauthenthicated and should therefore only be run within a trusted network.

___

https://user-images.githubusercontent.com/62394594/163397707-ca51e8fc-24f2-42d8-b84f-1fead1304064.mp4

Web interface accessed from a mobile phone

___

https://user-images.githubusercontent.com/62394594/163397752-12009573-7315-4f28-bd83-b28f12e41982.mp4

Tasmota device -> Home Assistant -> REST Apis

___

Similar to the LED controller, modes are seperated into multiple files:

[S2L Helper](src/ch/dorianko/keller/helpers/S2LHelper.java)
----------
First, the Sound to Light helper grabs audio input from a connected microphone and passes it to the TarsosDSP library, which converts the audio data into frequency bins using the fourier transform (i.e. which frequencies are most prominent in the audio). It then takes the first 1000hz of this frequency data and normalizes it using data collected in a history buffer of the last 2.5 seconds by subtracting the mean and dividing by 0.8 of a standard deviation. Using this method, the algorithm effectively determines which frequencies are "out of the ordinary", and sends them to the LED controller which processes them further.

[Race Game Helper](src/ch/dorianko/keller/helpers/RaceGameHelper.java)
----------------
Processes a json object sent via the WebSocket api and converts it into lighting data.

https://user-images.githubusercontent.com/62394594/163398271-5db26505-bccf-4881-b249-ace86b67e445.mp4


[FullColorHelper](src/ch/dorianko/keller/helpers/FullColorHelper.java)
-----------------
Static color

[StrobeHelper](src/ch/dorianko/keller/helpers/StrobeHelper.java)
-----------------

[AmbientHelper](src/ch/dorianko/keller/helpers/AmbientHelper.java)
-----------------

See LED controller Readme for more information.
