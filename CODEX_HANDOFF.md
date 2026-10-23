# EggHolder — Codex Handoff and Project Memory

This is the continuation note for future Codex sessions. Read **Current status** and **Next tasks** first. It records the user’s goals, workflow, release setup, verified compatibility claims, and important credential-handling preference. It contains no credential value.

## Current status (2026-10-23)

- Repository: `https://github.com/TasfiqSami/EggHolder`
- Workspace: `D:\Tasfiq\EggHolder\EggHolder`
- Main branch; remote `origin` points to the repository above.
- Latest published GitHub release: `v2.2.0` (`v2.1.0` and `v2.0.0` remain available).
- Release URL: `https://github.com/TasfiqSami/EggHolder/releases/tag/v2.2.0`.
- Release asset: `EggHolder.jar`, 287,397 bytes, SHA-256 `3F292DA20FE65461B2E22B1C646ADCEA9B973C3D994B0351BD0E7561F688B049`.
- Release tag points to commit `35c79511403cf3cd56fdbfb52f3ec43c30b87ef1` on `main`.
- Modrinth project already exists: title EggHolder, slug `eggholder`, ID `jm0TqH0V`; intended plugin route `https://modrinth.com/plugin/eggholder`. Do not create a duplicate project.
- Last known Modrinth state from prior check: project was `processing` with approval requested; version 2.1.0 (`laOokhbA`) was listed. Recheck status before any future upload or page edits.
- The user asked to preserve the encrypted Modrinth token file. **Do not delete** `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`; never print, paste, commit, or put a token in this file. Do not re-use the old token that was pasted into chat.
- No live Minecraft server was started for this work. Automated build/resource tests are the available verification. Do not claim full runtime verification.

## Latest work in progress — v2.2.0

The v2.2.0 work is pushed and released:

- Managed End startup and lazy preparation now call `prepareWorld(false)`, so routine map preparation respects `managed-end-world.clear-region-first`; explicit `/endwar world prepare` still forces a clean rebuild.
- Added prominent config and README notes explaining that startup preparation edits the configured End world, and that clearing removes blocks inside schematic bounds. `prepare-on-startup: false` defers this work until `/start`; `clear-region-first: false` overlays the schematic during routine prep.
- `/endwar audience close|resolve` now reports accurately when no vote is open. `EndWarService.closeAudienceVote` returns success/failure.
- `/eggholder add` now requires an exact online player name instead of Bukkit’s ambiguous partial match. Tab completion supplies online candidates.
- Improved `/eggholder` and `/team` usage text.
- Dead players can no longer deal melee or player-fired projectile damage; kit/team/audience/teleport GUI handlers now reject dead or mismatched viewers.
- Cancelled block placement/break events no longer update bridge-collapse tracking.
- Extracted schematic paths are normalized and constrained to the plugin data directory, preventing accidental `../` path escape.
- Version set to `2.2.0` in `pom.xml` and `plugin.yml`.
- Final verification passed after the security changes: `mvn -B clean verify` with Paper API 1.20.6 and `mvn -B clean -Dpaper.version=1.21.11-R0.1-SNAPSHOT verify`; all 4 automated tests passed at both endpoints.
- CI run `40020460030` and Maven package run `40020460048` both succeeded for commit `35c7951`.
- GitHub release asset was uploaded and its GitHub SHA-256 digest matched the local jar hash above.

## Next tasks — important

1. If continuing Modrinth work, check moderation/listing for project `jm0TqH0V`, and update its description or upload v2.2.0 only when review status and user scope allow it. The latest request asked for GitHub release; no v2.2.0 Modrinth upload was made.
2. A live Paper runtime smoke test of command behavior/security remains unperformed. The user earlier asked for source fixes without a real plugin test; do not describe compile/API checks as runtime validation.
3. On future changes, review GitHub push CI and Maven package workflows, update README and this handoff, and publish a new release only when requested.
4. Continue reporting concrete verification limits; the source-level security review does not establish that every third-party plugin combination or server runtime is vulnerability-free.

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
- `.github/workflows/maven.yml` tests Paper API 1.20.6 and 1.21.11 using Java 21.
- `.github/workflows/maven-publish.yml` runs on pushes and publishes unique `2.2.0-ci.<run_number>.<attempt>` versions using Actions `GITHUB_TOKEN` and `packages: write`. It should not need the Modrinth token.
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
