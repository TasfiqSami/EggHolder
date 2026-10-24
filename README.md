# EggHolder / End War

`EggHolder` is a Paper plugin for running a high-pressure End PvP event where one player carries the Dragon Egg and becomes the main target for the entire server.

This README is written for people who:

- have never used this plugin before
- may not know the End War event format
- need to understand what the plugin does without someone explaining it live

## What This Plugin Is

This plugin turns a normal Minecraft End fight into a structured event.

The core idea is simple:

1. A Dragon Egg becomes the most important objective in the match.
2. Whoever gets the egg becomes the `EggHolder`.
3. The EggHolder becomes globally visible, tracked, and much more dangerous.
4. Everybody else hunts them, steals the egg, or survives the event.

The plugin adds:

- automatic EggHolder detection
- live coordinate tracking with a bossbar
- custom EggHolder gear and effects
- death spectator/ghost mode
- team system
- hunter kits
- match start flow
- side objectives and event modifiers
- admin live controls

It is built for competitive, event-style gameplay on a Paper server.

## What Is "End War"?

`End War` is an event style where players or teams enter the End and fight over the Dragon Egg after the Ender Dragon dies.

In this plugin:

- the Dragon Egg is the central objective
- the player holding it is heavily announced to everyone
- the event keeps pressure high with systems like End Storm, anti-camp punishments, supply drops, hotspots, overtime, revenge mark, no-build zones, and spectator audience voting

So even if someone does not know the event beforehand, they can think of it like this:

`One player has the objective. Everyone knows it. That player is powerful, but also hunted. The match gets more dangerous over time until someone wins.`

## Server Requirements

- `Paper 1.20.6` through `1.21.11` (API compatibility checks run against both endpoints)
- `Java 21`
- Maven only if you want to build from source

## Downloads

