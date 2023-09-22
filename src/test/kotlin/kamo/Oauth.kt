package kamo

import io.kotest.matchers.shouldBe
import kamo.bridge.auth.DeviceCodeFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

object Oauth {
    @Test
    fun testAuthorizeDevice() = runTest {
        val data = DeviceCodeFlow.authorizeDevice()
        data.message shouldBe "To sign in, use a web browser to open the page ${data.verification_uri} and enter the code ${data.user_code} to authenticate."
        println(data)
    }

    @Test
    fun testUserAuth() = runBlocking {
        val data = DeviceCodeFlow.authorizeDevice()
        println(data.message)
        val authData = DeviceCodeFlow.pollUserAuth(data)
        authData.token_type shouldBe "Bearer"
        println(authData)
    }
}