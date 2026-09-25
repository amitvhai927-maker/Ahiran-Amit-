package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.JarvisIntentParser
import com.example.data.model.DeviceActionType
import com.example.data.model.JarvisAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jarvis AI", appName)
  }

  @Test
  fun `test call intent parsing`() {
    val result = JarvisIntentParser.parseCommand("call 9876543210")
    assertNotNull(result)
    assertTrue(result!!.needsConfirmation)
    assertTrue(result.action is JarvisAction.CallAction)
    assertEquals("9876543210", (result.action as JarvisAction.CallAction).phoneNumber)
  }

  @Test
  fun `test whatsapp intent parsing`() {
    val result = JarvisIntentParser.parseCommand("send whatsapp message to 9876543210 saying meeting at 5")
    assertNotNull(result)
    assertTrue(result!!.needsConfirmation)
    assertTrue(result.action is JarvisAction.WhatsAppAction)
    assertEquals("9876543210", (result.action as JarvisAction.WhatsAppAction).phoneNumber)
  }

  @Test
  fun `test upi payment parsing`() {
    val result = JarvisIntentParser.parseCommand("pay 500 to rahul@oksbi")
    assertNotNull(result)
    assertTrue(result!!.needsConfirmation)
    assertTrue(result.action is JarvisAction.PaymentAction)
    val action = result.action as JarvisAction.PaymentAction
    assertEquals(500.0, action.amount, 0.01)
    assertEquals("rahul@oksbi", action.upiId)
  }

  @Test
  fun `test torch command parsing`() {
    val onResult = JarvisIntentParser.parseCommand("torch on")
    assertNotNull(onResult)
    assertTrue(onResult!!.action is JarvisAction.DeviceAction)
    assertEquals(DeviceActionType.TORCH_ON, (onResult.action as JarvisAction.DeviceAction).type)
  }

  @Test
  fun `test persona switch to maya parsing`() {
    val result = JarvisIntentParser.parseCommand("switch to maya")
    assertNotNull(result)
    assertTrue(result!!.action is JarvisAction.PersonaSwitchAction)
    assertEquals(com.example.data.model.AiPersona.MAYA, (result.action as JarvisAction.PersonaSwitchAction).newPersona)
  }

  @Test
  fun `test note creation parsing`() {
    val result = JarvisIntentParser.parseCommand("note banao: buy milk and bread")
    assertNotNull(result)
    assertTrue(result!!.action is JarvisAction.NoteAction)
    assertEquals("buy milk and bread", (result.action as JarvisAction.NoteAction).noteText)
  }

  @Test
  fun `test app launch parsing`() {
    val result = JarvisIntentParser.parseCommand("open youtube")
    assertNotNull(result)
    assertTrue(result!!.action is JarvisAction.AppLaunch)
    assertEquals("youtube", (result.action as JarvisAction.AppLaunch).appName.lowercase())
  }
}

