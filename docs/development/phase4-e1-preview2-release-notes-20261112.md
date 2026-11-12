# KOIKI Framework 0.1.0-preview.2 Release notes案

状態：**DRAFT／未公開**。ローカル技術候補の検証結果は[同梱対応結果](../architecture/validation/phase4-e1-sources-javadoc-results-20261112.md)を参照する。公開対象commit・最終候補hash・Release作成は未確定であり、この文書は公開指示を意味しない。

## 変更点

- 配布ZIPに本体12 JAR・14 POMに加え、Java sourceのある11座標へ`-sources.jar`、公開packageのある5座標へ`-javadoc.jar`を同梱する。
- sourceはinternalを含む実装参照用。internal packageは互換性を保証するPublic APIではない。Javadocは公開packageを対象にし、記述の拡充は今回行わない。Java sourceを持たない`koiki-testing`にはclassifier JARを付けない。
- 同梱`prepare-koiki.ps1`でclassifierもローカルMaven repositoryへ登録し、42 payloadのhashを照合する。IDEとMavenで同じrepositoryを選択する。source／Javadoc JARをApplicationの依存へ追加する必要はない。
- Session JDBC Starterのauto-configurationをBootの`SessionAutoConfiguration`より後へ並べ、Applicationが独自の`CookieSerializer`を宣言しなくてもBoot既定serializerでlogout handler／customizerが成立するよう是正した。Application独自serializerの優先は維持する。

## 移行時の注意

**`koiki/fragments :: header`を削除した。** preview.1まで同梱していたheaderはReference固有の表題・業務routeを含んでいたため、Reference所有の`reference/fragments :: header`へ移した。Customer Applicationは自分のheader／navigationを持ち、旧fragment参照を置き換える。共通の`koiki/fragments :: head(title)`は引き続き利用できる。これはJavaのPublic API互換検査では検出されないresourceの変更である。

配布manifestはschema2となる。preview.2のZIPにはpreview.2に同梱された準備scriptを使用し、preview.1のscriptを流用しない。逆にschema1のZIPはその版の同梱scriptを使う。公開済み`framework-0.1.0-preview.1`は変更しない。

Parentへsource／Javadoc生成pluginを追加していないため、Applicationの通常buildにこれらの生成goalは追加されない。Java Public API・依存baseline・migrationの追加変更もない。

## 検証・残件

ローカル候補でFramework111件、準備script16確認、独立Consumer1 test／8 checkpoint、42 payload登録・hash照合、実行JARへのclassifier非混入が成立した。今回のヘッダー／Session是正は別の[Windows自動検証](../architecture/validation/phase4-consumer-feedback-windows-verification-20261112.md)と[Owner目視確認](../architecture/validation/phase4-consumer-feedback-header-manual-20261112.md#6-ownerの目視確認報告2026-11-12)に追跡する。

IDEで`IdentityQuery`のJava sourceと型hoverのJavadocが表示されることはOwner確認済み。成果の固定と最終候補の生成／照合、CI、公開後のremote取得・空recipient登録・Consumer確認はそれぞれ別の記録を必要とする。現候補は未コミット差分を含む技術検証用であり、そのまま公開対象と確定していない。

O-1（logout customizerの全chain自動適用・明示呼出しによる二重登録）、正式support、実案件PC／IDE／CI採用、D3／Phase4全体の受入は、この版の同梱対応で解消・認定しない。
