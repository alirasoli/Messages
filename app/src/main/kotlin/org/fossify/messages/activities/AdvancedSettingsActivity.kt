package org.fossify.messages.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.value
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityAdvancedSettingsBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.extractVerificationCode
import org.fossify.messages.helpers.DEFAULT_CODE_REGEX

class AdvancedSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityAdvancedSettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.advancedSettingsNestedScrollview))
        setupMaterialScrollListener(
            scrollingView = binding.advancedSettingsNestedScrollview,
            topAppBar = binding.advancedSettingsAppbar
        )
        setupCodeRegex()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.advancedSettingsAppbar, NavigationIcon.Arrow)
        updateTextColors(binding.advancedSettingsHolder)
        binding.codeRegexReset.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupCodeRegex() = binding.apply {
        codeRegexEdittext.setText(config.codeRegex)
        codeRegexEdittext.addTextChangedListener(simpleTextWatcher { onRegexChanged() })
        codeRegexTestEdittext.addTextChangedListener(simpleTextWatcher { updateTestResult() })
        codeRegexReset.setOnClickListener {
            codeRegexEdittext.setText(DEFAULT_CODE_REGEX)
        }
        updateTestResult()
    }

    private fun onRegexChanged() = binding.apply {
        val pattern = codeRegexEdittext.value
        if (pattern.isNotBlank() && parseRegex(pattern) != null) {
            codeRegexLayout.error = null
            config.codeRegex = pattern
        } else {
            codeRegexLayout.error = getString(R.string.invalid_regex)
        }
        updateTestResult()
    }

    private fun updateTestResult() = binding.apply {
        val regex = parseRegex(codeRegexEdittext.value)
        val message = codeRegexTestEdittext.value
        codeRegexTestResult.text = when {
            regex == null || message.isEmpty() -> ""
            else -> message.extractVerificationCode(regex)
                ?.let { getString(R.string.regex_test_found, it) }
                ?: getString(R.string.regex_test_none)
        }
    }

    private fun parseRegex(pattern: String): Regex? {
        if (pattern.isBlank()) return null
        return try {
            Regex(pattern)
        } catch (_: Exception) {
            null
        }
    }

    private fun simpleTextWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = onChanged()
    }
}
