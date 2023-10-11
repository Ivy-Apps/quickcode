package com.ivy.quickcode.lexer

sealed interface Token {
    data class RawText(val text: String) : Token

    data class Variable(val name: String) : Token {
        companion object {
            val syntax = TokenSyntax(
                tag = "{{",
                endTag = "}}",
            )
        }
    }

    data object If : Token {
        val syntax = TokenSyntax(
            tag = "#if",
        )

        override fun toString() = syntax.tag
    }

    sealed interface IfExpression : Token {
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

    data object Then : Token {
        val syntax = TokenSyntax(
            tag = "#then",
        )

        override fun toString() = syntax.tag
    }

    data object ElseIf : Token {
        val syntax = TokenSyntax(
            tag = "#elseif",
        )

        override fun toString() = syntax.tag
    }

    data object Else : Token {
        val syntax = TokenSyntax(
            tag = "#else",
        )

        override fun toString() = syntax.tag
    }

    data object EndIf : Token {
        val syntax = TokenSyntax(
            tag = "#endif",
        )

        override fun toString() = syntax.tag
    }
}

data class TokenSyntax(
    val tag: String,
    val endTag: String? = null,
)
