# Nature Reborn

Fabric mod for Minecraft **26.3**. Spiritual take on classic Nature Overhaul: vanilla plants and trees grow, spread, and die by biome. No new blocks — behavior only. No 26.2 backport.

## What it does

- Grass, flowers, cactus, sugar cane, mushrooms, lily pads, dead bushes
- Trees seed on forest edges, thin interiors, occasionally die (rare fallen logs)
- Crops spread on slopes (±1), fences block spread, auto-replant, senescence
- Cobble weathers mossy near water/rain/moss
- Fire can last long enough to spread
- Lumberjack is experimental and off

Rates live in datapack JSON: `data/naturereborn/naturereborn/species/*.json`. Override with a datapack; `/reload` picks them up.

## Requirements

- Java 25, Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3
- Cloth Config 26.3.158 and Mod Menu — **client only** (recommended)

Dedicated server: Nature Reborn + Fabric API. Cloth is not required on the server. Config is `config/naturereborn.json`.

## Commands

`/naturereborn status|scan|reload` or `/no status|scan|reload` (moderator).

## Build

```bash
./gradlew build
```

Tag `v0.1.0-alpha` to publish a GitHub Release.

## License

[GPL-3.0-or-later](LICENSE).
