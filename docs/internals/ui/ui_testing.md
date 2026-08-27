# 컴포저블 테스트

> `build.gradle.kts` (루트 `subprojects`) · `page/ui/src/test/kotlin/page/ui/HoverHandoffTest.kt` — 창 없이 UI 를 몰아보는 하네스

포인터가 어디로 갔는지, 키 조합이 먹었는지 같은 것은 순수 함수로 뽑아도 동어반복만 남는다. 실제로 이벤트를 넣고 결과를 봐야 안다. `runComposeUiTest` 는 창을 띄우지 않고 컴포저블을 오프스크린에 그린 뒤 포인터·키 이벤트를 넣는다. 개발자 화면을 건드리지 않고 CI(헤드리스 Linux)에서도 돈다.

> English: [ui_testing_en.md](https://monkshark.github.io/page-ide/#internals/ui/ui_testing_en.md)

---

## 어디에 붙어 있나

루트 `build.gradle.kts` 가 Kotlin/JVM + Compose 플러그인을 함께 쓰는 모듈에 `compose.desktop.uiTestJUnit4` 를 자동으로 건다. 모듈 빌드 파일을 고칠 필요 없이 `page:ui` · `page:app` · `page:workspace` · `page:atlas` · `page:runtime` · `page:language` 어디서든 바로 쓴다.

`page:atlas-view` 는 멀티플랫폼이라 이 컨벤션에서 빠져 있다. 거기에 UI 테스트가 필요해지면 `jvmTest` 소스셋에 따로 걸어야 한다.

---

## 두 가지 함정

**`waitForIdle()` 은 에디터에서 영원히 안 끝난다.** `CodeEditor` 의 캐럿 깜빡임이 `while (true) { delay(530) }` 라서 씬이 idle 상태에 도달하지 않는다. 시계를 직접 몰아야 한다.

```kotlin
mainClock.autoAdvance = false
setContent { ... }
repeat(3) { mainClock.advanceTimeByFrame() }
```

**`moveTo` 만으로는 호버가 안 걸린다.** 포인터가 씬 안에 있다는 사실이 먼저 서야 한다.

```kotlin
onNodeWithTag("editor").performMouseInput {
    enter(Offset(40f, 20f))
    moveTo(Offset(41f, 20f))
}
```

---

## 지연 동작을 확인하는 법

시계를 수동으로 두면 "얼마 뒤에 일어나는가" 를 단언할 수 있다. 호버 인계가 그 예다 — 포인터가 텍스트를 벗어나도 팝업으로 옮겨 갈 시간을 주느라 바로 닫지 않는다.

```kotlin
onNodeWithTag("editor").performMouseInput { exit() }
mainClock.advanceTimeBy(100)
assertNotNull(hovered.last())      // 아직 열려 있다

mainClock.advanceTimeBy(400)
assertEquals(null, hovered.last()) // 인계 시간이 지나면 닫힌다
```

새 테스트를 쓸 때는 고친 코드를 잠깐 되돌려 **실패하는지 먼저 확인하는 게 좋다.** 통과만 확인하면 아무것도 안 재는 테스트를 알아채지 못한다.

---

- [목차로 돌아가기](https://monkshark.github.io/page-ide/#README_kr.md)
