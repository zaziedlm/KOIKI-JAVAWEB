# Phase 3 P3-B4 optimistic conflict / cache

## 1. Status and boundary

| Item | Result |
|---|---|
| Date | 2026年9月14日 |
| Architecture Owner | Shuichi Kataoka |
| Phase / CP | Phase 3 Reference Vertical Slice / P3-B4 |
| Status | COMPLETE / OWNER APPROVED |
| Start HEAD | `e5115dd`（P3-B4 contract Owner approval） |
| Ownership | Reference expense、Reference-owned cache configuration、非配布Browser Tooling |

P3-B4-D1〜D10の承認契約に従い、楽観lock競合をReference専用409画面として可視化し、
active経費科目option一覧だけへ短いlocal cacheを適用した。Framework Public API、Starter、BOM、
migration、Root Reactor module、workflowおよびremote設定は変更しない。

## 2. Implemented boundary

| Area | Result |
|---|---|
| Conflict | `CONCURRENT_MODIFICATION`だけをHTTP 409の専用Thymeleaf画面へ変換 |
| Recovery | 元のapplicant / approver / accounting scopeに対応する最新detail linkを表示し、自動retryしない |
| Disclosure | 例外class、stack trace、SQL、Session IDおよび内部actorを画面へ露出しない |
| Lock | hidden version、Application比較、JPA `@Version`を維持し、先行transactionだけを確定 |
| Cache target | active経費科目のID / code / nameから成る表示専用の変更不能一覧だけ |
| Cache ownership | expenseのJdbcClient read-model Adapter内部、固定cache名 / key、`sync=true` |
| Cache implementation | Reference applicationだけにSpring Cache / Caffeineを追加 |
| Freshness | `maximumSize=1,expireAfterWrite=30s`。event evict、preload、refresh-aheadなし |
| Fail-safe | commandのmaster current-value確認は非cacheのまま。staleな廃止科目を拒否 |
| Browser fixture | 同一approverの独立2 BrowserContextと、別applicantの`SUBMITTED`申請 |

通常HTMLとThymeleafを主軸に維持し、競合操作をHTMX化していない。認可、Role / Permission、
Session、承認scope、expense transactionおよびmaster管理queryはcache対象外である。

## 3. Automated verification

| Check | Result | Evidence |
|---|---|---|
| 409 MVC / Security | PASS | 承認POSTの競合を409、専用文言、scope付き最新detail linkとして描画。内部detailを非露出 |
| PostgreSQL optimistic lock | PASS | 独立したstale JPA persistence contextのflushを`OptimisticLockException`で拒否 |
| State / Audit atomicity | PASS | 先行approveだけがstate / version / Business Auditを確定し、後発reasonを保存しない |
| Cache initial / hit / TTL | PASS | 初回DB load、TTL前stale hit、1秒test TTL後の同期reload |
| Empty result | PASS | 空一覧もTTL内でcache |
| Loader failure | PASS | transaction内の一時的なtable unavailableで例外を発生させ、rollback復旧後の次回load成功を確認 |
| Immutable value | PASS | 返却一覧への変更を`UnsupportedOperationException`で拒否 |
| Stale command fail-safe | PASS | Form用cacheに廃止前optionが残っても、非cache current-value queryが作成を拒否しrowを作らない |
| Browser harness compile | PASS | 2 BrowserContextの先行approve / 後発reject競合journeyをtest compile |
| Headed Playwright | PASS | Owner端末でP3-B3回帰とP3-B4独立2 BrowserContext競合の2 tests、failure / error / skip 0、6.707 s |
| Live DB / Audit | PASS | 対象申請は`APPROVED`、version 2、reasonなし。`APPROVE_EXPENSE / SUCCESS`だけ1件 |
| Root Reactor clean verify | PASS | 16 projects成功、Reference 85 tests、failure / error / skip 0、実行可能JAR生成 |

loader failure testのtable renameはPostgreSQL transaction内だけで行い、同じtransactionをrollbackして
schemaを復元する。production migrationまたは永続的なtest route / failure switchは追加していない。

