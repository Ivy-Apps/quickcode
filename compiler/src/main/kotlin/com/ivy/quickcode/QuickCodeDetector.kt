package com.ivy.quickcode

class QuickCodeDetector {
    fun detectQuickCode(template: String): Boolean {
        val specialSyntaxPattern = """\{\{.+?}}|#if \{\{.+?}}""".toRegex()
        return specialSyntaxPattern.containsMatchIn(template)
    }

}