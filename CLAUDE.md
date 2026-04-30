# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

**Apache Atlas** is a metadata management and data governance platform for the Hadoop ecosystem. This is an ODP (Open Data Platform) customized version 2.4.0.3.3.6.3-101.

**Core Capabilities:**
- Metadata catalog for technical and business metadata
- Data lineage tracking (column-level across Hive, HBase, Kafka, Sqoop, Storm)
- Tag-based classification system
- Full-text and structured search
- Policy-based authorization (simple JSON or Ranger integration)
- Immutable audit trail
- REST API for metadata operations

**Technology Stack:**
- Language: Java 8
- Build: Maven 3.5.0+
- Graph Database: JanusGraph 1.0.0
- Storage Backend: HBase 2.5.0 (via JanusGraph)
- Search Index: Apache Solr 8.11.3
- Messaging: Apache Kafka 2.8.2
- Web Framework: Jersey 1.19.4, Jetty 9.4.56
- Spring Framework: 5.3.39

---

## Build Commands

### Full Build

```bash
# Set Maven memory options
export MAVEN_OPTS="-Xms2g -Xmx2g"

# Clean build with tests
mvn clean install

# Build distribution packages
mvn clean package -Pdist
```

### Skip Tests

```bash
# Skip all tests
mvn clean install -DskipTests

# Skip unit tests only
mvn clean install -DskipUTs=true

# Skip integration tests only
mvn clean install -DskipITs=true
```

### Build Specific Modules

```bash
# Build single module
mvn clean install -pl <module-name> -am

# Example: Build only the repository module
mvn clean install -pl repository -am
```

### Graph Provider Selection

```bash
# JanusGraph (default)
mvn clean install

# Explicitly specify JanusGraph
mvn clean install -DGRAPH-PROVIDER=janus
```

### Build Options

```bash
# Skip code quality checks
mvn clean install -DskipCheck=true

# Skip minification of frontend (faster builds)
mvn clean install -PskipMinify

# Skip documentation generation
mvn clean install -DskipDocs=true
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Single Test Class

```bash
# Using Surefire
mvn test -Dtest=ClassName

# Example: Run entity tests
mvn test -Dtest=EntityREST*
```

### Run Single Test Method

```bash
mvn test -Dtest=ClassName#methodName
```

### Run Tests in Specific Module

```bash
mvn test -pl <module-name>

# Example: Test repository module
mvn test -pl repository
```

### Integration Tests

```bash
# Run integration tests
mvn verify

# Run ITs in specific module
mvn verify -pl webapp
```

### Test Configuration

Tests use TestNG framework. Surefire is configured with:
- Fork count: 2C (2 forks per CPU core)
- Reuse forks: false
- Test output redirected to files in `target/surefire-reports/`

---

## Architecture

### High-Level Module Structure

```
apache-atlas/
├── intg/              # Integration module - Core type system, models, exceptions
├── repository/        # Metadata repository - Entity store, graph operations, audit
├── graphdb/           # Graph database abstraction layer
│   ├── api/          # Graph DB API interfaces
│   ├── common/       # Common graph utilities
│   ├── janus/        # JanusGraph implementation
│   └── janus-hbase2/ # JanusGraph with HBase2 backend
├── webapp/            # REST API server (Jersey/Jetty)
├── server-api/        # Server API interfaces
├── client/            # Java client libraries (v1, v2)
├── notification/      # Kafka notification system
├── authorization/     # Authorization framework
├── common/            # Shared utilities
├── addons/            # Data source bridges/hooks
│   ├── hive-bridge/
│   ├── hbase-bridge/
│   ├── kafka-bridge/
│   ├── sqoop-bridge/
│   └── storm-bridge/
├── dashboardv2/       # Legacy UI (Backbone.js)
├── dashboardv3/       # Modern UI (React)
├── distro/            # Distribution packaging
└── tools/             # Maintenance utilities
```

### Core Architecture Layers

**1. Type System (intg)**
- Defines Atlas type model: EntityDef, ClassificationDef, RelationshipDef
- Located in `intg/src/main/java/org/apache/atlas/type/`
- Types are strongly typed with inheritance support

**2. Repository Layer (repository)**
- Entity CRUD operations: `repository/src/main/java/org/apache/atlas/repository/store/`
- Graph transaction management via Spring AOP advisors
- Audit trail: All entity operations logged to HBase
- Core services:
  - EntityStore: Entity lifecycle management
  - RelationshipStore: Relationship management
  - TypeDefStore: Type definition management

**3. Graph Database Abstraction (graphdb)**
- Pluggable graph backend via `AtlasGraphProvider`
- Current implementation: JanusGraph with HBase2 storage
- Graph query DSL in `graphdb/api/src/main/java/org/apache/atlas/repository/graphdb/`
- Transaction boundaries managed by `GraphTransactionInterceptor`

**4. REST API Layer (webapp)**
- JAX-RS resources in `webapp/src/main/java/org/apache/atlas/web/rest/`
- Key endpoints:
  - `/v2/entity` - Entity operations (EntityREST.java)
  - `/v2/types` - Type management (TypesREST.java)
  - `/v2/search` - Search operations (DiscoveryREST.java)
  - `/v2/lineage` - Lineage queries (LineageREST.java)
  - `/v2/glossary` - Business glossary (GlossaryREST.java)
  - `/v2/relationship` - Relationship operations (RelationshipREST.java)

**5. Notification System (notification)**
- Kafka-based async notification bus
- Hooks publish metadata changes to `ATLAS_HOOK` topic
- Atlas server consumes notifications via `NotificationHookConsumer`
- Enables decoupled integration with data sources

**6. Bridges/Hooks (addons)**
- Each bridge captures metadata from a data source
- Common pattern: Hook intercepts operations → Creates entities → Publishes to Kafka
- Hive Bridge: Captures table/column lineage from HiveQL
- HBase Bridge: Captures HBase table/namespace metadata
- Kafka Bridge: Captures topic/schema metadata

### Key Design Patterns

**Graph Transaction Management:**
- Uses Spring AOP to wrap service methods in transactions
- `@GraphTransaction` annotation marks transactional boundaries
- Transaction interceptor in `repository/GraphTransactionInterceptor.java`

**Plugin Classloaders:**
- Graph database implementations loaded via isolated classloaders
- Prevents dependency conflicts between different graph backends
- Implementation in `plugin-classloader/`

**Type Registry:**
- Centralized type system registry in `AtlasTypeRegistry`
- Maintains in-memory cache of type definitions
- Lazy loading and initialization on startup

**Entity Audit:**
- Every entity mutation recorded in audit repository
- Default: HBase-based audit (production)
- InMemory implementation for testing

---

## Development Workflow

### Running Atlas Locally

After building, Atlas server artifacts are in `distro/target/`. Refer to `ATLAS-ODP-DEPLOYMENT-GUIDE.md` for deployment instructions.

### Configuration Files

Runtime configuration in `distro/src/conf/`:
- `atlas-application.properties` - Main configuration
- `atlas-env.sh` - Environment variables, JVM settings
- `atlas-logback.xml` - Logging configuration
- `users-credentials.properties` - Basic auth users

### Code Style

Atlas uses Checkstyle for code quality:
```bash
# Run checkstyle
mvn checkstyle:check

