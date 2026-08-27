# Testing composables

> `build.gradle.kts` (root `subprojects`) · `page/ui/src/test/kotlin/page/ui/HoverHandoffTest.kt` — driving UI without a window

Where the pointer went, or whether a key combination landed, cannot be pulled out into a pure function without the test becoming a tautology. You have to feed real events and look at what happens. `runComposeUiTest` renders a composable offscreen and injects pointer and key events into it. It never touches the developer's screen and runs headless on CI.

> 한국어: [ui_testing.md](https://monkshark.github.io/page-ide/#internals/ui/ui_testing.md)

---

## Where it comes from

The root `build.gradle.kts` adds `compose.desktop.uiTestJUnit4` to every module that applies both the Kotlin/JVM and Compose plugins. No module build file needs editing — `page:ui`, `page:app`, `page:workspace`, `page:atlas`, `page:runtime`, and `page:language` all have it.

`page:atlas-view` is multiplatform and falls outside that convention. If it ever needs UI tests, wire the dependency into its `jvmTest` source set separately.

---

## Two traps

**`waitForIdle()` never returns in the editor.** `CodeEditor` blinks its caret with `while (true) { delay(530) }`, so the scene never reaches idle. Drive the clock yourself.

```kotlin
mainClock.autoAdvance = false
setContent { ... }
repeat(3) { mainClock.advanceTimeByFrame() }
```

**`moveTo` alone does not produce a hover.** The pointer has to be established inside the scene first.

```kotlin
onNodeWithTag("editor").performMouseInput {
    enter(Offset(40f, 20f))
    moveTo(Offset(41f, 20f))
}
```

---

## Asserting on delayed behaviour

A manual clock lets you assert *when* something happens. The hover handoff is one: leaving the text does not close the popup at once, because the pointer may be on its way to it.

```kotlin
onNodeWithTag("editor").performMouseInput { exit() }
mainClock.advanceTimeBy(100)
assertNotNull(hovered.last())      // still open

mainClock.advanceTimeBy(400)
assertEquals(null, hovered.last()) // closed once the handoff window passes
```

When writing a new test, briefly revert the fix and **check that it fails first.** Confirming only that it passes hides a test that measures nothing.

---

- [Back to index](https://monkshark.github.io/page-ide/#README_en.md)
