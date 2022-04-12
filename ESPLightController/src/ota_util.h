#pragma once
#include <WiFi.h>
#include <WiFiClient.h>
#include <WebServer.h>
#include <ESPmDNS.h>
#include <Update.h>
#include "secrets.h"

#ifndef WLAN_SSID
#pragma message "Define WLAN ssid and pw for OTA!"
#endif

const char *host = "esp32";
const char *ssid = WLAN_SSID;
const char *password = WLAN_PW;

// All code below this line is copied from an arduino example sketch
// I do not take responsibility

WebServer server(80);

/*
 * Login page
 */
const char *loginIndex =
	"<form name='loginForm'>"
	"<table width='20%' bgcolor='A09F9F' align='center'>"
	"<tr>"
	"<td colspan=2>"
	"<center><font size=4><b>ESP32 Login Page</b></font></center>"
	"<br>"
	"</td>"
	"<br>"
	"<br>"
	"</tr>"
	"<td>Username:</td>"
	"<td><input type='text' size=25 name='userid'><br></td>"
	"</tr>"
	"<br>"
	"<br>"
	"<tr>"
	"<td>Password:</td>"
	"<td><input type='Password' size=25 name='pwd'><br></td>"
	"<br>"
	"<br>"
	"</tr>"
	"<tr>"
	"<td><input type='submit' onclick='check(this.form)' value='Login'></td>"
	"</tr>"
	"</table>"
	"</form>"
	"<script>"
	"function check(form)"
	"{"
	"if(form.userid.value=='admin' && form.pwd.value=='admin')"
	"{"
	"window.open('/serverIndex')"
	"}"
	"else"
	"{"
	" alert('Error Password or Username')/*displays error message*/"
	"}"
	"}"
	"</script>";

/*
 * Server Index Page
 */

const char *serverIndex =
	"<script src='https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js'></script>"
	"<form method='POST' action='#' enctype='multipart/form-data' id='upload_form'>"
	"<input type='file' name='update'>"
	"<input type='submit' value='Update'>"
	"</form>"
	"<div id='prg'>progress: 0%</div>"
	"<script>"
	"$('form').submit(function(e){"
	"e.preventDefault();"
	"var form = $('#upload_form')[0];"
	"var data = new FormData(form);"
	" $.ajax({"
	"url: '/update',"
	"type: 'POST',"
	"data: data,"
	"contentType: false,"
	"processData:false,"
	"xhr: function() {"
	"var xhr = new window.XMLHttpRequest();"
	"xhr.upload.addEventListener('progress', function(evt) {"
	"if (evt.lengthComputable) {"
	"var per = evt.loaded / evt.total;"
	"$('#prg').html('progress: ' + Math.round(per*100) + '%');"
	"}"
	"}, false);"
	"return xhr;"
	"},"
	"success:function(d, s) {"
	"console.log('success!')"
	"},"
	"error: function (a, b, c) {"
	"}"
	"});"
	"});"
	"</script>";

/*
 * setup function
 */
bool doOtaSetup()
{

	// Connect to WiFi network
	WiFi.begin(ssid, password);

	// Wait for connection
	while (WiFi.status() != WL_CONNECTED)
	{
		delay(200);
	}

	/*use mdns for host name resolution*/
	if (!MDNS.begin(host))
	{ // http://esp32.local
		// :(
		return false;
	}
	/*return index page which is stored in serverIndex */
	server.on("/", HTTP_GET, []()
			  {
    server.sendHeader("Connection", "close");
    server.send(200, "text/html", loginIndex); });
	server.on("/serverIndex", HTTP_GET, []()
			  {
    server.sendHeader("Connection", "close");
    server.send(200, "text/html", serverIndex); });
	/*handling uploading firmware file */
	server.on(
		"/update", HTTP_POST, []()
		{
    server.sendHeader("Connection", "close");
    server.send(200, "text/plain", (Update.hasError()) ? "FAIL" : "OK");
    ESP.restart(); },
		[]()
		{
			HTTPUpload &upload = server.upload();
			if (upload.status == UPLOAD_FILE_START)
			{
				if (!Update.begin(UPDATE_SIZE_UNKNOWN))
				{ // start with max available size
				  // Update.printError(Serial);
				}
			}
			else if (upload.status == UPLOAD_FILE_WRITE)
			{
				/* flashing firmware to ESP*/
				if (Update.write(upload.buf, upload.currentSize) != upload.currentSize)
				{
					// Update.printError(Serial);
				}
			}
			else if (upload.status == UPLOAD_FILE_END)
			{
				if (Update.end(true))
				{ // true to set the size to the current progress
				}
				else
				{
					// Update.printError(Serial);
				}
			}
		});
	server.begin();
	return true;
}

void doOtaLoop(void)
{
	server.handleClient();
}