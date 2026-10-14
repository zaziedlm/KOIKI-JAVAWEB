# S1管理設定scope／TTL供給：実装・検証記録（2026-10-07）

**最新結果：新規5 class／40件、初回基盤6 class／55件、既存25 class／99件とpackage済みE2E1件はPASS。実装・検証結果はOWNER APPROVED（2026-10-07、§7）。** [限定開始票§8](../../development/phase4-s1-managed-scope-ttl-limited-start-review-20261007.md#8-owner承認条件付き限定開始2026-10-07)の対象だけを実施した。補強後のLoader・登録／実DB・Architectureも関連再検証で成功。本番code／JARのhash不変を確認。変更16ファイルは未commit。

## 1. source固定とpreflight

- Owner操作で文書4件をcommitした`db1ace1769dc20516d2d9dacfcdd50b489c29bf7`をsource固定。branchは`feature/phase4-s1-reference-foundation`、開始時の作業ツリーclean、`git diff --check`成功。初回基盤commitは`a18a76d`。
- 権限付きread-only診断でavailable memory17,050,184 KiB（約16.26 GiB）、disk46,972,116,992 bytes（約43.75 GiB）。開始条件8 GiB／10 GiB以上。稼働container0、既存PostgreSQL17-alpine imageあり。既存停止containerを変更していない。
- Java21.0.12.1、Repository Maven Wrapper3.9.16を確認。`mvn`はPATHになかったため、既存`mvnw.cmd`を使用。追加install・artifact取得なし。
- offline `-pl koiki-reference-app -am -DskipTests test-compile`はSUCCESS（10.082秒）。E2E POMのoffline validateはSUCCESS（0.849秒）。前者は13-project reactor・既存plugin／依存経路確認で、testは未実行。
- 既存Chromium151.0.7922.34のheadless起動・静的title確認・browser／driver終了がPASS。download禁止。既存Playwright classpathと非配布BrowserPreflightを使用。
- cached Jackson core／databind3.1.5にstrict duplicate detectionと専用JsonMapperが存在することをJAR署名で確認し、実装compileもSUCCESS。共有ObjectMapper・POM・Frameworkを変更していない。
- 管理設定の実担当・実配備先・実UUID・時計監視／許容差はOPEN。今回の一時設定と可変Clockはtest所有で、本番ACLや時刻同期の受入を認定しない。

## 2. 実装の責務

`ManagedRecoveryConfigurationLoader`はローカル絶対path・正規化／real path・通常file・NOFOLLOWと親symbolic linkの確認、64 KiB上限、同一bytesのSHA-256とUTF-8／JSON検査を行う。未知field・重複key・tuple・不正UUID／能力／期間・manifest差異を拒否し、エラーにpath／JSON／causeを含めない。

snapshotはimmutable、64 grant以内、最大TTL10分、最大設定期間30分。Scopeはuser・能力・environment・publication完全一致、期間外UNAVAILABLE。TTLは設定環境・期間・残期間を確認し、発行時刻取得後にも期限を再検査する。未接続policyの追加検査は既定false。既存testの有限policyだけ明示肯定に対応。

条件付き構成はfoundation有効かつfile modeだけ管理Adapterを登録する。disabled／未指定は従来のUNAVAILABLE／empty、通常foundation無効では設定読取なし。不正mode・明示fileの不備は起動拒否。設定は起動時1回で、transaction内のfile／外部I/Oやhot reloadはない。

## 3. case対応と結果記録先

開始票L01〜L12／S01〜S08／T01〜T08／R01〜R06／D01〜D06は同prefixのJUnit methodへ対応し、5 class／40 invocationを照合する。複数負例を1method内で確認する枝は開始票対応を保つ。初回6 class／55件と既存25 class／99件・E2E1件は別集合として照合し、Architecture2件を重複加算しない。

新規・初回再検証は非配布`build-support/reference-e2e-verification/target/managed-suite-20261007/`へclass別sanitized XML／log／manifestと資源sample・source hashを保存する。class別Maven、fork1／reuseForks=false、各heap768 MiB、並列無効、DB同時1／1 GiB／CPU1／max_connections16、pool2／idle0と既存用途別grantを維持する。既存／E2Eの結果と最終cleanupは完了後に記録する。

Agentはgit add／commit／pushを行っていない。新規の供給設定・有限UUID・故障注入はtestだけで、実設定配備・受付／worker認証・停止／provider／publication接続・sender／listener／runner・Modulith／Framework変更は未実施。

## 4. 新規40件・初回55件の結果と補強

| class | invocation／結果 | 最新証拠 |
|---|---|---|
| ManagedRecoveryConfigurationLoaderTest | 12／PASS | `managed-loader-portable-20261007/` |
| ManagedRecoveryScopeAdapterTest | 8／PASS | `managed-suite-final-20261007/` |
| ManagedRecoveryTtlPolicyAdapterTest | 8／PASS | 同上 |
| ManagedRecoveryConfigurationRegistrationTest | 6／PASS | `managed-registration-final-20261007/` |
| ManagedRecoveryConfigurationDbTest | 6／PASS | 同上 |
| RecoveryPermitTest／RecoveryConsumptionTest | 12＋3／PASS | `managed-suite-final-20261007/` |
| NotificationFoundationTransactionTest／PersistenceTest | 16＋12／PASS | 同上 |
| NotificationFoundationRegistrationTest | 7／PASS | `managed-registration-final-20261007/` |
| NotificationFoundationMigrationTest | 5／PASS | `managed-suite-final-20261007/` |

全結果はfailure／error／skip0。新規40件・初回55件は独立集合。既存Architecture2件はsuiteと関連再確認でも成功したが、既存99件へ含めるので新規件数へ重複加算しない。

最初の`managed-suite-20261007/`も40＋55＋Architecture2件が成功。WindowsのUNC共有pathを読取前に拒否する補強と、読取ACL拒否／junctionの実fixture検証、設定path変換の例外非露出を追加し、別run `managed-suite-final-20261007/`で同じ40＋55＋2件を再検証した。失敗を無条件反復したものではなく、初回結果を保持して変更枝を確認した。

final suiteは12 class／97 invocationで、class別実経過合計342.73秒。資源66 sample、memory最小14.91 GiB、disk最小43.53 GiB、DB同時最大1。DB側inspect／SHOW、用途別pool、heapとcleanupの検査を初回harnessで維持し、各class10分以内。新規・初回再検証はそれぞれ採用済み時間上限以内。

LoaderのL11はWindowsで、test一時fileのowner READ_DATAをDENYして読取失敗・例外cause非露出を確認し、finallyでACLを復元。test一時directory内にjunctionを作り、経由読取の拒否とjunction／helper終了を確認。既存CIがLinux対象であるため、testをOS別のACL／POSIX権限・junction／symlinkへ整え、Loader12件を`managed-loader-portable-20261007/`で再確認した（14.55秒、failure／error／skip0）。POSIX側はこの端末では未実行で、CI PASSを主張しない。本番code変更はない。

管理設定有効時のR06をIdentityだけでなくmaster／expenseの実Entity登録との共存まで補強した。test所有の追加sourceから既存package-private persistence Configurationを限定scanで読み、EntityScanの複製をせず、IdentityUserEntity／DepartmentEntity／ExpenseRequest／RecoveryPermit／RecoveryConsumptionの共存を実DBで確認。test helperに追加source指定を加えただけで、本番構成やgrantを拡大していない。

この補強後に、新規登録6件・実DB6件、初回登録7件とArchitecture2件を`managed-registration-final-20261007/`で再確認し、計21件はすべてPASS（class別38.20／39.04／44.54／18.56秒）。初回55件の不変規則・枝を減らしていない。関連再検証の件数を新規40件等へ加算しない。

実DBの肯定側は実Public IdentityQuery／Recorders・制限付きJPA transactionを使い、現在対象と運用証拠だけtest有限入力。現在能力失効、scope外の保存0＋Security Audit、Business Audit INSERT権限拒否時の発行rollback、TTL取得後に設定期限を跨いだ場合の保存／Audit0、旧context終了→設定改版→再起動時の失効反映と既存expiresAt／消費保持を確認。設定file改変だけでは旧snapshotは変更されず、設定期間外は必要操作を拒否する。

## 5. 既存99件＋package済みE2E・最終sourceとcleanup

`managed-regression-20261007/`で既存25 class／99件・E2E1件は全PASS。初回baselineのclass名・invocation数との差分0、failure／error／skip0。開始19:22:33〜終了19:33:30 JST、657.30秒（約10分57秒）で60分以内。資源124 sample、memory最小15.54 GiB、disk最小43.34 GiB、DB同時最大1。

E2E前のoffline clean packageは52.340秒でSUCCESS。Reference child PID26228、heap768 MiB、実pool4／minimum idle1。browser／issuer／child／port／一時logのcleanupが成功し、外側のread-only確認でもchild残存なし・稼働container0。既存停止containerを変更していない。

**JAR SHA-256：** `C20B55C82CCC14F8C74D1067E93B904770B3AF5E444F5EA73BA389F7D0D80F27`。JARには管理設定の本番クラスが入り、ManagedRecoveryTestSupport／NotificationFoundationDbHarness／隔離grant SQLは含まれない。正式設定file・test user／Clock・障害注入を含めていない。

回帰実行前後のnotification main／test source hash差分0、HEADは`db1ace1769dc20516d2d9dacfcdd50b489c29bf7`を維持。回帰後のR06補強はtest3ファイルだけで、main sourceとJARのhash差分0を確認し、上記関連21件で検証。LoaderのOS別test補強もmain codeを変更していない。run別source hash・manifest／XMLを保全し、単一runですべての最終test差分を実行したと説明しない。

最終source hashは`managed-registration-final-20261007/final-source-hashes.json`へ保存。最終確認時の今回`managed-*` run directoryのrawは2,749,096 bytesで1 GiB以内。Maven／forkはclass単位逐次、当該DB／Ryuk終了後だけ次へ進み、monitorもfinallyで終了した。複数run・補強と回帰を含め採用済み全検証180分以内。heap上限はprocess全memory、DB CPU1は全process CPU、transaction10秒は全JVM／外部I/Oの強制停止を意味しない。

## 6. 設定選択と残条件・レビュー対象

設定選択は開始票の `koiki.reference.notification.managed-config.mode`（未指定disabled／file）、同prefixのpath／expected-sha256／expected-revision／environment-id。foundation=falseでは読取・登録しない。true＋fileだけ起動snapshotを読み、不備なら追加構成の起動を拒否する。true＋disabledは従来の未接続拒否へ戻る。通常properties・Security／依存・migrationは変更していない。

Ownerレビューの中心は、Loaderの同一bytes検査／例外非露出、snapshotの不変性・上限、Scopeの完全一致と設定期間、TTLの残期間と発行直前再検査、条件付きBean選択、実DB／既存保護の拒否・rollback。差分はmain8ファイル、test7ファイル、本Evidence1ファイルの計16件（変更4＋新規12）。git add／commit／pushは行っていない。

本結果は管理設定供給と必要な発行再検査のlocal検証。実装結果のOwner受入は§7で成立。実担当・実path／ACL・信頼manifest・時計監視／許容差・実UUID、現在対象・停止／provider証拠の運用接続、本人受付／worker委譲、通知／復旧／Level2・DoD・正式受渡し・remoteは未成立／対象外を維持する。次は確認またはOwner操作によるcommit固定。

## 7. 実装・検証結果のOwner承認（2026-10-07）

Architecture Ownerはレビューポイントを参照して今回の差分と本検証記録を確認し、次の発言により問題なしとして承認した。

> レビューポイントを確認しながら、差分と検証記録のOwnerレビューしました、問題なしとして承認いたします

承認対象は§6の16ファイルによる管理設定scope／TTL供給、発行直前再検査、条件付き登録と、その限定範囲の検証結果。状態を`COMPLETE / OWNER APPROVED`とする。本承認で§6の残条件・対象外を解消せず、後段実装、Phase 4全体、DoD、正式受渡しまたはremote操作の承認へ拡張しない。

Agentは本承認記録の反映だけを行い、git add／commit／pushは行っていない。commitはOwner操作、またはOwnerによるコミット操作の確認後に行う。
