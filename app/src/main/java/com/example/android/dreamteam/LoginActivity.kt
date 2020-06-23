package com.example.android.dreamteam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var callbackManager: CallbackManager
    val RET: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setGoogleButton()
        setFacebookButton()
        auth = FirebaseAuth.getInstance()
    }

    //check: if user already authenticated, go ahead!
    override fun onStart(){
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    //catch the result of the Sign-In Activity and process with FireBase
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RET) { //it's Google Auth.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if(account!=null){
                    Log.w("GOOGLESIGNIN", "successfully retrieved account")
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "UNAUTHENTICATED: Google Login Failed!", Toast.LENGTH_LONG)
                    .show()
                Log.w("GOOGLESIGNIN", "signInResult:failed code=" + e.getStatusCode());
            }
        } else{ //it's Facebook Auth
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    //+++++++++++++++++++++++++++BEGIN: FACEBOOK SIGN-IN SETUP+++++++++++++++++++++++++++

    //initialize Facebook login button
    private fun setFacebookButton(){
        callbackManager = CallbackManager.Factory.create()
        facebook_button.setReadPermissions("email", "public_profile")
        facebook_button.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                //if Login was successful, proceed with FireBase Login
                firebaseAuthWithFacebook(loginResult.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(
                    baseContext, "Facebook Authentication cancelled (onCancel).",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(
                    baseContext, "facebook Authentication failed (onError).",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in successful
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Sign in failed
                    Toast.makeText(
                        baseContext, "FireBase Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    //+++++++++++++++++++++++++++END: FACEBOOK SIGN-IN SETUP+++++++++++++++++++++++++++

    //+++++++++++++++++++++++++++BEGIN: GOOGLE SIGN-IN SETUP+++++++++++++++++++++++++++
    //prepare the Google Sign-In phase to get the token from Google
    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    //set listener on the Google Sign-In button
    private fun setGoogleButton(){
        configureGoogleSignIn()
        sign_in_button.setOnClickListener{
            signIn()
        }
    }
    //if button is pressed, start activity of signing in
    private fun signIn(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RET)
    }


    //if Google Sign-In auth was successful, get the instance of current user from FireBase and launch HomeActivity
    private fun firebaseAuthWithGoogle(acc: GoogleSignInAccount){
        val credential = GoogleAuthProvider.getCredential(acc.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                //authenticated, continue with navigation!!!
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "UNAUTHENTICATED: Firebase Login Failed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    //+++++++++++++++++++++++++++END: GOOGLE SIGN-IN SETUP+++++++++++++++++++++++++++

}