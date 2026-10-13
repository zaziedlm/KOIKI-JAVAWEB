# S1 新PC環境の読取確認（2026-10-13）

状態：環境棚卸し完了。後続工程の実行preflight・開始承認ではない。

Ownerの「新PCの環境を確認を先行しましょう」に従い、[R1引継ぎ](../../development/phase4-s1-r1-fixed-results-cross-pc-handoff-20261013.md)を入口として確認した。R1はCOMPLETE / OWNER ACCEPTEDを維持し、追加検証を行っていない。

## Sourceと環境

- workspace：`C:\KOIKI\koiki-java\KOIKI-JAVAWEB`
- branch：`feature/phase4-s1-reference-async-r1`
- HEAD：`18cb8b626328e84a5ea5d51c27bba9efd2d27bee`。成果commit `610c589`を含む。
- 確認開始時の作業ツリーはclean。ローカルorigin追跡参照も同じHEAD。remote照会は未実施。

| 項目 | 読取結果 |
|---|---|
| OS | Windows 11 Pro、10.0.26100、amd64 |
| JDK | Eclipse Temurin 21.0.12、`java -version`と`javac -version`成立 |
| JAVA_HOME | `C:\Users\s-kataoka\AppData\Roaming\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\21` |
| Maven | PATH上の`mvn`なし。Repositoryの`mvnw.cmd -o -version`で3.9.16／Java 21.0.12を確認 |
| Maven home | `C:\Users\s-kataoka\.m2\wrapper\dists\apache-maven-3.9.16-bin\4200fbc6\apache-maven-3.9.16` |
| 依存cache | `.m2/repository`あり。Modulith events-api／starter-jdbc／events-jdbc 2.1.1、Testcontainers core／PostgreSQL 2.0.5、Playwright／driver-bundle 1.62.0のJAR実体あり |
| Maven設定 | ユーザー`.m2/settings.xml`なし。その他の設定・mirror・proxy・認証や取得可否は未検証 |
| Docker | Rancher DesktopのClient 29.1.4-rd、Server Engine 29.1.3、linux／x86_64。24 CPU、memory約15.50 GiB |
| image | `postgres:17-alpine`（`18cfe3ef5e68`）、`testcontainers/ryuk:0.14.0`（`7c1a8a9a47c7`）あり。他imageには操作していない |
| 稼働container | `docker ps`の読取時点で0件 |
| host資源 | memory総量31.78 GiB／空き11.9 GiB。C空き499.18 GiB、D空き176.68 GiB |
| browser | Chrome／Edgeあり。`%LOCALAPPDATA%\ms-playwright`にchromium-1234／chromium_headless_shell-1234と各exeあり。起動・Playwrightとのrevision適合は未検証 |
| Testcontainers設定 | ユーザー`.testcontainers.properties`あり。内容・適合は未検証、秘密を出力していない |
| 旧PC証拠 | `tmp/r1-975af1d-20261012-first`なし。旧XML・log・runner・固定JARを移送済みとは扱わない |

既存JavaはPID 30304（creation 2026-10-13 09:57:06）と25248（09:57:20）、共通親PID 44788。ExecutablePathはVS CodeのRed Hat Java extension内JREであった。これらを保護対象として扱い、終了・変更していない。PIDとcreationは今回の観測値であり、後続実行時には再確認する。

## 権限エラーと再確認

通常sandboxではDocker config読取と`npipe:////./pipe/docker_engine`接続、OS／processのCIM読取が拒否された。AGENTS.mdの承認済み診断手順と実行環境の権限付き承認手順に従って同じ読取を再実行し、Docker Server応答・container／image・OS資源・既存processを確認した。Engine障害とは判定しない。

Maven Wrapperの通常sandbox実行は、配布物を扱うInstallerの接続処理で`java.net.SocketException: Permission denied: connect`となった。`-o`はWrapper自身の配布物取得を禁止する保証ではなかった。同じversion確認について、必要時のMaven 3.9.16配布物取得を含む権限付き実行承認を経て再実行し、exit 0で上記versionを確認した。成功出力にdownload表示はなく、今回新規取得したとは断定しない。

JDKのversion確認とWrapper実行にCDS shared archiveのbuild差異warningが出た。version確認は成立しているが、buildへの影響は未検証。JDK設定を変更していない。

## 後続へ渡す条件

今回build・test・browser起動・container起動停止・依存解決・remote操作は行っていない。主要cacheの存在は全transitive・pluginの充足、hash適合、オフラインbuild成立を示さない。新規取得の必要一覧は、後続目的・最小検証集合の確定後に対象限定で確認する。

資源はR1開始票の旧目安（空きmemory8 GiB／disk10 GiB）を読取時点で満たすが、旧予算・旧実行承認は再利用しない。後続開始前に目的・対象・資源／時間上限・停止／cleanup条件とOwner判断を確定し、環境・source・artifact・所有記録を新規に固定する。
