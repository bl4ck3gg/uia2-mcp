# uia2-mcp — archived

This project has moved.

Everything here now lives in **[bl4ck3gg/adbox-mcp]** under `agent/`, and it is
no longer only a UI automation server, so it is no longer called `uia2`.
The renamed device-side agent is `adbox-agent`.

| Was | Is |
|---|---|
| repository `uia2-mcp` | `adbox-mcp`, directory `agent/` |
| package `bl4ck3gg.uia2mcp` | `bl4ck3gg.adbox.agent` |
| artifact `uia2-mcp.apk` | `adbox-agent.apk` |
| `app_process ... bl4ck3gg.uia2mcp.Main` | `app_process ... bl4ck3gg.adbox.agent.Main` |

## Why the move

The APK was already embedded inside adbox as a payload, so keeping its sources
in a second repository meant two places to change the same protocol and two
versions to keep in step, with nothing gained. adbox now builds the agent from
`agent/` and embeds the result as `internal/embedded/adbox-agent.apk`.

## Why the rename

`uia2-mcp` named the first thing the process did. It went on to carry four:

- UI automation (`ui_dump`, `ui_click`, `ui_wait`)
- a reflection evaluator reaching any hidden API (`device_eval`)
- a VpnService client (the vendored gnirehtet)
- a packet bridge connecting the app to a forwarded TCP port

The new name says which side it belongs to rather than which job came first.

## What is still true here

The history, the tags, and the MIT licence (which carries the original
xiaocong 2015 copyright notice from the `uiautomator` bootstrap shims) remain
in this repository. Nothing was rewritten; the work continued elsewhere.

[bl4ck3gg/adbox-mcp]: https://github.com/bl4ck3gg/adbox-mcp
