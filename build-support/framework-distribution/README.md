# KOIKI fixed distribution tooling (E1 preview)

This tooling builds and prepares a complete, fixed KOIKI version. It does not
create repositories, tags, releases, or upload assets. The initial proposed
version is `0.1.0-preview.1`; it is not the Phase 5 production/support release.
All modules share one version. Do not mix modules from different distributions.

## Prepare a development PC or CI runner

Requirements: PowerShell 7, JDK 21, Maven 3.9.16, GitHub CLI for remote downloads,
and access to the normal Maven repositories for third-party dependencies/plugins.
Neither this ZIP nor its preparation script installs those tools. Only KOIKI
artifacts are bundled: 14 POMs and 12 JARs, plus manifest and installation guidance.

Use a reviewed copy of `prepare-koiki.ps1` pinned to a distribution repository
commit, and the ZIP SHA-256 recorded with the selected version. Repository name
and the following version are examples, not currently published coordinates.

```powershell
./prepare-koiki.ps1 -Version 0.1.0-preview.1 `
  -Repository OWNER/koiki-artifacts -ExpectedSha256 <pinned-64-character-hash>
```

The script downloads exactly `framework-<version>` /
`koiki-framework-<version>.zip` using `gh release download`, checks the ZIP and
all payload hashes, then installs the supplied POMs/JARs with Maven. No manual
unpacking or Framework source build is required. Existing conflicting coordinates
are rejected. Identical payloads can be reused; partial registration is not an
atomic transaction and must be retried with the same verified ZIP after fixing
the reported issue. Do not delete unrelated Maven cache contents. An abandoned
`.koiki-prepare.lock` must only be removed after confirming no preparation is running.

Default installation is the user's `.m2/repository`. Use `-LocalRepository` to
select a different directory, and pass the same `-Dmaven.repo.local=...` to builds
and configure the IDE consistently. `-Maven` accepts an absolute Maven executable
or wrapper path. The script invokes Maven outside the project so missing KOIKI
Parent artifacts do not prevent initial installation. A completion record is
written under `.koiki-distributions/` only after all 26 payloads are verified.

For local verification, use `-LocalArchive <path>` instead of `-Repository`.
`-Offline` affects Maven registration only; it requires cached install-plugin
dependencies and does not make a subsequent application build offline-capable.

Private assets require GitHub CLI authentication with repository read access.
SSH clone authentication alone does not authenticate the release download.
In Actions, supply a suitable read credential using `GH_TOKEN` from Secrets.
The workflow repository's `GITHUB_TOKEN` does not automatically read a different
private repository. Never put credentials in this script, POMs, or manifests.

```yaml
# Example only: repository, hash and read credential must be configured.
- name: Prepare fixed KOIKI version
  shell: pwsh
  env:
    GH_TOKEN: ${{ secrets.KOIKI_ASSET_READ_TOKEN }}
  run: >-
    ./tools/prepare-koiki.ps1 -Version 0.1.0-preview.1
    -Repository OWNER/koiki-artifacts
    -ExpectedSha256 ${{ vars.KOIKI_ARCHIVE_SHA256 }}
- name: Build application
  run: ./mvnw -B verify
```

Download the script separately from a pinned distribution repository commit, or
retain a reviewed copy in the application repository. Cache is optional: empty
cache builds must work with access to third-party Maven repositories. The same
artifact version is used for development, testing and production when approved.

## Create a local candidate

Producer requirements additionally include Python 3.12+ and the repository Maven
Wrapper. Run `create-candidate.py --version <fixed-version> --output <new-path>
--maven-repository <producer-cache>`; add `--offline` only with prepared dependencies.
It snapshots tracked and non-ignored source files, captures dirty-source hashes,
converts only staged POMs, runs the selected modules' tests/package, checks embedded
versions and writes a ZIP. The checkout's POMs are unchanged. `source-inventory.json`
and `generated-poms.json` beside the ZIP identify exact source and version conversion.
The ZIP SHA-256 is outside the ZIP; internal hashes are in `SHA256SUMS` and manifest.
Publish the exact verified ZIP without rebuilding. Fixed version content must not
be replaced after publication. This tool is local staging, not a publishing command.

## Scope and known limits

Parent/BOM, architecture contracts/rules, nine Starters and `koiki-testing` are
distribution candidates; install-all is not depend-on-all. Select required
Starters in the application. Testing/ArchUnit helpers are not blanket production
runtime dependencies. Reference/SPA/harness/seed/keys and third-party JARs are absent.
Java 21 build/target and Boot 4.1.1 are the current baseline. Java 25/VT remains
opt-in with the limited D2 evidence. S1 asynchronous delivery/recovery/duplicate
suppression gaps remain; a ZIP does not certify all DoD or production operations.
R-05 Entity coexistence and R-06 migration topology require independent Consumer
evidence. Local ZIP tests, real GitHub downloads and recipient PC/CI acceptance
are separate results; consult the E1 plan and version-specific evidence before use.

## Continue release work on a local PC

Cloud release creation/upload is outside the continuation scope. Use the
[VSCode handoff](../../docs/development/phase4-e1-vscode-handoff-20261030.md)
for the existing public `zaziedlm/KOIKI-JAVAWEB` repository, candidate transfer,
source/provenance checks, and local-PC release/download steps. Binary assets are
transferred separately from Git. The old dedicated-repository publishing package
is superseded; `publish-initial-release.ps1` is a retired, fail-fast guard.
The original verified ZIP retains the README bundled at generation time; changed
checkout documentation does not change those verified bytes.