- [GitHub release 2.2.3](https://github.com/TasfiqSami/EggHolder/releases/tag/v2.2.3) — includes `EggHolder.jar`.
- [Modrinth 2.2.3 jar](https://cdn.modrinth.com/data/jm0TqH0V/versions/MUrL8MbB/EggHolder.jar). The existing Modrinth project is still being reviewed, so its public listing may not appear in search yet.

### Server Software Compatibility

EggHolder is a **Paper API plugin**, not a mod-loader mod. Compatibility depends on the server providing the Paper APIs used by the plugin and behaving like a normal single-threaded Bukkit/Paper server.

| Server software | Support status | Notes |
|---|---|---|
| Paper | Supported | API compatibility is compiled in CI against 1.20.6 and 1.21.11. |
| Purpur | Supported as a Paper-compatible fork | Use a Minecraft version in the supported range. |
| Pufferfish, Leaf, Gale | Paper-fork compatible; not separately runtime-tested | These are included as best-effort compatible software. Report issues with the exact server build and version. |
| Spigot | Not supported yet | This build directly uses Paper-only API classes and methods (including registry and Adventure integrations). Listing Spigot as supported would be misleading; it needs a compatibility port and Spigot runtime tests first. |
| Folia | Not supported | The plugin uses Bukkit's standard scheduler and has not been adapted to Folia's region and entity schedulers. Setting a Folia support flag alone would not make it safe. |
| Forge / NeoForge / Fabric / Quilt | Not directly supported | These are mod loaders, not Bukkit/Paper plugin runtimes. A hybrid server that embeds Bukkit may load some plugins, but EggHolder is not tested or supported on those hybrids. |

Paper and Purpur are the recommended supported choices. Leaf, Gale, and Pufferfish are now explicitly included as best-effort Paper-fork compatible software, but EggHolder has not been run on them. Plain Spigot is not yet supported because this build uses Paper-only APIs; the required compatibility port and live tests have not been done. CI compiles against Paper API 1.20.6 and 1.21.11; this is API compilation coverage, not live-server runtime testing. Optional newer gear adapts on older supported versions: Netherite Spears fall back to tridents, Maces fall back to Netherite axes if unavailable, and unknown enchantments are skipped.

## Managed End Map

This plugin can now manage its own custom `world_the_end` from a bundled schematic.

That means the jar can:

- ship with the End island map inside the plugin itself
- create or load the configured End world automatically
- paste the bundled map into that world on startup
- use the map center as the event center
- detect crystal tower pedestals from the schematic layout
- reuse those tower anchors for dragon crystals and optional event objectives

The bundled schematic is:

- `maps/ScarworldEndIsland.schem`

Important operator note:

- the managed End system is controlled in `config.yml` under `managed-end-world`
- if enabled, the plugin treats the configured End as an event map world rather than a normal vanilla End
- startup preparation edits the configured world; with `clear-region-first: true`, blocks inside the schematic bounds are cleared before the map is pasted
- set `prepare-on-startup: false` to defer preparation until the match starts, and set `clear-region-first: false` to overlay the schematic without clearing its bounds
- admins can rebuild or inspect the managed map in-game with `/endwar world ...`

## Version 2.2.3

- Replaced the generic falling supply-drop display with the nine-part BDEngine Airdrop model by Yirf; the visual remains after landing and is removed with the cache.
- Bundled the display model locally, with per-drop entity tracking so movement and cleanup are limited to that drop.
- Documented the model attribution and BDL BY-NC terms.

## Version 2.2.2

- Fixed Paper 1.21.11 scoreboard exceptions caused by copying named team colors into player-specific sidebars; colored team prefixes remain visible.
- Fixed legacy `minecraft:chain` block states in the bundled End schematic by mapping them to `minecraft:iron_chain` on modern servers.
- Consolidated push CI and GitHub Packages publishing into one workflow. The package publishes only after both Paper API compatibility builds pass, and its CI version follows the release version in `pom.xml`.

## Version 2.2.0

- Fixed routine managed-map preparation so `clear-region-first` is respected; `/endwar world prepare` remains an explicit clean rebuild.
- Audience vote close/resolve commands now report when no vote is open.
- EggHolder assignment uses exact online player names to avoid selecting the wrong target from an ambiguous partial name.
- Command usage messages now show the required player and team arguments more clearly.
- Eliminated players can no longer damage living players with melee or player-fired projectiles, or trigger actions through stale kit/team/audience menus.
- Cancelled block events no longer enter or corrupt the bridge-collapse tracker, and extracted schematic paths are constrained to the plugin data folder.

## Main Game Loop

### Pre-game

Players join the server and receive:

- a `Team Menu` item
- a `Hunter Kits` item

Before the match starts, players can:

- create teams
- invite players
- join teams
- select a kit

If a player never joins a team, the plugin can still place them into a solo team when the event starts.

### Match Start

When an admin runs:

```text
/start
```

the plugin:

- spawns a large End portal structure at the configured overworld center
- prepares End spawn platforms for teams
- scatters teams around the configured End center
- locks building and combat briefly after entry
- starts the event phase systems

### EggHolder Phase

Once somebody gets the Dragon Egg:

- they automatically become the `EggHolder`
- everyone gets chat and title announcements
- a global bossbar tracks their live coordinates
- their name/tag/tab styling changes
- they receive event gear and effects
- an item display appears above their head

If the EggHolder dies, logs out too long, or loses the egg, the plugin reacts automatically based on config.

### Death / Spectator Phase

When a normal player dies:

- they enter a dead/spectator-style event state
- they are hidden from living players
- they can fly
- they remain in the tab list with italic styling
- they receive a `Recovery Compass` teleporter
- they can teleport to alive players using a GUI

Dead players can also participate through `Audience Tools` when enabled.

### Late Game Pressure

As the match continues, the plugin can intensify the event with:

- `Overtime`
- `Mercyless Endgame`
- `No Escape Timer`
- `Panic Phase`
- `Bridge Collapse`
- `End Storm`
- `Hotspot Rotation`
- `Egg Burn Timer`

This is designed to stop slow, boring endings and force action.

## Core Systems

### 1. EggHolder System

The EggHolder is the current player carrying the Dragon Egg.

By default, the plugin can auto-detect the holder by inventory scanning. Manual assignment also exists for admin control.

When a player becomes EggHolder, the plugin can:

- announce them globally in chat
- announce them with titles
- show a live bossbar
- give them special weapons
- apply potion effects
- glow them with a colored team
- change tab and nametag prefix
- show a Dragon Egg or other configured item above their head

### 2. Bossbar Tracking

The bossbar is shown globally and updates in real time.

Default format:

```text
EGGHOLDER <Player> X: <x> Y: <y> Z: <z>
```

Configurable:

- enabled/disabled
- color
- style
- update speed
- text format
- hidden coordinate text

### 3. EggHolder Gear

By default, the holder receives:

- `Netherite Spear`
- `Mace`

Everything about these items is editable:

- enable/disable
- item material
- amount
- slot
- name
- lore
- enchantments
- enchant levels
- unbreakable state

### 4. Death State

Dead players are not fully removed from the match experience.

Instead, they become a ghost/audience role with:

- flight
- vanish from alive players
- no interaction with normal gameplay
- italic list name
- teleporter compass GUI
- optional admin revival

If a dead player is revived:

- they are restored to normal player state
- their spectator protections are cleared
- they can take damage again
- void deaths in the End can revive at a safe platform instead of instantly dying again

### 5. Teams

The plugin includes a full team system.

Players can:

- create teams
- invite players
- accept invites
- rename teams
- leave teams
- kick members
- disband teams
- use the GUI menu instead of commands

Team features:

- configurable max team size
- automatic unique team names
- leader rename support with `/team rename <name>`
- unique hex color per team tag
- unique personal color per player name inside the team display
- team prefix formatting
- team kill tracking
- scoreboard integration

By default, friendly fire is disabled between teammates.

Important gameplay note:

- teams lock once the active match is running

This prevents last-second abuse during live combat.

### 6. Hunter Kits

Players can choose a hunter kit before fighting.

Default included kits:

- `Duelist`
- `Titan`
- `Scout`
- `Saboteur`

Every kit is defined in `kits.yml`, including:

- icon
- description
- potion effects
- items
- enchantments
- hotbar slot placement

Players can open the kit GUI using the provided menu item.

Important gameplay note:

- kits are meant to be chosen before real fighting starts
- kit switching is blocked once a living player is actively inside the End during a running match

### 7. Sidebar Scoreboard

The plugin supports a configurable sidebar scoreboard in `scoreboard.yml`.

Default sidebar information includes:

- match phase
- current EggHolder
- player kills
- kill streak
- team name
- team kills
- online teammate list
- teammate health

This is fully editable through scoreboard lines.

### 8. Audience Tools

Dead players can still influence the match through timed audience voting.

Admins can also control this live.

Default audience effects include:

- instant storm burst
- forced supply drop
- forced hotspot rotation
- holder reveal
- holder heal burst

### 9. Match Pressure / Competitive Features

The plugin includes many optional pressure systems.

### End Storm

Creates periodic End chaos with lightning and harmful pressure on players in the End.

### Anti-Camp Detector

Punishes an EggHolder who stays in the same area too long.

### Egg Drop Countdown

When the egg drops, pickup can be delayed briefly so the drop becomes a contested moment.

### Clutch Steal Bonus

The player who grabs the dropped egg can receive a short buff burst.

### Overtime Win Condition

After enough time passes, the EggHolder starts taking ongoing pressure damage so the round cannot stall forever.

### Revenge Mark

If someone kills the EggHolder, they can be publicly marked as the next visible target.

### Center No-Build Ring

Prevents building near the configured center objective area.

### Egg Burn Timer

If a dropped egg sits untouched too long, it can burn out and respawn at center.

### Kill Confirm Reward

Killing the EggHolder can grant effects and optional commands/rewards.

### Combat Momentum Buff

Damaging the holder can give hunters short pressure buffs.

### Egg Shockwave

Picking up the egg can trigger a knockback shockwave around the new holder.

### Hotspot Rotation

A temporary buff zone appears and rotates around the End to force movement and map control.

### End Crystal Objectives

Teams can capture End crystal points for reward effects.

### Panic Phase

Triggers when the EggHolder drops below a configured health threshold.

### Bridge Collapse Event

Player-built End bridges outside the core zone can start collapsing in waves.

### Anti-Clean Mechanic

The killer of the EggHolder can receive brief protection against instant cleanup.

### Mercyless Endgame

A late phase where revives are disabled and the event becomes final.

### No Escape Timer

If the holder keeps the egg long enough, escape tools like pearls and wind charges can be blocked.

### Supply Drops

The plugin can call in public loot drops from the sky.

Default implementation:

- a falling, multi-part Block Display airdrop model
- publicly announced coordinates
- protected until landing
- configurable loot table

The bundled airdrop appearance is **Airdrop** by **Yirf**, model 8073 on [Block Display Models](https://block-display.com/bd/8073). It is adapted from the model's published display transforms and distributed under **BDL BY-NC** (attribution required, non-commercial use, derivatives allowed). The model license applies to this asset; it does not change the license of EggHolder itself.

## Commands

### Admin Commands

### `/eggholder add <player>`

Manually assigns a player as the EggHolder.

### `/eggholder remove <player>`

Removes EggHolder status and cleans up effects, bossbar, display, and role items.

### `/eggholder reload`

Reloads:

- `config.yml`
- `messages.yml`
- `kits.yml`
- `scoreboard.yml`

without restarting the server.

### `/revive <player>`

Revives a dead player.

If `Mercyless Endgame` is active, revives are blocked.

### `/start`

Starts the event flow and opens the overworld portal.

### `/endwar start`

Alternate admin start command.

### `/endwar stop`

Stops the match and cleans up runtime systems.

### `/endwar status`

Shows the current match phase, EggHolder, elapsed match time, enabled event features, and scheduled or active event timers.

### `/endwar feature <feature> <on|off|trigger>`

Live real-time control for event features.

Examples:

```text
/endwar feature end-storm on
/endwar feature supply-drops trigger
/endwar feature anti-camp-detector off
```

### `/endwar audience <open|close|resolve>`

Opens, closes, or resolves audience voting in real time.

### `/endwar world prepare`

Rebuilds the managed End map from the bundled schematic and refreshes its crystal anchors.

Use this while the match is stopped if you want a clean event map again.

### `/endwar world status`

Shows the current managed End world status, center point, and detected crystal anchor count.

### Player Commands

### `/team create [name]`

Creates a team.

### `/team invite <player>`

Invites a player to your team.

### `/team accept <player|team>`

Accepts a team invite.

### `/team leave`

Leaves your team.

### `/team kick <player>`

Kicks a player from your team if you are the leader.

### `/team disband`

Disbands your team if you are the leader.

### `/team menu`

Opens the GUI team menu.

## Configuration Files

This plugin is intentionally very editable.

### Text colors and formatting

Text in `messages.yml`, `config.yml`, `kits.yml`, and `scoreboard.yml` accepts legacy `&` color/style codes, section-sign codes, `&#RRGGBB` and `#RRGGBB` hex colors, and extended legacy RGB codes such as `&x&F&F&0&0&A&A`. Adventure MiniMessage tags are also supported, including named colors, `<#RRGGBB>`, gradients such as `<gradient:#8B5CF6:#22D3EE>text</gradient>`, bold, italic, underline, strikethrough, obfuscation, reset, and standard click/hover tags in component-rendered text. Placeholders such as `%player%` remain supported.

### `config.yml`

Main gameplay configuration.

This file controls:

- bossbar settings
- EggHolder visuals
- holder gear
- holder effects
- death state
- teleporter settings
- team settings
- match start settings
- feature toggles
- feature timings
- audience vote behavior
- supply drop loot

On startup and `/eggholder reload`, EggHolder validates feature switches, known timing values, materials, and potion effects. Invalid feature switches are reset to their documented defaults in memory, timing values are clamped to safe bounds, and item/effect loaders use their fallback behavior. Validation warnings are written to the server console. Validation does not rewrite the YAML files.

### `messages.yml`

All visible text shown to players.

This includes:

- prefix
- command feedback
- event announcements
- titles
- subtitles
- kill and egg messages
- admin control messages

If you want to translate or re-theme the plugin, this is the first file to edit.

### `kits.yml`

Defines all hunter kits.

Each kit can customize:

- name
- icon
- description
- potion effects
- items
- enchantments
- slots

### `scoreboard.yml`

Defines the sidebar scoreboard layout.

You can fully change:

- scoreboard title
- each line
- what information is visible

## Recommended Admin Flow

If you are hosting the event, a simple workflow is:

1. Configure `config.yml`, `messages.yml`, `kits.yml`, and `scoreboard.yml`.
2. If you use the bundled custom End map, verify it with `/endwar world status`.
3. Start the server and let players join.
4. Tell players to choose teams and kits.
5. Run `/start`.
6. Let teams enter the overworld portal.
7. Monitor the event using:
   - `/endwar feature ...`
   - `/endwar audience ...`
   - `/endwar world status`
   - `/revive <player>`
   - `/eggholder reload`

## Recommended Player Flow

If you are a participant:

1. Join the server.
2. Open the Team Menu and choose your team.
3. Open the Kit Menu and choose your hunter kit.
4. Wait for the admin to start the match.
5. Enter the End portal.
6. Fight for the Dragon Egg.
7. Hunt the EggHolder or defend your teammate if they become the holder.
8. If you die, use ghost mode and teleporter tools until revived or until the event ends.

## Build From Source

```bash
mvn clean package
```

This also runs the automated resource and configuration contract tests. To run the tests without packaging:

```bash
mvn test
```

The final jar will be created at:

```text
target/EggHolder.jar
```

## GitHub Packages (Maven)

The Maven artifact is published to [GitHub Packages](https://github.com/TasfiqSami/EggHolder/packages) for developers who want to consume EggHolder as a Maven dependency. A push runs one workflow: it builds against Paper API 1.20.6 and 1.21.11, then publishes a unique CI package only after both builds pass. Pull requests run both builds but do not publish. CI versions follow the release version in `pom.xml`, for example `2.2.3-ci.42.1`. To publish a stable Maven version, run **EggHolder CI and Package** from GitHub Actions using **Run workflow** and enter the release version (for example `2.2.3`); leave it blank to publish a unique CI version.

Coordinates for the current release:

```xml
<dependency>
    <groupId>me.tasfiq</groupId>
    <artifactId>eggholder</artifactId>
    <version>2.2.3</version>
</dependency>
```

Repository URL: `https://maven.pkg.github.com/TasfiqSami/EggHolder`.

GitHub Packages requires Maven authentication to download artifacts. Create a GitHub Personal Access Token (classic) with `read:packages`, then add it to your local Maven `~/.m2/settings.xml` (do not commit this file or token):

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_TOKEN</password>
    </server>
  </servers>
</settings>
```

The server ID must match the repository ID `github`. GitHub's Maven registry requires authentication for package downloads; see [GitHub's Maven registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

For a Minecraft server installation, download `EggHolder.jar` from GitHub Releases instead.

## Install On Server

1. Build the jar or use the packaged one.
2. Place `EggHolder.jar` into your server `plugins/` folder.
3. Start the server once so the config files generate.
4. Edit the YAML files if needed.
5. Restart the server or run `/eggholder reload`.

## Notes For Server Owners

- This plugin is designed for competitive event play, not vanilla survival balance.
- Most behavior is configurable, but bad configs can still create unfair matches.
- If you plan to host large events, test your chosen settings on a staging server first.
- The plugin is built around Paper APIs and should not be treated as a generic Bukkit/Spigot drop-in.

## Quick Glossary

### EggHolder

The current player holding the Dragon Egg.

### End War

An End-based PvP event centered around control of the Dragon Egg.

### Dead State

The plugin's spectator/ghost mode for eliminated players.

### Audience Tools

Voting powers available to dead players when enabled.

### Hotspot

A temporary buff zone in the End.

### Merciless Endgame

Late match phase where revives stop and the event becomes final.

## Summary

If you only remember one thing, remember this:

`EggHolder` is a full event-control plugin for running a Dragon Egg PvP war in the End, with automatic tracking, teams, kits, dead-player audience tools, live admin controls, and strong late-game pressure systems.
