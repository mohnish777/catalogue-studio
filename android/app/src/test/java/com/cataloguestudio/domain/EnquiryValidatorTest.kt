package com.cataloguestudio.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class EnquiryValidatorTest {
    @Test
    fun `valid names preserve international characters and trim surrounding spaces`() {
        for (name in listOf("Mohnish Chittoory", "O'Brien", "Shraddha-Marie", "श्रद्धा", "李 明")) {
            assertThat(EnquiryValidator.validateName("  $name  "))
                .isEqualTo(Result.Success(name))
        }
    }

    @Test
    fun `blank names are required`() {
        for (name in listOf("", "   ", "\t")) {
            assertThat(EnquiryValidator.validateName(name))
                .isEqualTo(Result.Error(EnquiryValidationError.NAME_REQUIRED))
        }
    }

    @Test
    fun `names containing controls are rejected before trimming can hide them`() {
        for (name in listOf("Alex\nSmith", "Alex\rSmith", "Alex\r\n", "\nAlex", "Alex\tSmith", "Alex\u0000")) {
            assertThat(EnquiryValidator.validateName(name))
                .isEqualTo(Result.Error(EnquiryValidationError.NAME_INVALID))
        }
    }

    @Test
    fun `names accept 100 characters and reject 101 after trimming`() {
        val boundary = "a".repeat(100)
        assertThat(EnquiryValidator.validateName(" $boundary "))
            .isEqualTo(Result.Success(boundary))
        assertThat(EnquiryValidator.validateName("a".repeat(101)))
            .isEqualTo(Result.Error(EnquiryValidationError.NAME_TOO_LONG))
    }

    @Test
    fun `valid emails allow plus addressing and subdomains while preserving case`() {
        for (email in listOf("Alex@example.com", "alex.smith+catalogue@shop.example.co.uk", "o'brien@example.com")) {
            assertThat(EnquiryValidator.validateEmail("  $email  "))
                .isEqualTo(Result.Success(email))
        }
    }

    @Test
    fun `blank emails are required`() {
        for (email in listOf("", "   ")) {
            assertThat(EnquiryValidator.validateEmail(email))
                .isEqualTo(Result.Error(EnquiryValidationError.EMAIL_REQUIRED))
        }
    }

    @Test
    fun `malformed addresses and header-like input are rejected`() {
        for (email in listOf(
            "alex", "alex@localhost", "@example.com", "alex@@example.com",
            "alex smith@example.com", "Alex <alex@example.com>", ".alex@example.com",
            "alex.@example.com", "alex..smith@example.com", "alex@-example.com",
            "alex@exam_ple.com", "alex@example.com\r\nBcc: other@example.com",
            "\nalex@example.com", "alex@example.com\r\n", "alex\u0000@example.com",
            "a".repeat(65) + "@example.com",
        )) {
            assertThat(EnquiryValidator.validateEmail(email), email)
                .isEqualTo(Result.Error(EnquiryValidationError.EMAIL_INVALID))
        }
    }

    @Test
    fun `email length boundary is 254 characters after trimming`() {
        val boundary = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61)
        assertThat(boundary.length).isEqualTo(254)
        assertThat(EnquiryValidator.validateEmail(" $boundary "))
            .isEqualTo(Result.Success(boundary))
        assertThat(EnquiryValidator.validateEmail(boundary + "d"))
            .isEqualTo(Result.Error(EnquiryValidationError.EMAIL_TOO_LONG))
    }

    @Test
    fun `message is optional and whitespace-only input becomes empty`() {
        for (message in listOf("", "   ", "\r\n")) {
            assertThat(EnquiryValidator.validateMessage(message)).isEqualTo(Result.Success(""))
        }
    }

    @Test
    fun `message preserves internal line breaks and trims surrounding whitespace`() {
        assertThat(EnquiryValidator.validateMessage("  Hello\nIs this available?  "))
            .isEqualTo(Result.Success("Hello\nIs this available?"))
    }

    @Test
    fun `message accepts 2000 characters and rejects 2001 after trimming`() {
        val boundary = "a".repeat(2_000)
        assertThat(EnquiryValidator.validateMessage(" $boundary "))
            .isEqualTo(Result.Success(boundary))
        assertThat(EnquiryValidator.validateMessage(boundary + "a"))
            .isEqualTo(Result.Error(EnquiryValidationError.MESSAGE_TOO_LONG))
    }
}
