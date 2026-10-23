# EggHolder — Codex Handoff and Project Memory

This is the continuation note for future Codex sessions. Read **Current status** and **Next tasks** first. It records the user’s goals, workflow, release setup, verified compatibility claims, and important credential-handling preference. It contains no credential value.

## Current status (2026-10-23)

- Repository: `https://github.com/TasfiqSami/EggHolder`
- Workspace: `D:\Tasfiq\EggHolder\EggHolder`
- Main branch; remote `origin` points to the repository above.
- Latest GitHub release: [`v2.2.2`](https://github.com/TasfiqSami/EggHolder/releases/tag/v2.2.2).
- Release asset: [`EggHolder.jar`](https://github.com/TasfiqSami/EggHolder/releases/download/v2.2.2/EggHolder.jar), 287,562 bytes, SHA-256 `F3B1925FA09722CA164E642A7D0FEF1D5E25B304CA0619A82A764345C88BF2F3`.
- Release tag points at commit `bb7d6cb5d6b4400bf002e28ab3fa89aecf0e36f8` on `main` (later documentation pushes may advance main; the release tag remains the source of this jar).
- Modrinth project already exists: title EggHolder, slug `eggholder`, ID `jm0TqH0V`; intended plugin route `https://modrinth.com/plugin/eggholder`. Do not create a duplicate project.
- Latest Modrinth state: project is still `processing` with approval requested; v2.2.2 (`KeGPGUrf`) is uploaded/listed beside 2.1.0 and 2.0.0. Public search/listing visibility is unconfirmed until project approval.
- Modrinth direct v2.2.2 file: `https://cdn.modrinth.com/data/jm0TqH0V/versions/KeGPGUrf/EggHolder.jar`.
- The user asked to preserve the encrypted Modrinth token file. **Do not delete** `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`; never print, paste, commit, or put a token in this file. Do not re-use the old token that was pasted into chat.
- Fixed Paper 1.21.11 `Team colors must have hex values` exceptions in `SidebarService.syncTeamsFromMain` by no longer copying team colors into per-player sidebars; configured colored prefixes remain intact.
- The same server smoke test confirmed managed End startup and 10 crystal anchors, `/eggholder reload`, `/endwar world status`, feature on/off and invalid-feature handling, EggHolder add/remove invalid-target handling, revive invalid-target handling, audience open/close/resolve, and interactive team create/invite/accept/rename/kick/disband/leave/menu flows.
- Two local offline-mode Mineflayer bots connected to an isolated localhost Paper server for command/GUI checks. `/endwar start`, duplicate start rejection, `/endwar stop`, and managed-world status succeeded after the fix. Latest Paper log had zero EggHolder warnings/errors, command exceptions, or failed event handlers. This is not a full match or combat-balance test.

## Latest work in progress — v2.2.2

v2.2.2 is built, runtime-checked, pushed, and released. It addresses issues found by launching a real Paper 1.21.11 server:

- Removed copying of named Bukkit team colors into each player's scoreboard. Paper 1.21.11 requires hex team colors there; colored prefixes remain intact, avoiding exceptions during sidebar refresh and event start/stop.
- Map legacy `minecraft:chain` schematic states to `minecraft:iron_chain` for modern server parsing.
- Consolidated the prior two GitHub workflow definitions into one CI/package workflow. It tests both API endpoints, publishes packages only on pushes after tests pass, and derives CI package versions from the current Maven revision. This should make one workflow run per push (CI shows two matrix jobs and one dependent publish job).
- Added README release notes and updated Maven package docs to version 2.2.2.
- Final `mvn -B clean verify` on Paper API 1.20.6 and `mvn -B clean -Dpaper.version=1.21.11-R0.1-SNAPSHOT verify` passed; both ran all 5 contract tests. The 1.21.11 endpoint jar was attached to the release.
- Isolated Paper 1.21.11 v2.2.2 startup prepared the bundled map with 10 crystal anchors and no unsupported schematic warning. Bot-driven team workflows and audience controls succeeded. Match start, duplicate start response, stop, and world status succeeded without EggHolder errors.
- GitHub release asset digest matched local SHA-256 `F3B1925FA09722CA164E642A7D0FEF1D5E25B304CA0619A82A764345C88BF2F3`.
- GitHub workflow run `40022133749` passed both API matrix builds and the GitHub Packages publish job.
- Modrinth version `2.2.2` ID `KeGPGUrf` uploaded to existing project `jm0TqH0V`; Paper/Purpur metadata for 1.20.6–1.21.11, `EggHolder.jar`, 287,562 bytes. Project review remains processing. Modrinth Modify Project API does not expose `project_type`, so type/route correction is unresolved; do not delete/recreate based on a guess. Seek dashboard/support guidance after review.

## Next tasks — important

1. Check whether Modrinth approved project `jm0TqH0V`. If still processing, use dashboard/support to request the public plugin classification/route. v2.2.2 is already uploaded; do not duplicate it.
2. Future code pushes should use the consolidated one-workflow CI/package action; latest run `40022133749` passed all jobs and published Maven package.
3. Full competitive match balance, death/revive interactions, Paper API 1.20.6 live runtime, Spigot runtime, and Folia are not covered by the localhost Paper 1.21.11 smoke test.
4. Preserve `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`; never put credentials in the repository or Markdown.

## User goals and usual requests

The user’s recurring EggHolder work includes:

- Find and fix gameplay bugs, errors, edge cases, and command problems.
- Improve the plugin and modernize its configuration while keeping operator behavior clear.
- Expand server software and Minecraft version support where feasible, and state unsupported software honestly.
- Build/check the plugin without relying on a live server when the user asks for source/static fixes.
- Publish finished jars in GitHub Releases and, when requested, update the existing Modrinth plugin listing.
- Configure GitHub Actions so pushes run CI and update/publish GitHub Packages.
- Update `README.md` and this handoff with what changed and what remains.

Communication preference: execute authorized work instead of repeatedly asking for approval; be clear about limits and outstanding work. Never expose or delete the encrypted Modrinth token file. A previous message says “don’t delete this token”; it does not authorize placing the token in repository files or output.

## Compatibility claims

- Java 21.
- CI/API compilation endpoints: Paper API 1.20.6 and 1.21.11.
- Paper is the primary target. Purpur and other Paper API forks (including Pufferfish, Leaf, Gale where versions are available) are expected to work but have not had live runtime testing.
- Plain Spigot is not currently a supported target where the source requires Paper APIs. Folia is unsupported because the plugin uses the normal Bukkit scheduler and is not region-thread safe.
- Forge, NeoForge, Fabric, and Quilt do not directly load Bukkit/Paper plugins.
- Don’t broaden these claims without implementing and testing the relevant target.

## GitHub automation and releases

- GitHub repository: `TasfiqSami/EggHolder`.
- GitHub Releases known before v2.2.0: `v2.1.0` and `v2.0.0`.
- GitHub Packages Maven coordinates: `me.tasfiq:eggholder`; repository `https://maven.pkg.github.com/TasfiqSami/EggHolder`.
- `.github/workflows/maven.yml` is the only workflow: tests Paper API 1.20.6 and 1.21.11 using Java 21, then publishes Maven package on push after checks pass. Run `40022133749` passed two matrix builds and publish.
- CI package version is based on `<revision>` from `pom.xml`, suffixed with `-ci.<run_number>.<attempt>`, using Actions `GITHUB_TOKEN` and `packages: write`. It does not need the Modrinth token.
- An optional dependency graph submission is allowed to fail because GitHub returned 403 on it previously; this is separate from Maven CI/package publication.
- Always inspect `git status`, branch, and current remote before publishing. Check for an already-existing release/tag to avoid duplicates.

## Modrinth project and credential handling

- Existing project: `jm0TqH0V`, slug `eggholder`, expected public route `https://modrinth.com/plugin/eggholder`.
- Version 2.0.0 ID: `3KmzUZKb`; version 2.1.0 ID: `laOokhbA`.
- Last known 2.1.0 metadata advertised Paper/Purpur and Minecraft 1.20.6 through 1.21.11; uploaded artifact hash was verified in that session.
- Last known review status was project `processing`, approval requested. Project page/API content edits previously failed during review. Check current state before updates.
- Encrypted token file: `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`. Preserve it. Never echo decrypted content or persist it anywhere. A token was previously pasted in chat; do not copy it. Use only an authorized local credential mechanism if Modrinth publishing is later requested.

## Project background / previous fixes

- EggHolder is a Paper End PvP event plugin built around the Dragon Egg carrier. Features include teams, kits, live tracking, dead-player spectator tools, admin controls, objectives, audience votes, supply drops, and configurable late-game pressure.
- Managed End schematic resource: `maps/ScarworldEndIsland.schem`.
- Prior compatibility release lowered build API baseline to Paper 1.20.6 and added material fallbacks for optional newer spear/mace gear; unknown enchantments are skipped.
- Prior bug-fix pass (`402b088`): only mark deaths during an active match, safely add Dragon Eggs to inventory before removing their block, ignore cancelled/dead-player pickup effects, and reset anti-camp anchor when the holder changes worlds.
- Earlier command/distribution improvements added team/help tab completions, GitHub Releases and Maven package workflows, plus Modrinth metadata for Paper/Purpur. Consult current `README.md` and Git history for precise implementation details.

## Build and checks

Use Java 21. In the previous environment it was available at `%TEMP%\temurin-21\jdk-21.0.12.1+1`; that temporary path may be gone in a future session.

```powershell
mvn -B clean verify
mvn -B clean '-Dpaper.version=1.21.11-R0.1-SNAPSHOT' verify
```

The normal jar is `target/EggHolder.jar`. Do not run a live server test unless that becomes an explicit part of the task. Clearly distinguish static review, Maven tests/builds, CI API compatibility, and actual runtime compatibility.

## Memory maintenance

After substantive work, update this document’s release state, test results, commits, workflow status, and the prioritized next-task checklist. Keep the Modrinth credential out of Markdown and preserve the encrypted local file.
