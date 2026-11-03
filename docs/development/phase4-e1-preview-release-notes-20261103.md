# KOIKI Framework 0.1.0-preview.1

E1固定版配布の開発previewです。正式support付き提供、Phase 4全体完了、全DoD成立の認定ではありません。

## 配布対象

- Repository: `zaziedlm/KOIKI-JAVAWEB`（public）
- Tag: `framework-0.1.0-preview.1`
- 公開対象commit: `3887d39aa52270d6a643af199da2bfdaa11d264d`
- Asset: `koiki-framework-0.1.0-preview.1.zip`（205,900 bytes）
- SHA-256: `470a2fac4a30d3c323e38341b855e23e263c7aced501c3c8a378e06db1a8b27c`
- 全14座標を同一版で配布（14 POM＋12 JAR）。Reference・Consumer・検証Toolingは配布しません。

## Sourceと候補の対応

2026-11-03にJAR一式を新しくビルドした候補を使用します。旧candidate-3は公開しません。
manifestの生成元は`3887d39aa52270d6a643af199da2bfdaa11d264d`です。
点検記録・notes案2ファイルが未commitだったため`sourceDirty=true`で、その文書内容もsource inventoryに保存しています。
Framework・POM・Wrapper・生成／準備scriptに未commit差分はありません。

版変換はstaging内で行い、checkoutのMaven版は`0.1.0-SNAPSHOT`を維持します。
配布対象14モジュールは旧候補から機能変更せず、現在のsourceから再ビルドしています。

- Source inventory SHA-256: `c7fcabeaab9e3d7e6a3c6606b27c381fa68936f0d2b36bb07550c2824a531fd8`
- Staging変換記録 SHA-256: `0818938495832ecda4e591552cd7bed467f4ae6af2a131308728c0f398cff214`

## 検証と制限

Windows再現結果ではTemurin21.0.13+9／Maven3.9.16で候補生成111件、準備6確認、Consumer1 invocation／8 checkpoint、通常root449実行PASS＋100 skipが訂正後に成立しています。初回失敗・訂正履歴を含む限定結果です。

今回の新規生成はTemurin21.0.13+9／Maven3.9.16、offline `clean package`で111件PASS（failure／error／skip 0）、45.054秒でした。14座標／26 POM・JAR、manifest対象28ファイルのsize／hash検査が成立しています。
通常root・DB journeyの追加実行はしておらず、上記2026-10-30の結果と区別します。
2026-11-03にRelease公開とV09の限定技術確認が成立しました。GitHub asset digestと生成ZIPのhash一致、準備scriptによるremote実取得、新しい専用Maven repositoryへの14座標登録、独立Consumerの`-DskipTests package`成功（26.371秒）を確認しました。登録26 payloadのhashもmanifestと一致し、installer lock残留はありません。
今回ConsumerのJUnit／DB journeyは再実行していません。実案件各PCの既定`.m2`、実IDE／実案件CI／チーム受領V10、結果Owner受入は未完了として維持します。

R-05はCustomer自身のEntityScan明示、R-06はData Starter明示・同一DataSource／public schemaの限定構成です。Consumer HTTP確認はtest application contextであり、packaged JARの別process受入ではありません。
S1既知未達・人手HOLD、C2 H07未実証、D1結果Owner受入未記録を維持します。

## 取得

公開後、当該commitの`build-support/framework-distribution/prepare-koiki.ps1`を使用し、版・repository・上記SHA-256を明示してください。

```powershell
./prepare-koiki.ps1 -Version 0.1.0-preview.1 `
  -Repository zaziedlm/KOIKI-JAVAWEB `
  -ExpectedSha256 470a2fac4a30d3c323e38341b855e23e263c7aced501c3c8a378e06db1a8b27c
```

必要ツールはPowerShell7、JDK21、Maven3.9.16、GitHub CLIです。第三者依存は通常のMaven repositoryから取得します。
ZIP同梱READMEも現在のcheckoutから再収録しました。上記repository・版・hashを明示して取得してください。
同一版assetの差替え・異なる版のモジュール混在は行わないでください。
