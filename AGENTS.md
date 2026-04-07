# DdlUtils Fork - Agent Instructions

## Build & Test Commands

```bash
# Run all tests (uses in-memory HSQLDB by default)
mvn test

# Run a single test class
mvn test -Dtest=TestDatabaseIO

# Run tests against a specific database
mvn test -Djdbc.properties.file=jdbc.properties.postgresql
```

## Test Configuration

- Default test database: in-memory HSQLDB (via `jdbc.properties.file` property in pom.xml)
- Test property files in `src/test/resources/jdbc.properties.*`: `hsqldb`, `derby`, `mysql41`, `mysql50`, `postgresql`, `firebird`, `mckoi`, `axion`
- Tests requiring live database connection are gated on `jdbc.properties.file` being set
- Test patterns: `**/Test*.java`, `**/*TestCase.java`

## Build Requirements

- Maven 3.6+
- Java 8 (enforced by `animal-sniffer-maven-plugin`)
- Source encoding: ISO-8859-1

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

- `src/main/java/org/apache/ddlutils/` - Core library
- `src/test/java/org/apache/ddlutils/` - Tests (platform-specific tests in `platform/` subdirectory)
- `src/test/resources/` - Test resources including DTD and XML schemas

## Key Packages

- `org.apache.ddlutils` - Core API (Platform, PlatformFactory, PlatformUtils)
- `org.apache.ddlutils.model` - Database model classes (Database, Table, Column, ForeignKey)
- `org.apache.ddlutils.platform` - Database-specific implementations (builders, model readers)
- `org.apache.ddlutils.io` - XML parsing/writing (DatabaseIO, DataReader)
- `org.apache.ddlutils.alteration` - Schema comparison and migration
- `org.apache.ddlutils.dynabean` - Dynamic bean support for query results
- `org.apache.ddlutils.task` - Ant task implementations

## Git Branch Naming

- `fix/XXX` - Bug fixes
- `feature/XXX` - New features
