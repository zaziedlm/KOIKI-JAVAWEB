# KOIKI Observability Starter

Phase 1b CP5／CP6のFramework-owned leaf artifactである。Spring Boot組込みstructured logging、Servlet
requestの相関ID、Micrometer Context Propagationを適用するSpring標準`TaskDecorator`、およびActuatorの
基本health contractを構成する。

- console structured loggingの低優先度既定は`logstash`
- `X-Request-ID`を検証して受け入れ、不在または不正ならUUIDを生成する
- MDC keyは`requestId`。request終了時に必ず復元または削除する
- `@Async`自体の有効化と業務ログはApplicationが所有する
- Web公開endpointはhealthだけを低優先度既定とし、component名とstatusを公開してdetailは公開しない
- liveness／readiness probeを有効化するが、外部依存を各probeへ自動追加しない
- DB等をreadinessへ含める判断はApplicationが所有する
- SecurityContext accessor、exporter、cloud backend、業務語彙は含めない
- KOIKI独自`HealthIndicator`は提供せず、Spring Boot標準contributorを使用する
- Public Java APIは提供しない
