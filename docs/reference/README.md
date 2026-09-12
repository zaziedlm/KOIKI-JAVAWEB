# Reference

- [KOIKI-JavaWeb-FW Reference Application 業務仕様 v0.1](KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md):
  Reference Applicationの業務scope、状態遷移、不変条件、権限matrix、代表Use Case、
  module間連携、およびPhase別受入条件の正本

本directoryはReference Ownershipの仕様文書を管理する。Reference ApplicationはFrameworkの
利用例であり、Framework内部として扱わない。Walking SkeletonのJava class、Template、SQLを
正式Referenceへ直接移植しない。

## Phase 2 identity reference

Phase 2の`koiki-reference-app/identity`はTier 1の正式Referenceであり、Controller → Application Use Case →
Framework Public contractによるIdentity user参照とRole付与 / 剥奪を実演する。Framework Entity、Repository、internal package、
migrationを所有せず、管理操作のMethod Security、Business Audit、全Session失効、optimistic versionと失敗時rollbackを扱う。

- [source](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity)
- [acceptance](../architecture/validation/phase2-p2-b4-b4-5-closeout.md)
- [packaged verification](../../build-support/security-foundation-verification/verify-p2-b4-reference-journey.ps1)
- [developer entry](../development/phase2-developer-journey.md)

このReferenceはCustomer Project Template、初期user provisioning、固定Role体系または完成済み管理Applicationではない。
Customerは業務語彙、Role / Permission、画面、provisioning、migrationを自身のOwnershipで設計する。
