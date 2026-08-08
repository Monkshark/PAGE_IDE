<div align="center">

<img src="assets/logo_transparent.svg" alt="PAGE" width="96" />

# PAGE

**Kotlin 과 Compose Multiplatform 으로 처음부터 만든 다언어 데스크톱 IDE.**

[![CI](https://github.com/monkshark/page-ide/actions/workflows/ci.yml/badge.svg)](https://github.com/monkshark/page-ide/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-4285F4?logo=jetpackcompose&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-21%20toolchain-007396?logo=openjdk&logoColor=white)
![Status](https://img.shields.io/badge/status-pre--alpha-orange)

[문서](https://monkshark.github.io/page-ide/) · [개발기](https://monkshark.github.io/categories/page-개발기/) · [English README](README.md)

</div>

---

코드베이스는 동시에 네 가지다 — 텍스트이고, 그래프이고, 기록이고, 대화다. 에디터가 그 넷을 한
작업 공간에 담아야 한다는 생각에서 PAGE 를 만들고 있다. 지금은 24개 언어를 편집·탐색·실행하고,
필요한 언어 서버와 툴체인을 스스로 설치하며, 지금 읽고 있는 코드의 의존 그래프를 그린다.

> **pre-alpha.** 매일 자기 자신을 만드는 데 쓰고 있지만, 아직 유일한 에디터로 삼을 단계는 아니다.

## 기능

**언어 지능**
- **24개 언어** LSP 라우팅 — 자동완성, 진단, 정의 이동, 호버, 이름 변경
- 프로젝트 전체 심볼 인덱스 — 서버를 기다리지 않고 사용처·죽은 코드·안 쓰는 import 를 찾는다
- inline diff 가 붙은 코드 액션, 여러 파일을 건드리는 편집을 위한 검토 패널

**코드 실행**
- **13개 언어** 단일 파일 실행 — 프로젝트 구성도, 실행 설정도 필요 없다
- 증분 빌드 캐시: 소스가 그대로면 컴파일 자체를 건너뛴다
- 내장 터미널(pty4j), 실행 출력은 그 옆에 도킹된다

**툴체인 자동 설치**
- JDK · Node · Python · Go · Rust · .NET · Dart/Flutter · Swift · Clang 이 없으면 IDE 안에서
  받아 설치한다. 진행률은 상태바에, 로그는 설치 패널에 남는다
- Windows 에서는 MinGW-w64 와 MSVC SDK(xwin)까지 붙여 C/C++ 과 Swift 가 바로 돌아간다

**에디터**
- 분할 뷰, 멀티탭, 폴딩, 레인보우 브래킷, 스코프 가이드, inlay hint
- Glass 테마 9종, 각 테마에 맞춰 설계한 신택스 팔레트
- 모든 단축키가 액션 테이블 하나에 있다 — 팔레트에서 검색되고, 설정에 목록으로 뜨며,
  플랫폼에 따라 `Ctrl` 또는 `⌘` 로 표기된다

**Atlas**
- 빌드 메타데이터가 아니라 tree-sitter 로 뽑은 import·호출·모듈 그래프

<sub>지원 언어: Kotlin · Java · Python · TypeScript/JavaScript · Go · Rust · C/C++ · Swift ·
Dart/Flutter · Ruby · PHP · C# · Vue · Svelte · Bash · JSON · YAML · HTML · CSS · SQL ·
Markdown · Dockerfile</sub>

## 네 기둥

| | |
|---|---|
| **P**air | 작업 맥락을 지켜보고 대화하는 AI 동반자 — 관찰자·대화·에이전트·튜터 |
| **A**tlas | 코드베이스를 그래프로 — 모듈, 함수, 의존성 |
| **G**lass | 다크 우선 디자인 시스템 — 부드러운 모션, 집중, 매일 열고 싶은 미감 |
| **E**cho | 작업 자체의 시간축, 로컬에 남는다 |

Atlas 는 동작하고 Glass 는 거의 왔다. Pair 와 Echo 는 아직 스캐폴딩이다. 각 기둥이 무엇이 되려
하는지, 그리고 PAGE 가 **하지 않기로 한 것**은 [개요](https://monkshark.github.io/page-ide/#guides/overview.md)에 적어뒀다.

## 시작하기

Git 만 있으면 된다 — JDK 는 Gradle 이 알아서 받는다.

```bash
git clone https://github.com/monkshark/page-ide.git
cd page-ide
./gradlew :page:app:run
```

```bash
./gradlew build                 # 전체 모듈 + 테스트
./gradlew :page:runtime:test    # 단일 모듈
```

<sub>`JAVA_HOME` 이 없어진 JDK 를 가리키면 Gradle 이 시작 전에 멈춘다. 환경변수를 고치거나
`~/.gradle/gradle.properties` 에 `org.gradle.java.home=…` 으로 고정하면 된다.</sub>

## 아키텍처

Gradle 모듈 16개. 의존은 한 방향으로만 흐르고, 맨 아래 `page:core` 는 외부 의존이 하나도 없다.

```
page-ide/
├── shared-core/     파서, 그래프 모델, 경로              (jvm + wasmJs)
├── docs-viewer/     문서 사이트                          (wasmJs)
└── page/
    ├── core/        정체성, 공용 도메인 타입             (외부 의존 없음)
    ├── perf/        시작 성능 계측, UI 멈춤 감시
    ├── ui/          Glass 디자인 시스템, 코드 에디터
    ├── editor/      텍스트 버퍼, 편집 이력, 렉서, 인덱스
    ├── lsp/         LSP4J 클라이언트 — 전송, capabilities
    ├── language/    라우팅, 문서 동기화, 완성, 진단
    ├── runtime/     툴체인 설치, 프로세스 실행, 터미널
    ├── workspace/   파일 트리, 파일 조작, 이름 변경 리팩터, 검색
    ├── atlas-view/  overview 그래프 모델과 렌더러        (jvm + wasmJs)
    ├── atlas/       tree-sitter 코드 그래프, 패널
    ├── git/         `git status --porcelain` 연동
    ├── echo/        작업 시간축                          (스캐폴딩)
    ├── pair/        LLM 어댑터                           (스캐폴딩)
    └── app/         윈도우, 진입점, 조립
```

경계를 왜 여기에 그었는지는 [아키텍처 가이드](https://monkshark.github.io/page-ide/#guides/architecture.md)에 있다.

## 기록해둘 만한 작업

개발기에 풀어 쓴 문제들:

- **Kotlin LSP 콜드 스타트 145초 → 40초.** 언어 서버가 서브모듈마다 Gradle classpath 를 해석하고
  있었다. 루트에서 한 번 해석하면 전부 커버된다.
- **Windows 에서의 Swift.** GNU `@LongLink`·PAX tar 헤더, MSVC SDK 링크, 자식 프로세스 환경의
  `Path`/`PATH` 대소문자 충돌.
- **스크롤 20fps → 130fps.** 뷰포트 컬링, 줄 단위 레이아웃 캐시, 신택스·폴딩 파싱을 프레임
  경로 밖으로.

## 기여

- `main` 은 보호된다 — 브런치 → PR → CI 통과 → squash 머지.
- CI 는 ubuntu-latest + Temurin 21 + `./gradlew build`. 초록이어야 머지된다.
- 실제로 동작하는 코드에는 단위 테스트를 붙인다. 스캐폴딩은 면제.

## 라이선스

pre-alpha 동안은 미정.

## 연락

- 버그·제안: [GitHub Issues](https://github.com/monkshark/page-ide/issues)
- 개발기: <https://monkshark.github.io/categories/page-개발기/>
- 이메일: justinchoo0814@gmail.com
