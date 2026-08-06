# SyntaxPalette

> `shared-core/src/commonMain/kotlin/page/shared/syntax/SyntaxPalette.kt` — 신택스 토큰 색상 번들 · 인스턴스는 `page/ui/.../GlassTokens.kt`

`page:editor` 의 `TokenKind` 와 매핑되는 아홉 개 색 슬롯. 팔레트와 렉서가 분리돼 있어 테마마다 인스턴스만 갈아끼우면 된다. 각 Glass 팔레트가 자기 `SyntaxPalette` 를 들고 있고, 에디터는 활성 테마의 것을 `Glass.colors.syntax` 로 읽는다

> English: [syntax_colors_en.md](https://monkshark.github.io/page-ide/#modules/ui/syntax_colors_en.md)

---

## `SyntaxPalette`

```kotlin
data class SyntaxPalette(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val docComment: Color,
    val todoTag: Color,
    val annotation: Color,
    val type: Color,
    val identifier: Color,
    val function: Color = identifier,
    val property: Color = identifier,
    val parameter: Color = identifier,
    val template: Color = identifier,
)
```

`PUNCT` 슬롯은 없다. 괄호·세미콜론·콤마는 너무 자주 나와 색을 입히면 본문이 시끄럽다 → `colorFor` 가 `null` 을 돌려주고 본문색 그대로 둔다

---

## `SyntaxPreset`

`shared-core/.../SyntaxPreset.kt` 가 팔레트를 얼마나 쓸지 정한다. 팔레트는 역할마다 색을 항상 들고 있고, 프리셋이 그중 몇 개를 쓸지와 강조를 얹을지를 고른다

| 프리셋 | 역할 색 | 강조 |
| --- | --- | --- |
| `CALM` | 안 씀 — 호출·멤버가 일반 식별자로 보인다 | 없음 |
| `BALANCED` | 안 씀 | 호출 기울임 · 멤버 밑줄 · 파라미터 옅게 |
| `VIVID` | 씀 | 없음 |
| `EXPRESSIVE` | 씀 | 있음 |

`SyntaxRoles.refine(text, tokens)` 가 렉서가 끝난 뒤 식별자·문자열 토큰을 `FUNCTION`·`PROPERTY`·`PARAMETER`·`TEMPLATE` 로 승격시킨다. 손으로 쓴 렉서든 tree-sitter 든 역할 판별은 이 한 곳에서 나온다

설정 → Editor → Syntax colors 로 고르고 `EditorOptions.syntaxPreset` 에 저장된다

---

## 무지개 괄호

`BracketScan.pairs(text, tokens)` 가 파일을 한 번 훑어 괄호를 중첩 깊이와 함께 짝짓는다. 문자열·주석 안의 괄호는 건너뛴다. `SyntaxPalette.bracketAt(depth)` 는 팔레트 자기 색을 순환시켜, 고정된 무지개색이 아니라 테마마다 제 색으로 물든다

스코프 선도 같은 짝에서 나온다. 여는 괄호에서 짝까지 세로선을 긋기 때문에 인자가 줄바꿈돼도 열이 어긋나지 않는다. 설정 → Editor → Rainbow brackets · Scope guides 로 조절한다

---

## Signature palette

<div class="glassdoc">
<style>
.glassdoc .synx{border:1px solid #303846;border-radius:12px;overflow:hidden;background:#212630;}
.glassdoc .synx .r{display:flex;align-items:center;gap:12px;padding:11px 15px;border-bottom:1px solid rgba(255,255,255,.06);font-size:13px;color:#C4C6CE;}
.glassdoc .synx .r:last-child{border-bottom:0;}
.glassdoc .synx i{width:15px;height:15px;border-radius:4px;border:1px solid rgba(255,255,255,.1);flex:none;}
.glassdoc .synx .k{width:96px;flex:none;font-family:ui-monospace,Consolas,monospace;}
.glassdoc .synx .h{width:82px;flex:none;color:#8A92A6;font-family:ui-monospace,Consolas,monospace;font-size:12px;font-variant-numeric:tabular-nums;}
.glassdoc .synx .s{color:#8A92A6;font-family:ui-monospace,Consolas,monospace;}
</style>
<div class="synx">
<div class="r"><i style="background:#8E9CFF"></i><span class="k" style="color:#8E9CFF">keyword</span><span class="h">#8E9CFF</span><span class="s"><b style="color:#8E9CFF">class</b> · fun · val · return</span></div>
<div class="r"><i style="background:#63C2A6"></i><span class="k" style="color:#63C2A6">string</span><span class="h">#63C2A6</span><span class="s"><b style="color:#63C2A6">"hello"</b> · 'a' · """multi"""</span></div>
<div class="r"><i style="background:#9AB0EB"></i><span class="k" style="color:#9AB0EB">number</span><span class="h">#9AB0EB</span><span class="s"><b style="color:#9AB0EB">42</b> · 3.14f · 0xFF · 1e10</span></div>
<div class="r"><i style="background:#7F8AA4"></i><span class="k" style="color:#7F8AA4">comment</span><span class="h">#7F8AA4</span><span class="s"><b style="color:#7F8AA4">// line</b> · /* block */</span></div>
<div class="r"><i style="background:#6E8FA8"></i><span class="k" style="color:#6E8FA8">docComment</span><span class="h">#6E8FA8</span><span class="s"><b style="color:#6E8FA8">/** kdoc */</b> · @param</span></div>
<div class="r"><i style="background:#F08FC8"></i><span class="k" style="color:#F08FC8">todoTag</span><span class="h">#F08FC8</span><span class="s"><b style="color:#F08FC8">TODO</b> · FIXME · XXX</span></div>
<div class="r"><i style="background:#B49BE8"></i><span class="k" style="color:#B49BE8">annotation</span><span class="h">#B49BE8</span><span class="s"><b style="color:#B49BE8">@Composable</b> · @Override</span></div>
<div class="r"><i style="background:#6BBEC2"></i><span class="k" style="color:#6BBEC2">type</span><span class="h">#6BBEC2</span><span class="s"><b style="color:#6BBEC2">String</b> · MutableList</span></div>
<div class="r"><i style="background:#C4C6CE"></i><span class="k" style="color:#C4C6CE">identifier</span><span class="h">#C4C6CE</span><span class="s"><b style="color:#C4C6CE">count</b> · userName · items</span></div>
</div>
</div>

다른 테마의 신택스는 [Glass Design System](https://monkshark.github.io/page-ide/#modules/ui/glass_theme.md) 팔레트 카드에서 확인. `Cool`·`Frost` 는 GitHub 계열, `Graphite` 는 Darcula 계열, `Warm`·`Sand` 는 세피아 계열로 각자 매칭된다

---

## 사용처

렉서(`KotlinLexer`·`JavaLexer`·`JsonLexer`)가 뱉는 `TokenKind` 를 `EditorPanel.colorFor(kind, palette)` 가 색으로 변환한다

```kotlin
private fun colorFor(kind: TokenKind, palette: SyntaxPalette) = when (kind) {
    TokenKind.KEYWORD -> palette.keyword
    TokenKind.STRING -> palette.string
    TokenKind.NUMBER -> palette.number
    TokenKind.COMMENT -> palette.comment
    TokenKind.DOC_COMMENT -> palette.docComment
    TokenKind.TODO_TAG -> palette.todoTag
    TokenKind.ANNOTATION -> palette.annotation
    TokenKind.TYPE -> palette.type
    TokenKind.IDENTIFIER -> palette.identifier
    TokenKind.PUNCT -> null
}
```

`palette` 는 `Glass.colors.syntax` — 활성 테마를 바꾸면 신택스도 함께 바뀐다. 색이 없는 `PUNCT` 는 본문색(`text`)으로 렌더된다

---

- [목차로 돌아가기](https://monkshark.github.io/page-ide/#README_kr.md)
