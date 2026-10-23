# EggHolder — Codex Handoff and Project Memory

This is the continuation note for future Codex sessions. Read **Current status** and **Next tasks** first. It records the user’s goals, workflow, release setup, verified compatibility claims, and important credential-handling preference. It contains no credential value.

## Current status (2026-10-23)

- Repository: `https://github.com/TasfiqSami/EggHolder`
- Workspace: `D:\Tasfiq\EggHolder\EggHolder`
- Main branch; remote `origin` points to the repository above.
- Latest published GitHub release before this work: `v2.1.0` (`v2.0.0` also exists).
- Current work prepares `2.2.0`; release must only be marked complete after the commit is pushed, the release is created, and its attached jar is verified.
- Modrinth project already exists: title EggHolder, slug `eggholder`, ID `jm0TqH0V`; intended plugin route `https://modrinth.com/plugin/eggholder`. Do not create a duplicate project.
- Last known Modrinth state from prior check: project was `processing` with approval requested; version 2.1.0 (`laOokhbA`) was listed. Recheck status before any future upload or page edits.
- The user asked to preserve the encrypted Modrinth token file. **Do not delete** `%LOCALAPPDATA%\Modrinth\eggholder-token.dpapi`; never print, paste, commit, or put a token in this file. Do not re-use the old token that was pasted into chat.
- No live Minecraft server was started for this work. Automated build/resource tests are the available verification. Do not claim full runtime verification.

## Latest work in progress — v2.2.0

The current changes are not yet pushed/released unless this section is updated after publication:

- Managed End startup and lazy preparation now call `prepareWorld(false)`, so routine map preparation respects `managed-end-world.clear-region-first`; explicit `/endwar world prepare` still forces a clean rebuild.
- Added prominent config and README notes explaining that startup preparation edits the configured End world, and that clearing removes blocks inside schematic bounds. `prepare-on-startup: false` defers this work until `/start`; `clear-region-first: false` overlays the schematic during routine prep.
- `/endwar audience close|resolve` now reports accurately when no vote is open. `EndWarService.closeAudienceVote` returns success/failure.
- `/eggholder add` now requires an exact online player name instead of Bukkit’s ambiguous partial match. Tab completion supplies online candidates.
- Improved `/eggholder` and `/team` usage text.
- Version set to `2.2.0` in `pom.xml` and `plugin.yml`.
- Final verification required: Maven `clean verify` against Paper API 1.20.6 and 1.21.11 after the final code edits. These are compile/API/resource checks, not live-server command tests.
- Release artifact should be `target/EggHolder.jar`, uploaded as the asset for GitHub release `v2.2.0` after push.

## Next tasks — important

1. Finish both endpoint builds and ensure all 4 automated resource/config tests pass at each endpoint.
2. Review final diff and working tree; ensure no token or private credential entered files or logs.
3. Update this handoff with verified test results, commit SHA, release URL, release asset SHA-256, workflow run links, and any remaining limitations.
4. Commit and push the finished changes to `main`. The push should trigger CI and the GitHub Packages Maven deployment workflow automatically. Verify both workflows complete successfully.
5. Create GitHub release `v2.2.0`, attach the final `target/EggHolder.jar`, and verify the release asset/download and hash.
6. Do not claim every command “works in game” solely based on compilation. The source has been reviewed and commands compile; a Paper runtime smoke test remains unperformed because no live plugin test was requested/performed.
7. Recheck Modrinth moderation/listing when continuing that separate publication task. The current user specifically requested release in GitHub; do not upload a new Modrinth version unless the user again asks or clearly includes it.

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
