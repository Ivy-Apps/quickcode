package com.ivy.quickcode

import com.ivy.quickcode.data.*

class QuickCodeInterpreter(
    private val variables: Map<String, QCVariableValue>,
) {
    fun evaluate(
        ast: QuickCodeAst,
    ): String {
        return buildString {
            evaluateInternal(ast)
        }
    }

    private fun StringBuilder.evaluateInternal(ast: QuickCodeAst) {
        var current: QuickCodeAst? = ast
        while (current != null) {
            when (current) {
                is QuickCodeAst.Begin -> {
                    // do nothing
                }

                is IfStatement -> {
                    if (evaluate(current.condition)) {
                        evaluateInternal(current.thenBranch)
                    } else {
                        current.elseBranch?.let { evaluateInternal(it) }
                    }
                }

                is RawText -> {
                    append(current.text)
                }

                is Variable -> {
                    variables[current.name]?.let {
                        append(
                            when (it) {
                                is QCVariableValue.Bool -> it.value.toString()
                                is QCVariableValue.Str -> it.value
                            }
                        )
                    }
                }
            }
            current = current.next
        }
    }

    private fun evaluate(
        condition: IfStatement.Condition
    ): Boolean {
        return when (condition) {
            is IfStatement.Condition.And -> {
                evaluate(condition.cond1) && evaluate(condition.cond2)
            }

            is IfStatement.Condition.BoolVar -> {
                return (variables[condition.name] as? QCVariableValue.Bool)?.value ?: false
            }

            is IfStatement.Condition.Brackets -> {
                evaluate(condition.cond)
            }

            is IfStatement.Condition.Not -> {
                !evaluate(condition.cond)
            }

            is IfStatement.Condition.Or -> {
                evaluate(condition.cond1) || evaluate(condition.cond2)
            }
        }
    }
}