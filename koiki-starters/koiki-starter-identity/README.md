# KOIKI Identity Starter

Phase 2 P2-B2のFramework-owned Identity contract、内部JPA modelおよびproduction migrationを提供する。

- immutableなFramework user IDとlogin emailを分離し、canonical emailをDBでも一意にする
- User、Role、Permission、local credential、login attempt、external identity linkをFramework tableで所有する
- Public APIはuse-case contractとopaque valueに限定し、JPA Entity / Repository / encoded passwordを公開しない
- Identity migrationの実行にはapplication側で`koiki-starter-data`も導入する
- local password認証は`koiki.identity.local-authentication.enabled=true`で明示的に有効化する
- Spring標準のdelegating password encoder、永続User、ACCOUNT / SOURCE試行制御、lock / automatic unlockを提供する
- SOURCE保護は`APPLICATION`が既定で、32 byte以上のdeployment HMAC secretとkey IDを必須とする
- 実client sourceを識別する承認済み公開境界へ保護責務を移す場合だけ`EXTERNAL`を選択できる。ACCOUNT保護は継続する
- `IdentityAdministration`は、user / Role / Permission / local credential / external linkの管理操作を提供する
- 管理操作beanはBusiness / Security Audit、`CompromisedPasswordChecker`、`UserSessionInvalidator`がすべて存在する場合だけ構成する
- 必須依存が欠ける場合も`IdentityQuery`は維持し、管理操作beanだけを構成しない。管理機能が必須注入すればstartup時に欠落を検出する
- disable、password変更、Role / Permission変更、external unlinkは同期Session失効に成功しなければrollbackする
- account disableのSecurity Audit失敗だけは防御操作を継続してalertし、管理unlockのAudit失敗はunlockをrollbackする
- 管理操作のactorは認証済み`FrameworkPrincipal`のimmutable user IDとし、email、password、issuer / subjectをAuditへ複製しない
- local password resetとSpring Session adapterは含めない

Public APIとtable契約の正本は
`docs/architecture/validation/phase2-p2-b2-contract-review.md`のB2-C1〜C10とする。
