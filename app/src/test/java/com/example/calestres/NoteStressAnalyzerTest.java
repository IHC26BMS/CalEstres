package com.example.calestres;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NoteStressAnalyzerTest {
    @Test
    public void emptyNotesHaveNoEffect() {
        assertEquals(0.0, NoteStressAnalyzer.sentiment("  "), 0.0);
    }

    @Test
    public void distressWordsProduceNegativeSentiment() {
        assertTrue(NoteStressAnalyzer.sentiment("Hoy me siento muy estresado y agobiado") < 0);
    }

    @Test
    public void positiveWordsProducePositiveSentiment() {
        assertTrue(NoteStressAnalyzer.sentiment("Hoy fue un día tranquilo y feliz") > 0);
    }

    @Test
    public void negationReversesNearbySentiment() {
        assertTrue(NoteStressAnalyzer.sentiment("No estoy estresado") > 0);
    }
}
