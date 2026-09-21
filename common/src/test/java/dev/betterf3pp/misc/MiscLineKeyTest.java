package dev.betterf3pp.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The key derivation is the one piece of this mod that decides whether a user's "hide this line"
 * choice survives the next frame, so it gets tested directly. It is pure string logic with no
 * Minecraft types, which is why it can be.
 */
class MiscLineKeyTest {

    @Test
    @DisplayName("keeps the name half of a name: value line")
    void splitsOnFirstColon() {
        assertEquals("[Sodium] Chunks", MiscLineKey.of("[Sodium] Chunks: 128/512"));
    }

    @Test
    @DisplayName("only the first colon separates name from value")
    void splitsOnFirstColonOnly() {
        assertEquals("Shader", MiscLineKey.of("Shader: pack: complementary"));
    }

    @Test
    @DisplayName("the key does not change when the value does")
    void isStableAcrossFrames() {
        final String first = MiscLineKey.of("Chunks: 128/512");
        final String second = MiscLineKey.of("Chunks: 129/512");
        assertEquals(first, second);
    }

    @Test
    @DisplayName("a line without a colon keeps its shape but loses its numbers")
    void collapsesDigitsWhenThereIsNoColon() {
        assertEquals("Shader pass # active", MiscLineKey.of("Shader pass 42 active"));
        assertEquals(MiscLineKey.of("Shader pass 42 active"), MiscLineKey.of("Shader pass 7 active"));
    }

    @Test
    @DisplayName("formatting codes never reach the key")
    void stripsFormattingCodes() {
        assertEquals("Targeted Block", MiscLineKey.of("§nTargeted Block: 1 2 3"));
    }

    @Test
    @DisplayName("a line starting with a colon falls back to the whole line")
    void handlesLeadingColon() {
        assertEquals(": bare", MiscLineKey.of(": bare"));
    }

    @Test
    @DisplayName("blank and formatting-only lines have no identity")
    void hasNoKeyForBlankLines() {
        assertTrue(MiscLineKey.of("").isEmpty());
        assertTrue(MiscLineKey.of("   ").isEmpty());
        assertTrue(MiscLineKey.of("§r").isEmpty());
        assertTrue(MiscLineKey.of(null).isEmpty());
    }

    @Test
    @DisplayName("a pathological line cannot bloat the config")
    void truncatesLongKeys() {
        final String key = MiscLineKey.of("x".repeat(500));
        assertEquals(MiscLineKey.MAX_LENGTH, key.length());
    }

    @Test
    @DisplayName("lookup is case-insensitive, display is not")
    void normalizesForLookupOnly() {
        assertEquals("[sodium] chunks", MiscLineKey.normalize("[Sodium] Chunks"));
        assertEquals("[Sodium] Chunks", MiscLineKey.of("[Sodium] Chunks: 1"));
    }
}
