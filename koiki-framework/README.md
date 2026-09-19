# koiki-framework

再利用可能なKOIKI Framework LibraryのCanonical ownership locationです。

このdirectoryはRepository Architecture上のLogical / Target Structureを示す予約位置であり、現時点では
Maven moduleやproduction sourceを持ちません。現在のformal Framework配布単位の成果物は、責務に応じて
`koiki-architecture-contract`、`koiki-archunit-rules`、`koiki-starters/*`および`koiki-testing`へ配置しています。
このうちArchUnit RulesとTesting SupportはTooling-ownedのCustomer-facing testing artifactであり、
production runtime Starterとは区別します。

複数artifactに共通しそうという理由だけで、このdirectoryへ新しい共有moduleを作成しません。
Frameworkへの昇格は、複数案件での再利用性、業務語彙の不在、独立test、互換性およびOwner承認を確認してから行います。
