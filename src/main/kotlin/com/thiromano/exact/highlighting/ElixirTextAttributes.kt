package com.thiromano.exact.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object ElixirTextAttributes {
    val MODULE_NAME = key("ELIXIR_MODULE_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
    val FUNCTION_DECLARATION = key("ELIXIR_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
    val FUNCTION_CALL = key("ELIXIR_FUNCTION_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL)
    val ATOM = key("ELIXIR_ATOM", DefaultLanguageHighlighterColors.CONSTANT)
    val STRING = key("ELIXIR_STRING", DefaultLanguageHighlighterColors.STRING)
    val NUMBER = key("ELIXIR_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val KEYWORD = key("ELIXIR_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val COMMENT = key("ELIXIR_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val SIGIL = key("ELIXIR_SIGIL", DefaultLanguageHighlighterColors.STRING)
    val MODULE_ATTRIBUTE = key("ELIXIR_MODULE_ATTRIBUTE", DefaultLanguageHighlighterColors.METADATA)
    val VARIABLE = key("ELIXIR_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)

    /**
     * Maps a TextMate scope string to an Elixir-specific [TextAttributesKey],
     * or null if no custom mapping applies.
     */
    fun mapScopeToKey(scope: String): TextAttributesKey? = when {
        "entity.name.type.module" in scope -> MODULE_NAME
        "entity.name.function.call" in scope -> FUNCTION_CALL
        "entity.name.function" in scope -> FUNCTION_DECLARATION
        "variable.other" in scope -> VARIABLE
        "variable.language" in scope -> VARIABLE
        "constant.language.symbol" in scope -> ATOM
        "constant.numeric" in scope -> NUMBER
        "constant.character.escape" in scope -> null // let TextMate handle escapes
        "constant.language" in scope -> ATOM
        "string.regexp" in scope -> SIGIL
        "string.interpolated.regexp" in scope -> SIGIL
        "keyword.control.module" in scope -> KEYWORD
        "comment.documentation" in scope -> COMMENT
        "comment" in scope -> COMMENT
        "punctuation.definition.variable" in scope -> MODULE_ATTRIBUTE
        else -> null
    }

    private fun key(name: String, fallback: TextAttributesKey) =
        TextAttributesKey.createTextAttributesKey(name, fallback)
}
