package org.tc.mtracker.unit.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.tc.mtracker.common.receipt.ReceiptValidator;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ReceiptValidatorTest {

    private final ReceiptValidator validator = new ReceiptValidator();

    @Test
    void shouldAcceptPdfAndImages() {
        MockMultipartFile pdf = new MockMultipartFile(
                "receipt",
                "receipt.pdf",
                "application/pdf",
                "%PDF".getBytes()
        );

        assertThat(validator.isValid(pdf, null)).isTrue();
    }

    @Test
    void shouldRejectUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "receipt",
                "receipt.pdf",
                "text/plain",
                "plain".getBytes()
        );

        assertThat(validator.isValid(file, null)).isFalse();
    }

    @Test
    void shouldRejectUnsupportedExtensionWhenMimeTypeIsValid() {
        MockMultipartFile file = new MockMultipartFile(
                "receipt",
                "receipt.txt",
                "application/pdf",
                "%PDF".getBytes()
        );

        assertThat(validator.isValid(file, null)).isFalse();
    }
}
