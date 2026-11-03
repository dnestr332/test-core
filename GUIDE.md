# How to use test-core

Read this before writing your first page, screen or step. The classes make sense once you have seen the pattern
they are built for, and that pattern is not something you would guess from the class list.

test-core has three modules: **`web`** (Playwright, pages), **`mobile`** (Appium, screens on Android and iOS) and
**`base`**, which both build on and which also works on its own (API clients, scenario state, assertions, test-case
sync, a fault-injecting proxy). Web and mobile follow the same design; only the names differ:

| | Web | Mobile |
|---|---|---|
| a view of the app | page — `BasePage` | screen — a plain class nesting its element enum |
| the list of views | `AppPage` enum | your own enum, if your steps name screens |
| the elements of one view | `PageElement` enum (accessible role + name) | `ScreenElement` enum nested in the screen (a locator per constant) |
| view enum → view object | `BasePageResolver` | none: an element carries its own locator |
| actions / checks | `BaseActionFlow<P>` / `BaseAssertionFlow<P>` | `BaseActionFlow` / `BaseAssertionFlow`, on the element alone |

## The idea in one minute

You describe your app as **data**: one enum lists your pages or screens, and one enum per page or screen lists its
elements. A handful of **generic steps** then drive every element of every view:

```gherkin
# web
When user clicks "Edit User" button on Users page
Then the "Edit User" item on Users page is strictly visible

# mobile
When user taps Profile Save Button on Edit Profile screen
Then the Profile Save Message on Edit Profile screen is softly visible
```

Each of these lines is served by a step method that never changes, however big the app gets. To cover a new button
you add **one enum constant and its locator**. You do not write a `clickSaveButton()` method, and you do not write a
new step.

## Why it is built this way

- **The step count stays flat.** A suite with hundreds of pages or screens still has one click/tap step, one type step
  and a few assertion steps. Nobody has to search 40 step classes for "the one that clicks Save".
- **Every locator lives in exactly one place**: the element enum (web) or the screen's `resolve` switch (mobile). A UI
  change is a one-line fix.
- **Typos fail loudly, before the app is touched.** The Gherkin text is parsed into an enum constant; an unknown name
  throws and lists the valid ones, instead of timing out 30 seconds later on a locator that matches nothing.
- **Every action is logged the same way.** Clicks, typing and reads go through `ElementActions`, which logs each
  action with its locator and records the failure, so a failed run reads action by action.
- **Strict or soft is chosen in the scenario**, not in code: `is strictly visible` fails at once, `is softly visible`
  collects the failure and the scenario carries on.
- **Web and mobile read alike**, so moving between suites costs nothing.

## Use it with Spring

**The suggested setup is Spring with `cucumber-spring`**, with every page, screen, flow, resolver and scenario
context registered as a `@ScenarioScope` bean. test-core itself does not depend on Spring — any wiring works — but
the pattern is designed around a container, and Spring is what removes the boilerplate:

- **Fresh objects for every scenario.** `@ScenarioScope` gives each scenario its own pages, screens, flows and
  `BaseScenarioContext`, and throws them away afterwards. No state leaks from one scenario into the next, with no
  cleanup code.
- **Steps only declare what they need.** Cucumber creates step classes through Spring, so a step class lists the
  flows and screens it uses as constructor parameters and gets them. Nothing is constructed by hand.
- **The resolver is one line, and new views need no registration.** `context.getBean(screen.getScreenClass())` finds
  any page or screen the component scan picked up, so adding a screen is: write the class, add the enum constant.
  Without a container you keep a map from every enum constant to an instance, and update it for every new view.
- **Shared setup is configured once.** The driver or Playwright `Page`, `ElementActions`, `DriverWait` and the wait
  timeouts are beans built in one configuration class per app, with environment-specific values from properties or
  profiles.

