package com.mall.exam.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionFingerprintTest {

    @Test
    void fingerprintIgnoresCaseAndRepeatedWhitespace() {
        String first = QuestionFingerprint.sha256("A  short\npassage", "What IS true?");
        String second = QuestionFingerprint.sha256(" a short passage ", "what is true?");

        assertEquals(first, second);
        assertEquals(64, first.length());
    }
}
