# KOIKI API Starter

Servlet Spring MVCとJakarta ValidationをKOIKIのAPI runtime baselineとしてまとめるFramework-owned Starter。

Phase 1bで、独立Customer-like Consumerが通常のMaven coordinatesから利用できるdependency starterとして検証済みです。

Spring Boot標準設定を用いたJackson 3とpath API Versioningの既定値、およびSpring Framework標準の
Resilience annotation有効化を提供します。実装classは`internal`に閉じ、KOIKI独自Public Java APIは追加しません。
利用者はSpring Boot標準property、または`koiki.api.*` propertyで既定値をoverrideできます。

Spring Framework標準の`ProblemDetail`、`ErrorResponse`、`ResponseEntityExceptionHandler`を使い、
Validation、入力JSON、直接発生した`JacksonException`および未処理例外をRFC 9457形式へ統一する。
`code`を安定した識別子として付与し、Validationだけは拒否値を含まない`violations`を追加する。
例外messageとclass名は5xx responseへ露出しない。

`koiki.api.problem-details.enabled=false`でKOIKI handlerだけを無効化できる。Applicationが独自の
`ResponseEntityExceptionHandler`を提供した場合も自動構成はback offする。Application固有の業務例外は
ApplicationがSpring標準`ErrorResponse`へ変換し、その`code`とdetailをKOIKIが保持する。業務例外階層を
Framework Public APIとして提供しません。

Security、data、observabilityおよびserver-side UIは、それぞれ対応するStarterへ分離しています。
