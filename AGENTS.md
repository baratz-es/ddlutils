# DdlUtils Fork - Agent Instructions

## Build & Test Commands

```bash
# Run all tests (all modules, uses in-memory HSQLDB by default)
mvn test

# Run tests for a specific module
mvn test -pl ddlutils-lib
mvn test -pl ddlutils-ant

# Run a single test class (in ddlutils-lib)
mvn test -pl ddlutils-lib -Dtest=TestDatabaseIO

# Run tests against a specific database
mvn test -Djdbc.properties.file=jdbc.properties.postgresql

# Build specific module with dependencies
mvn install -pl ddlutils-lib -am

# Build all modules
mvn install
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