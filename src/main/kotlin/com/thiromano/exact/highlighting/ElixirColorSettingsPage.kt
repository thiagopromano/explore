package com.thiromano.exact.highlighting

import com.thiromano.exact.ElixirIcons
import com.intellij.lexer.EmptyLexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.psi.tree.IElementType
import javax.swing.Icon

class ElixirColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Elixir"

    override fun getIcon(): Icon = ElixirIcons.ELIXIR

    // Minimal highlighter for demo text only — actual highlighting is provided by TextMate
    override fun getHighlighter(): SyntaxHighlighter = object : SyntaxHighlighterBase() {
        override fun getHighlightingLexer() = EmptyLexer()
        override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = emptyArray()
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        TAG_DESCRIPTOR_MAP

    override fun getDemoText(): String = DEMO_TEXT

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Module name", ElixirTextAttributes.MODULE_NAME),
            AttributesDescriptor("Function declaration", ElixirTextAttributes.FUNCTION_DECLARATION),
            AttributesDescriptor("Function call", ElixirTextAttributes.FUNCTION_CALL),
            AttributesDescriptor("Atom", ElixirTextAttributes.ATOM),
            AttributesDescriptor("String", ElixirTextAttributes.STRING),
            AttributesDescriptor("Number", ElixirTextAttributes.NUMBER),
            AttributesDescriptor("Keyword", ElixirTextAttributes.KEYWORD),
            AttributesDescriptor("Comment", ElixirTextAttributes.COMMENT),
            AttributesDescriptor("Sigil", ElixirTextAttributes.SIGIL),
            AttributesDescriptor("Module attribute", ElixirTextAttributes.MODULE_ATTRIBUTE),
            AttributesDescriptor("Variable", ElixirTextAttributes.VARIABLE),
        )

        private val TAG_DESCRIPTOR_MAP = mapOf(
            "module" to ElixirTextAttributes.MODULE_NAME,
            "func_decl" to ElixirTextAttributes.FUNCTION_DECLARATION,
            "func_call" to ElixirTextAttributes.FUNCTION_CALL,
            "atom" to ElixirTextAttributes.ATOM,
            "string" to ElixirTextAttributes.STRING,
            "number" to ElixirTextAttributes.NUMBER,
            "keyword" to ElixirTextAttributes.KEYWORD,
            "comment" to ElixirTextAttributes.COMMENT,
            "sigil" to ElixirTextAttributes.SIGIL,
            "mod_attr" to ElixirTextAttributes.MODULE_ATTRIBUTE,
            "variable" to ElixirTextAttributes.VARIABLE,
        )

        private val DEMO_TEXT = """
<keyword>defmodule</keyword> <module>MyApp.UserController</module> <keyword>do</keyword>
  <keyword>use</keyword> <module>MyAppWeb</module>, <atom>:controller</atom>

  <mod_attr>@moduledoc</mod_attr> <string>"Handles user-related requests."</string>

  <keyword>alias</keyword> <module>MyApp.Accounts</module>
  <keyword>alias</keyword> <module>MyApp.Accounts.User</module>

  <mod_attr>@max_attempts</mod_attr> <number>3</number>

  <comment># Returns a list of users</comment>
  <keyword>def</keyword> <func_decl>index</func_decl>(<variable>conn</variable>, <variable>_params</variable>) <keyword>do</keyword>
    <variable>users</variable> = <module>Accounts</module>.<func_call>list_users</func_call>()
    <func_call>render</func_call>(<variable>conn</variable>, <atom>:index</atom>, <atom>users:</atom> <variable>users</variable>)
  <keyword>end</keyword>

  <keyword>def</keyword> <func_decl>show</func_decl>(<variable>conn</variable>, %{<string>"id"</string> => <variable>id</variable>}) <keyword>do</keyword>
    <keyword>case</keyword> <module>Accounts</module>.<func_call>get_user</func_call>(<variable>id</variable>) <keyword>do</keyword>
      {<atom>:ok</atom>, <variable>user</variable>} ->
        <func_call>render</func_call>(<variable>conn</variable>, <atom>:show</atom>, <atom>user:</atom> <variable>user</variable>)

      {<atom>:error</atom>, <atom>:not_found</atom>} ->
        <variable>conn</variable>
        |> <func_call>put_status</func_call>(<number>404</number>)
        |> <func_call>json</func_call>(%{<atom>error:</atom> <string>"User not found"</string>})
    <keyword>end</keyword>
  <keyword>end</keyword>

  <keyword>defp</keyword> <func_decl>authorize</func_decl>(<variable>conn</variable>, <variable>_opts</variable>) <keyword>do</keyword>
    <variable>token</variable> = <func_call>get_req_header</func_call>(<variable>conn</variable>, <string>"authorization"</string>)
    <variable>regex</variable> = <sigil>~r/Bearer\s+(.+)/</sigil>
    <module>Logger</module>.<func_call>info</func_call>(<string>"Authorizing request"</string>)
    <variable>conn</variable>
  <keyword>end</keyword>
<keyword>end</keyword>
""".trimIndent()
    }
}
