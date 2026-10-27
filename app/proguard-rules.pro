# Keep our entry point and every tool class. R8's whole-program analysis cannot
# see through string-driven dispatch like McpCore's tools/call.
-keep class bl4ck3gg.uia2mcp.** { *; }

# Keep the reflection shims that talk to the Android framework.
-keep class mirror.** { *; }
-keep class proxy.** { *; }
-keep class uiautomator.** { *; }

# Framework classes are provided by the platform, never bundled.
-dontwarn android.**
-dontwarn androidx.**
