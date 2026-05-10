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

- `Paper 1.21.11`
- `Java 21`
- Maven only if you want to build from source

The plugin uses newer API content such as `NETHERITE_SPEAR` and `LUNGE`, so it is meant for a modern Paper server.

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
- admins can rebuild or inspect the managed map in-game with `/endwar world ...`

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

- a falling Ender Chest style drop
- publicly announced coordinates
- protected until landing
- configurable loot table

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

The final jar will be created at:

```text
target/EggHolder.jar
```

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
