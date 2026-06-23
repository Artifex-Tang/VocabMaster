package com.vocabmaster.test;

import com.vocabmaster.test.service.TestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestServicePhraseTest {
    @Test void singleWord_notPhrase() { assertFalse(TestService.isPhrase("caring", "adjective")); }
    @Test void multiWord_isPhrase() { assertTrue(TestService.isPhrase("give up", "phrasal verb")); }
    @Test void longPhrase_isPhrase() { assertTrue(TestService.isPhrase("leave something to the last minute", "phrase")); }
    @Test void phrasePos_isPhrase_evenSingleToken() {
        assertTrue(TestService.isPhrase("anyword", "phrase"));
        assertTrue(TestService.isPhrase("anyword", "phrasal verb"));
    }
    @Test void spellingNotAllowedForPhrase() {
        assertTrue(TestService.allowsMode("give up", "phrasal verb", "choice"));
        assertTrue(TestService.allowsMode("give up", "phrasal verb", "listening"));
        assertFalse(TestService.allowsMode("give up", "phrasal verb", "spelling"));
    }
    @Test void spellingAllowedForSingleWord() {
        assertTrue(TestService.allowsMode("caring", "adjective", "spelling"));
    }
}
