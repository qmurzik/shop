package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Vanilla's tab-list ping only updates when the SERVER decides to broadcast it, which on some
 * BedWars servers is very infrequent. This measures a real, continuously-updating round-trip
 * time by timing a plain TCP connect to the server's own host:port on a background thread,
 * independent of anything the server chooses to send.
 */
public final class PingMeter {
    private final Minecraft mc;
    private volatile int lastPingMs = -1;
    private volatile String activeHost;
    private Thread worker;

    public PingMeter(Minecraft mc) { this.mc = mc; }

    public int getPing() { return lastPingMs; }

    public void tick() {
        ServerData sd = mc.getCurrentServerData();
        if (mc.thePlayer == null || sd == null || sd.serverIP == null) {
            lastPingMs = -1; activeHost = null; stopWorker(); return;
        }
        if (!sd.serverIP.equals(activeHost)) { activeHost = sd.serverIP; startWorker(activeHost); }
    }

    private void startWorker(final String address) {
        stopWorker();
        String host = address; int port = 25565;
        int idx = address.lastIndexOf(':');
        if (idx > 0 && idx < address.length() - 1) {
            try { port = Integer.parseInt(address.substring(idx + 1)); host = address.substring(0, idx); } catch (NumberFormatException ignored) {}
        }
        final String finalHost = host; final int finalPort = port;
        worker = new Thread("QMods-Ping") {
            @Override public void run() {
                while (address.equals(activeHost) && !isInterrupted()) {
                    long start = System.nanoTime();
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(finalHost, finalPort), 1500);
                        socket.close();
                        lastPingMs = (int) ((System.nanoTime() - start) / 1000000L);
                    } catch (Exception e) {
                        lastPingMs = -1;
                    }
                    try { Thread.sleep(1200L); } catch (InterruptedException ignored) { return; }
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    private void stopWorker() { if (worker != null) { worker.interrupt(); worker = null; } }
}
