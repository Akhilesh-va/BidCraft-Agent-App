package com.example.bidcarftagent.presentation.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext ctx: Context): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
        try {
            val clientIdResId = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
            if (clientIdResId != 0) {
                gsoBuilder.requestIdToken(ctx.getString(clientIdResId))
            }
        } catch (e: Exception) {
            // Ignore
        }
        val gso = gsoBuilder.build()
        return GoogleSignIn.getClient(ctx, gso)
    }
}

