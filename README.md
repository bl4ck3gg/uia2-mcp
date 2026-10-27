# uia2-mcp

Pure Java streamable MCP server for Android UIAutomator — compact hierarchy
projection, verified actions, zero-dependency `app_process` daemon.

- **728 KB standalone APK** — no Jackson, no jsonrpc4j, no bulky frameworks.
- **Dual transports**:
  - **Forward (HTTP)**: Streamable HTTP on a local port (`POST /mcp`), reachable
    via `adb forward`.
  - **Reverse (stream)**: Dials out to an existing upstream (`--connect
    localabstract:name` or `--connect tcp:host:port`) and serves newline-delimited
    JSON-RPC over the established stream. **Zero open ports on the device, zero
    exposure to neighbouring apps.**
- **Compact UI projection**: Directly walks live `AccessibilityNodeInfo` objects to
  emit minimal JSON (~90% smaller than raw dump). **Never serialises to XML.**
- **Verified actions**: `ui_click` adaptively watches the screen for observable
  changes before answering, eliminating arbitrary sleeps.
- **MIT Licensed**.

## Tools Exposed

- `ui_dump`: Capture screen elements with text, id, desc, class, and center coordinates.
- `ui_click`: Click by text, id, or coordinates, reporting `verified: true/false`, `settle_ms`, and observed changes.
- `ui_input`: Native touch events (text injection, key press, directional swipe).
- `ui_wait`: In-process adaptive polling until text appears or disappears.

## Build

```bash
./gradlew :app:assembleRelease
# outputs app/build/outputs/apk/release/uia2-mcp.apk (~728 KB)
```

## Run on Device

Push the APK to the device:

```bash
adb push app/build/outputs/apk/release/uia2-mcp.apk /data/local/tmp/
```

### Mode 1: Reverse (recommended)

Host sets up a reverse tunnel, device connects back:

```bash
# On host
adb reverse localabstract:uia2-mcp tcp:19999

# On device
CLASSPATH=/data/local/tmp/uia2-mcp.apk app_process / bl4ck3gg.uia2mcp.Main \
    --connect localabstract:uia2-mcp
```

### Mode 2: Forward HTTP

```bash
# On device
CLASSPATH=/data/local/tmp/uia2-mcp.apk app_process / bl4ck3gg.uia2mcp.Main --port 18080

# On host
adb forward tcp:18080 tcp:18080
curl -X POST http://127.0.0.1:18080/mcp -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## Credits

Bootstrap reflection shims derived from [xiaocong/uiautomator](https://github.com/xiaocong/uiautomator) (MIT License).
