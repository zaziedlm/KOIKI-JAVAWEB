# Phase 2 Gate C C-2 publish preflight correction review

## 1. Status and boundary

- 記録日: 2026年9月12日
- Gate: `Phase 2 / Gate C / C-2 corrective slice`
- failed main identity: `31724b60477157432706a1b5ffe8a5d6d0dc85df`
- corrective branch: `fix/phase2-publish-file-uri`
- corrective base: `origin/main` / `31724b60477157432706a1b5ffe8a5d6d0dc85df`
- status: `TOOLING CORRECTION MERGED / REPLACEMENT RUN PUBLISH SUCCEEDED / CAPTURE FAILED`
- Ownership: Tooling / Architecture Evidence / CI Policy
- production artifact / Framework Public API / migration change: 0
- remote package mutation: 0

Gate C-1の承認順序に従い、final PR #31、merge後main CI、protected environment作成およびPhase 2 snapshot workflowの
初回dispatchまで実行した。workflowはpublish前のread-only preflightで停止し、publish / verify jobはskipされた。
本記録は原因、remote非変更、最小修正と再実行前のOwner判断を固定する。

## 2. Completed remote evidence before failure

| Subject | Evidence | Result |
|---|---|---|
| Final PR | [#31](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/31)、head `712595e516f770e2c9bdeadc83f16ed0f3900175` | MERGED |
| PR CI | CI run `34683108279`、runtime run `34683108277` | required checks 7 / 7 success |
| Main merge | merge commit `31724b60477157432706a1b5ffe8a5d6d0dc85df` | SUCCESS |
| Main CI | CI run `34684470051`、runtime run `34684470058` | 全8 job success |
| Environment | `phase2-internal-snapshot` / `21785949807` | reviewer `zaziedlm`、self review可、admin bypass禁止、main限定、secret / variable 0 |
| Ruleset | `main-merge-protection` / `21140116` | active / strict / bypass 0、required 7件不変 |

## 3. Failed workflow and remote non-mutation

[snapshot run `34686248900`](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34686248900)は、
approved main SHA `31724b6...`に対する一回目のdispatchである。

| Job / step | Result |
|---|---|
| `Confirm approved Phase 2 source` | SUCCESS |
| formal package boundary | SUCCESS |
| aggregate Public API signature | SUCCESS |
| local publish and acquisition path | FAILURE |
| `Publish approved Phase 2 release unit` | SKIPPED |
| `Verify Phase 2 remote snapshot` | SKIPPED |

失敗messageは`Cannot bind argument to parameter 'RepositoryUrl' because it is an empty string.`である。
失敗後のGitHub Maven Packagesは既存9件、各version count 1、更新日時も従前のままであり、Phase 2新規4 packageは存在しない。
したがってsnapshot payload、manifestまたはaccepted baselineはremoteへ作成されていない。

## 4. Root cause and correction

`verify-p2-c2-publish-dry-run.ps1`が一時file repository URLを次のcastで生成していた。

```powershell
([uri]$fileRepository).AbsoluteUri
```

Windowsのabsolute pathではfile URIになったが、Ubuntuの`/tmp/...`は相対URIとして解釈され、`AbsoluteUri`が空文字になった。
production artifact、publish manifest、13座標、hash、signatureまたは`japicmp`の問題ではなく、preflight Toolingの
cross-platform path conversion defectである。

修正は`UriBuilder`でschemeを明示し、空またはfile以外のURIをfail-fastする1箇所に限定した。

```powershell
$repositoryUri = [UriBuilder]::new('file', '', -1, $fileRepository).Uri.AbsoluteUri
if ([string]::IsNullOrWhiteSpace($repositoryUri) -or -not ([uri]$repositoryUri).IsFile) {
    throw "Unable to create a file repository URI: $fileRepository"
}
```

## 5. Local verification

修正後に次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-publish-dry-run.ps1
```

結果は13 / 13 deploy、座標別snapshot value、24 payload SHA-256、aggregate signatureおよび11 same-source `japicmp`が
すべてSUCCESSである。Windows pathとUnix形式`/tmp/...`の双方が`UriBuilder`で`file:///...`になることも確認した。
検証後は13 moduleを明示してMaven `clean`し、一時repository / manifest / reportはscriptの`finally`で削除した。

## 6. Retry plan

sourceを変更するため、failed workflowのrerunは使用しない。

1. 本corrective sliceをOwner reviewしcommitする。
2. corrective branchをpushし、`main`向けfollow-up PRを作成する。
3. required checks 7 / 7成功後にmerge commit方式でmergeする。
4. 新しいmerge後main CIの全対象job成功を確認する。
5. environment / ruleset設定が不変であることをread-backする。
6. 新しい40文字main SHAを指定し、Phase 2 snapshot workflowを新規runとして一度dispatchする。
7. publish前environment approval、publish、Captureおよびfresh remote Verifyを確認する。

初回失敗run `34686248900`は失敗Evidenceとして保持し、rerun、package削除または旧main SHAでの再dispatchを行わない。

## 7. Architecture Owner review points

1. 初回runがpreflightで停止し、publish / verify skip、remote package mutation 0であるとの判定を受け入れるか。
2. 原因をUbuntu absolute pathのURI変換に限定したcross-platform Tooling defectとしてよいか。
3. `UriBuilder`とfile URI fail-fastだけの最小修正を承認するか。
4. 13座標 / 24 payload / signature / 11 `japicmp`のlocal再検証成功を受け入れるか。
5. failed runをrerunせず、§6の新しいPR / main CI / new main SHA / new workflow runの順序を承認するか。
6. environment / ruleset / required checks / publish unitを変更せず、旧SHAでのdispatch、無断retryまたはpackage削除を行わないか。
7. 本修正と承認記録のcommit後、corrective branch pushとfollow-up PR作成へ進めてよいか。

推奨結論は上記7点を承認し、§6の順序でGate C-2 remote Evidence取得を再開することである。
Owner承認前はcommit、push、PR、workflow dispatchまたはpublishを行わない。

## 8. Architecture Owner approval

2026年9月12日、Architecture Ownerは§7の7 review pointsを確認し、提案どおり承認した。
初回runがpublish前preflightで停止し、publish / verifyがskipされ、remote package mutationが0であるとの判定を受け入れる。
原因をUbuntu absolute pathのURI変換に限定したcross-platform Tooling defectとし、`UriBuilder`とfile URI fail-fastによる
最小修正、および13座標 / 24 payload / aggregate signature / 11 same-source `japicmp`のlocal再検証結果を承認する。

承認後のremote順序は§6のとおり、新しいPR、required checks、merge後main CI、設定read-back、新しいmain SHAを指定した
新規workflow runとする。environment、ruleset、required checksおよびpublish unitは変更せず、failed runのrerun、旧SHAでの
dispatch、無断retryまたはpackage削除を行わない。本修正と承認記録をcommitした後、corrective branchのpushとfollow-up PR作成へ
進めてよい。Gate C-3 final Architecture Owner review前はPhase 2を完了扱いしない。

## 9. Replacement run result

本修正はPR #32でrequired checks 7 / 7成功後、merge commit `e10f88c82824b9c4b34215480730fa52b8c19e0d`としてmainへ
反映された。同SHAのmerge後CI / runtime全8 job、environment / ruleset read-back成功後、snapshot run
`34693368000`を新規dispatchした。authorizeとpreflightは成功し、protected environment承認後の13座標publishも成功した。

一方、CaptureはGitHub Maven metadataに同一extensionの過去snapshotVersionが累積することを考慮せず、POMを1件だけと
仮定していたため`Expected one pom snapshotVersion; found 3.`で失敗した。hash-only manifest uploadとfresh remote Verifyはskipされた。
公開済みpayloadは削除、rerunまたは再publishせず、次のcorrective reviewへ引き継ぐ。
