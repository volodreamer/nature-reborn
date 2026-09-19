# Nature Reborn

Fabric mod for Minecraft **26.3** that makes vanilla nature dynamic: plants and trees grow, spread, and die by biome; crops senesce and reseeds; optional lumberjack; longer-lived fire.

Spiritual successor to classic Nature Overhaul. No new blocks in v1 — behavior only.

## Status

Phase 0 scaffold. Gameplay systems land in later commits.

## Requirements

- Java 25
- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Cloth Config 26.3.158 (config screens)
- Mod Menu 21.0.0-beta.1 (optional, client)

Sodium 0.9.2 is a runtime compatibility target, not a compile dependency.

## Build

```bash
gradle wrapper --gradle-version 9.6.0   # first time, if you have no wrapper
./gradlew build
```

GitHub Actions builds on every push and uploads `build/libs/`.

## Config

Server/common settings live in `config/naturereborn.json` after first launch. Dedicated servers do not need the client.

## License

[GPL-3.0-or-later](LICENSE).

You may distribute binaries, including commercially, only if you also provide complete corresponding source under GPLv3. Closed-source forks are not allowed.
