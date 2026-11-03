# E1：ローカルPC公開前点検（2026-11-03）

**後続判断：** Ownerは既存candidate-3の公開から、現在sourceのJAR一式を再生成して公開・取得する検証へ変更した。配布先はReleases、受領側は既存独立Consumerを使用することを明示した。以下のcandidate-3公開案は履歴であり、現行候補は[再生成・公開記録](phase4-e1-regeneration-and-publication-20261103.md)を参照する。

## 1. 作業範囲と現在地

Ownerは[引継ぎ文書](../../development/phase4-e1-vscode-handoff-20261030.md)を入口に、Cloud未実施のJAR ZIP公開をローカルPCで検証するよう指示した。
本記録はE1／ToolingのV09公開前点検。Framework変更・通常root再実行・V10の代行・PR／mergeは行っていない。
公開対象commitとnotesの確認前にtag／Release作成・asset uploadは行っていない。

## 2. 点検結果

| 項目 | 結果 |
|---|---|
| Branch | `docs/phase4-s1-b1-c2-guarantee-review-20261030` |
| HEAD／remote branch | `3887d39aa52270d6a643af199da2bfdaa11d264d`で一致 |
| 開始時checkout | clean |
| 実行環境 | Windows11ローカルPC、Temurin21.0.13+9／Maven3.9.16 |
| GitHub CLI主体 | `zaziedlm` |
| Repository | `zaziedlm/KOIKI-JAVAWEB`、PUBLIC、viewerPermission ADMIN |
| 対象tag／Release | local／remote tagなし、Release not found |
| 候補 | 既存local candidate-3、ZIP 205,619 bytes |
| ZIP SHA-256 | `e3cfe45514cd0af67177416467c052bc61b815bda375c57a4a12cc7798992928`、既存記録と一致 |
| Source inventory SHA-256 | `06c6f8d8b70de307f3addc8be0400aa844b540aa51eaaad1eb71783cdd246d49`、既存記録と一致 |
| Staging変換記録 SHA-256 | `0818938495832ecda4e591552cd7bed467f4ae6af2a131308728c0f398cff214`、既存記録と一致 |
| Payload | manifest対象28ファイルのsize／hash一致、ZIP全30 entryと展開済み候補のhash一致 |
| Source inventory照合 | 1,390ファイル。差分13ファイルはいずれも文書／配布対象外の検証補助。追加tracked pathなし |
| 配布対象14モジュール | source inventoryから差分・追加・削除なし |
| Root POM／Wrapper | 生成元commitから差分なし、inventory照合も一致 |

制限内のGitHub CLI実行はproxy接続拒否で失敗し、`gh auth status`もinvalid表示となった。
権限付きの同じ読取確認で認証・接続・権限確認が成立した。認証情報の変更は行っていない。
初回の追加path照合ではGitの日本語path引用により12件が見かけ上の追加となったが、`core.quotepath=false`による再照合で追加0を確認した。

## 3. 公開案と次の確認

- 公開対象commit案：`3887d39aa52270d6a643af199da2bfdaa11d264d`。
- tag：`framework-0.1.0-preview.1`。既存public repoへprereleaseを作成し、candidate-3の同じZIPをuploadする。
- [Release notes案](../../development/phase4-e1-preview-release-notes-20261103.md)に元manifestのsource SHA／dirty、公開commitとの内容対応、同梱READMEの旧例示、既知制限を記載。
- 公開対象・notes確認後、tag作成／push・Release作成／upload、remote bytesの実取得・hash照合、新しい専用Maven repositoryへの14座標登録とConsumer `-DskipTests package`でV09を確認する。
- 失敗時はremote状態を点検し、既存tag／assetを自動削除・差替えしない。

V09は公開・実取得前のため未完了。V10、結果Owner受入、正式support付き提供、PR／main統合、D3も未完了。
S1既知未達・人手HOLD、C2 H07、D1結果受入未記録は変更しない。
本記録・notes案は未commit。今回の公開案に作業branchの追加commit／push・PR／mergeを含めない。
