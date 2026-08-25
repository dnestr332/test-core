# dnestr-test-core

Multi-module test automation core providing base utilities and specialized frameworks for Web (Playwright) and Mobile (Appium) automation.

## Project Structure

This repository is a Maven multi-module project:

- **`dnestr-base-core`**: Shared core utilities including custom assertions (`Softly`, `Hardly`), a base API client, scenario/failure context, logging (`BaseFailureCatcher`, `BasePrettyPrinter`), common UI states (`FieldState`, `ButtonState`, `ToggleState`, `VisibleState`, `AssertionState`), and a BDD feature-file sync engine that syncs tagged Gherkin scenarios to TestRail and Azure DevOps.
- **`dnestr-web-core`**: Web automation framework built on top of [Microsoft Playwright](https://playwright.dev/java/).
- **`dnestr-mobile-core`**: Mobile automation framework built on top of [Appium](https://appium.io/) and Selenium, supporting both Android and iOS.

## Consuming this library

Published via [JitPack](https://jitpack.io/). Add the JitPack repository and the module(s) you need as a dependency, pinned to a released tag (e.g. `2.0.8`):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.dnestr332</groupId>
        <artifactId>dnestr-base-core</artifactId>
        <version>2.0.8</version>
    </dependency>
    <!-- and/or dnestr-web-core / dnestr-mobile-core -->
</dependencies>
```

## Requirements

- **Java**: JDK 21
- **Maven**: 3.8+
- **Lombok**: Required for compilation (plugin installed in IDE)
- **Playwright Dependencies**: For web automation (run `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"` if needed)
- **Appium Server**: Required for mobile automation

## Setup & Installation

Clone the repository and install the artifacts to your local Maven repository:

```bash
mvn clean install -DskipTests
```

## Scripts & Commands

- **Build all modules**: `mvn clean compile`
- **Run all tests**: `mvn test`
- **Install to local repository**: `mvn clean install`
- **Run tests for a specific module**:
  ```bash
  mvn test -pl dnestr-base-core
  ```

# 2.0.8

## Base Core

### Additions
- Added a generic BDD feature-file sync engine (`com.dnestr.core.tools.bddsync`) that scans `.feature` files for tagged scenarios and rewrites their tags after syncing to an external test management tool
- Added TestRail integration (`com.dnestr.core.tools.testrail`) - creates TestRail cases from scenarios tagged `@sync` and tags them back with the created case ID
- Added Azure DevOps integration (`com.dnestr.core.tools.ado`) - creates or updates ADO test cases from scenarios tagged `@S:` (sprint) and `@us:` (PBI IDs), resolving/creating the sprint's test suite and linking PBIs; scenarios already tagged `@tc:` are updated in place instead of re-created
- Added `jackson-databind` dependency, required by RestAssured to serialize the request bodies used by the new TestRail/ADO clients

# 2.0.0

## Breaking Changes

### Base Core
- Reorganized package structure
- Added FieldState
- Improved state handling

### Web Core
- BaseElement -> PageElement
- BaseTableCell -> TableColumn
- AppTable -> PageTable
- Refactored assertion flows

### Mobile Core
- Reorganized package structure
- Added AppAction
- Moved MobilePlatform to enums
- Improved flow architecture
