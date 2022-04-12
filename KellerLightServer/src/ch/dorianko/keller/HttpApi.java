package ch.dorianko.keller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ch.dorianko.keller.helpers.AmbientHelper;
import ch.dorianko.keller.helpers.FullColorHelper;
import ch.dorianko.keller.helpers.S2LHelper;
import ch.dorianko.keller.packets.FreezeFramePacket;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpApi extends Thread implements HttpHandler {

    public HttpApi() {
    }

    @Override
    public void run() {
        try {
            System.out.println("Http api starting");
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
            server.createContext("/action", this);
            server.createContext("/", (e) -> {
                String response = "";
                var output = e.getResponseBody();
                var resource = getClass().getClassLoader().getResourceAsStream("index.html");
                byte[] bytes = resource.readAllBytes();
                e.sendResponseHeaders(200, bytes.length);
                output.write(bytes);
                output.close();
            });

            server.start();
            System.out.println("Http api started");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
            server.stop(0);
            System.out.println("Http api stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var query = queryToMap(httpExchange.getRequestURI().getQuery());
        var act = query.getOrDefault("act", "");
        String response = "";
        var output = httpExchange.getResponseBody();
        response = "success";
        httpExchange.sendResponseHeaders(200, response.length());
        output.write(response.getBytes());
        output.close();
        switch (act) {
            case "operation": {
                var mode = query.getOrDefault("mode", "").toLowerCase();
                switch(mode){
                    case "ambient":
                        System.out.println("Set to ambient mode by http");
                        MainClass.led.setHelper(new AmbientHelper(MainClass.led));
                        break;
                    case "s2l":
                        System.out.println("Set to s2l mode by http");
                        MainClass.led.setHelper(new S2LHelper(MainClass.led));
                        break;
                    case "fullcolor":
                        System.out.println("Set to fullcolor mode by http");
                        int r = Integer.parseInt(query.getOrDefault("red", "0"));
                        int g = Integer.parseInt(query.getOrDefault("green", "0"));
                        int b = Integer.parseInt(query.getOrDefault("blue", "0"));

                        System.out.println("Set to fullcolor mode by ws");
                        Color c = new Color(r, g, b);
                        if (MainClass.led.getHelper().isEmpty() || !(MainClass.led.getHelper().get() instanceof FullColorHelper))
                            MainClass.led.setHelper(new FullColorHelper(MainClass.led, c));
                        else
                            ((FullColorHelper) MainClass.led.getHelper().get()).setColor(c);
                        break;
                    default:
                        System.out.println("Unrecognized operation: "+mode);
                }
                break;
            }
            case "set":

                var key = query.getOrDefault("key", "").toLowerCase();
                var value = Integer.parseInt(query.getOrDefault("value", "-9999999"));

                switch(key){ // keys without value
                    case "force_hit":
                        MainClass.led.getHelper().ifPresent(h -> {
                            if(!(h instanceof S2LHelper))
                                return;
                            ((S2LHelper) h).doForceHit = true;
                        });
                        return;
                    case "freeze_frame":
                        MainClass.led.sendPacket(new FreezeFramePacket());
                        return;
                }
                if (value == -9999999)
                    return;
                switch (key) { // keys with value
                    case "led_brightness":
                        MainClass.led.setBrightness(Util.clip(value, 1, 255));
                        return;
                    case "led_brightness_relative": {
                        int cur = MainClass.led.getLedBrightness();
                        cur += value;
                        cur = Util.clip(cur, 1, 255);
                        System.out.println("Setting brightness to "+cur+" from http");
                        MainClass.led.setBrightness(cur);
                        return;
                    }
                    case "strobe":
                        MainClass.led.getHelper().ifPresent(h -> {
                            if(!(h instanceof S2LHelper))
                                return;
                            ((S2LHelper) h).doForceStrobe = value != 0;
                        });
                        return;
                }
                System.out.println("Unrecognized option: "+key);
                return;
        }
    }
}
