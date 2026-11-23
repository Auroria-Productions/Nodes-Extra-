# Nodes-Extra public release build

The primary `build.sh` compiles against your real `milzzynodes.jar` (a local API dependency). Do not upload the core plugin to this public repository just to build Nodes-Extra.

The GitHub Actions release workflow instead compiles against minimal **compile-time-only** Java signature stubs in `src/ci/stubs/phonon/nodes/`, plus Paper API. Those stubs contain no Nodes implementation and are **excluded from the output JAR**. The job validates the JAR archive and runs the graph/election/serialization/command routing tests. The real-core compatibility test is omitted in CI because the private core is intentionally not available there; it was verified locally against the supplied MilzzyNodes build.

A JAR compiled with these stubs still **requires** `milzzynodes` at runtime and must be tested on a staging Paper server before deployment. The plugin has not been live-server-tested.

To build locally with your real core and dependencies, run `bash build.sh` as documented in README.md. This remains the preferred route for compatibility checks after core updates.

A successful release job attaches `Nodes-Extra.jar` and `Nodes-Extra-1.1.0-Source.zip` to the existing draft release, then publishes v1.1.0 only after both uploads succeed.
