# KOIKI-JavaWeb-FW Architecture Governance

**版:** v0.1  
**決定日:** 2026年8月14日  
**状態:** Accepted  
**Architecture Owner:** Shuichi Kataoka  
**Decided by:** Shuichi Kataoka

## 1. 目的

KOIKI-JavaWeb-FWのArchitecture判断、ADR承認、Phase完了判定、および定期reviewを、
一人projectの実態に合わせて停止なく追跡可能にする。

形式上の役職を増やすことではなく、誰が判断し、何を根拠に、どこへ記録したかを
Repositoryから再現できることをGovernanceの目的とする。

## 2. Architecture Owner

Architecture Ownerは、**Shuichi Kataoka**とする。2026年8月14日時点では、
本RepositoryのPrimary Maintainerとして、設計・実装判断を行う。

Primary Maintainerが交代した場合、Architecture Ownerも自動的には移管しない。本書を更新し、
旧OwnerまたはRepository管理権限を持つ者が移管記録を残した時点で交代とする。

Architecture Ownerは、グランドデザイン§9.4に加えて次を行う。

- 判断対象の正本と実装証拠を確認する
- ADRの状態と承認日を記録する
- DoDを実演または証拠で確認してPhase完了を判定する
- 未達、例外、保留を完了として扱わず、理由と再判定条件を記録する
- Framework、Reference、Customer、Walking SkeletonのOwnership混在を防ぐ

## 3. 代理者と継続性

一人projectである間は、実在しない代理者を形式的に任命しない。Architecture Owner不在時は、
昇格、Public API変更、ADR承認、Phase完了等の最終判断を停止する。権限を持たない者が
Owner判断を代行しない。

判断再開に必要な文脈は次で確保する。

- グランドデザイン、ADR、DoD、検証記録
- Git commitとPull Requestの履歴
- OpenSpec changeを採用した変更では、そのproposal、design、spec、tasks、archive
- 四半期Architecture Reviewの記録

次のいずれかが成立した時点で、代理者1名を任命して本書を更新する。

- Architecture判断を継続的に担える二人目のMaintainerが参加した
- 外部利用者へ正式releaseまたはsupportを提供する
- Primary Maintainerが長期不在に備える必要があると判断した

代理者はRepositoryへのアクセスだけでなく、正本、ADR、Ownership、DoDを理解し、
Architecture Ownerと同じ証拠基準で判断できる者に限る。

## 4. 判断と承認の記録

一人projectでは自己reviewを認める。ただし、口頭または記憶上の承認だけで完了としない。
次のいずれかへ、対象、判断、根拠、日付を記録する。

- ADRまたはADR register
- Phase Closeout文書
- Architecture Review記録
- 設計変更を含むPull Request

ADRとPhase完了の承認記録には、最低限次を含める。

| 項目 | 内容 |
|---|---|
| 対象 | ADR ID、Phase、または変更対象文書 |
| Decision | Accepted、Rejected、Deferred、またはIncomplete |
| Evidence | test、validation文書、review対象への参照 |
| Rationale | 判断理由と主要trade-off |
| Decided by | Architecture Owner |
| Date | 判定日 |
| Revisit trigger | 再判断が必要になる条件。不要な場合は`None` |

## 5. 四半期Architecture Review

開発活動がある各暦四半期に1回以上、Architecture Ownerが自己reviewを実施する。
活動休止中の四半期はreviewを省略できるが、再開時の最初のreviewに休止期間と外部変化を記録する。

記録は`docs/architecture/reviews/YYYY-Qn-architecture-review.md`へ置き、次の標準agendaを使用する。

1. Framework昇格候補の審議
2. 業務moduleのTier妥当性
3. Spring Modulith採用Level
4. 第三者libraryの追従状況
5. Spring BootおよびJava baseline
6. Public API変更とjapicmp結果
7. Agent Skillsの妥当性
8. 保留中の将来構想
9. 未完了DoD、例外、risk、次四半期の再判断対象

各項目は`No change`を許容するが、未確認のまま省略しない。Architecture判断が発生した場合は、
review記録だけで閉じず、該当ADRまたは正本も更新する。

## 6. Phase完了判定

Architecture Ownerは、対象Phaseの全DoDについて、実演結果または再現可能な証拠を確認する。
判定結果はPhase Closeout文書へ記録する。

- 全件達成: `COMPLETE`
- 外部要因による例外: 理由、影響、期限、再判定条件を記録したうえで明示承認
- 未実演、証拠不足、保留: `INCOMPLETE`

DoD本文を変更して達成扱いにする場合は、旧条件との対応と変更理由を残す。

## 7. 見直し

本Governanceは、代理者任命、Maintainer交代、正式release開始、外部support開始、または
四半期reviewで運用上の不足が判明したときに見直す。
