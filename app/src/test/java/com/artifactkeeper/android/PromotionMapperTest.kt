package com.artifactkeeper.android

import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.ui.screens.staging.PromotionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Regression coverage for the 1.2.1 staging-to-promotion routing. These tests
 * pin the field mapping between the app's local staging models and the SDK's
 * promotion models (under api/v1/promotion) so the UI contract stays stable.
 */
class PromotionMapperTest {

    @Test
    fun `toSdkPromoteRequest maps force to skip policy check and comment to notes`() {
        val local = PromoteArtifactRequest(
            targetRepositoryKey = "maven-releases",
            force = true,
            comment = "ship it",
        )

        val sdk = PromotionMapper.toSdkPromoteRequest(local)

        assertEquals("maven-releases", sdk.targetRepository)
        assertEquals(true, sdk.skipPolicyCheck)
        assertEquals("ship it", sdk.notes)
    }

    @Test
    fun `toSdkBulkRequest converts string ids to uuids and drops invalid ids`() {
        val good = UUID.randomUUID().toString()
        val local = BulkPromoteRequest(
            artifactIds = listOf(good, "not-a-uuid"),
            targetRepositoryKey = "npm-releases",
            force = false,
            comment = null,
        )

        val sdk = PromotionMapper.toSdkBulkRequest(local)

        assertEquals(1, sdk.artifactIds.size)
        assertEquals(UUID.fromString(good), sdk.artifactIds.first())
        assertEquals("npm-releases", sdk.targetRepository)
        assertEquals(false, sdk.skipPolicyCheck)
        assertNull(sdk.notes)
    }

    @Test
    fun `toLocalPromotionResponse carries over promoted flag target and message`() {
        val sdk = com.artifactkeeper.client.models.PromotionResponse(
            policyViolations = emptyList(),
            promoted = true,
            source = "maven-staging",
            target = "maven-releases",
            message = "Promoted",
            promotionId = UUID.randomUUID(),
        )

        val local = PromotionMapper.toLocalPromotionResponse(sdk, artifactId = "art-1")

        assertTrue(local.success)
        assertEquals("Promoted", local.message)
        assertEquals("art-1", local.artifactId)
        assertEquals("maven-releases", local.targetRepositoryKey)
    }

    @Test
    fun `toLocalPromotionResponse synthesizes a message when the SDK omits one`() {
        val sdk = com.artifactkeeper.client.models.PromotionResponse(
            policyViolations = emptyList(),
            promoted = true,
            source = "maven-staging",
            target = "maven-releases",
            message = null,
            promotionId = null,
        )

        val local = PromotionMapper.toLocalPromotionResponse(sdk, artifactId = "art-1")

        assertEquals("Promoted to maven-releases", local.message)
    }

    @Test
    fun `toLocalBulkResponse maps counts and per-artifact results`() {
        val sdk = com.artifactkeeper.client.models.BulkPromotionResponse(
            failed = 1,
            promoted = 2,
            total = 3,
            results = listOf(
                com.artifactkeeper.client.models.PromotionResponse(
                    policyViolations = emptyList(),
                    promoted = true,
                    source = "s",
                    target = "art-a",
                    message = "ok",
                ),
                com.artifactkeeper.client.models.PromotionResponse(
                    policyViolations = emptyList(),
                    promoted = false,
                    source = "s",
                    target = "art-b",
                    message = "blocked",
                ),
            ),
        )

        val local = PromotionMapper.toLocalBulkResponse(sdk)

        assertEquals(3, local.totalRequested)
        assertEquals(2, local.totalSucceeded)
        assertEquals(1, local.totalFailed)
        assertEquals(2, local.results.size)
        assertTrue(local.results[0].success)
        assertNull(local.results[0].error)
        assertFalse(local.results[1].success)
        assertEquals("blocked", local.results[1].error)
    }

    @Test
    fun `toLocalHistoryEntry derives name from path and maps repo keys`() {
        val artifactId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val promotedBy = UUID.randomUUID()
        val sdk = com.artifactkeeper.client.models.PromotionHistoryEntry(
            artifactId = artifactId,
            artifactPath = "com/example/app/1.2.3/app-1.2.3.jar",
            createdAt = OffsetDateTime.parse("2026-01-15T10:30:00Z"),
            id = id,
            sourceRepoKey = "maven-staging",
            status = "promoted",
            targetRepoKey = "maven-releases",
            notes = "release cut",
            promotedBy = promotedBy,
            promotedByUsername = "brandon",
        )

        val local = PromotionMapper.toLocalHistoryEntry(sdk)

        assertEquals(id.toString(), local.id)
        assertEquals(artifactId.toString(), local.artifactId)
        assertEquals("app-1.2.3.jar", local.artifactName)
        assertNull(local.artifactVersion)
        assertEquals("maven-staging", local.sourceRepositoryKey)
        assertEquals("maven-releases", local.targetRepositoryKey)
        assertEquals(promotedBy.toString(), local.promotedBy)
        assertEquals("brandon", local.promotedByUsername)
        assertEquals("release cut", local.comment)
        assertFalse(local.forced)
        assertEquals("2026-01-15T10:30Z", local.promotedAt)
    }
}
