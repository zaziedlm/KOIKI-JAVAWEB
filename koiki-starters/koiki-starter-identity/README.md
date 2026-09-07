# KOIKI Identity Starter

Phase 2 P2-B2のFramework-owned Identity contract、内部JPA modelおよびproduction migrationを提供する。

- immutableなFramework user IDとlogin emailを分離し、canonical emailをDBでも一意にする
- User、Role、Permission、local credential、login attempt、external identity linkをFramework tableで所有する
- Public APIはuse-case contractとopaque valueに限定し、JPA Entity / Repository / encoded passwordを公開しない
- Identity migrationの実行にはapplication側で`koiki-starter-data`も導入する
- local authentication / attempt / lock動作はB2-3、Identity administration / Audit連携はB2-4で完成させる
- local password resetとSpring Session adapterは含めない

Public APIとtable契約の正本は
`docs/architecture/validation/phase2-p2-b2-contract-review.md`のB2-C1〜C10とする。
