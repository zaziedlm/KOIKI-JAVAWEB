# KOIKI Audit Starter

Phase 2 P2-B1のFramework-owned Audit contractと内部JPA永続化を提供する。

- `BusinessAuditRecorder`は既存transactionを必須とし、業務変更とAudit rowを同時にcommit / rollbackする
- `SecurityAuditRecorder`は`REQUIRES_NEW`で独立commitする
- Auditの正本はDB rowであり、Application log、ファイルまたは外部log backendへ依存しない
- actor、event、resource等の値にPassword、token、secret、raw emailまたは外部subjectを渡さない
- production Flyway migration `V2026090300__create_koiki_audit.sql`を同梱し、非配布fixtureとは分離する
- Spring transaction、JPA Entity / Repository、transaction分類enumをPublic APIへ露出しない

Public APIとtransaction判断は
`docs/architecture/validation/phase2-p2-b1-contract-review.md`のB1-C1〜C7を正本とする。
