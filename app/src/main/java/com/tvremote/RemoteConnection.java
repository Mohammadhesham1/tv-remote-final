package com.tvremote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles sending commands to the TV over Wi-Fi.
 * Uses UDP port 7777 for fire-and-forget commands (fast),
 * and TCP port 7778 for reliable delivery (text input, app launch).
 */
public class RemoteConnection {
    private static final String TAG = "RemoteConnection";
    public static final int UDP_PORT = 7777;
    public static final int TCP_PORT = 7778;

    private String tvIp;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionCallback {
        void onSuccess();
        void onError(String message);
    }

    public RemoteConnection(String tvIp) {
        this.tvIp = tvIp;
    }

    public void setTvIp(String ip) {
        this.tvIp = ip;
    }

    public String getTvIp() {
        return tvIp;
    }

    /** Send a command via UDP (fast, no guarantee) */
    public void sendUDP(String command) {
        sendUDP(command, null);
    }

    public void sendUDP(String command, ConnectionCallback callback) {
        final String ip = tvIp;
        if (ip == null || ip.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError("No TV IP set"));
            return;
        }
        executor.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(2000);
                byte[] data = command.getBytes("UTF-8");
                InetAddress addr = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, addr, UDP_PORT);
                socket.send(packet);
                Log.d(TAG, "UDP sent: " + command + " -> " + ip);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (IOException e) {
                Log.e(TAG, "UDP error: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /** Send a command via TCP (reliable, for text/app launch) */
    public void sendTCP(String command, ConnectionCallback callback) {
        final String ip = tvIp;
        if (ip == null || ip.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError("No TV IP set"));
            return;
        }
        executor.execute(() -> {
            try (Socket socket = new Socket(ip, TCP_PORT);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                socket.setSoTimeout(3000);
                writer.println(command);
                Log.d(TAG, "TCP sent: " + command + " -> " + ip);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (IOException e) {
                Log.e(TAG, "TCP error: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /** Ping the TV to check if it's reachable */
    public void ping(String ip, PingCallback callback) {
        executor.execute(() -> {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                boolean reachable = addr.isReachable(2000);
                mainHandler.post(() -> callback.onResult(reachable));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public interface PingCallback {
        void onResult(boolean reachable);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
