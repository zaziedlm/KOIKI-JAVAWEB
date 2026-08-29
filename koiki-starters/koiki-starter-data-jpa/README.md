# KOIKI Data JPA Starter

Phase 1b CP6のFramework-owned JPA profile leaf artifactである。Spring BootのJPA Starterを集約し、
`spring.jpa.open-in-view=false`を上書き可能な低優先度既定として適用する。

- persistence-neutralな`koiki-starter-data`とは分離する
- Applicationは`spring.jpa.open-in-view`を明示的に上書きできる
- OSIVを有効へ戻すと、EntityのWeb境界露出を実行時に検出しにくくなる
- 業務Entity、Repository、migration、KOIKI独自Java APIは含めない