## 4. Human checkpoint

Owner checkpointは次の順で実行した。

1. [DONE] Local Run Guideに従い、使い捨てPostgreSQLを起動した。
2. [DONE] Root `clean verify`で生成した実行可能JARを、30秒TTLの通常設定で起動した。
3. [DONE] demo seedを一度だけ投入し、一時login credentialをそのterminal内だけで扱った。
4. [DONE] headed Playwrightで独立2 BrowserContextの競合journeyを実行した。
5. [DONE] 409文言、最新detail link、最新`APPROVED`状態、stale却下reason非保存を実browser assertion、Owner reviewおよびDBで確認した。
6. [DONE] DBのstate / version / reason、Business Audit件数を突合し、application logのparameter maskingと内部detail非露出を確認した。
7. [DONE] 経費科目名とactiveをDBで変更し、TTL前のstale表示、TTL後の再load、廃止科目command拒否を確認した。
8. [DONE] review後にapplicationをgraceful shutdownし、使い捨てDB containerを`--rm`で破棄した。

credential、password hash、CSRF token、Cookie値およびSession IDはEvidenceへ保存しない。

2026年9月14日のcheckpointでは、package済みJAR、使い捨てPostgreSQLおよびdemo seedを使用した。
headed PlaywrightはP3-B3回帰とP3-B4競合の2 testsを成功させた。直後のDB突合では競合対象
`b3000000-0000-4000-8000-000000000102`が`APPROVED`、version 2、decision reasonなしであり、
Business Auditは`APPROVE_EXPENSE / SUCCESS`が1件だけだった。先行transactionだけのstate / Audit確定と、
後発rollbackを実アプリでも確認した。経費科目管理の非cache queryではDB更新後の名称を即時表示する一方、
経費申請作成のoption一覧は同じDB更新後も30秒TTL内で更新前の名称を表示し、cache対象の分離とTTL前stale hitを
Ownerが目視確認した。TTL経過後の再表示ではDBの新名称へ更新され、次回参照による同期reloadも確認した。

その申請画面を保持したまま経費科目をDBで廃止し、表示上は残っているstale optionを使って下書き保存を試みた。
実アプリは「選択した部門または経費科目は現在利用できません。」として拒否した。DBのexpense総数はseed時の3件、
入力した明細は0件を維持し、Business Auditも先行承認の1件だけだった。これにより表示cacheをcommandの
current-value判断へ流用せず、非cacheの有効master確認がfail-safeに働くことを画面 / DB / Auditで確認した。
application logではFramework / Reference migration、Identity AuthenticationProvider、TomcatおよびApplicationの
正常起動を確認した。予期しないERROR、stack trace、password、Cookie、Session ID、CSRF tokenまたは入力parameter値の
出力はなかった。Spring Securityによる明示AuthenticationProvider選択時の構成WARNが1件あるが、承認済みIdentity
providerを使用する現行構成に対応する既知の起動警告であり、P3-B4の障害またはsecret露出として扱わない。

## 5. Architecture Owner approval record

Architecture Ownerは2026年9月14日、次の判断と検証Evidenceを一体として承認した。

1. Reference専用409競合画面。
2. 自動retryなし・最新詳細への誘導。
3. active経費科目optionだけの30秒cache。
4. master管理queryの非cache境界。
5. command current-value確認によるfail-safe。
6. Reference / Tooling限定とdeferred scope。
7. 既知のIdentity AuthenticationProvider起動WARNを非blockingとする判断。

以上によりP3-B4を`COMPLETE / OWNER APPROVED`とする。次はGate BでP3-B1〜B4横断のWeb / query acceptance、
accessibility、log / Audit / DB Evidenceを棚卸しする。Gate B承認前にP3-C0 REST contract reviewへ進まない。
REST、distributed cache、Framework cache Starter / Public API、MyBatis競合およびremote変更はdeferredを維持する。
