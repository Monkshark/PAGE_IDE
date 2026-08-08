# SyntaxPalette

> `shared-core/src/commonMain/kotlin/page/shared/syntax/SyntaxPalette.kt` — Syntax token color bundle · instances in `page/ui/.../GlassTokens.kt`

Nine color slots mapped to `TokenKind` in `page:editor`. Palette and lexer are decoupled, so each theme just swaps its instance. Every Glass palette carries its own `SyntaxPalette`, and the editor reads the active theme's via `Glass.colors.syntax`.

> 한국어: [syntax_colors.md](https://monkshark.github.io/page-ide/#internals/ui/syntax_colors.md)

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

There is no `PUNCT` slot. Parens, semicolons, and commas are too frequent — coloring them adds noise, so `colorFor` returns `null` and leaves them in the body color.

---

## `SyntaxPreset`

`shared-core/.../SyntaxPreset.kt` decides how much of the palette the editor actually uses. The palette always carries a color per role; the preset picks how many of them show up and whether emphasis is layered on top.

| Preset | Role colors | Emphasis |
| --- | --- | --- |
| `CALM` | no — calls and members read as plain identifiers | no |
| `BALANCED` | no | italic calls, underlined members, dimmer parameters |
| `VIVID` | yes | no |
| `EXPRESSIVE` | yes | yes |

`SyntaxRoles.refine(text, tokens)` upgrades identifier and string tokens into `FUNCTION`, `PROPERTY`, `PARAMETER` and `TEMPLATE` after any lexer runs, so both the hand written and tree-sitter backends get roles from one place.

Settings → Editor → Syntax colors selects the preset; `EditorOptions.syntaxPreset` persists it.

---

## Rainbow brackets

`BracketScan.pairs(text, tokens)` walks the file once and pairs brackets with their nesting depth, skipping anything inside strings and comments. `SyntaxPalette.bracketAt(depth)` cycles the palette's own hues, so each theme rainbows in its own colors instead of a fixed set.

Scope guides come from the same pairs: a rail runs from an opening bracket down to its match, which keeps it on the right column when arguments wrap. Settings → Editor → Rainbow brackets and Scope guides control both.

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

The other themes' syntax colors are on the palette cards in [Glass Design System](https://monkshark.github.io/page-ide/#internals/ui/glass_theme_en.md). `Cool` and `Frost` follow GitHub tones, `Graphite` follows Darcula, and `Warm` and `Sand` match a sepia mood.

---

## Usage

The lexers (`KotlinLexer`, `JavaLexer`, `JsonLexer`) emit `TokenKind`, which `EditorPanel.colorFor(kind, palette)` turns into color.

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

`palette` is `Glass.colors.syntax` — switch the active theme and the syntax colors follow. The uncolored `PUNCT` renders in the body color (`text`).

---

- [Back to contents](https://monkshark.github.io/page-ide/#README.md)
