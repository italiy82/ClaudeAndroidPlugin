package com.claudeplugin.api

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object ApiKeyStorage {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("ClaudeAndroidPlugin", "anthropic-api-key")
    )

    fun saveKey(apiKey: String) {
        val credentials = Credentials("claude-plugin", apiKey)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun loadKey(): String {
        return PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
    }

    fun clearKey() {
        PasswordSafe.instance.set(credentialAttributes, null)
    }
}
