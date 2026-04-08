# DdlUtils Fork - Agent Instructions

## Build & Test Commands

```bash
# Run all tests (all modules, uses in-memory Derby by default)
mvn test

# Run tests for a specific module
mvn test -pl ddlutils-lib
mvn test -pl ddlutils-ant

# Run a single test class (in ddlutils-lib)
mvn test -pl ddlutils-lib -Dtest=TestDatabaseIO

# Run tests against a specific database (ddlutils-lib; JDBC settings in src/test/resources)
mvn test -pl ddlutils-lib -Djdbc.properties.file=jdbc.properties.postgresql

# Build specific module with dependencies
mvn install -pl ddlutils-lib -am

# Build all modules
mvn install
```

## Tests contra PostgreSQL (Podman)

Los ajustes de conexión están en `ddlutils-lib/src/test/resources/jdbc.properties.postgresql` (por defecto: `localhost`, puerto **5432**, base **ddlutils**, usuario **postgres**, contraseña **root123**). El contenedor debe usar los mismos valores.

1. **Descargar la imagen** (si no la tienes en local):

```bash
podman pull docker.io/library/postgres:14
```

2. **Levantar PostgreSQL 14** (ajusta el nombre del contenedor si ya existe):

```bash
podman run -d --name ddlutils-pg14 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root123 \
  -e POSTGRES_DB=ddlutils \
  -p 5432:5432 \
  docker.io/library/postgres:14
```

Si el puerto **5432** del host está ocupado, mapea otro (p. ej. `-p 5433:5432`) y en `jdbc.properties.postgresql` cambia la URL a `jdbc:postgresql://localhost:5433/ddlutils`.

3. **Comprobar** que acepta conexiones:

```bash
podman exec ddlutils-pg14 pg_isready -U postgres -d ddlutils
podman exec ddlutils-pg14 psql -U postgres -d ddlutils -c 'select 1'
```

4. **Ejecutar tests** desde el directorio `ddlutils-fork`:

```bash
# Una sola clase (p. ej. comprobar conexión antes de la suite completa)
mvn test -pl ddlutils-lib \
  -Djdbc.properties.file=jdbc.properties.postgresql \
  -Dtest=TestDynaSqlQueries

# Todos los tests del módulo ddlutils-lib contra PostgreSQL
mvn test -pl ddlutils-lib -Djdbc.properties.file=jdbc.properties.postgresql
```

**Nota:** `TestDatatypes` no ejecuta sus casos cuando el perfil JDBC es PostgreSQL (los roundtrips de tipos están alineados con Derby/HSQL embebidos; el comportamiento en PostgreSQL difiere). El resto de tests del módulo sí se ejecutan contra la base indicada.

## Test Configuration

- Default test database: in-memory Derby (via `jdbc.properties.file` property in pom.xml)
- Test property files in `src/test/resources/jdbc.properties.*`: `derby-embedded`, `hsqldb`, `derby`, `mysql41`, `mysql50`, `postgresql`, `firebird`
- Tests requiring live database connection are gated on `jdbc.properties.file` being set
- Test patterns: `**/Test*.java`, `**/*TestCase.java`

## Build Requirements

- Maven 3.6+
- Java 8 (enforced by `animal-sniffer-maven-plugin`)
- Source encoding: UTF-8

## Code Style

- Oracle Sun conventions
- Line limit: 120 characters max
- Source code in English
- Avoid static imports except in the class that declares the static member
- `extends`/`implements`: 4-space indent on line after class definition
- Opening brace on next line for class definitions
- Indentation: 4 spaces for Java/XML
- Line endings: CRLF (Windows compatibility)

## Project Structure

Multi-module Maven project with the following modules:

- `ddlutils-lib/` - Core library
  - `src/main/java/org/apache/ddlutils/` - Core library source
  - `src/test/java/org/apache/ddlutils/` - Tests
  - `src/test/resources/` - Test resources including DTD and XML schemas

- `ddlutils-ant/` - Ant task implementations
  - `src/main/java/org/apache/ddlutils/task/` - Ant task source

- `old_stuff/` - Legacy/unused files (to be cleaned up)

## Modules

- **ddlutils-lib**: Core library containing DDL parsing, database platform support, XML I/O, and schema alteration/migration
- **ddlutils-ant**: Ant task implementations for DdlUtils operations

## Key Packages (in ddlutils-lib)

- `org.apache.ddlutils` - Core API (Platform, PlatformFactory, PlatformUtils)
- `org.apache.ddlutils.model` - Database model classes (Database, Table, Column, ForeignKey)
- `org.apache.ddlutils.platform` - Database-specific implementations (builders, model readers)
- `org.apache.ddlutils.io` - XML parsing/writing (DatabaseIO, DataReader)
- `org.apache.ddlutils.alteration` - Schema comparison and migration
- `org.apache.ddlutils.dynabean` - Dynamic bean support for query results
- `org.apache.ddlutils.task` - Ant task implementations (in ddlutils-ant)

## Git Branch Naming

- `fix/XXX` - Bug fixes
- `feature/XXX` - New features
