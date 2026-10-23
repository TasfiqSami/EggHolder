# EggHolder — Codex Handoff and Project Memory

This is the continuation note for future Codex sessions. Read **Current status** and **Next tasks** first. It records the user’s goals, workflow, release setup, verified compatibility claims, and important credential-handling preference. It contains no credential value.

## Current status (2026-10-23)

- Repository: `https://github.com/TasfiqSami/EggHolder`
- Workspace: `D:\Tasfiq\EggHolder\EggHolder`
- Main branch; remote `origin` points to the repository above.
- Latest published GitHub release before this work: `v2.2.0`; this session is preparing `v2.2.2` after runtime testing found a release-blocking exception. Do not describe v2.2.2 as published until verified below.
- Modrinth project already exists: title EggHolder, slug `eggholder`, ID `jm0TqH0V`; intended plugin route `https://modrinth.com/plugin/eggholder`. Do not create a duplicate project.
- Last known Modrinth state from prior check: project was `processing` with approval requested; version 2.1.0 (`laOokhbA`) was listed. Recheck status before any future upload or page edits.
- The user asked to preserve the encrypted Modrinth token file. **Do not delete** `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`; never print, paste, commit, or put a token in this file. Do not re-use the old token that was pasted into chat.
- Paper 1.21.11 runtime smoke test found and reproduced `Team colors must have hex values` exceptions in `SidebarService.syncTeamsFromMain`, breaking lobby sidebar refresh, `/endwar start`, and `/endwar stop`. Fix underway: do not copy the Bukkit team color into per-player sidebars; their prefixes still carry the configured color.
- The same server smoke test confirmed managed End startup and 10 crystal anchors, `/eggholder reload`, `/endwar world status`, feature on/off and invalid-feature handling, EggHolder add/remove invalid-target handling, revive invalid-target handling, audience open/close/resolve, and interactive team create/invite/accept/rename/kick/disband/leave/menu flows.
- Two local offline-mode Mineflayer bots connected to an isolated localhost Paper server for command/GUI checks. Do not extrapolate this into a full match or combat-balance test.

## Latest work in progress — v2.2.2

The v2.2.0 work is pushed and released. v2.2.2 addresses runtime issues found by launching a real Paper 1.21.11 server:

- Removed copying of named Bukkit team colors into each player's scoreboard. Paper 1.21.11 requires hex team colors there; colored prefixes remain intact, avoiding exceptions during sidebar refresh and event start/stop.
- Map legacy `minecraft:chain` schematic states to `minecraft:iron_chain` for modern server parsing.
- Consolidated the prior two GitHub workflow definitions into one CI/package workflow. It tests both API endpoints, publishes packages only on pushes after tests pass, and derives CI package versions from the current Maven revision. This should make one workflow run per push (CI shows two matrix jobs and one dependent publish job).
- Added README release notes and updated Maven package docs to version 2.2.2.
- Runtime testing found the bug before publishing. Rebuild the jar after fixes, rerun tests and Paper smoke tests, then update this handoff with verified release URLs/digests/workflow status.

## Next tasks — important

1. Finish v2.2.2 two-endpoint Maven verification and a clean Paper 1.21.11 restart/match start/stop check. Confirm no new plugin exceptions in latest.log.
2. Publish GitHub release `v2.2.2` and attach `target/EggHolder.jar`; verify asset digest and release tag.
3. Push the consolidated workflow, confirm exactly one workflow run per push, and verify its Maven package publish job succeeded with a version derived from 2.2.2.
4. Check the existing Modrinth project `jm0TqH0V` moderation state. If it accepts a new file while preserving the existing project and no credential needs to be exposed, upload the same release jar and record the version. Keep the encrypted token file untouched.
5. Update this handoff with results, release links and any remaining limits. Full match balance, Folia, plain Spigot, and every third-party fork are not covered by the localhost Paper smoke test.

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
- `.github/workflows/maven.yml` is being consolidated into the only workflow: tests Paper API 1.20.6 and 1.21.11 using Java 21, then publishes Maven package on push after checks pass.
- CI package version is based on `<revision>` from `pom.xml`, suffixed with `-ci.<run_number>.<attempt>`, using Actions `GITHUB_TOKEN` and `packages: write`. It should not need the Modrinth token.
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
