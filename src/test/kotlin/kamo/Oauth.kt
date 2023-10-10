package kamo

import io.kotest.matchers.shouldBe
import kamo.bridge.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

object Oauth {
    @Test
    fun testAuthorizeDevice() = runTest {
        val data = authorizeDevice()
        data.message shouldBe "To sign in, use a web browser to open the page ${data.verification_uri} and enter the code ${data.user_code} to authenticate."
        println(data)
    }

    @Test
    fun testUserAuth() = runBlocking {
        val data = authorizeDevice()
        println(data.message)
        val authData = pollUserAuth(data)
        authData.token_type shouldBe "Bearer"
        println(authData)
    }

    @Test
    fun testXboxLiveAuth() = runBlocking {
        val data = authorizeDevice()
        println(data.message)
        val authData = pollUserAuth(data)
        println(authData)
        val xblData = obtainXBLToken(authData.access_token)
        println(xblData)
    }

    @Test
    fun testXsts() = runBlocking {
        val data = authorizeDevice()
        println(data.message)
        val authData = pollUserAuth(data)
        println(authData)
        val xblData = obtainXBLToken(authData.access_token)
        println(xblData)
        val xstsData = obtainXSTSToken(xblData)
        println(xstsData)
    }

    @Test
    fun testMc() = runBlocking {
        val data = authorizeDevice()
        println(data.message)
        val authData = pollUserAuth(data)
        println(authData)
        val xblData = obtainXBLToken(authData.access_token)
        println(xblData)
        val xstsData = obtainXSTSToken(xblData)
        println(xstsData)
        val mcdata = obtainMCToken(xstsData)
        println(mcdata)
    }
}