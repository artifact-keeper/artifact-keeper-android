package com.artifactkeeper.android

import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.BulkPromotionResponse
import com.artifactkeeper.android.data.models.BulkPromotionResult
import com.artifactkeeper.android.data.models.CveSummary
import com.artifactkeeper.android.data.models.HealthCheck
import com.artifactkeeper.android.data.models.LicenseSummary
import com.artifactkeeper.android.data.models.LocalHealthResponse
import com.artifactkeeper.android.data.models.PolicyStatus
import com.artifactkeeper.android.data.models.PolicyViolation
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.data.models.PromotionHistoryEntry
import com.artifactkeeper.android.data.models.PromotionResponse
import com.artifactkeeper.android.data.models.RepositoryType
import com.artifactkeeper.android.data.models.SavedServer
import com.artifactkeeper.android.data.models.StagingArtifact
import com.artifactkeeper.android.data.models.StagingRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    // =========================================================================
    // SavedServer serialization
    // =========================================================================

    @Test
    fun `SavedServer round-trip serialization`() {
        val server = SavedServer(
            id = "abc-123",
            name = "Production",
            url = "https://registry.example.com/",
            addedAt = 1700000000000L
        )
        val encoded = json.encodeToString(server)
        val decoded = json.decodeFromString<SavedServer>(encoded)
        assertEquals(server, decoded)
    }

    @Test
    fun `SavedServer deserialization from JSON string`() {
        val jsonStr = """
            {"id":"s1","name":"Dev","url":"http://localhost:8080/","addedAt":1234567890}
        """.trimIndent()
        val server = json.decodeFromString<SavedServer>(jsonStr)
        assertEquals("s1", server.id)
        assertEquals("Dev", server.name)
        assertEquals("http://localhost:8080/", server.url)
        assertEquals(1234567890L, server.addedAt)
    }

    // =========================================================================
    // RepositoryType enum
    // =========================================================================

    @Test
    fun `RepositoryType has all expected values`() {
        val values = RepositoryType.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(RepositoryType.LOCAL))
        assertTrue(values.contains(RepositoryType.REMOTE))
        assertTrue(values.contains(RepositoryType.VIRTUAL))
        assertTrue(values.contains(RepositoryType.STAGING))
    }

    @Test
    fun `RepositoryType fromString is case-insensitive`() {
        assertEquals(RepositoryType.LOCAL, RepositoryType.fromString("local"))
        assertEquals(RepositoryType.LOCAL, RepositoryType.fromString("LOCAL"))
        assertEquals(RepositoryType.LOCAL, RepositoryType.fromString("Local"))
        assertEquals(RepositoryType.REMOTE, RepositoryType.fromString("remote"))
        assertEquals(RepositoryType.VIRTUAL, RepositoryType.fromString("virtual"))
        assertEquals(RepositoryType.STAGING, RepositoryType.fromString("staging"))
    }

    @Test
    fun `RepositoryType fromString defaults to LOCAL for unknown values`() {
        assertEquals(RepositoryType.LOCAL, RepositoryType.fromString("unknown"))
        assertEquals(RepositoryType.LOCAL, RepositoryType.fromString(""))
    }

    // =========================================================================
    // PolicyStatus enum
    // =========================================================================

    @Test
    fun `PolicyStatus has all expected values`() {
        val values = PolicyStatus.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(PolicyStatus.PASSING))
        assertTrue(values.contains(PolicyStatus.FAILING))
        assertTrue(values.contains(PolicyStatus.WARNING))
        assertTrue(values.contains(PolicyStatus.PENDING))
    }

    @Test
    fun `PolicyStatus fromString is case-insensitive`() {
        assertEquals(PolicyStatus.PASSING, PolicyStatus.fromString("passing"))
        assertEquals(PolicyStatus.PASSING, PolicyStatus.fromString("PASSING"))
        assertEquals(PolicyStatus.FAILING, PolicyStatus.fromString("Failing"))
        assertEquals(PolicyStatus.WARNING, PolicyStatus.fromString("warning"))
        assertEquals(PolicyStatus.PENDING, PolicyStatus.fromString("pending"))
    }

    @Test
    fun `PolicyStatus fromString defaults to PENDING for unknown values`() {
        assertEquals(PolicyStatus.PENDING, PolicyStatus.fromString("unknown"))
        assertEquals(PolicyStatus.PENDING, PolicyStatus.fromString(""))
    }

    // =========================================================================
    // StagingArtifact defaults and serialization
    // =========================================================================

    @Test
    fun `StagingArtifact has correct default values`() {
        val artifact = StagingArtifact(
            id = "a1",
            name = "my-lib",
            path = "/com/example/my-lib/1.0/my-lib-1.0.jar",
            createdAt = "2024-01-01T00:00:00Z"
        )
        assertNull(artifact.repositoryKey)
        assertNull(artifact.version)
        assertNull(artifact.contentType)
        assertEquals(0L, artifact.sizeBytes)
        assertNull(artifact.checksumSha256)
        assertEquals("pending", artifact.policyStatus)
        assertNull(artifact.cveSummary)
        assertNull(artifact.licenseSummary)
        assertTrue(artifact.policyViolations.isEmpty())
        assertTrue(artifact.canPromote)
        assertNull(artifact.updatedAt)
    }

    @Test
    fun `StagingArtifact full round-trip serialization`() {
        val artifact = StagingArtifact(
            id = "art-1",
            repositoryKey = "staging-maven",
            name = "my-lib",
            path = "/com/example/my-lib/1.0/my-lib-1.0.jar",
            version = "1.0.0",
            contentType = "application/java-archive",
            sizeBytes = 1024000,
            checksumSha256 = "abc123def456",
            policyStatus = "passing",
            cveSummary = CveSummary(total = 3, criticalCount = 1, highCount = 2),
            licenseSummary = LicenseSummary(total = 5, allowedCount = 4, deniedCount = 1, licenses = listOf("MIT", "GPL-3.0")),
            policyViolations = listOf(
                PolicyViolation(
                    id = "v1",
                    policyId = "p1",
                    policyName = "No GPL",
                    severity = "high",
                    message = "GPL license detected"
                )
            ),
            canPromote = false,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-02T00:00:00Z"
        )
        val encoded = json.encodeToString(artifact)
        val decoded = json.decodeFromString<StagingArtifact>(encoded)
        assertEquals(artifact, decoded)
    }

    @Test
    fun `StagingArtifact deserialization with snake_case JSON`() {
        val jsonStr = """
            {
                "id": "a2",
                "repository_key": "staging-npm",
                "name": "express",
                "path": "/express/-/express-4.18.2.tgz",
                "size_bytes": 2048,
                "policy_status": "failing",
                "can_promote": false,
                "created_at": "2024-06-15T12:00:00Z"
            }
        """.trimIndent()
        val artifact = json.decodeFromString<StagingArtifact>(jsonStr)
        assertEquals("a2", artifact.id)
        assertEquals("staging-npm", artifact.repositoryKey)
        assertEquals(2048L, artifact.sizeBytes)
        assertEquals("failing", artifact.policyStatus)
        assertFalse(artifact.canPromote)
    }

    // =========================================================================
    // StagingRepository defaults and serialization
    // =========================================================================

    @Test
    fun `StagingRepository has correct default values`() {
        val repo = StagingRepository(
            id = "r1",
            key = "staging-maven",
            name = "Staging Maven",
            format = "maven",
            createdAt = "2024-01-01T00:00:00Z"
        )
        assertEquals("staging", repo.repoType)
        assertFalse(repo.isPublic)
        assertNull(repo.description)
        assertEquals(0L, repo.storageUsedBytes)
        assertEquals(0, repo.artifactCount)
        assertEquals(0, repo.pendingCount)
        assertEquals(0, repo.passingCount)
        assertEquals(0, repo.failingCount)
        assertNull(repo.targetRepositoryKey)
        assertNull(repo.updatedAt)
    }

    @Test
    fun `StagingRepository round-trip serialization`() {
        val repo = StagingRepository(
            id = "r2",
            key = "staging-npm",
            name = "Staging NPM",
            format = "npm",
            repoType = "staging",
            isPublic = false,
            description = "NPM staging area",
            storageUsedBytes = 50_000_000,
            artifactCount = 120,
            pendingCount = 10,
            passingCount = 100,
            failingCount = 10,
            targetRepositoryKey = "npm-releases",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-06-15T00:00:00Z"
        )
        val encoded = json.encodeToString(repo)
        val decoded = json.decodeFromString<StagingRepository>(encoded)
        assertEquals(repo, decoded)
    }

    // =========================================================================
    // PromotionHistoryEntry serialization
    // =========================================================================

    @Test
    fun `PromotionHistoryEntry has correct defaults`() {
        val entry = PromotionHistoryEntry(
            id = "ph1",
            artifactId = "a1",
            sourceRepositoryKey = "staging-maven",
            targetRepositoryKey = "maven-releases",
            promotedAt = "2024-06-15T12:00:00Z"
        )
        assertNull(entry.artifactName)
        assertNull(entry.artifactVersion)
        assertNull(entry.promotedBy)
        assertNull(entry.promotedByUsername)
        assertNull(entry.comment)
        assertFalse(entry.forced)
    }

    @Test
    fun `PromotionHistoryEntry round-trip serialization`() {
        val entry = PromotionHistoryEntry(
            id = "ph2",
            artifactId = "a2",
            artifactName = "react",
            artifactVersion = "18.2.0",
            sourceRepositoryKey = "staging-npm",
            targetRepositoryKey = "npm-releases",
            promotedBy = "user-123",
            promotedByUsername = "admin",
            comment = "Approved by QA",
            forced = true,
            promotedAt = "2024-06-15T12:00:00Z"
        )
        val encoded = json.encodeToString(entry)
        val decoded = json.decodeFromString<PromotionHistoryEntry>(encoded)
        assertEquals(entry, decoded)
    }

    // =========================================================================
    // CveSummary defaults
    // =========================================================================

    @Test
    fun `CveSummary has zero defaults`() {
        val summary = CveSummary()
        assertEquals(0, summary.total)
        assertEquals(0, summary.criticalCount)
        assertEquals(0, summary.highCount)
        assertEquals(0, summary.mediumCount)
        assertEquals(0, summary.lowCount)
    }

    // =========================================================================
    // LicenseSummary defaults
    // =========================================================================

    @Test
    fun `LicenseSummary has zero defaults and empty licenses`() {
        val summary = LicenseSummary()
        assertEquals(0, summary.total)
        assertEquals(0, summary.allowedCount)
        assertEquals(0, summary.deniedCount)
        assertEquals(0, summary.unknownCount)
        assertTrue(summary.licenses.isEmpty())
    }

    // =========================================================================
    // PromoteArtifactRequest serialization
    // =========================================================================

    @Test
    fun `PromoteArtifactRequest defaults and serialization`() {
        val request = PromoteArtifactRequest(targetRepositoryKey = "maven-releases")
        assertFalse(request.force)
        assertNull(request.comment)

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<PromoteArtifactRequest>(encoded)
        assertEquals(request, decoded)
    }

    // =========================================================================
    // BulkPromoteRequest and BulkPromotionResponse serialization
    // =========================================================================

    @Test
    fun `BulkPromoteRequest round-trip serialization`() {
        val request = BulkPromoteRequest(
            artifactIds = listOf("a1", "a2", "a3"),
            targetRepositoryKey = "npm-releases",
            force = true,
            comment = "Batch promotion"
        )
        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<BulkPromoteRequest>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `BulkPromotionResponse round-trip serialization`() {
        val response = BulkPromotionResponse(
            totalRequested = 3,
            totalSucceeded = 2,
            totalFailed = 1,
            results = listOf(
                BulkPromotionResult(artifactId = "a1", success = true, message = "ok"),
                BulkPromotionResult(artifactId = "a2", success = true, message = "ok"),
                BulkPromotionResult(artifactId = "a3", success = false, error = "Policy violation"),
            )
        )
        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<BulkPromotionResponse>(encoded)
        assertEquals(response, decoded)
    }

    // =========================================================================
    // LocalHealthResponse serialization
    // =========================================================================

    @Test
    fun `LocalHealthResponse round-trip serialization`() {
        val health = LocalHealthResponse(
            status = "healthy",
            checks = mapOf(
                "database" to HealthCheck(status = "up", responseTimeMs = 12),
                "storage" to HealthCheck(status = "up", responseTimeMs = 5),
            )
        )
        val encoded = json.encodeToString(health)
        val decoded = json.decodeFromString<LocalHealthResponse>(encoded)
        assertEquals(health, decoded)
    }

    @Test
    fun `LocalHealthResponse defaults to empty checks`() {
        val jsonStr = """{"status":"healthy"}"""
        val health = json.decodeFromString<LocalHealthResponse>(jsonStr)
        assertEquals("healthy", health.status)
        assertTrue(health.checks.isEmpty())
    }

    // =========================================================================
    // PromotionResponse serialization
    // =========================================================================

    @Test
    fun `PromotionResponse round-trip serialization`() {
        val response = PromotionResponse(
            success = true,
            message = "Promoted successfully",
            artifactId = "a1",
            targetRepositoryKey = "maven-releases",
            promotedAt = "2024-06-15T12:00:00Z"
        )
        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<PromotionResponse>(encoded)
        assertEquals(response, decoded)
    }
}
