# KOIKI Testing Support

Customer testからSpring Boot TestcontainersとPostgreSQL TestcontainersをBOM管理された同一依存で
利用するためのTooling artifactである。CP4では独自Java abstractionを追加せず、Spring Boot標準の
`@ServiceConnection`とTestcontainers APIを直接利用する。