# Generate checkstyle report
mvn checkstyle:checkstyle
```

Build configuration: `build-tools/src/main/resources/atlas/`

### Code Analysis

```bash
# Run Findbugs
mvn findbugs:check

# Generate code metrics report
mvn javancss:report

# Run Apache RAT license check
mvn apache-rat:check
```

---

## Key Implementation Notes

### Graph Backend

- **Default**: JanusGraph 1.0.0 with HBase2 storage backend and Solr indexing
- Graph schema initialization in `repository/src/main/java/org/apache/atlas/repository/graph/GraphBackedSearchIndexer.java`
- Index management in `repository/src/main/java/org/apache/atlas/repository/store/graph/v2/AtlasGraphUtilsV2.java`

### Entity Model

Atlas uses a rich entity model:
- **Entity**: Instance of a type (e.g., a specific Hive table)
- **Classification**: Tags/labels attached to entities
- **Relationship**: Typed relationships between entities
- All have GUIDs for global identity

Entity serialization happens in:
- V2 API: `intg/src/main/java/org/apache/atlas/model/instance/`
- Graph mapping: `repository/src/main/java/org/apache/atlas/repository/store/graph/v2/`

### Search Implementation

Two search backends:
1. **Basic Search**: Simple attribute-based queries
2. **DSL Search**: Advanced query language (based on ANTLR grammar)

DSL grammar: `repository/src/main/antlr4/org/apache/atlas/query/antlr4/AtlasDSLParser.g4`

### Lineage Computation

Lineage stored as graph relationships:
- Input/Output relationships between processes and datasets
- Lineage queries traverse graph starting from entity GUID
- Direction: INPUT (upstream) or OUTPUT (downstream)
- Implementation in `repository/src/main/java/org/apache/atlas/discovery/EntityLineageService.java`

---

## Testing Practices

### Test Categories

1. **Unit Tests**: Fast, isolated tests with mocked dependencies
2. **Integration Tests**: Test with embedded HBase/Solr/Kafka
3. **End-to-End Tests**: Full server lifecycle tests

### Test Utilities

- `test-tools/` module provides testing utilities
- Embedded servers for HBase, Kafka, Solr in test scope
- `TestUtilsV2` in `intg` module for common test helpers

### Graph Test Infrastructure

Graph tests extend `AtlasGraphUtilsV1Test` or use `TestUtils.createAtlasGraph()`

---

## Debugging

### Enable Debug Logging

Edit `distro/src/conf/atlas-logback.xml`:

```xml
<logger name="org.apache.atlas" level="DEBUG"/>
<logger name="org.apache.atlas.repository" level="TRACE"/>
```

### Common Issues

**OutOfMemoryError during build:**
```bash
export MAVEN_OPTS="-Xms2g -Xmx4g"
```

**Test failures due to port conflicts:**
Tests use embedded services on random ports, but check for:
- HBase: 16000-16050
- Kafka: 9092, 9027
- Solr: 8983, 8984
- Zookeeper: 2181, 2182

**Graph backend initialization failures:**
Check HBase and Zookeeper are accessible and `atlas.graph.storage.hostname` is configured correctly.

---

## Module Dependencies

Key dependency relationships:
- `repository` depends on `intg`, `graphdb/api`, `notification`
- `webapp` depends on `repository`, `server-api`
- Bridges depend on `intg`, `notification`
- `client` modules depend only on `intg`

When modifying `intg` module, rebuild dependent modules:
```bash
mvn clean install -pl intg,repository,webapp -am
```

---

## Version and Branch Information

Current version: `2.4.0.3.3.6.3-101` (ODP customized)

This is a vendor-specific build for ODP stack integration with Hadoop 3.3.6.3.3.6.3-101 and Hive 4.0.1.3.3.6.3-101.

Main branch: `master`

---

## Additional Resources

- **Official Atlas Docs**: https://atlas.apache.org
- **Deployment Guide**: `ATLAS-ODP-DEPLOYMENT-GUIDE.md` in project root
- **REST API Scripts**: `dev-support/atlas-scripts/` contains curl-based API examples
- **Issue Tracker**: https://issues.apache.org/jira/browse/ATLAS
