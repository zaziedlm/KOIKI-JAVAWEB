# KOIKI fixed distribution tooling (E1 preview)

This tooling builds and prepares a complete, fixed KOIKI version. It does not
create repositories, tags, releases, or upload assets. The initial proposed
version was `0.1.0-preview.1`. This tooling targets the unpublished
`0.1.0-preview.2` candidate; it is not the Phase 5 production/support release.
All modules share one version. Do not mix modules from different distributions.

## Prepare a development PC or CI runner

Requirements: PowerShell 7, JDK 21, Maven 3.9.16, GitHub CLI for remote downloads,
and access to the normal Maven repositories for third-party dependencies/plugins.
Neither this ZIP nor its preparation script installs those tools. Only KOIKI
artifacts are bundled: 14 POMs, 12 main JARs, 11 sources JARs and 5 Javadoc JARs,
plus manifest and installation guidance (42 payloads / 46 files in total).
The fixed classifier inventory is recorded in the producer's
`classifier-inventory.json`: sources cover all Java-bearing modules, including
internal implementation sources; Javadoc covers only the five modules with
public packages. `koiki-testing` has no Java sources and no classifier JARs.
Use each version's bundled installer; this schema-2 installer rejects schema 1.

Use a reviewed copy of `prepare-koiki.ps1` pinned to a distribution repository
commit, and the ZIP SHA-256 recorded with the selected version. The distribution
repository is the existing public `zaziedlm/KOIKI-JAVAWEB`. The preview below
is a candidate until version-specific publication and download are verified.

```powershell
./prepare-koiki.ps1 -Version 0.1.0-preview.2 `
  -Repository zaziedlm/KOIKI-JAVAWEB -ExpectedSha256 <pinned-64-character-hash>
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
written under `.koiki-distributions/` only after all 42 payloads are verified,
including every sources/Javadoc hash. Missing classifier files are reinstalled;
conflicting classifier bytes are rejected before any registration begins.

For IDE source and documentation lookup, use the same local Maven repository
as the application build and refresh the Maven project. Standard classifier
filenames allow the IDE to attach sources/Javadoc where supported. Actual IDE
attachment is a separate recipient check. Do not add classifier artifacts as
application dependencies; they are not runtime libraries.

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
    ./tools/prepare-koiki.ps1 -Version 0.1.0-preview.2
    -Repository zaziedlm/KOIKI-JAVAWEB
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
converts only staged POMs, runs the selected modules' tests/local install, checks embedded
versions and writes a ZIP. The checkout's POMs are unchanged. `source-inventory.json`
and `generated-poms.json` beside the ZIP identify exact source and version conversion.
The ZIP SHA-256 is outside the ZIP; internal hashes are in `SHA256SUMS` and manifest.
The explicitly selected producer cache receives local fixed-version artifacts
so the separate classifier goals can resolve inter-module dependencies.
Source Plugin 3.4.0 and Javadoc Plugin 3.12.0 are pinned in Tooling only; the
distributed Parent does not make recipients run these goals. Javadoc uses
`all,-missing` doclint and excludes internal packages. Source bytes/public HTML,
local-path absence and identical main-JAR hashes before/after these goals are checked.
Publish the exact verified ZIP without rebuilding. Fixed version content must not
be replaced after publication. This tool is local staging, not a publishing command.

## Scope and known limits

Preview.2 includes the consumer-feedback Session logout configuration-order fix.
It also removes the Reference-specific `koiki/fragments :: header` from the
Framework. Applications must own their navigation/header; the shared
`koiki/fragments :: head(title)` remains. Update any old header references
before adopting this version. Published preview.1 remains unchanged.

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

Publishing (tag/release creation and asset upload) must run on a local PC after
checking GitHub authentication and write access to the target repository. Do not
run publishing from Codex Cloud or another cloud execution environment. Use the
[VSCode handoff](../../docs/development/phase4-e1-vscode-handoff-20261030.md)
for the existing public `zaziedlm/KOIKI-JAVAWEB` repository, the Windows-generated
candidate, source/provenance checks, and local-PC release/download commands.
Candidate generation, installation and the Consumer were reproduced on Windows
with Temurin 21.0.13+9; see the linked evidence for scope and corrections.
The Cloud continuation ZIP/bundle is not required. Binary assets stay outside Git.
There is no active publishing script: the handoff uses Git/GitHub CLI directly.
`publish-initial-release.ps1` is a retired, fail-fast guard for the old repository plan.
Recipient CI may download/install a published asset; it is not a publishing environment.
This checkout README was corrected after candidate-3 was built. Its bundled README
still contains the earlier repository examples; pass `-Repository zaziedlm/KOIKI-JAVAWEB`.
Do not modify that verified ZIP to incorporate these text changes: doing so creates
different bytes/hash and requires a new candidate and the necessary verification.
