package com.artifactkeeper.android.data.models

// =============================================================================
// Type aliases from old model names → SDK model names
// This file bridges the app's existing code to the generated SDK types,
// minimizing changes in ViewModels and Composable screens.
// =============================================================================

// --- Core / Pagination ---
typealias Pagination = com.artifactkeeper.client.models.Pagination

// --- Packages ---
typealias PackageItem = com.artifactkeeper.client.models.PackageResponse
typealias PackageListResponse = com.artifactkeeper.client.models.PackageListResponse

// --- Builds ---
typealias BuildItem = com.artifactkeeper.client.models.BuildResponse
typealias BuildListResponse = com.artifactkeeper.client.models.BuildListResponse

// --- Repositories ---
typealias Repository = com.artifactkeeper.client.models.RepositoryResponse
typealias RepositoryListResponse = com.artifactkeeper.client.models.RepositoryListResponse

// --- Artifacts ---
typealias Artifact = com.artifactkeeper.client.models.ArtifactResponse
typealias ArtifactListResponse = com.artifactkeeper.client.models.ArtifactListResponse

// --- Security Scores ---
typealias RepoSecurityScore = com.artifactkeeper.client.models.ScoreResponse

// --- Scans ---
typealias ScanResult = com.artifactkeeper.client.models.ScanResponse
typealias ScanListResponse = com.artifactkeeper.client.models.ScanListResponse
typealias ScanFinding = com.artifactkeeper.client.models.FindingResponse
typealias ScanFindingListResponse = com.artifactkeeper.client.models.FindingListResponse

// --- Auth ---
typealias LoginRequest = com.artifactkeeper.client.models.LoginRequest
typealias LoginResponse = com.artifactkeeper.client.models.LoginResponse
typealias SetupStatusResponse = com.artifactkeeper.client.models.SetupStatusResponse
typealias UserInfo = com.artifactkeeper.client.models.UserResponse
typealias ChangePasswordRequest = com.artifactkeeper.client.models.ChangePasswordRequest

// --- Admin: Users ---
typealias AdminUser = com.artifactkeeper.client.models.UserResponse
typealias AdminUserListResponse = com.artifactkeeper.client.models.UserListResponse
typealias CreateUserRequest = com.artifactkeeper.client.models.CreateUserRequest
typealias CreateUserResponse = com.artifactkeeper.client.models.CreateUserResponse

// --- Admin: Groups ---
typealias AdminGroup = com.artifactkeeper.client.models.GroupResponse
typealias AdminGroupListResponse = com.artifactkeeper.client.models.GroupListResponse
typealias CreateGroupRequest = com.artifactkeeper.client.models.CreateGroupRequest

// --- SSO ---
typealias SSOProvider = com.artifactkeeper.client.models.SsoProviderInfo

// --- Peers ---
typealias PeerInstance = com.artifactkeeper.client.models.PeerInstanceResponse
typealias PeerListResponse = com.artifactkeeper.client.models.PeerInstanceListResponse
typealias RegisterPeerRequest = com.artifactkeeper.client.models.RegisterPeerRequest
typealias PeerConnection = com.artifactkeeper.client.models.PeerResponse
typealias AssignRepoRequest = com.artifactkeeper.client.models.AssignRepoRequest

// --- Webhooks ---
typealias Webhook = com.artifactkeeper.client.models.WebhookResponse
typealias WebhookListResponse = com.artifactkeeper.client.models.WebhookListResponse
typealias CreateWebhookRequest = com.artifactkeeper.client.models.CreateWebhookRequest
typealias TestWebhookResponse = com.artifactkeeper.client.models.TestWebhookResponse

// --- Security Policies ---
typealias SecurityPolicy = com.artifactkeeper.client.models.PolicyResponse
typealias CreatePolicyRequest = com.artifactkeeper.client.models.CreatePolicyRequest
typealias UpdatePolicyRequest = com.artifactkeeper.client.models.UpdatePolicyRequest
typealias TriggerScanRequest = com.artifactkeeper.client.models.TriggerScanRequest
typealias TriggerScanResponse = com.artifactkeeper.client.models.TriggerScanResponse

// --- Operations ---
typealias AdminStats = com.artifactkeeper.client.models.SystemStats
typealias StorageBreakdownItem = com.artifactkeeper.client.models.RepositoryStorageBreakdown
typealias HealthResponse = com.artifactkeeper.client.models.HealthResponse
typealias HealthLogEntry = com.artifactkeeper.client.models.ServiceHealthEntry
typealias AlertState = com.artifactkeeper.client.models.AlertState

// --- TOTP ---
typealias TotpSetupResponse = com.artifactkeeper.client.models.TotpSetupResponse
typealias TotpEnableResponse = com.artifactkeeper.client.models.TotpEnableResponse
typealias TotpCodeRequest = com.artifactkeeper.client.models.TotpCodeRequest
typealias TotpVerifyRequest = com.artifactkeeper.client.models.TotpVerifyRequest
typealias TotpDisableRequest = com.artifactkeeper.client.models.TotpDisableRequest

// --- SBOM ---
typealias SbomResponse = com.artifactkeeper.client.models.SbomResponse
typealias SbomContentResponse = com.artifactkeeper.client.models.SbomContentResponse
typealias SbomComponent = com.artifactkeeper.client.models.ComponentResponse
typealias CveHistoryEntry = com.artifactkeeper.client.models.CveHistoryEntry
typealias CveTrends = com.artifactkeeper.client.models.CveTrends
typealias GenerateSbomRequest = com.artifactkeeper.client.models.GenerateSbomRequest

// --- License Policies ---
typealias LicensePolicy = com.artifactkeeper.client.models.LicensePolicyResponse

// --- Staging / Promotion ---
// NOTE: PromoteArtifactRequest, PromotionResponse, BulkPromoteRequest,
// BulkPromotionResponse, and PromotionHistoryResponse are defined as local
// classes in LocalModels.kt because the staging endpoints (/api/v1/staging/...)
// use different field names/types than the SDK's promotion endpoints.

// --- Dependency-Track ---
typealias DtStatus = com.artifactkeeper.client.models.DtStatusResponse
typealias DtProject = com.artifactkeeper.client.models.DtProject
typealias DtFinding = com.artifactkeeper.client.models.DtFinding
typealias DtComponentFull = com.artifactkeeper.client.models.DtComponentFull
typealias DtProjectMetrics = com.artifactkeeper.client.models.DtProjectMetrics
typealias DtPortfolioMetrics = com.artifactkeeper.client.models.DtPortfolioMetrics
typealias DtPolicyViolation = com.artifactkeeper.client.models.DtPolicyViolation
typealias DtAnalysisResponse = com.artifactkeeper.client.models.DtAnalysisResponse
typealias DtPolicyFull = com.artifactkeeper.client.models.DtPolicyFull
typealias UpdateDtAnalysisRequest = com.artifactkeeper.client.models.UpdateAnalysisBody

// --- Virtual Repository Members ---
typealias VirtualMember = com.artifactkeeper.client.models.VirtualMemberResponse
typealias VirtualMembersResponse = com.artifactkeeper.client.models.VirtualMembersListResponse
typealias AddMemberRequest = com.artifactkeeper.client.models.AddVirtualMemberRequest
