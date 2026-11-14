package cn.lux.music.sources

/** Private Binder protocol; script/expressions/results never cross core:api. */
internal object SandboxWire {
    const val RUN = 1
    const val STOP = 2
    const val RESULT = 3
    const val SCRIPT = "script"
    const val EXPRESSION = "expression"
    const val VALUE = "value"
    const val ERROR = "error"
    const val DEADLINE = "deadline"
    const val MAX_EXPRESSION_CHARS = 8192
    const val MAX_RESULT_CHARS = 32_768
    const val MAX_EXECUTION_MS = 5000
}
