package com.nathan.djavarp.launcher.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nathan.djavarp.launcher.model.ServerInfo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous SA:MP server query helper.
 *
 * Implements the standard SA:MP query protocol over UDP:
 *   header  : "SAMP" (4 bytes ASCII) + 4 bytes IP + 2 bytes port (LE) + 1 byte query type
 *   query types: 'i' = info, 'p' = ping, 'r' = rules, 'c' = clients, 'd' = detailed
 *
 * Response for 'i' (info) starts with the same 11-byte header echoed back, followed by:
 *   1 byte  password
 *   2 bytes players (LE)
 *   2 bytes max players (LE)
 *   4 bytes hostname length (LE) + hostname bytes
 *   4 bytes gamemode length (LE) + gamemode bytes
 *   4 bytes language length (LE) + language bytes
 */
public final class SampQuery {

    private static final String TAG = "SampQuery";
    private static final int TIMEOUT_MS = 3000;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(ServerInfo info);
        void onError(Exception e);
    }

    private SampQuery() {
    }

    /** Async info query. The callback fires on the main thread. */
    public static void queryInfo(final String ip, final int port, final Callback cb) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ServerInfo info = new ServerInfo(ip, port);
                DatagramSocket socket = null;
                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    socket = new DatagramSocket();
                    socket.setSoTimeout(TIMEOUT_MS);

                    byte[] reqInfo = buildPacket(addr, port, 'i', null);
                    DatagramPacket out = new DatagramPacket(reqInfo, reqInfo.length, addr, port);

                    long t0 = System.currentTimeMillis();
                    socket.send(out);

                    byte[] buf = new byte[2048];
                    DatagramPacket in = new DatagramPacket(buf, buf.length);
                    socket.receive(in);
                    int rttInfo = (int) (System.currentTimeMillis() - t0);

                    parseInfoResponse(in.getData(), in.getLength(), info);
                    info.ping = rttInfo;

                    // Now do a second 'p' ping for a tighter measurement.
                    int pingMs = doPing(socket, addr, port);
                    if (pingMs > 0) info.ping = pingMs;

                    final ServerInfo result = info;
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onResult(result); }
                    });
                } catch (final Exception e) {
                    Log.w(TAG, "queryInfo failed: " + ip + ":" + port + " - " + e.getMessage());
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onError(e); }
                    });
                } finally {
                    if (socket != null && !socket.isClosed()) socket.close();
                }
            }
        });
    }

    /** Async ping-only query. */
    public static void queryPing(final String ip, final int port, final Callback cb) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    socket = new DatagramSocket();
                    socket.setSoTimeout(TIMEOUT_MS);

                    int rtt = doPing(socket, addr, port);
                    if (rtt < 0) throw new java.net.SocketTimeoutException("ping timeout");

                    final ServerInfo info = new ServerInfo(ip, port);
                    info.ping = rtt;
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onResult(info); }
                    });
                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        @Override public void run() { cb.onError(e); }
                    });
                } finally {
                    if (socket != null && !socket.isClosed()) socket.close();
                }
            }
        });
    }

    private static int doPing(DatagramSocket socket, InetAddress addr, int port) throws Exception {
        byte[] random = new byte[4];
        new Random().nextBytes(random);
        byte[] req = buildPacket(addr, port, 'p', random);
        DatagramPacket out = new DatagramPacket(req, req.length, addr, port);

        long t0 = System.currentTimeMillis();
        socket.send(out);

        byte[] buf = new byte[64];
        DatagramPacket in = new DatagramPacket(buf, buf.length);
        socket.receive(in);
        long t1 = System.currentTimeMillis();
        return (int) (t1 - t0);
    }

    /**
     * Build SAMP query header: "SAMP" + 4 bytes IP + 2 bytes port (LE) + 1 byte type
     * + optional payload.
     */
    private static byte[] buildPacket(InetAddress addr, int port, char queryType, byte[] payload) {
        int len = 4 + 4 + 2 + 1 + (payload != null ? payload.length : 0);
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) 'S');
        bb.put((byte) 'A');
        bb.put((byte) 'M');
        bb.put((byte) 'P');
        // raw IP bytes (network order = big-endian as returned by InetAddress)
        bb.put(addr.getAddress());
        bb.putShort((short) (port & 0xFFFF));
        bb.put((byte) queryType);
        if (payload != null) bb.put(payload);
        return bb.array();
    }

    private static void parseInfoResponse(byte[] data, int length, ServerInfo out) throws Exception {
        if (length < 11) throw new IllegalStateException("response too short");

        ByteBuffer bb = ByteBuffer.wrap(data, 0, length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(11); // skip echoed header

        out.hasPassword = bb.get() != 0;
        out.players = bb.getShort() & 0xFFFF;
        out.maxPlayers = bb.getShort() & 0xFFFF;

        out.hostname = readLengthPrefixedString(bb);
        out.gamemode = readLengthPrefixedString(bb);
        out.language = readLengthPrefixedString(bb);
    }

    private static String readLengthPrefixedString(ByteBuffer bb) {
        int len = bb.getInt();
        if (len < 0 || len > bb.remaining()) {
            return "";
        }
        byte[] tmp = new byte[len];
        bb.get(tmp);
        // Try UTF-8 first, fallback to Cp1251 (Russian-style SAMP servers commonly use this)
        try {
            String s = new String(tmp, "UTF-8");
            // simple sanity check: if hostname has replacement chars, fallback
            if (s.indexOf('\uFFFD') >= 0) {
                return new String(tmp, "Cp1251");
            }
            return s;
        } catch (Exception e) {
            try { return new String(tmp, "Cp1251"); } catch (Exception ex) { return ""; }
        }
    }
}
