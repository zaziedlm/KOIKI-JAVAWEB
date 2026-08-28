# KOIKI API Starter

Servlet Spring MVCとJakarta ValidationをKOIKIのAPI runtime baselineとしてまとめるFramework-owned Starter。

CP1では独立Customer-like Consumerが通常のMaven coordinatesで利用するdependency starterとして成立させた。

CP2ではSpring Boot標準設定を用いたJackson 3とpath API Versioningの既定値、およびSpring Framework標準の
Resilience annotation有効化を追加する。実装classは`internal`に閉じ、KOIKI独自Public Java APIは追加しない。
利用者はSpring Boot標準property、または`koiki.api.*` propertyで既定値をoverrideできる。

Problem Detailsと統一例外形式はCP3、SecurityはPhase 2、dataとobservabilityは後続CPの責務であり、
このStarterへ先行実装しない。
