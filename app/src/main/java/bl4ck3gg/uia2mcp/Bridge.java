package bl4ck3gg.uia2mcp;

import android.net.LocalServerSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Bridge joins the two halves of a split process: an installed app that owns a
 * VpnService, and this one, started by app_process.
 *
 * THE PROBLEM IT SOLVES. A VpnService can only be installed by an installed
 * application -- the system binds it by package name and a human grants it. But
 * the process that owns the tunnel cannot be reached from outside: the device's
 * adb daemon forwards to ports the shell can bind, not to an app's sandbox.
 *
 * So the two halves are split by where the packets must go:
 *
 *   - the app reads raw IPv4 from the tunnel and connects to localabstract:gnirehtet
 *   - nothing is listening there, because that socket was always meant to be served
 *     by adb reverse pointing back at a host that is on the wrong side of a tunnel
 *
 * This class is the missing listener. Started by app_process with shell privileges,
 * it binds the abstract socket the app dials, binds a TCP port the host can reach
 * through adb forward, and pumps bytes between whichever pair arrives.
 *
 * WHY THIS BEATS adb reverse. reverse directs the device to dial the host, which
 * means the host side must listen and the caller must reach it backwards -- through
 * a channel that can only dial forwards. forward moves the same traffic the other
 * way, so an ordinary forward connection (a SOCKS5 CONNECT through an SSH tunnel)
 * is enough, and no second port has to be opened anywhere.
 *
 * The pump is deliberately byte-transparent. The relay protocol's 4-byte client id
 * passes through it untouched, because this process understands no protocol at all.
 */
public class Bridge {

    private final int port;
    private final String abstractName;

    public Bridge(int port, String abstractName) {
        this.port = port;
        this.abstractName = abstractName;
    }

    public void run() throws IOException {
        // NAMESPACE MATTERS MORE THAN THE NAME. gnirehtet dials
        // `new LocalSocketAddress("gnirehtet")`, whose default namespace is
        // RESERVED -- a path under /dev/socket. A LocalServerSocket built from a
        // bare name binds ABSTRACT instead, an unrelated namespace, so the two
        // ends would carry the same string and still never meet. Bind RESERVED
        // explicitly to match the client that was written years before this.
        LocalSocketAddress addr = new LocalSocketAddress(
                abstractName, LocalSocketAddress.Namespace.RESERVED);
        LocalServerSocket deviceSide = new LocalServerSocket(addr.getAddress());
        Main.step("bridge listening on reserved socket " + abstractName);

        ServerSocket hostSide = new ServerSocket();
        hostSide.setReuseAddress(true);
        // Loopback only: the device's adb daemon forwards to it, nothing else needs to.
        hostSide.bind(new InetSocketAddress("127.0.0.1", port));
        Main.step("bridge listening on 127.0.0.1:" + port);

        while (true) {
            LocalSocket app = null;
            Socket host = null;
            try {
                app = deviceSide.accept();
                Main.step("bridge: app connected");
                host = hostSide.accept();
                Main.step("bridge: host connected, pumping");

                pump(app, host);
                Main.step("bridge: session closed");
            } catch (IOException e) {
                Main.step("bridge: session error: " + e);
            } finally {
                closeQuietly(app);
                closeQuietly(host);
            }
        }
    }

    /**
     * Pumps both directions and returns when either side finishes.
     *
     * Both legs run on their own thread because a byte stream is full-duplex: a
     * single loop that alternated would deadlock the moment both ends sent at
     * once, which for a packet tunnel is immediately.
     */
    private static void pump(final LocalSocket app, final Socket host) {
        Thread up = new Thread(() -> copy(app, host), "bridge-up");
        Thread down = new Thread(() -> copy(host, app), "bridge-down");
        up.start();
        down.start();
        try {
            up.join();
            down.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void copy(Object from, Object to) {
        try {
            InputStream in = from instanceof LocalSocket
                    ? ((LocalSocket) from).getInputStream()
                    : ((Socket) from).getInputStream();
            OutputStream out = to instanceof LocalSocket
                    ? ((LocalSocket) to).getOutputStream()
                    : ((Socket) to).getOutputStream();

            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
            // A closed peer is the normal end of a session, not a failure.
        }
    }

    private static void closeQuietly(Object c) {
        try {
            if (c instanceof LocalSocket) ((LocalSocket) c).close();
            else if (c instanceof Socket) ((Socket) c).close();
        } catch (IOException ignored) {
        }
    }
}