One thing to know: a scenario-scoped driver bean is a Spring proxy, and a proxy is not an `AndroidDriver` or an
`IOSDriver`. Lib methods that need a driver capability (`lockDevice`, `getClipboardText`, screen recording,
`hideKeyboard`) must be given the real driver behind the proxy (Spring's `AopUtils` / `Advised` unwrap it).

---

## How a step reaches the app

```
web:     "user clicks "Edit User" button on Users page"
           │ parameter type → Pages.USERS ──BasePageResolver──► UsersPage
           │ BaseElementResolver → UsersElement.EDIT_USER
           └──► UsersPage.locator(EDIT_USER) ──► ElementActions.click

mobile:  "user taps Profile Save Button on Edit Profile screen"
           │ parameter type → EditProfileElement.PROFILE_SAVE_BUTTON (parsed from the Edit Profile screen's enum)
           └──► PROFILE_SAVE_BUTTON.getLocator() ──► ElementActions.click  (wait, act, fallbacks, logging)
```

The string becomes an enum through `EnumUtils`, which ignores case, spaces and dashes: `Profile Save Button`,
`profile-save-button` and `PROFILE_SAVE_BUTTON` are the same constant. **It matches the constant's name, never a
label**, so name constants the way you want them read in Gherkin.

---

## Web, step by step

1. **Elements.** One enum per page implementing `PageElement`, each constant carrying the element's accessible name
   and ARIA role. `BasePage.locator(element)` turns that pair into `page.getByRole(role, name)`, so a web element
   needs no separate locator.

   ```java
   @Getter
   @RequiredArgsConstructor
   public enum UsersElement implements PageElement {
       SEARCH_USER("Search user", AriaRole.COMBOBOX),
       EDIT_USER("Edit", AriaRole.BUTTON);

       private final String label;
       private final AriaRole role;
   }
   ```

2. **Pages.** One class per page extending `BasePage`. It overrides `locator(element)` only for the few elements a
   role and name cannot find, and adds page-specific helpers (a table row by record id, say).
3. **Page list.** One enum implementing `AppPage` (here `Pages`), each constant with its path, e.g. `USERS("/users")`.
4. **Resolvers.** `BasePageResolver` turns a page constant into its `BasePage`. `BaseElementResolver` turns the
   element's name in the step into that page's `PageElement` constant, since every page has its own element enum.
5. **Flows and steps.** `BaseActionFlow<P>` (`click(element, page)`, `type(element, page, value)`) and
   `BaseAssertionFlow<P>` (`verifyVisibleState`, `verifyButtonState`, `verifyFieldState`, `verifyTextEquals`, …)
   behind generic steps, with `{pageName}` registered as a parameter type:

   ```java
   @When("user clicks {string} button on {pageName} page")
   public void userClicks(String rawElement, Pages page) {
       actionFlow.click(elementResolver.resolveElement(page, rawElement), page);
   }
   ```

   `BaseContextResolver` handles the special values in step text: `"valid"` generates fresh test data and remembers
   it, `"expected"` reads back something remembered earlier.

## Mobile, step by step

### 1. One class per screen, with its elements

One class per screen, nesting its element enum. Each constant carries its locator, so the screen class is the one
place to look for an element and its locator.

```java
public class EditProfileScreen {

    @Getter
    @RequiredArgsConstructor
    public enum EditProfileElement implements ScreenElement {
        EDIT_PROFILE_TITLE(byTestId("edit-profile-title")),
        PROFILE_EMAIL_INPUT(byTestId("edit-profile-email")),
        PROFILE_SAVE_BUTTON(byTestId("edit-profile-save")),
        PROFILE_SAVE_MESSAGE(byTestId("edit-profile-save-message"));

        private final By locator;
    }
}
```

- `ScreenElement` carries one locator, built however your app is best found: here `TestIdLocators.byTestId`, a React
  Native `testID` that matches on both Android and iOS. `DualLocatorElement` carries an Android and an iOS `By`, for
  an app whose two builds name an element differently.
- A constant whose locator needs logic (a platform's native alert, a child of another element) overrides
  `getLocator()` in its own body. An element that differs per record (a row by id) is a method taking the key.
- A screen that also reads or waits (a list of cards, say) holds `ElementActions` and is a bean your flows inject.

### 2. Name the screens your steps use

An enum whose constants name each screen's element enum lets a step say which screen an element is on, and parses
the element from that screen's enum only, so a name needs to be unique on its screen alone:

```java
@Getter
@RequiredArgsConstructor
public enum Screens {
    SETTINGS(SettingsScreen.SettingsElement.class),
    EDIT_PROFILE(EditProfileScreen.EditProfileElement.class);

    private final Class<? extends Enum<?>> elements;
}
```

### 3. Wire the generic steps

A parameter type turns the Gherkin words into the element, and each step is a single call into a flow.
`AppFlow` extends `BaseActionFlow` and `AppAssertions` extends `BaseAssertionFlow`; both start out empty.

```java
public class AppParameters {

    @ParameterType("(?i)([a-zA-Z0-9 _-]+) on ([a-zA-Z0-9 _-]+) screen")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ScreenElement screenElement(String element, String screen) {
        return (ScreenElement) EnumUtils.parse((Class) EnumUtils.parse(Screens.class, screen).getElements(), element);
    }
}

@RequiredArgsConstructor
public class ScreenSteps {

    private final AppFlow flow;
    private final AppAssertions assertions;

    @When("user taps {screenElement}")
    public void userTaps(ScreenElement element) {
        flow.tap(element);
    }

    @When("user sets {screenElement} as {string}")
    public void userTypes(ScreenElement element, String value) {
        flow.type(element, value);
    }

    @Then("the {screenElement} is {assertionState} {visibleState}")
    public void isVisible(ScreenElement element, AssertionState assertion, VisibleState visible) {
        assertions.verifyVisible(element, assertion, visible);
    }
}
```

A flow overrides `tap` or `type` for an element that needs more than a click, and adds the multi-step sequences a
scenario needs (navigation, a form filled in one go).

## Shared vocabulary for both

`base` provides the state enums every assertion step uses: `AssertionState` (`strictly` / `softly`), `VisibleState`
(`visible` / `hidden`), `ButtonState` (`enabled` / `disabled`), `FieldState` (`editable` / `read only`),
`ToggleState` and `SwitchState`. Register a parameter type for each once, and every suite shares the same words.

Soft assertions collect on the thread until something calls `Softly.assertAll()`. Call it in an `@After` hook, or a
soft failure is never reported.

---

## The base module

`base` has no UI dependency, so it serves API-only suites as well as the web and mobile ones.

- **API layer — `BaseApiClient`.** One subclass per service: it supplies the base URL and path, and gets a
  REST Assured `baseSpec()` plus the auth helpers `withBearer`, `withBasic`, `withApiKeyHeader`, `withApiKeyQuery`,
  `withOAuth2` and `withHeaders`. Seeding test data through the API rather than the UI is faster and far less flaky,
  and a non-200 answer fails the scenario at the real cause.
- **Scenario state — `BaseScenarioContext<K>`.** A per-scenario key/value store for values one step produces and a
  later one uses (a created record's id, generated test data). Typed keys come from your own enum; string keys cover
  names only known at run time. Make it a `@ScenarioScope` bean so each scenario gets its own.
- **Assertions — `Hardly` and `Softly`.** `Hardly` fails at once; `Softly` collects every failure of a scenario and
  reports them together on `assertAll()`. `AssertionState` picks between them from the Gherkin text.
- **Action logging — `FailureCatcher`, `PrettyPrinter`.** Every `ElementActions` call runs through the catcher,
  which writes one line under the `ElementActions` logger once the action ends: the action, its duration, the locator
  and the value typed (masked when the locator mentions a password). An action slower than 3 s logs at WARN, a failed
  one at ERROR with the error's first line; the exception itself is rethrown unchanged.
- **Utilities.**
  - `EnumUtils` — loose text to enum constant (`parse`, `parseSealed`, `tryParse`).
  - `TextUtils` — numbers, digits and dates out of UI text.
  - `WaitUtils` — polling for non-UI conditions (a DB row, a background job).
  - `PropReaderUtils` — `.properties` with system-property and environment-variable overrides.
  - `CsvReaderUtils` — small CSV test-data files.
  - `OverrideUtils` — applies string overrides onto a config object.
  - `ProcessUtils` — runs a command-line tool with a timeout.
  - `AllureReportUtils` — Allure's environment and executor files.
  - `CommonUtils` — proxy-free class names for logs, sorted-list checks.
- **Tools.**
  - **Test-case sync (`tools.bddsync`, `tools.testrail`, `tools.ado`).** `BddSyncEngine` scans the `.feature` files
    and hands each tagged scenario to a provider: `TestRailSyncProvider` creates or updates a TestRail case for every
    scenario tagged `@sync` or `@C<id>`, and `AdoSyncProvider` does the same for Azure DevOps test cases. A new case's
    id is written back into the scenario's tags, so the feature files stay the single source.
  - **Fault proxy (`tools.faultproxy`).** An HTTP proxy between the app and its backend that can answer in place of
    the server, delay a request, drop the response or refuse the connection, per method and path and for a set number
    of requests. It is how a suite tests retries, timeouts and offline handling without touching the backend.

---

## When to write something specific

The generic steps cover single actions and single checks. Write code only when that is not enough:

- **A business sequence** (fill a form and submit, sign in) → one method on a flow subclass, called by one step. The
  step stays one line; the flow holds the sequence.
- **An element the enum cannot express** (a list row keyed by record id, a scrolled-to item) → a method on the page
  or screen that takes the key and returns a `Locator` or `By`, e.g. `orderRow(orderId)`.
- **A check that reads several values** → in the step itself, through the assertion flow's `assert*`/`softAssert*`.

What not to write:

- **A method or step that only clicks one element.** That is the generic click/tap step with one more enum constant.
- **One method per element** (`clickSave`, `clickCancel`, …). A method that differs from its sibling only by the
  element is the generic step with a parameter.
- **A raw driver call where a lib method exists.** `ElementActions` logs and records failures; a bare
  `element.click()` leaves no trace in the run log.

## Where to find things

| You want to… | Use |
|---|---|
| click, type, read text, check visible/enabled | `ElementActions` (web and mobile) |
| tap only if it shows, tap until the next screen shows | mobile `ElementActions.clickIfVisible` / `clickIfAppears` / `clickUntilVisible` / `clickWhileVisible` |
| wait for gone, for an animation to stop, with a custom timeout | mobile `DriverWait.absent` / `untilStable` / `visible(locator, timeout)` |
| tap at a point, long press, double tap, drag, swipe, scroll to an element | `GestureUtils` |
| open/close/restart the app, deep links, keys, the keyboard, lock, network, location, notifications, the app in front, recording | `DeviceUtils` |
| read an element's or the whole screen's texts, an element's center or id | `ElementUtils` |
| alerts, permission prompts, notifications, the camera app | `SystemActions`, with locators in `SystemLocators` |
| device state without a session: permissions, app data, installed check, reboot, location | `AdbUtils` (Android), `XcrunUtils` (iOS: simctl for a simulator, devicectl for a real iPhone) |
| refuse a call on the platform that has no such thing | `TestContext.requireAndroid` / `requireIos` |
| call a backend API | a `BaseApiClient` subclass |
| share a value between steps | your `BaseScenarioContext` subclass |
| numbers, digits or dates out of UI text | `TextUtils` |
| wait on a non-UI condition | `WaitUtils`; `waitUntilOnCallerThread` when the condition uses scenario-scoped beans |
| make the backend misbehave (delay, drop, refuse, fake a response) | `tools.faultproxy.FaultProxy` |
| keep TestRail or Azure DevOps cases in step with the feature files | `tools.bddsync.BddSyncEngine` with a provider |

On mobile, a method that works on one platform has it in its name (`pressAndroidBack`, `openIosNotificationCenter`)
and throws on the other; everything else works on both.
