package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("نبرد پادشاهان", appName)
  }

  @Test
  fun `test asset loader intercepts index html`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    val response = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/index.html"))
    org.junit.Assert.assertNotNull("Index HTML response should not be null", response)
    val content = response?.data?.bufferedReader()?.use { it.readText() } ?: ""
    println("Response content length: ${content.length}")
    org.junit.Assert.assertTrue("Content should contain root", content.contains("root"))

    val jsResponse = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/assets/index-WA3TFXYh.js"))
    println("JS response: $jsResponse, mimeType: ${jsResponse?.mimeType}")
    org.junit.Assert.assertNotNull("JS response should not be null", jsResponse)

    val cssResponse = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/assets/index-C-lJ72WD.css"))
    println("CSS response: $cssResponse, mimeType: ${cssResponse?.mimeType}")
    org.junit.Assert.assertNotNull("CSS response should not be null", cssResponse)

    val imgResponse = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/assets/rostam_hero_1791041080508-hArU2IiH.jpg"))
    println("Img response: $imgResponse, mimeType: ${imgResponse?.mimeType}")
    org.junit.Assert.assertNotNull("Image response should not be null", imgResponse)

    val videoResponse = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/assets/card_video_fire.mp4"))
    println("Video response: $videoResponse, mimeType: ${videoResponse?.mimeType}")
    org.junit.Assert.assertNotNull("Card video response should not be null", videoResponse)

    val ghSyncResponse = assetLoader.shouldInterceptRequest(android.net.Uri.parse("https://appassets.androidplatform.net/assets/assets/github-cloud-sync.js"))
    println("GitHub sync response: $ghSyncResponse")
    org.junit.Assert.assertNotNull("GitHub sync script should not be null", ghSyncResponse)
  }

  @Test
  fun `test github storage service sha256`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = GitHubStorageService(context)
    val hash = service.calculateSha256("test_data_string")
    org.junit.Assert.assertTrue("Hash should not be empty", hash.isNotEmpty())
    org.junit.Assert.assertEquals("Hash length should be 64 for sha256", 64, hash.length)
  }
}
