package com.ivy.quickcode

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class DummyTest : FreeSpec({
    "test" {
        2 + 2 shouldBe 4
    }
})