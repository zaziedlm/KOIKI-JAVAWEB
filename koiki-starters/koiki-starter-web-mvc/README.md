# KOIKI Web MVC Starter

Phase 3 P3-B2のServlet MVC / Thymeleaf / Bean Validation依存集約と、KOIKI共通の
classpath resourceを提供する。

- Java Public APIは持たない。
- Security、Identity、Sessionまたは業務routeを自動構成しない。
- `/koiki-web/**`から共通styleを配布する。
- `koiki/fragments`を共通head / navigation fragmentとして提供する。
- HTMX 2.0.10はlocal assetとして固定し、interactionとCSRF統合はP3-B3で実証済みである。
- Customer固有theme、業務Form、Controller、View DTOを所有しない。
