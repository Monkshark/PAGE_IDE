<div align="center">

<img src="assets/logo_transparent.svg" alt="PAGE" width="128" />

# PAGE

[![CI](https://github.com/monkshark/page-ide/actions/workflows/ci.yml/badge.svg)](https://github.com/monkshark/page-ide/actions/workflows/ci.yml)
[![설계 문서](https://img.shields.io/badge/design%20docs-monkshark.github.io-4C5AB8)](https://monkshark.github.io/page-ide/)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-pre--alpha-orange)

**Kotlin 과 Compose Multiplatform 으로 만든 다언어 데스크톱 IDE.**

[설계 문서](https://monkshark.github.io/page-ide/) · [English](README.md)

<!-- 데모: 녹화 후 아래 주석을 풀고 assets/demo.gif 로 교체
<img src="assets/demo.gif" alt="PAGE 사용 화면" width="880" />
-->

</div>

PAGE 는 24개 언어를 편집·탐색·실행하고, 필요한 언어 서버와 툴체인을 스스로 설치한다. 이름이
곧 만들려는 것이다 — **P**air, **A**tlas, **G**lass, **E**cho: AI 동반자, 코드 그래프,
디자인 시스템, 그리고 작업의 시간축.

> pre-alpha. 매일 자기 자신을 만드는 데 쓰지만, 아직 유일한 에디터로 삼을 단계는 아니다.

## 기능

- 24개 언어 언어 서버 라우팅 — 자동완성, 진단, 정의 이동, 이름 변경
- 서버 없이도 사용처·죽은 코드·안 쓰는 import 를 찾는 프로젝트 전체 심볼 인덱스
- 13개 언어 단일 파일 실행, 증분 빌드 캐시와 내장 터미널
- 툴체인 IDE 내 설치 — JDK, Node, Python, Go, Rust, .NET, Dart, Swift, Clang, MSVC
- inline diff 가 붙은 코드 액션, 여러 파일을 건드리는 편집을 위한 검토 패널
- 빌드 메타데이터가 아니라 tree-sitter 로 그리는 import·호출·모듈 그래프
- 분할 뷰, 폴딩, inlay hint, 테마 9종, 모든 단축키를 담은 액션 테이블 하나

<sub>Kotlin · Java · Python · TypeScript/JavaScript · Go · Rust · C/C++ · Swift · Dart/Flutter ·
Ruby · PHP · C# · Vue · Svelte · Bash · JSON · YAML · HTML · CSS · SQL · Markdown · Dockerfile</sub>

## 빌드

아직 릴리스는 없다. JDK 는 Gradle 이 받아오므로 Git 만 있으면 된다.

```bash
git clone https://github.com/monkshark/page-ide.git
cd page-ide
./gradlew :page:app:run
```

`./gradlew build` 는 전체 모듈을 컴파일하고 테스트를 돌린다.

## 설계 문서

아직 사용 설명서는 없다 — 쓸 수 있게 배포된 것이 없기 때문이다. 지금 있는 문서는 PAGE 를
고치는 사람을 위한 것이다.

- [개요](https://monkshark.github.io/page-ide/#guides/overview.md) — PAGE 가 무엇을 위한 것이고, 무엇을 하지 않을 것인가
- [아키텍처](https://monkshark.github.io/page-ide/#guides/architecture.md) — 16개 모듈, 단방향 의존, 스택 결정
- [시작하기](https://monkshark.github.io/page-ide/#guides/getting_started.md) — PAGE 자체를 실행하고 디버깅하기
- [내부 구조](https://monkshark.github.io/page-ide/#README_kr.md) — 모듈별 설계 노트

## 기여

`main` 은 보호된다. 브런치를 파고 PR 을 열어 CI 가 초록이 된 뒤 머지한다. 실제로 동작하는
코드에는 테스트를 붙인다. 모듈을 추가하기 전에 [아키텍처 가이드](https://monkshark.github.io/page-ide/#guides/architecture.md)를 먼저 읽는다.

## 라이선스

pre-alpha 동안은 미정.

## 연락

[GitHub Issues](https://github.com/monkshark/page-ide/issues) · justinchoo0814@gmail.com
