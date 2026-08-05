package page.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlassSignatureTest {

    private fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAA(fg: Color, bg: Color, label: String) {
        val ratio = contrast(fg, bg)
        assertTrue(ratio >= 4.5, "$label 대비 ${"%.2f".format(ratio)} < 4.5")
    }

    @Test
    fun signaturePalettesResolve() {
        val dark = glassTokensFor(GlassPalette.Signature)
        val light = glassTokensFor(GlassPalette.SignatureLight)
        assertEquals(GlassPalette.Signature, dark.palette)
        assertEquals(GlassPalette.SignatureLight, light.palette)
        assertFalse(dark.color.isLight)
        assertTrue(light.color.isLight)
    }

    @Test
    fun signatureDepthTokensAreDistinct() {
        val c = glassTokensFor(GlassPalette.Signature).color
        assertTrue(c.surfaceL1 != c.surfaceL2)
        assertTrue(c.surfaceL2 != c.surfaceL3)
        assertTrue(c.surfaceL3 != c.surfaceRaised)
        assertTrue(c.success != c.danger)
    }

    @Test
    fun textAndMutedMeetAAOnEverySurface() {
        for (palette in listOf(GlassPalette.Signature, GlassPalette.SignatureLight)) {
            val c = glassTokensFor(palette).color
            for ((name, surface) in listOf("L1" to c.surfaceL1, "L2" to c.surfaceL2, "L3" to c.surfaceL3)) {
                assertAA(c.text, surface, "$palette text/$name")
                assertAA(c.muted, surface, "$palette muted/$name")
            }
        }
    }

    private fun assertCeiling(fg: Color, bg: Color, ceiling: Double, label: String) {
        val ratio = contrast(fg, bg)
        assertTrue(ratio <= ceiling, "$label 대비 ${"%.2f".format(ratio)} > $ceiling")
    }

    @Test
    fun tunedPalettesStayUnderContrastCeiling() {
        val tuned = listOf(
            GlassPalette.Signature,
            GlassPalette.SignatureLight,
            GlassPalette.Cool,
            GlassPalette.Frost,
        )
        for (palette in tuned) {
            val c = glassTokensFor(palette).color
            assertCeiling(c.text, c.background, 10.5, "$palette text")
            assertCeiling(c.syntax.identifier, c.background, 10.5, "$palette identifier")
            for ((name, token) in listOf(
                "keyword" to c.syntax.keyword,
                "string" to c.syntax.string,
                "number" to c.syntax.number,
                "type" to c.syntax.type,
            )) {
                assertCeiling(token, c.background, 8.5, "$palette $name")
            }
        }
    }

    @Test
    fun highContrastPalettesKeepTheirBite() {
        for (palette in listOf(GlassPalette.Midnight, GlassPalette.Forest, GlassPalette.Warm)) {
            val c = glassTokensFor(palette).color
            assertTrue(
                contrast(c.text, c.background) > 12.0,
                "$palette 은 고대비 선택지로 남아야 함",
            )
        }
    }

    @Test
    fun signatureSyntaxMeetsAAOnBase() {
        val c = glassTokensFor(GlassPalette.Signature).color
        val base = c.surfaceL1
        assertAA(c.syntax.keyword, base, "keyword")
        assertAA(c.syntax.string, base, "string")
        assertAA(c.syntax.type, base, "type")
        assertAA(c.syntax.comment, base, "comment")
    }
}
