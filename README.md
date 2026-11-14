# Char's Toolbox 1.3

One file to drop into a Forge 1.20.1 profile: fourteen handy client-side parts, each with its own on/off switch.

## The fourteen parts

| Part | What it does | How you use it | At first |
|---|---|---|---|
| Map (`charmap` 1.2, carried) | minimap, world map, waypoints | M map, B add waypoint, = / - zoom | on (off with Xaero's / JourneyMap) |
| Far View (`charlod` 1.2, carried) | land past the render distance | F9 on/off, F10 settings | on (off with Distant Horizons) |
| Main Menu (`charmenu` 3.0, carried) | five title-screen scenes (cherry house, snowy cabin, sunset cove, Nether hideaway, autumn woods) or Random, rounded buttons; Play, Options and Mods each fly to their own spot with its own pose for you and the cat | landscape button on the title screen = pick a scene; gear = its settings | on (off with FancyMenu and the like) |
| Teams (`charteams` 1.1, carried) | team up with friends | U | on |
| Config Helper (`charconfig` 1.1, carried) | every setting on one screen | `/configs` | on |
| Fortress Boxes (`fortressboxes` 1.1, carried) | fortress edges in the Nether | B | on |
| Bring my settings (`suitcase`) | keys, options, servers, switches follow you to a new profile | asks by itself; gear | on |
| Key clash fixer (`keyclash`) | keys that do two things, Change / Clear / Fix for me with undo | notice card; gear | on |
| Zoom (`zoom`) | smooth zoom, mouse slowed to match | hold H, scroll while held | on (off with another zoom mod) |
| Little HUD (`littlehud`) | coordinates, facing, biome, day and time, clock | gear: lines and corner | off |
| Pickup list (`pickuplist`) | "+3 Iron Ore" beside the hotbar | - | off |
| Gear watch (`gearwatch`) | wear strip, one meow, tool guard at 3 uses left | gear | on |
| Memory watch (`memorywatch`) | a card when memory runs low, a line on the pause screen | gear | on |
| Cat corner (`catcorner`) | Sleepy Cat; Fullbright ('), Auto Rejoin, Auto Eat | gear: a switch each | on, the three helpers off (all off when another mod already has them) |


## Building

Java 17. `gradlew build` makes `build/libs/chartoolbox-<version>.jar`.

## Licence

MIT, see `LICENSE`.
