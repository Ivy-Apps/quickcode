package com.ivy.quickcode

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class QuickCodeDetectorTest : FreeSpec({

    lateinit var service: QuickCodeDetector

    beforeEach {
        service = QuickCodeDetector()
    }

    "empty string" {
        // given
        val input = ""

        // when
        val output = service.detectQuickCode(input)

        // then
        output.shouldBeFalse()
    }

    "string with no special syntax" {
        // given
        val input = "This is a test string with no special syntax."

        // when
        val output = service.detectQuickCode(input)

        // then
        output.shouldBeFalse()
    }

    "string with variable syntax" {
        // given
        val input = "This string contains a {{variable}}."

        // when
        val output = service.detectQuickCode(input)

        // then
        output.shouldBeTrue()
    }

    "string with if syntax" {
        // given
        val input = "This string contains an #if {{condition}}."

        // when
        val output = service.detectQuickCode(input)

        // then
        output.shouldBeTrue()
    }

    "string with var and if" {
        // given
        val input = "This string contains a {{variable}} and an #if {{condition}}."

        // when
        val output = service.detectQuickCode(input)

        // then
        output.shouldBeTrue()
    }
})
