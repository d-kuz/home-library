Проект для управления книгами и авторами.

**Основные возможности**:
- CRUD операции с книгами и авторами
- Пакетное добавление книг с автоматическим созданием авторов при необходимости
- Кэширование авторов
- Поддержика многопоточности
- Интеграционные тесты (TestContainers)
- Тесты с kafka (TestContainers)

**Технологический стек**
- Java 21
- Spring Boot 3.5.11
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- Spring Cache
- Spring Web
- Spring Kafka (в тестовой части)
- Testcontainers + Doker (для интеграционного тестирования)
- OpenAPI (springdoc-openapi) для документации API
