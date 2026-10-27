package bl4ck3gg.uia2mcp;

import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * McpHttpServer exposes {@link McpCore} over Streamable HTTP on a local port.
 *
 * This is the forward transport: the device listens and a peer connects through
 * `adb forward`. {@link ReverseClient} is the inverted one. Both share McpCore, because
 * only the direction of the handshake differs.
 */
public class McpHttpServer extends NanoHTTPD {

    private final McpCore core;

    public McpHttpServer(int port, McpCore core) {
        super(port);
        this.core = core;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if ("/mcp".equals(uri) || "/".equals(uri)) {
            if (Method.POST.equals(method)) {
                return handlePost(session);
            }
            if (Method.GET.equals(method)) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "uia2-mcp ready\n");
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private Response handlePost(IHTTPSession session) {
        try {
            int len = 0;
            String cl = session.getHeaders().get("content-length");
            if (cl != null) len = Integer.parseInt(cl);
            if (len <= 0 || len > 1 << 20) len = 1 << 16;

            byte[] buf = new byte[len];
            int read = session.getInputStream().read(buf, 0, len);
            String body = new String(buf, 0, Math.max(read, 0), StandardCharsets.UTF_8);

            String resp = core.handle(body);
            if (resp == null) {
                return newFixedLengthResponse(Response.Status.ACCEPTED, "application/json", "");
            }

            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    new ByteArrayInputStream(out), out.length);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\""
                            + Json.esc(e.toString()).replace("\"", "") + "\"}}");
        }
    }
}
