package com.ivy.quickcode.lexer.model

sealed interface QuickCodeToken {
    data class RawText(val text: String) : QuickCodeToken

    data class Variable(val name: String) : QuickCodeToken {
        companion object {
            val syntax = TokenSyntax(
                tag = "{{",
                endTag = "}}",
            )
        }
    }

    data object If : QuickCodeToken {
        val syntax = TokenSyntax(
            tag = "#if",
        )

        override fun toString() = syntax.tag
    }

    sealed interface IfExpression : QuickCodeToken {
        data class BoolVariable(val name: String) : IfExpression {
            companion object {
                val syntax = TokenSyntax(
                    tag = "{{",
                    endTag = "}}"
                )
            }
        }

        data object Not : IfExpression {
            val syntax = TokenSyntax(
                tag = "NOT",
            )

            override fun toString() = syntax.tag
        }

        data object And : IfExpression {
            val syntax = TokenSyntax(
                tag = "AND",
            )

            override fun toString() = syntax.tag
        }

        data object Or : IfExpression {
            val syntax = TokenSyntax(
                tag = "OR",
            )

            override fun toString() = syntax.tag
        }

        data object OpenBracket : IfExpression {
            val syntax = TokenSyntax(
                tag = "(",
            )

            override fun toString() = syntax.tag
        }

        data object CloseBracket : IfExpression {
            val syntax = TokenSyntax(
                tag = ")",
            )

            override fun toString() = syntax.tag
        }
    }

    data object Then : QuickCodeToken {
        val syntax = TokenSyntax(
            tag = "#then",
        )

        override fun toString() = syntax.tag
    }

    data object ElseIf : QuickCodeToken {
        val syntax = TokenSyntax(
            tag = "#elseif",
        )

        override fun toString() = syntax.tag
    }

    data object Else : QuickCodeToken {
        val syntax = TokenSyntax(
            tag = "#else",
        )

        override fun toString() = syntax.tag
    }

    data object EndIf : QuickCodeToken {
        val syntax = TokenSyntax(
            tag = "#endif",
        )

        override fun toString() = syntax.tag
    }
}

