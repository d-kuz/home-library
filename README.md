Проект для управления книгами и авторами с поддержкой кэширования, фильтрации и интеграции с Kafka.

**Основные возможности**:
- CRUD операции с книгами и авторами
- Пакетное создание книг с автоматическим созданием авторов
- Кэширование авторов (Spring Cache)
- Поддержика многопоточности
- Интеграционные тесты (TestContainers)

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
