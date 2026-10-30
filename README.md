# test-core

Multi-module test automation core providing base utilities and specialized frameworks for Web (Playwright) and Mobile (Appium) automation.

> **New here? Read [GUIDE.md](GUIDE.md) first.** It shows the enum-driven pattern the framework is built around — one
> generic click/type/verify step for every element of every page or screen — with a step-by-step walkthrough for web
> and mobile, how to wire it with Spring, and what the base module offers.

## Project Structure

This repository is a Maven multi-module project:

- **`base`**: Shared core utilities including custom assertions (`Softly`, `Hardly`), a base API client, scenario/failure context, logging (`BaseFailureCatcher`, `PrettyPrinter`), common UI states (`FieldState`, `ButtonState`, `ToggleState`, `VisibleState`, `AssertionState`), and a BDD feature-file sync engine that syncs tagged Gherkin scenarios to TestRail and Azure DevOps.
- **`web`**: Web automation framework built on top of [Microsoft Playwright](https://playwright.dev/java/).
- **`mobile`**: Mobile automation framework built on top of [Appium](https://appium.io/) and Selenium, supporting both Android and iOS.

## Consuming this library

Published via [JitPack](https://jitpack.io/). Add the JitPack repository and the module(s) you need as a dependency, pinned to a released tag (e.g. `4.0.3`):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.dnestr332.test-core</groupId>
        <artifactId>base</artifactId>
        <version>4.0.3</version>
    </dependency>
    <!-- and/or web / mobile -->
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
  mvn test -pl base
  ```

# 4.0.3

## Breaking Changes

### Mobile Core
- `DeviceControlUtils.setAndroidHeadsUpNotifications(udid, SwitchState)` takes a `SwitchState` instead of a `boolean`
- `MobileElementUtils` and `MobileValidationUtils` are `final`

## Behaviour changes

### Mobile Core
- `AppInteractionUtils.hideKeyboard` on iOS presses the keyboard's own Return or Done key when WebDriverAgent cannot dismiss it (React Native's default keyboard has no other way out), and taps near the top only if the keyboard is still up. Next, Go and Search are never pressed, since they move focus or submit
- `AppInteractionUtils` builds no locators of its own: system UI comes from `SystemLocators`, test ids from `TestIdLocators`

## Base Core

### Additions
- `states.ValidState` - `VALID` / `INVALID`, for a field's validity

## Mobile Core

### Additions
- `SystemLocators.iosKeyboardDismissKey` (the keyboard's Return or Done key) and `iosKeyboardDoneButton` (Done on the keyboard's toolbar)
- `TestIdLocators.iosTestIdPredicate`, `iosByTestIdPredicate` and `androidScrollIntoViewByTestId`, used by `AppInteractionUtils.scrollIntoView`

## Other
- Javadoc on every public type and method
- README's dependency snippet uses JitPack's multi-module group id, `com.github.dnestr332.test-core`
- Dependencies: Appium java-client 11.0.0, Selenium 4.52.0, Playwright 1.64.0, REST Assured 6.3.0, commons-codec 1.23.0, SLF4J 2.0.20, Lombok 1.18.48; tests on JUnit 6.1.3 and Mockito 5.24.0; maven-compiler-plugin 3.16.0. None of the resolved dependencies has a known vulnerability in OSV

# 4.0.2

## Breaking Changes

### Base Core
- `ElementUtils` -> `TextUtils`

### Mobile Core
- `MobileDeviceUtils` is split by where the action happens: `DeviceControlUtils` controls the device and the app from outside it (lifecycle, background, deep links, orientation, keys, lock, network, location, clipboard, notifications, camera, recording); `AppInteractionUtils` acts inside the app (taps and gestures, swipe and scroll, keyboard, screen texts)
- `MobileDeviceUtils.disableAndroidPushNotifications` is removed: without a udid adb refuses to pick a device when several are attached. Use `DeviceControlUtils.setAndroidHeadsUpNotifications(udid, enabled)`
- `BaseActionFlow` takes `(ElementActions, BaseScreenResolver)`, as the web module's takes its page resolver
- `BaseAssertionFlow` takes `(ElementActions, BaseScreenResolver)`, holds the element checks and takes the screen as an `AppScreen` constant, as the web module's takes a page constant

## Behaviour changes

### Mobile Core
- `AppInteractionUtils.hideKeyboard` (was `MobileDeviceUtils.hideKeyboard`) does nothing when no keyboard is shown, and on Android no longer presses Back when the hide fails: Back left the screen whenever the keyboard was not really up (an emulator with a hardware keyboard reports one shown while drawing none). iOS still falls back to a tap near the top

## Base Core

### Additions
- `WaitUtils.waitUntilOnCallerThread` - polls on the calling thread, for conditions that read thread-bound state (scenario-scoped beans, `ThreadLocal`s)
- `WaitUtils.sleepSeconds(seconds, reason)` - a fixed sleep that logs why
- `EnumUtils.parseSealed` - resolves a constant across the enums a sealed interface permits
- `TextUtils` - reads values out of UI text: `getStringAsDouble` and `getStringAsDoubleReplaceCommas` (moved from `ElementUtils`), `getAmountAfterLast` (the amount after the last currency marker), `lastDigits` (last N digits of a phone or card number), `parseDateTime` (UI dates against several patterns, with a default year)
- `CommonUtils` - `prettyClassName` (simple class name without a runtime proxy suffix) and `isSorted`
- `ProcessUtils.run` - runs a command-line tool with a timeout and returns its output lines
- `AllureReportUtils` - writes Allure's `environment.properties` and `executor.json`
- `tools.faultproxy` (`FaultProxy`, `FaultRule`, `Fault`, `ProxiedRequest`) - an HTTP/1.1 keep-alive proxy that answers, delays, refuses or drops matched requests, for testing an app against a flaky backend
- `ElementAction.LONG_PRESS` and `DOUBLE_TAP` log labels

## Mobile Core

### Additions
- `GUIDE.md` - how the framework is meant to be used: the enum-driven pages and screens, generic steps, Spring wiring, the base module, and where each helper lives
- `AppInteractionUtils` (inside the app):
  - gestures: `doubleTap`, `dragAndDrop`
  - scrolling: `scrollIntoView(id)` and `scrollIntoView(id, timeout)` (Android `UiScrollable`, iOS `mobile: scroll` by name predicate), `swipeUpUntil`, `isScrollableBySwipeUp`
  - keyboard: `typeIntoFocusedElement`, `tapIosKeyboardDoneButton`
  - screen content: `getPageSourceTexts`
- `DeviceControlUtils` (outside the app):
  - apps: `runAppInBackground`, `openDeepLink`
  - keys and device state: `pressHome`, `pressAndroidKey`, `pressAndroidBack`, `lockDevice`, `unlockDevice`, `setAndroidConnectivity`, `setDeviceLocation`, `getClipboardText`, `getUdid`
  - notifications: `getAndroidNotifications`, `openAndroidNotifications`, `openIosNotificationCenter`, `setAndroidHeadsUpNotifications`
  - media: `injectAndroidEmulatorCameraImage`, `startScreenRecording`, `stopScreenRecording`
- `AdbUtils` - Android device control through adb by udid: devices and emulators, shell and emulator console, permissions and their flags, foreground package, app version, force-stop, UiAutomator2 server reset, location services, time zone, emulator GPS fix, reboot-and-wait
- `SimctlUtils` - booted iOS simulators, an installed app's version and push delivery (`sendIosSimulatorPush`), through `xcrun simctl`
- `ElementActions`: `clickIfVisible`, `clickIfAppears`, `clickUntilVisible`, `clickWhileVisible`, `isVisible(locator, timeout)`, `longPress`, `doubleTap`, `append`
- `DriverWait`: `visible(locator, timeout)`, `absent`, `untilStable`
- `MobileElementUtils`: `centerOf`, `getId`, `closestToCenterY`, `childText`, `replaceValueByBackspace`
- `MobileValidationUtils.isChecked`
- `SystemActions` - taps the OS's own UI on both platforms: `tapAlertButton`, `answerPermissionPrompt`, `tapNotification` (Android's shade, iOS's Notification Center), `takeAndroidEmulatorCameraPhoto`
- `locators.TestIdLocators` (`byTestId`, `byAnyTestId`, `firstByTestIdPrefix`, `allByTestIdPrefix` - a React Native testID on both platforms), `locators.SystemLocators` (permission, alert, notification and camera controls, including `iosNotification`), `locators.XPathUtils.literal` (quotes any text, both quote kinds included)
- `screens.AppScreen` and `screens.BaseScreenResolver` - a screen enum and the lookup from it to its `BaseScreen`, mobile's counterpart to the web module's `AppPage` and `BasePageResolver`
- `BaseAssertionFlow` element checks: `verifyVisible`, `verifyTextEquals`, `verifyTextContains` (re-read for up to 10 s; `textMatches` is the hook for keywords), `verifyItemState`, `verifyFieldState`
- `BaseActionFlow`: `tap`, `type` and `text` by screen enum

### Other
- A method that works on one platform only names it (`pressAndroidBack`, `tapIosKeyboardDoneButton`) and throws `UnsupportedOperationException` on the other
- Element-center maths in `longPress`, `doubleTap` and `FallbackActions` now goes through `MobileElementUtils.centerOf`
- Unit test coverage added for every addition

# 4.0.1

## Mobile Core

### Additions
- `MobileDeviceUtils.longPress` - presses and holds the center of an element for a given duration
- `MobileDeviceUtils.drawWithFinger` - draws a continuous zigzag inside a box (e.g. to fill a signature pad); throws `IllegalArgumentException` if `strokes < 1`
- `DriverWait.waitForAlert` - waits for a native alert (e.g. an OS permission dialog) and returns it

### Other
- `MobileDeviceUtils.tapByCoordinates` no longer pads its gesture with a leading empty pause
- Unit test coverage added for `MobileDeviceUtils` and `DriverWait`
- Appium bumped to 10.1.1, Jackson to 2.22.4

# 4.0.0

## Breaking Changes

### Base Core
- `BasePrettyPrinter` -> `PrettyPrinter`

### Web Core
- `com.dnestr.web.logs.PrettyPrinter` -> `WebPrettyPrinter`

### Mobile Core
- `com.dnestr.mobile.logs.PrettyPrinter` -> `MobilePrettyPrinter`
- `com.dnestr.mobile.pages` -> `com.dnestr.mobile.screens` (`BaseScreen`, `Screen`, `Clickable`, `Closable`)
- `com.dnestr.mobile.enums.MobilePlatform` -> `com.dnestr.mobile.context.MobilePlatform`

## Base Core

### Additions
- `CsvReaderUtils` - reads small CSV test-data files (`readRows`) and looks up a cell by key column (`cell`); supports quoted cells and a leading UTF-8 BOM
- `BaseScenarioContext.addToList` / `getList` for accumulating values under one key, for both typed and dynamic keys

### Other
- Unit test coverage added for `CsvReaderUtils`, `WaitUtils`, the new `BaseScenarioContext` list helpers, and mobile's `MobilePrettyPrinter`

# 3.0.2

## Web Core

### Other
- `BasePage.onlyMatch` now delegates to `ElementActions.waitForCondition` instead of calling `Page.waitForCondition` directly
- Javadoc added for `BasePage`'s scoped `byRole` overloads, `onlyMatch`, and `isTextVisible`
- Unit test coverage added for `BasePage`

# 3.0.1

## Breaking Changes

### Base Core
- Renamed `VisibleState.NOT_VISIBLE` to `VisibleState.HIDDEN`

## Base Core

### Fixes
- `mockito-core`'s inline mock maker now loads as a build-time `-javaagent` via Surefire instead of self-attaching dynamically, removing the JDK "dynamic agent loading" warning

### Other
- Javadoc added across `base`

## Web Core

### Additions
- `ElementActions.waitForCondition`, `isVisibleWithinTimeout`, `isHiddenWithinTimeout`, `clickIfVisible` - generic, non-throwing bounded waits
- `ElementActions.waitForStability` reworked: added an absolute max-wait cap so it can no longer hang indefinitely under continuous DOM mutation, added `characterData` observation, and made the debounce/max-wait configurable

### Other
- Javadoc added across `web`

## Mobile Core

### Fixes
- `MobileDeviceUtils.swipe` now respects the requested `direction` on Android (previously always swiped up, regardless of the argument)

### Other
- Javadoc added across `mobile`

# 3.0.0

## Breaking Changes

- Renamed the repository from `dnestr-test-core` to `test-core` (update the JitPack multi-module groupId accordingly)
- Renamed modules and artifacts for brevity, since the `dnestr` prefix was redundant with the `com.github.dnestr332` groupId:
  - `dnestr-base-core` -> `base`
  - `dnestr-web-core` -> `web`
  - `dnestr-mobile-core` -> `mobile`
- No package or code changes - only module/artifact names and directory layout

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
