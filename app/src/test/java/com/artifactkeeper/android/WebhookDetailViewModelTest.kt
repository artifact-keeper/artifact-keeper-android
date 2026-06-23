package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.integration.WebhookDetailUiState
import com.artifactkeeper.android.ui.screens.integration.WebhookDetailViewModel
import com.artifactkeeper.client.apis.WebhooksApi
import com.artifactkeeper.client.models.DeliveryListResponse
import com.artifactkeeper.client.models.DeliveryResponse
import com.artifactkeeper.client.models.PayloadTemplate
import com.artifactkeeper.client.models.RotateWebhookSecretResponse
import com.artifactkeeper.client.models.WebhookResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WebhookDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockWebhooksApi = mockk<WebhooksApi>()
    private val mockApiClient = mockk<ApiClient> {
        every { webhooksApi } returns mockWebhooksApi
    }

    private val webhookId = UUID.randomUUID()
    private val now: OffsetDateTime = OffsetDateTime.parse("2026-06-22T10:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun webhook() = WebhookResponse(
        createdAt = now,
        eventSchemaVersion = "v1",
        events = listOf("artifact_uploaded"),
        id = webhookId,
        isEnabled = true,
        name = "ci-hook",
        payloadTemplate = PayloadTemplate.generic,
        url = "https://example.test/hook",
    )

    private fun delivery(success: Boolean, id: UUID = UUID.randomUUID()) = DeliveryResponse(
        attempts = 1,
        createdAt = now,
        event = "artifact_uploaded",
        id = id,
        payload = emptyMap<String, Any>(),
        success = success,
        webhookId = webhookId,
        responseStatus = if (success) 200 else 500,
    )

    @Test
    fun `initial state is empty`() {
        val state = WebhookDetailUiState()
        assertNull(state.webhook)
        assertTrue(state.deliveries.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.newSecret)
    }

    @Test
    fun `load populates webhook and deliveries`() = runTest {
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns Response.success(webhook())
        coEvery { mockWebhooksApi.listDeliveries(webhookId, null, null, null) } returns Response.success(
            DeliveryListResponse(items = listOf(delivery(true), delivery(false)), total = 2),
        )

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.load(webhookId)

        val state = vm.uiState.value
        assertEquals(webhookId, state.webhook?.id)
        assertEquals(2, state.deliveries.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load tolerates missing deliveries`() = runTest {
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns Response.success(webhook())
        coEvery { mockWebhooksApi.listDeliveries(webhookId, null, null, null) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "none"))

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.load(webhookId)

        assertNotNull(vm.uiState.value.webhook)
        assertTrue(vm.uiState.value.deliveries.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load sets error when webhook fetch fails`() = runTest {
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "missing"))

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.load(webhookId)

        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.webhook)
    }

    @Test
    fun `redeliver calls api and reloads deliveries`() = runTest {
        val deliveryId = UUID.randomUUID()
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns Response.success(webhook())
        coEvery { mockWebhooksApi.listDeliveries(webhookId, null, null, null) } returns Response.success(
            DeliveryListResponse(items = listOf(delivery(true, deliveryId)), total = 1),
        )
        coEvery { mockWebhooksApi.redeliver(webhookId, deliveryId) } returns Response.success(delivery(true, deliveryId))

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.load(webhookId)
        vm.redeliver(webhookId, deliveryId)

        coVerify { mockWebhooksApi.redeliver(webhookId, deliveryId) }
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `redeliver sets error on failure`() = runTest {
        val deliveryId = UUID.randomUUID()
        coEvery { mockWebhooksApi.redeliver(webhookId, deliveryId) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, "boom"))
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns Response.success(webhook())
        coEvery { mockWebhooksApi.listDeliveries(webhookId, null, null, null) } returns Response.success(
            DeliveryListResponse(items = emptyList(), total = 0),
        )

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.redeliver(webhookId, deliveryId)

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `rotateSecret surfaces the new secret`() = runTest {
        coEvery { mockWebhooksApi.getWebhook(webhookId) } returns Response.success(webhook())
        coEvery { mockWebhooksApi.listDeliveries(webhookId, null, null, null) } returns Response.success(
            DeliveryListResponse(items = emptyList(), total = 0),
        )
        coEvery { mockWebhooksApi.rotateWebhookSecret(webhookId) } returns Response.success(
            RotateWebhookSecretResponse(
                id = webhookId,
                previousSecretExpiresAt = now,
                secret = "new-secret-value",
                secretDigest = "digest",
            ),
        )

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.rotateSecret(webhookId)

        assertEquals("new-secret-value", vm.uiState.value.newSecret)
        coVerify { mockWebhooksApi.rotateWebhookSecret(webhookId) }
    }

    @Test
    fun `rotateSecret sets error on failure`() = runTest {
        coEvery { mockWebhooksApi.rotateWebhookSecret(webhookId) } returns
            Response.error(409, okhttp3.ResponseBody.create(null, "conflict"))

        val vm = WebhookDetailViewModel(mockApiClient)
        vm.rotateSecret(webhookId)

        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.newSecret)
    }
}
