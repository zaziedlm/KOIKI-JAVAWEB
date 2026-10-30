# E1 local verification

Run only within the execution scope / time and resource budget in E1 plan §7.4.
These scripts do not publish, modify Root CI or operate on a real project database.

1. Create a candidate with `framework-distribution/create-candidate.py` and record
   its ZIP SHA-256. Candidate generation runs the selected module tests/package.
2. Run `prepare-koiki.ps1 -Version 0.1.0-preview.1 -LocalArchive <zip>
   -ExpectedSha256 <hash> -LocalRepository <new-recipient-repository> -Maven <absolute-wrapper>`.
   Start with an empty recipient repository; allow normal Maven third-party access.
3. `python verify-preparation.py --pwsh <pwsh> --maven <absolute-wrapper>
   --archive <zip> --repository <prepared-recipient-repository>` checks corrupted
   ZIP/payload rejection, unsafe archive paths, simulated download failure,
   identical reuse and conflicting installed content. It restores its single
   deliberately modified POM in `finally`. Use an isolated repository only.
4. `python verify-consumer.py --repository <prepared-recipient-repository>
   --output <new-evidence-directory>` starts one owned PostgreSQL 17 container,
   creates the limited role, runs the Consumer journey and captures the dependency
   tree. It always removes the owned container and records cleanup status.
5. Run normal Root regression once, sequentially, with the existing normal-root
   DB limits and finite JVM settings in the plan. It does not certify candidate
   version bytes; candidate Consumer checks do that separately.

PowerShell 7 / Java 21 / Maven 3.9.16 and Python 3.12+ are prerequisites. Docker
access is needed only for the DB journey and normal Root tests. Maven plugin
downloads and application dependencies are normal third-party network traffic.
`--offline` on the Consumer runner requires its third-party dependencies already
cached and is not a substitute for the empty-cache preparation check.

Rejection checks use a local fake `gh` function for the download-failure boundary.
They do not authenticate or call GitHub. A real `gh release download` test must
follow publication to the selected repository; recipient Windows/IDE and project
Actions acceptance must be recorded separately. Source fixtures and failure
switches stay out of the formal ZIP.
