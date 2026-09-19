# AGENTS.md

## Architecture

This app follows Google's recommended layered architecture: UI layer, an optional Domain layer, and a Data layer. Data flows up through Flow/StateFlow; events flow down through function calls. Do not put business logic in Activities, Fragments, or Composables.

* **UI layer**: Composables plus a ViewModel per screen or feature. Composables read UI state and emit events; they never own business logic or talk to the network or database directly.
* **Domain layer**: Use case classes, added only when logic is reused across more than one ViewModel or when a ViewModel is getting bloated. Do not add a Domain layer preemptively for a screen with a single trivial use case.
* **Data layer**: Repositories are the single source of truth for a given kind of data. Room, DataStore, and Retrofit/Ktor services live behind repository interfaces. ViewModels and use cases depend on repository interfaces, never on DAOs or API services directly.

Use a single-activity architecture. One Activity hosts all Compose destinations through Navigation 3. Do not create a new Activity for a new screen.

Favor unidirectional data flow everywhere: state flows down from ViewModel to Composable as a single immutable UI state object; events flow up from Composable to ViewModel as function calls or a sealed event type. Do not pass mutable state or callbacks that mutate ViewModel internals directly into a Composable's parameters.

When adding a new feature, mirror the folder and module structure already in the repo (feature-first packages, with `ui`, `domain`, and `data` separated). Do not introduce a new organizing scheme for one feature.

## Statelessness

Prefer stateless Composables. A Composable should take its data and callbacks as parameters and should not hold state that outlives a single recomposition unless that state is purely presentational (for example, an animation's transient value or a scroll position local to that Composable).

* Hoist state to the nearest ViewModel or to the caller. If two Composables need to share state, hoist it to their common parent rather than reaching sideways.
* Use `remember` and `rememberSaveable` only for UI-local, disposable state (text field focus, expanded/collapsed toggles, scroll state). Do not use `remember` as a substitute for a ViewModel to store data that needs to survive process death or that represents business state.
* Avoid singletons and global mutable state. If something needs to be shared across the app, inject it through DI rather than reaching for a top-level `var` or an object with mutable fields.
* Prefer pure functions for mapping and transformation logic (DTO to domain model, domain model to UI state). Pure functions are easy to unit test and easy for an agent to reason about without running the app.

## Unit Testing

Every ViewModel, use case, mapper, and repository implementation needs unit test coverage.

* Test ViewModels by asserting on emitted UI state, not on implementation details. Use Turbine to collect Flow/StateFlow emissions.
* Test use cases and mappers as pure functions: given input, assert expected output. No Android framework dependency should be needed to run these tests.
* Test repositories against fakes or in-memory implementations of their data sources (an in-memory Room database, a fake API service) rather than mocking every internal call.
* Favor fakes over mocks where practical. Mocks are acceptable for narrow collaborator boundaries, but a hand-written fake is usually clearer and more resilient to refactors.
* Write the test alongside the code in the same change. Do not defer tests to a follow-up task.
* Keep unit tests fast and JVM-only where possible. Reserve instrumented/Compose UI tests for behavior that genuinely requires the Android framework or rendering (navigation flows, actual composition, accessibility checks).
* Name tests by behavior, not by method: `emitsErrorState_whenNetworkCallFails` rather than `testFetchData2`.

## UI: Material You Expressive, Not Reinvented

Use Material 3 (Material You) components and the Expressive theming system as provided. Do not hand-roll custom versions of components that Material 3 already supplies (buttons, cards, navigation bars, sheets, dialogs, chips, switches, sliders, etc).

* Theme through `MaterialTheme` color scheme, typography, and shape tokens. Do not hardcode colors, font sizes, or corner radii in individual Composables.
* Use dynamic color (`dynamicColorScheme`) where the design calls for a personalized, system-driven palette, and fall back to a defined static scheme otherwise. Do not invent a parallel theming mechanism.
* When a design needs something Material 3 does not provide out of the box, extend or compose existing Material 3 components first. Only drop to a fully custom implementation when composition genuinely cannot achieve the result, and note why in a code comment.
* Respect Material motion and shape specs (expressive shapes, standard easing and duration tokens) rather than introducing bespoke animation curves for routine interactions.
* Support light/dark theme and dynamic color from day one for any new screen. Do not ship a screen that only looks correct in one theme.

## Standard Practices

* Kotlin only for new code. Do not introduce Java files.
* Use Coroutines and Flow for asynchronous work and streams of data. Do not introduce callbacks, RxJava, or LiveData into new code.
* Use the Gradle Kotlin DSL and the version catalog (`libs.versions.toml`) for all dependencies. Do not hardcode a dependency string outside the catalog.
* Use Hilt for dependency injection. Constructor-inject dependencies; avoid field injection except where the framework requires it (Activities, Fragments).
* Keep Composables small and focused. If a Composable function is doing layout, business logic, and state management all at once, split it.
* Null safety: avoid `!!`. Handle nullability explicitly or restructure the code so the null case cannot occur.
* Prefer immutable data classes for UI state and domain models. Avoid `var` properties on shared or long-lived objects.
* All user-facing text lives in per-feature `values/strings_<feature>.xml` resources read with `stringResource` (never hardcoded); add a locale via a `values-<code>/` set plus entries in `res/xml/locales_config.xml` and `androidResources.localeFilters`, and format dates/numbers with `Locale.current`.
* Run existing lint and static analysis checks before considering a change complete. Do not suppress a lint warning without a comment explaining why.

## Build, Format & Lint

* `./gradlew check` runs unit tests, Android Lint, ktlint, Spotless, and detekt. Run this before considering a change complete.
* `./gradlew ktlintCheck` / `./gradlew ktlintFormat` checks / auto-formats Kotlin and build scripts. Configuration lives in `.editorconfig` (LF, 2-space indent, 120-column limit, Compose PascalCase allowed).
* `./gradlew spotlessApply` / `./gradlew spotlessCheck` formats / checks XML via Spotless with Eclipse WTP. XML style lives in `config/spotless/xml.prefs`.
* `./gradlew detekt` runs static analysis. Rules and thresholds live in `config/detekt/detekt.yml`.
* `./gradlew lint` runs Android Lint.
* ktlint, Spotless, and detekt are wired into `check`; do not disable them without a documented reason.
* Kotlin and XML sources are pinned to LF. Keep `.gitattributes` intact so CRLF does not reappear on Windows checkouts and break ktlint.
