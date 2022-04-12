package ch.dorianko.keller;

import ch.dorianko.keller.helpers.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;

public class WSApi extends WebSocketServer {

    public WSApi(int port) {
        super(new InetSocketAddress(port));
        this.setReuseAddr(true);
    }

    private void shutdown() {
        try {
            this.stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        super.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        webSocket.send("hi");
        System.out.println(webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to ws");
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        System.out.println(webSocket.getRemoteSocketAddress() + " disconnected from ws");
    }


    private void processFullColor(JSONObject obj) {
        var ledAlias = MainClass.led; // TODO: don't do this

        float r = obj.optFloat("red", 0);
        float g = obj.optFloat("green", 0);
        float b = obj.optFloat("blue", 0);

        System.out.println("Set to fullcolor mode by ws");
        Color c = new Color((int) r, (int) g, (int) b);
        if (ledAlias.getHelper().isEmpty() || !(ledAlias.getHelper().get() instanceof FullColorHelper))
            ledAlias.setHelper(new FullColorHelper(ledAlias, c));
        else
            ((FullColorHelper) ledAlias.getHelper().get()).setColor(c);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        var obj = new JSONObject(s);
        var ledAlias = MainClass.led; // TODO: don't do this

        if (obj.has("operation")) {
            switch (obj.optString("operation")) {
                case "game":
                    if(ledAlias.getHelper().isEmpty() || !(ledAlias.getHelper().get() instanceof RaceGameHelper))
                        ledAlias.setHelper(new RaceGameHelper(ledAlias));
                    ((RaceGameHelper) ledAlias.getHelper().get()).processGameData(obj);
                    break;
                case "ambient":
                    System.out.println("Set to ambient mode by ws");
                    ledAlias.setHelper(new AmbientHelper(ledAlias));
                    break;
                case "s2l":
                    System.out.println("Set to s2l mode by ws");
                    ledAlias.setHelper(new S2LHelper(ledAlias));
                    break;
                case "strobe":
                    System.out.println("Set to strobe mode by ws");
                    ledAlias.setHelper(new StrobeHelper(ledAlias));
                    break;
                case "fullcolor":
                    this.processFullColor(obj);
                    break;
            }

        }
        if (obj.has("brightness")) {
            ledAlias.setBrightness(Util.clip((int) obj.getFloat("brightness"), 0, 255));
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        setConnectionLostTimeout(10);
    }
}
