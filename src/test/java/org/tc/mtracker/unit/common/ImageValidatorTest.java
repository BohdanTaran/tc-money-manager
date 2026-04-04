package org.tc.mtracker.unit.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.tc.mtracker.common.image.ImageValidator;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ImageValidatorTest {

    private final ImageValidator validator = new ImageValidator();

    @Test
    void shouldAcceptSupportedImageTypes() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        assertThat(validator.isValid(file, null)).isTrue();
    }

    @Test
    void shouldRejectUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "text/plain",
                "image".getBytes()
        );

        assertThat(validator.isValid(file, null)).isFalse();
    }

    @Test
    void shouldRejectUnsupportedExtensionEvenWhenMimeTypeLooksValid() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.txt",
                "image/jpeg",
                "image".getBytes()
        );

        assertThat(validator.isValid(file, null)).isFalse();
    }
}
