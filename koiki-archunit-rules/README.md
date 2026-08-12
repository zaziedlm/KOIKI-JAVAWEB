# koiki-archunit-rules

Customer Applicationへ配布するArchUnit rulesのWalking Skeletonです。

Phase 0では代表規則と外部Consumerからの利用可能性を検証します。Maven座標は
`dev.koiki.walkingskeleton`の一時座標ですが、Java APIは正式候補の
`org.koikifw.archunit`で検証します。

Phase 1aへ持ち込む際は、検証済みのルール実装を正式artifactへ適応し、
全39規則へ拡張します。
