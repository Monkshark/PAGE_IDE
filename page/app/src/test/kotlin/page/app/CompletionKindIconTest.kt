package page.app

import page.lsp.CompletionItemKind
import page.ui.CompletionKindIcon
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionKindIconTest {

    @Test
    fun `methods functions and constructors map to the method glyph`() {
        assertEquals(CompletionKindIcon.METHOD, completionKindIcon(CompletionItemKind.METHOD))
        assertEquals(CompletionKindIcon.METHOD, completionKindIcon(CompletionItemKind.FUNCTION))
        assertEquals(CompletionKindIcon.METHOD, completionKindIcon(CompletionItemKind.CONSTRUCTOR))
    }

    @Test
    fun `properties and fields map to the property glyph`() {
        assertEquals(CompletionKindIcon.PROPERTY, completionKindIcon(CompletionItemKind.PROPERTY))
        assertEquals(CompletionKindIcon.PROPERTY, completionKindIcon(CompletionItemKind.FIELD))
    }

    @Test
    fun `class and struct share the class glyph while interface is distinct`() {
        assertEquals(CompletionKindIcon.CLASS, completionKindIcon(CompletionItemKind.CLASS))
        assertEquals(CompletionKindIcon.CLASS, completionKindIcon(CompletionItemKind.STRUCT))
        assertEquals(CompletionKindIcon.INTERFACE, completionKindIcon(CompletionItemKind.INTERFACE))
    }

    @Test
    fun `enum members keywords and values map to their glyphs`() {
        assertEquals(CompletionKindIcon.ENUM, completionKindIcon(CompletionItemKind.ENUM))
        assertEquals(CompletionKindIcon.ENUM, completionKindIcon(CompletionItemKind.ENUM_MEMBER))
        assertEquals(CompletionKindIcon.KEYWORD, completionKindIcon(CompletionItemKind.KEYWORD))
        assertEquals(CompletionKindIcon.KEYWORD, completionKindIcon(CompletionItemKind.OPERATOR))
        assertEquals(CompletionKindIcon.VARIABLE, completionKindIcon(CompletionItemKind.VARIABLE))
        assertEquals(CompletionKindIcon.VARIABLE, completionKindIcon(CompletionItemKind.VALUE))
        assertEquals(CompletionKindIcon.CONSTANT, completionKindIcon(CompletionItemKind.CONSTANT))
        assertEquals(CompletionKindIcon.MODULE, completionKindIcon(CompletionItemKind.MODULE))
        assertEquals(CompletionKindIcon.SNIPPET, completionKindIcon(CompletionItemKind.SNIPPET))
    }

    @Test
    fun `unmapped kinds fall back to the generic glyph`() {
        assertEquals(CompletionKindIcon.GENERIC, completionKindIcon(CompletionItemKind.TEXT))
        assertEquals(CompletionKindIcon.GENERIC, completionKindIcon(CompletionItemKind.FILE))
        assertEquals(CompletionKindIcon.GENERIC, completionKindIcon(CompletionItemKind.TYPE_PARAMETER))
    }
}
