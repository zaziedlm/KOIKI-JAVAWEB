# KOIKI API Starter

Servlet Spring MVCとJakarta ValidationをKOIKIのAPI runtime baselineとしてまとめるFramework-owned Starter。

CP1では独立Customer-like Consumerが通常のMaven coordinatesで利用するdependency starterとして成立させ、
Java classやKOIKI独自Public APIを持たない。Spring Boot Starterと同じく、空の将来moduleではなくPOMの
依存契約そのものが成果物である。Jackson、Resilience、API Versioning、Problem Details等の
auto-configurationは、それぞれのpositive / negative / override検証を伴うCP2／CP3で追加する。
