# S1 R2 branch・source固定・preflight記録（2026-10-13）

状態：branch作成・clean source固定 COMPLETE、訂正承認後のpreflight COMPLETE FOR LIMITED TOOLING CREATION（2026-10-13、§5〜7）。Tooling作成・package／testは未開始。§3〜4の停止記録を保持する。

**後続実行（2026-10-13）：** Ownerの継続指示でTooling4 fileを作成し、compile／通常・async package、正常対照1＋負例7の初回検証を実施した。7 PASS／1 FAIL（能力不足403期待に対して500）で停止、直接回帰4件は未実施。[作成・選択検証記録](phase4-s1-r2-implementation-and-selected-verification-20261013.md)を現行結果の入口とする。以下の作成未開始はpreflight出口時点の履歴。

## 1 承認とsource固定

[R2開始票§8](../../development/phase4-s1-r2-purpose-and-minimal-verification-owner-review-20261013.md#8-owner承認記録と文書固定対象2026-10-13)の条件付き開始、11文書のlocal commit指示、後続の「承認済みのR2 branch作成・clean source固定・preflightへと、順次進めていきましょう」に従った。

- branch：`feature/phase4-s1-reference-failure-isolation-r2`
- source commit：`cfe9d9efb4cb2bbcdc273df578c75225b539bc08`
- source固定時：clean、追跡1194 fileのbytes／SHA-256を記録。
- run：`tmp/r2-cfe9d9e-20261013-preflight/`。Git ignored、旧R1 runner・予算・PIDを流用していない。
- `source-fixed.json` SHA-256：`4325768838D8A42C5B671F02A9AE16BBBCBF1B31B3A8C73E8D5D26ED6B50F37A`。

branch作成は実行環境の権限付き承認を経て成立した。remote操作は行っていない。

## 2 preflightの成立部分

権限付きread-only診断でDocker Engine 29.1.3のServer応答、稼働container0を確認した。PG／Ryukのimage実体は次のとおり。

- `postgres:17-alpine`：`sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73`
- `testcontainers/ryuk:0.14.0`：`sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0`

Docker割当24 CPU／16646848512 bytes、host空きmemory13 GiB／C空き499.09 GiB。保護対象JavaはPID 30304（creation 09:57:06.289039 JST）と25248（09:57:20.917818 JST）、共通親44788、VS Code Red Hat Java extensionのJRE。終了・変更していない。

JDK21・Wrapper3.9.16は[直前の新PC確認](phase4-s1-new-pc-environment-readiness-20261013.md)を入力とした。Wrapperは今回のMaven起動に進んだが、依存解決は§3で停止した。主要plugin cacheのdirectory存在を確認したが、plugin全transitive・offline build成立は未判定。

Modulith 2.1.1 JARを読取hash照合した。

| JAR | SHA-256 |
|---|---|
| events-core | `DBF64A359D94A776FFCC22BCAB26FBD2A1FE4F529171AC3EC0343F472BF9DAEF` |
| events-jdbc | `730EDBCFF692D600FB93469C09F6C69394F1BD6825C1732AB1A3EAC217665A15` |

HTTP source読取では、承認正常時302、scope不成立404、version不一致409、dependency failure503、master同期veto409へ対応する既存Controller／handlerを確認した。権限不足はmethod securityの拒否を既存Securityで処理する。CSRFを正当に通した403の実証は未実施。

`javap`のcore読取でlistener例外時のhandleFailureと未完了logの経路を確認した。一方、jdbc側は部分出力の後にJARアクセス拒否となった。部分出力を完全な失敗状態契約の成立証拠とせず、権限付きで同じ読取を再確認する対象として残した。

## 3 停止理由

Tooling依存確認として、保存済みdependency plugin 3.9.0でoffline `build-classpath`を初回1回実行した。PowerShellのnative argument渡しで引用していなかった`-Dmdep.outputFile=C:/.../tooling-classpath.txt`が分割され、Mavenは`No plugin found for prefix '.outputFile=C'`、exit 1となった。Agentのコマンド指定誤りであり、依存不足やRepositoryのbuild失敗と判定しない。

Mavenの所要出力は0.404秒。compile・package・test・依存download・container／browser起動は行っていない。依存classpathは未成立。開始票の追加再実行0回・未成立時停止に従い、自動再実行せずpreflightを停止した。

初期停止台帳の経過秒数に、PowerShellのJSON日時自動変換と文字列化によるtimezone脱落が混入した。開始／停止の保存UTC文字列は整合しており、その文字列から訂正台帳を別途作成した。誤った負の残予算を採用しない。履歴`preflight-stopped.json`を上書きせず、`preflight-stopped-budget-correction.json`を時間算定の正本とする。

UTC開始は2026-10-13 01:55:09.840839（10:55:09.840839 JST）、停止01:57:29.413272（10:57:29.413272 JST）。待ちも含む保守的上限は140秒、45分予算の残下限2560秒、cleanup300秒は未使用。Owner待ちは実行時間に含めない契約だが、残時間を再試行許可として流用しない。

今回所有Java／containerは起動0、停止時cleanup対象なし。Gitのsource固定点を変更せず、このEvidenceだけを後続の未commit文書として追加した。

## 4 必要な最小訂正と再開条件

訂正候補はPowerShellでproperty argument全体を引用した、同じoffline Tooling classpath確認**追加1回**。plugin更新・download・POM変更は含めない。

```powershell
.\mvnw.cmd -o -B -ntp -f build-support/reference-e2e-verification/pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.9.0:build-classpath '-Dmdep.outputFile=C:/KOIKI/koiki-java/KOIKI-JAVAWEB/tmp/r2-cfe9d9e-20261013-preflight/tooling-classpath.txt'
```

Ownerの訂正・追加preflight実行判断後に、jdbc JARの権限付き読取、normal／async packageに必要な依存・pluginの有限確認、対象trigger SQL・注入到達証拠、library失敗状態、provider要求記録、所有process／異常終了cleanup方式を確定する。不足取得は一覧提示後の別判断。条件が成立するまでTooling4 fileの作成・12件の検証へ進まない。

## 5 訂正承認とoffline確認の成立

Ownerは本記録と訂正案を示して「確認し、承認いたします」と明示した。これに従い引用符を修正した同じTooling classpath確認を追加1回実行し、exit 0／BUILD SUCCESS、Maven1.356秒。classpathは46 JAR、各実体とSHA-256を記録した。初回の引数失敗を上書き・成功件数へ加算しない。

残preflightで次も各初回1回、権限付きoffline実行で成立した。

| 読取・依存診断 | 結果 |
|---|---|
| events-jdbc V2／completion interceptor／registryのjavap | 全exit 0、端末内textへ保全。jdbc JARのsandbox拒否を診断再確認で解消 |
| Reference async build-classpath | exit 0、Maven1.630秒、193 JARの実体・hash保全 |
| Reference async go-offline | exit 0、Maven2.405秒、対象依存とbuild plugin解決 |
| Tooling go-offline | exit 0、Maven1.463秒、対象依存とbuild plugin解決 |
| Error Prone2.50.0／NullAway0.13.8 | processor JAR実体あり |
| source hash再照合 | 固定追跡1194 fileの差分0 |

追加downloadは行っていない。normal構成は同じPOMでasync専用JDBC依存を除く構成。cacheのKOIKI snapshotは依存診断入力であり、正式成果物へ昇格させない。後続packageは固定sourceから必要reactor moduleを`-am`で作り、通常／asyncを別epoch・別JARで照合する。go-offlineはcompile／package／testの成立を示さず、processorの全推移依存やMaven実行時だけ必要なartifactに不足が出た場合は停止して一覧を提示する。

library metadataでは、listenerの失敗経路がregistry.markFailedに接続し、JDBC V2が対象IDのstatusをFAILEDへUPDATEする契約を確認した。R2-01は実DBでFAILED・completion_dateなし、対象listener失敗log、stubの503到着／受理0と承認／Audit保持を照合する。metadata読取をcase PASSとしない。

## 6 実装前に確定した注入・観測・cleanup方式

### 6.1 隔離DBの注入

Tooling4 file内のDB helperで、fresh DB起動・migration・対象request作成後だけ`kkref_r2_test` schemaを作成する。publication／Auditそれぞれに専用sequenceを置き、対象trigger到達で`nextval`を実行する。sequence進行は承認transactionのrollbackと別なので、rollbackされた注入台帳を到達証拠にする問題を避ける。開始時の`is_called=false`と実行後の`is_called=true`・last_value=1をSELECT専用observerで照合する。注入未到達はBLOCKEDとする。

publicationは`kkref_notification_async.event_publication`のBEFORE INSERTだけを対象とし、`event_type`がexpense-owned ExpenseApprovedEvent、listener_idがExpenseApprovalListener、`serialized_event::jsonb ->> 'expenseRequestId'`が今回UUIDの場合だけ拒否する。承認Auditは`public.koiki_audit_event`のBEFORE INSERTで、audit_type=BUSINESS／event_type=EXPENSE_WORKFLOW／action=APPROVE_EXPENSE／resource_id=今回UUIDに限定する。

関数本体のtemplateは次の形。列・型は固定source／V5とFramework既存Audit migrationに照合した。SQLはここでは実行していない。

```sql
-- Toolingが上記の対象条件をIFへ設定する。UUIDは検証済みUUID文字列のみ。
IF <対象条件> THEN
  PERFORM nextval('kkref_r2_test.<対象別sequence>');
  RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = '<R2対象別marker>';
END IF;
RETURN NEW;
```

P0001を選び、DataIntegrityViolationのCONFLICTへ故意に変換しない。publication失敗の承認HTTP期待は既存dependency handlerによる503、実Audit保存失敗はBusinessAuditRecorderのAuditRecordingExceptionを介する503。serialized JSONの形が契約と違えば注入未到達として停止し、文字列LIKEだけで対象が一致したことにしない。trigger／functionのDROPはhelperのcloseで行い、失敗時は所有DB全体をcleanupし未成立を記録する。production migration／DBには追加しない。

### 6.2 provider・拒否観測

stubの既定受理契約を維持し、R2専用modeだけで受理前503を返す。独立した到着recordをkey／event ID／payload hash／response statusに限定し、受理台帳と分離する。request本文・tokenを証拠へ保存しない。不成立caseは対象publication0／到着0／受理0を最大5秒の有限pollで確認し、子process終了後に再照合する。501等の別応答、timeout・切断・受理後失敗は追加しない。

HTTP期待は正常302、権限不足403、scope外404、古いversion409、master同期veto409。対象部門とIdentity fixtureの権限を正当に設定し、CSRF正当tokenを取得する。scope外や能力不足の主体に承認詳細GETを成功させる前提を置かず、その主体が読めるpageからCSRFを取得する。

### 6.3 所有・停止・cleanup

新runのsource identityと、固定normal／async JAR hashを入力として記録する。launchしたProcess objectのPID・startInstant・ExecutablePathと、Testcontainers objectが作成したcontainer ID／session labelを所有記録へ保存する。他sessionのJava／containerや旧PCのPIDは停止対象にしない。

shared fixtureでは、constructor／launch／case失敗・close途中例外を含め、各所有資源のcleanupを独立したfinallyで試みる。Reference子process正常終了を30秒待ち、未終了時だけ同じ所有Process objectの限定force終了・追加5秒確認。issuer／stub／HTTP client／DBも、先行close失敗で後続cleanupが抜けないようにする。元例外とcleanup例外は両方保持し、secret除去・bounded log保全も後続finallyで実施する。これは既存helper内の4 file範囲での作成条件であり、現時点の実cleanup実証ではない。

Maven／forkは各集合直列、forkCount1・reuseForks=trueを明示する案。集合間に今回sessionのRyuk終了を確認し、旧／新Ryukを重ねず上限1を維持する。開始時と有限sampleでJVM4／PG1／client8・heap／pool／container上限を確認する。sample・最終所有集合・protected baselineを保存し、未終了／所有不明なら次集合へ進めない。新しい常駐runner・OS設定変更・全Java停止は作らない。

## 7 preflight出口

branchと固定source、必要cache・library契約、対象限定注入、到着／受理の観測、所有／cleanup方式を整理し、承認済み4 fileのTooling作成へ進めるpreflight条件を満たした。今回所有process／containerの起動は0、cleanup対象なし。次は新しいsource差分manifestを記録してToolingを作成し、有限package・artifact固定・R2-08対照→負例7→直接回帰4へ進む。

preflight成立はR2 case結果の受入、実行cleanupの証明、production修正・追加再実行・remote承認ではない。budgetは同runの訂正台帳と再開台帳で累積管理し、Owner待ち・中断を残時間許可へ読み替えない。原固定sourceはcleanで保全し、本Evidenceだけを後続未commit差分として管理する。

`preflight-completed.json`では、初回の保守的140秒に訂正後の診断tool実行を丸めた18秒を加え、累積上限158秒・残下限2542秒（cleanup300秒予約）とした。文書編集とOwner承認待ちは除外する。preflight8分上限内。これは今回の12件検証を追加実施した台帳ではなく、以後の集合・再実行上限は開始票のままである。
