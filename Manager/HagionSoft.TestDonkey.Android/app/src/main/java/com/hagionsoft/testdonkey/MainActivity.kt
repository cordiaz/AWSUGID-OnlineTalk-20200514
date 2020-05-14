package com.hagionsoft.testdonkey

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.Auth0
import com.auth0.android.Auth0Exception
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.VoidCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var auth0: Auth0
    private lateinit var credentialsManager: SecureCredentialsManager

    companion object {
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_ID_TOKEN = "id_token"
        const val EXTRA_CLEAR_CREDENTIALS = "clear_cred"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth0 = Auth0(this)
        auth0.isOIDCConformant = true
        credentialsManager = SecureCredentialsManager(this,  AuthenticationAPIClient(auth0),  SharedPreferencesStorage(this))

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }

        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            login()
        }

        if (intent.getBooleanExtra(EXTRA_CLEAR_CREDENTIALS, false)) {
            logout()
        } else {
            if (credentialsManager.hasValidCredentials()) {
                // Obtain the existing credentials and move to the next activity
                showNextActivity()
            } else {
                login()
            }
        }
    }

    private fun login() {
        WebAuthProvider.login(auth0)
            .withScheme("demo")
            .withScope("openid offline_access")
            .withAudience(
                String.format(
                    "https://%s/userinfo",
                    getString(R.string.com_auth0_domain)
                )
            )
            .start(this@MainActivity, object : AuthCallback {
                override fun onFailure(dialog: Dialog) {
                    // Show error Dialog to user
                }

                override fun onFailure(exception: AuthenticationException?) {
                    // Show error to user
                }

                override fun onSuccess(credentials: Credentials) {
                    credentialsManager.saveCredentials(credentials)
                    showNextActivity()
                }
            })
    }

    private fun showNextActivity() {
        credentialsManager.getCredentials(object: BaseCallback<Credentials, CredentialsManagerException> {
            override fun onSuccess(payload: Credentials?) {
                payload?.let {
                    credentials ->

                    val intent = Intent(this@MainActivity, TopicActivity::class.java)
                    intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.accessToken)
                    intent.putExtra(EXTRA_ID_TOKEN, credentials.idToken)

                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(error: CredentialsManagerException?) {

            }

        })


    }

    private fun logout() {
        WebAuthProvider.logout(auth0)
            .withScheme("demo")
            .start(this, object : VoidCallback {
                override fun onSuccess(payload: Void?) {
                    credentialsManager.clearCredentials()
                }
                override fun onFailure(error: Auth0Exception) {
                    showNextActivity()
                }
            })
    }
}
