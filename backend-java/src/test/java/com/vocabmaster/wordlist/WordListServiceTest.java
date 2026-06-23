package com.vocabmaster.wordlist;

import com.vocabmaster.wordlist.service.WordListService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordListServiceTest {
    @Test void unitComplete_whenAllLearned() { assertTrue(WordListService.isUnitComplete(10, 10)); }
    @Test void unitNotComplete_whenSomeUnlearned() { assertFalse(WordListService.isUnitComplete(10, 9)); }
    @Test void unitNotComplete_whenZeroLearned() { assertFalse(WordListService.isUnitComplete(10, 0)); }
    @Test void unitComplete_emptyUnit() { assertTrue(WordListService.isUnitComplete(0, 0)); }
}
