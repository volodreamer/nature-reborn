# Nature Reborn

Fabric mod for Minecraft **26.3**. Spiritual take on classic Nature Overhaul: vanilla plants and trees grow, spread, and die by biome. No new blocks in this version — behavior only.

## What it does

- Grass, tall grass, ferns, and flowers clone near existing plants and die in shade or the wrong biome
- Trees plant saplings on forest edges, thin crowded interiors, and occasionally die (rare fallen logs)
- Jungle / dark oak use spaced 2×2 sapling groups
- Crops spread inside a fenced field, convert dirt/grass to farmland, senesce, and auto-replant
- Villages stay clearer (paths, buildings, no new forest in the streets)
- Cactus, sugar cane, and mushrooms follow soil and biome rules
- Fire can last long enough to spread
- **Lumberjack** is experimental and **off** (Mods → Nature Reborn → Trees)

## Requirements

- Java 25
- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Cloth Config 26.3.158
- Mod Menu 21.0.0-beta.1 (optional)

Sodium 0.9.2 is a runtime target, not a compile dependency.

## Build

```bash
./gradlew build
```

GitHub Actions uploads `build/libs/` on each push.

## Config

`config/naturereborn.json` after first launch, or Mod Menu. Dedicated servers need only the common jar.

## License

[GPL-3.0-or-later](LICENSE).
