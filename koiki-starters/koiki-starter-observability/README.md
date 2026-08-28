# KOIKI Observability Starter

Phase 1b CP5のFramework-owned leaf artifactである。Spring Boot組込みstructured logging、Servlet requestの
相関IDおよびMicrometer Context Propagationを適用するSpring標準`TaskDecorator`を構成する。

- console structured loggingの低優先度既定は`logstash`
- `X-Request-ID`を検証して受け入れ、不在または不正ならUUIDを生成する
- MDC keyは`requestId`。request終了時に必ず復元または削除する
- `@Async`自体の有効化と業務ログはApplicationが所有する
- SecurityContext accessor、exporter、cloud backend、業務語彙は含めない
- Public Java APIは提供しない
