package com.thiromano.exact.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class ExpertConfigurable : Configurable {

    private var panel: JPanel? = null
    private var expertPathField: TextFieldWithBrowseButton? = null
    private var minQueryLengthSpinner: JSpinner? = null

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

        val pathHint = JBLabel(
            "Path to the expert or start_expert executable. If left empty, expert will be resolved from PATH."
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = JBUI.Fonts.smallFont()
        }

        minQueryLengthSpinner = JSpinner(
            SpinnerNumberModel(ExpertSettings.DEFAULT_MIN_QUERY_LENGTH, 1, 10, 1)
        )

        val queryHint = JBLabel(
            "Minimum number of characters before workspace symbol search returns results."
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = JBUI.Fonts.smallFont()
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Expert binary path:", expertPathField!!, 1, false)
            .addComponentToRightColumn(pathHint, 0)
            .addLabeledComponent("Min symbol query length:", minQueryLengthSpinner!!, 1, false)
            .addComponentToRightColumn(queryHint, 0)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = ExpertSettings.getInstance()
        return expertPathField?.text != (settings.expertPath ?: "") ||
            minQueryLengthSpinner?.value != settings.minQueryLength
    }

    override fun apply() {
        val settings = ExpertSettings.getInstance()
        settings.expertPath = expertPathField?.text?.takeIf { it.isNotBlank() }
        settings.minQueryLength = minQueryLengthSpinner?.value as? Int ?: ExpertSettings.DEFAULT_MIN_QUERY_LENGTH
    }

    override fun reset() {
        val settings = ExpertSettings.getInstance()
        expertPathField?.text = settings.expertPath ?: ""
        minQueryLengthSpinner?.value = settings.minQueryLength
    }

    override fun disposeUIResources() {
        panel = null
        expertPathField = null
        minQueryLengthSpinner = null
    }
}
