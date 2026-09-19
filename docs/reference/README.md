# Reference

業務アプリ開発チームがKOIKIの提供範囲、構成選択、Referenceの使い方およびCustomer Ownershipを最初から確認する場合は、
[アプリ開発チーム向け引継ぎガイド](../development/application-team-handoff-guide.md)から開始してください。
このdirectoryは、その後に参照するReference Applicationの仕様と実行手順を管理します。

- [KOIKI-JavaWeb-FW Reference Application 業務仕様 v0.1](KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md):
  Reference Applicationの業務scope、状態遷移、不変条件、権限matrix、代表Use Case、
  module間連携、およびPhase別受入条件の正本
- [KOIKI-JavaWeb-FW Reference Application ローカル手動起動ガイド v0.1](KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md):
  package済み実行可能JAR、使い捨てPostgreSQL、実行時設定、停止と切り分け、初期データ境界、および
  明示実行する非配布local demo data Tooling

## 現行Reference Applicationの確認経路

目的に応じて次の入口を使う。通常Session MVCとBearer APIは同じReference Applicationを利用するが、
認証方式、fixtureおよび検証責務を混在させない。

| Purpose | Entry | Boundary |
|---|---|---|
| KOIKIの提供物／非提供物と最初のCustomer-owned moduleを確認する | [アプリ開発チーム向け引継ぎガイド](../development/application-team-handoff-guide.md) | P4-AR5でOwner承認済み。実チーム受入はP4-AR6で行う |
| 業務仕様、状態、不変条件、Permission、ACを確認する | [Reference Application業務仕様](KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md) | 要求の正本。起動手順ではない |
| package済みJARを通常profileで起動し、Session MVCを手動操作する | [ローカル手動起動ガイド](KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md) | local demo seedは使い捨てDB限定。`api-bearer`を有効化しない |
| 起動済み通常profileへ実ChromiumでHTMX / CSRF / history / 409をfocused確認する | [Reference Browser Verification](../../build-support/reference-browser-verification/README.md) | Root Reactor外・非配布。ApplicationとDBは手動起動する |
| package済みJARのBearer REST APIをfocused確認する | [Reference API Verification](../../build-support/reference-api-verification/README.md) | test-only issuer / key / token / userをprocess内生成し、自動で`api-bearer`を有効化する |
| Bearer API、Session browser、DB / Audit / logを代表journeyとして一括確認する | [Reference Critical Journey E2E](../../build-support/reference-e2e-verification/README.md) | Root Reactor外の非配布Tooling。同じentrypointをrequired checkから実行。focused negative matrixは複製しない |
| Security Starter、profileおよびCustomer側依存選択を確認する | [Phase 2 Developer Journey](../development/phase2-developer-journey.md) | Phase 2 Security契約の正本。Phase 3 fixtureを正式構成へ昇格しない |
| MVC単一JARとReact / Next.js / ALBのUI・認証境界を比較する | [UI / Authentication Profile Selection Guide](../development/frontend-authentication-profile-guide.md) | P4-AR5入力。未実装profileをReferenceの実績と混同しない |

KOIKI利用全体はアプリ開発チーム向け引継ぎガイドから開始します。Referenceの確認へ入ったら、最初に業務仕様を確認し、
日常の画面確認は通常profileのローカル手動起動ガイドを使用します。変更内容に応じてbrowserまたはAPI focused Toolingを選び、
Reference全体の代表経路を確認するときだけcritical E2Eを使用します。
各Toolingのcredential、token、Cookie、key、demo seedおよびcleanup境界はリンク先を正本とし、ここへ複製しない。

本directoryはReference Ownershipの仕様文書を管理する。Reference ApplicationはFrameworkの
利用例であり、Framework内部として扱わない。Walking SkeletonのJava class、Template、SQLを
正式Referenceへ直接移植しない。

## Phase 2 identity reference

Phase 2の`koiki-reference-app/identity`はTier 1の正式Referenceであり、Controller → Application Use Case →
Framework Public contractによるIdentity user参照とRole付与 / 剥奪を実演する。Framework Entity、Repository、internal package、
migrationを所有せず、管理操作のMethod Security、Business Audit、全Session失効、optimistic versionと失敗時rollbackを扱う。

- [source](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity)
- [acceptance](../architecture/validation/phase2-p2-b4-b4-5-closeout.md)
- [packaged verification](../../build-support/security-foundation-verification/verify-p2-b4-reference-journey.ps1)
- [developer entry](../development/phase2-developer-journey.md)

このReferenceはCustomer Project Template、初期user provisioning、固定Role体系または完成済み管理Applicationではない。
Customerは業務語彙、Role / Permission、画面、provisioning、migrationを自身のOwnershipで設計する。
