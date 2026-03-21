package com.thiromano.exact.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

class ExpertConfigurable : Configurable {

    private var panel: JPanel? = null
    private var expertPathField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "Exact: Elixir Support"

    override fun createComponent(): JComponent {
        expertPathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                "Select Expert Binary",
                "Path to the Expert language server executable",
                null,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
            )
        }

        val hint = JBLabel(
            "Path to the expert or start_expert executable. If left empty, expert will be resolved from PATH."
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = JBUI.Fonts.smallFont()
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Expert binary path:", expertPathField!!, 1, false)
            .addComponentToRightColumn(hint, 0)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = ExpertSettings.getInstance()
        return expertPathField?.text != (settings.expertPath ?: "")
    }

    override fun apply() {
        val settings = ExpertSettings.getInstance()
        settings.expertPath = expertPathField?.text?.takeIf { it.isNotBlank() }
    }

    override fun reset() {
        val settings = ExpertSettings.getInstance()
        expertPathField?.text = settings.expertPath ?: ""
    }

    override fun disposeUIResources() {
        panel = null
        expertPathField = null
    }
}
