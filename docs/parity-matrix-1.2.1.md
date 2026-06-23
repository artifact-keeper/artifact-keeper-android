# Android client parity matrix vs API v1.2.1

Source of truth: `artifact-keeper-api/openapi.json` at tag v1.2.1 (390 operations across 290 paths).
App surface scanned: `app/src/main/java/com/artifactkeeper/android` (ui/screens and data/api).
SDK module: vendored `:sdk` regenerated from v1.2.1 (openapi-generator 7.21.0, kotlin + jvm-retrofit2 + kotlinx_serialization + useCoroutines).

## Status legend

- **exists**: an app screen or ViewModel calls this operation through the SDK and the call is reachable from a UI screen.
- **stale**: the app calls a path/operation that no longer matches the 1.2.1 spec and needs migrating. No rows are stale now: the three promotion calls were migrated to `api/v1/promotion` in #85 and are live.
- **missing**: in scope for mobile but not yet wired up in the app. The Notes column flags honest deferrals (chunked/raw uploads, gate authoring, signing-config write, advanced-search filters UI, SDK-duplicate or empty-body operations).
- **N/A-on-mobile**: deliberately out of scope for the mobile client. The standard is narrow: ONLY raw identity-provider config (SSO/OIDC/LDAP/SAML create/update/delete/toggle/test under `api/v1/admin/sso`) and lifecycle policy authoring (create/update/delete under `api/v1/admin/lifecycle`). Everything else is in scope, including user and role management, group and permission management, settings reads, SSO login/exchange flows, lifecycle execute/preview, service accounts, plugins, email subscriptions, migration write operations, and chunked upload.

## Per-section summary

| Section | exists | stale | missing | N/A-on-mobile | Total |
|---------|-------:|------:|--------:|--------------:|------:|
| Artifacts | 28 | 0 | 34 | 0 | 62 |
| Staging | 3 | 0 | 30 | 0 | 33 |
| Integration | 14 | 0 | 58 | 0 | 72 |
| Security | 59 | 0 | 13 | 0 | 72 |
| Operations | 4 | 0 | 19 | 0 | 23 |
| Administration | 8 | 0 | 92 | 16 | 116 |
| Cross-cutting | 8 | 0 | 4 | 0 | 12 |
| **All** | **124** | **0** | **250** | **16** | **390** |

## Artifacts

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| artifact-labels | `GET /api/v1/artifacts/{id}/labels` | `list_artifact_labels` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| artifact-labels | `PUT /api/v1/artifacts/{id}/labels` | `set_artifact_labels` | missing | Artifacts |  |  |
| artifact-labels | `DELETE /api/v1/artifacts/{id}/labels/{label_key}` | `delete_artifact_label` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| artifact-labels | `POST /api/v1/artifacts/{id}/labels/{label_key}` | `add_artifact_label` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| artifacts | `GET /api/v1/artifacts/{id}` | `get_artifact` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| artifacts | `GET /api/v1/artifacts/{id}/metadata` | `get_artifact_metadata` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| artifacts | `GET /api/v1/artifacts/{id}/stats` | `get_artifact_stats` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| builds | `GET /api/v1/builds` | `list_builds` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/builds/BuildsScreen.kt` |  |
| builds | `POST /api/v1/builds` | `create_build` | missing | Artifacts |  |  |
| builds | `GET /api/v1/builds/diff` | `get_build_diff` | missing | Artifacts |  |  |
| builds | `GET /api/v1/builds/{id}` | `get_build` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/builds/BuildDetailScreen.kt` |  |
| builds | `PUT /api/v1/builds/{id}` | `update_build` | missing | Artifacts |  |  |
| builds | `POST /api/v1/builds/{id}/artifacts` | `add_build_artifacts` | missing | Artifacts |  |  |
| packages | `GET /api/v1/packages` | `list_packages` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/packages/PackagesScreen.kt` |  |
| packages | `GET /api/v1/packages/{id}` | `get_package` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/packages/PackageDetailScreen.kt` |  |
| packages | `GET /api/v1/packages/{id}/versions` | `get_package_versions` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/packages/PackageDetailScreen.kt` |  |
| repositories | `GET /api/v1/repositories` | `list_repositories` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/RepositoriesScreen.kt` |  |
| repositories | `POST /api/v1/repositories` | `create_repository` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/CreateRepositoryScreen.kt` |  |
| repositories | `DELETE /api/v1/repositories/{key}` | `delete_repository` | missing | Artifacts |  |  |
| repositories | `GET /api/v1/repositories/{key}` | `get_repository` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/RepositoryDetailScreen.kt` |  |
| repositories | `PATCH /api/v1/repositories/{key}` | `update_repository` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/RepositoryDetailScreen.kt` |  |
| repositories | `GET /api/v1/repositories/{key}/artifacts` | `list_artifacts` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/RepositoryDetailScreen.kt` |  |
| repositories | `DELETE /api/v1/repositories/{key}/artifacts/{path}` | `delete_artifact` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/RepositoryBrowseScreen.kt` |  |
| repositories | `GET /api/v1/repositories/{key}/artifacts/{path}` | `get_repository_artifact_metadata` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/RepositoryBrowseScreen.kt` |  |
| repositories | `PUT /api/v1/repositories/{key}/artifacts/{path}` | `upload_artifact` | missing | Artifacts |  | raw upload deferred |
| repositories | `GET /api/v1/repositories/{key}/cache-ttl` | `get_cache_ttl` | missing | Artifacts |  |  |
| repositories | `PUT /api/v1/repositories/{key}/cache-ttl` | `set_cache_ttl` | missing | Artifacts |  |  |
| repositories | `POST /api/v1/repositories/{key}/cache/invalidate` | `invalidate_cache` | missing | Artifacts |  |  |
| repositories | `GET /api/v1/repositories/{key}/download/{path}` | `download_artifact` | missing | Artifacts |  | raw download deferred |
| repositories | `GET /api/v1/repositories/{key}/members` | `list_virtual_members` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/VirtualMembersScreen.kt` |  |
| repositories | `POST /api/v1/repositories/{key}/members` | `add_virtual_member` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/VirtualMembersScreen.kt` |  |
| repositories | `PUT /api/v1/repositories/{key}/members` | `update_virtual_members` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/VirtualMembersScreen.kt` |  |
| repositories | `DELETE /api/v1/repositories/{key}/members/{member_key}` | `remove_virtual_member` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/repositories/VirtualMembersScreen.kt` |  |
| repositories | `GET /api/v1/repositories/{key}/pypi-tracks` | `list_pypi_tracks` | missing | Artifacts |  |  |
| repositories | `DELETE /api/v1/repositories/{key}/pypi-tracks/{project}` | `delete_pypi_track` | missing | Artifacts |  |  |
| repositories | `PUT /api/v1/repositories/{key}/pypi-tracks/{project}` | `put_pypi_track` | missing | Artifacts |  |  |
| repositories | `DELETE /api/v1/repositories/{key}/routing-rules` | `delete_routing_rules` | missing | Artifacts |  |  |
| repositories | `GET /api/v1/repositories/{key}/routing-rules` | `get_routing_rules` | missing | Artifacts |  |  |
| repositories | `POST /api/v1/repositories/{key}/routing-rules` | `set_routing_rules` | missing | Artifacts |  |  |
| repositories | `POST /api/v1/repositories/{key}/test-upstream` | `test_upstream` | missing | Artifacts |  |  |
| repositories | `PUT /api/v1/repositories/{key}/upstream-auth` | `set_upstream_auth` | missing | Artifacts |  |  |
| repositories | `GET /api/v1/tree` | `get_tree` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/RepositoryBrowseScreen.kt` |  |
| repositories | `GET /api/v1/tree/content` | `get_content` | missing | Artifacts |  | SDK returns Response<Unit>, no body to render; deferred |
| repository-labels | `GET /api/v1/repositories/{key}/labels` | `list_repo_labels` | missing | Artifacts |  |  |
| repository-labels | `PUT /api/v1/repositories/{key}/labels` | `set_repo_labels` | missing | Artifacts |  |  |
| repository-labels | `DELETE /api/v1/repositories/{key}/labels/{label_key}` | `delete_repo_label` | missing | Artifacts |  |  |
| repository-labels | `POST /api/v1/repositories/{key}/labels/{label_key}` | `add_repo_label` | missing | Artifacts |  |  |
| repository_tokens | `GET /api/v1/repositories/{key}/tokens` | `list_repo_tokens` | missing | Artifacts |  |  |
| repository_tokens | `POST /api/v1/repositories/{key}/tokens` | `create_repo_token` | missing | Artifacts |  |  |
| repository_tokens | `DELETE /api/v1/repositories/{key}/tokens/{token_id}` | `revoke_repo_token` | missing | Artifacts |  |  |
| repository_tokens | `GET /api/v1/repositories/{key}/tokens/{token_id}` | `get_repo_token` | missing | Artifacts |  |  |
| search | `GET /api/v1/search/advanced` | `advanced_search` | missing | Artifacts |  | ViewModel method present but no screen invokes it; filters UI pending |
| search | `GET /api/v1/search/checksum` | `checksum_search` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/search/SearchScreen.kt` |  |
| search | `GET /api/v1/search/quick` | `quick_search` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/search/SearchScreen.kt` |  |
| search | `GET /api/v1/search/recent` | `recent` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/search/SearchScreen.kt` |  |
| search | `GET /api/v1/search/suggest` | `suggest` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/search/SearchScreen.kt` |  |
| search | `GET /api/v1/search/trending` | `trending` | exists | Artifacts | `app/src/main/java/com/artifactkeeper/android/ui/screens/search/SearchScreen.kt` |  |
| uploads | `POST /api/v1/uploads` | `create_session` | missing | Artifacts |  | chunked upload deferred |
| uploads | `DELETE /api/v1/uploads/{session_id}` | `cancel` | missing | Artifacts |  | chunked upload deferred |
| uploads | `GET /api/v1/uploads/{session_id}` | `get_session_status` | missing | Artifacts |  | chunked upload deferred |
| uploads | `PATCH /api/v1/uploads/{session_id}` | `upload_chunk` | missing | Artifacts |  | chunked upload deferred |
| uploads | `PUT /api/v1/uploads/{session_id}/complete` | `complete` | missing | Artifacts |  | chunked upload deferred |

## Staging

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| Curation | `GET /api/v1/curation/packages` | `list_curation_packages` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/packages/bulk-approve` | `bulk_approve` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/packages/bulk-block` | `bulk_block` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/packages/re-evaluate` | `re_evaluate` | missing | Staging |  |  |
| Curation | `GET /api/v1/curation/packages/{id}` | `get_curation_package` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/packages/{id}/approve` | `approve_package` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/packages/{id}/block` | `block_package` | missing | Staging |  |  |
| Curation | `GET /api/v1/curation/rules` | `list_curation_rules` | missing | Staging |  |  |
| Curation | `POST /api/v1/curation/rules` | `create_curation_rule` | missing | Staging |  |  |
| Curation | `DELETE /api/v1/curation/rules/{id}` | `delete_curation_rule` | missing | Staging |  |  |
| Curation | `PUT /api/v1/curation/rules/{id}` | `update_curation_rule` | missing | Staging |  |  |
| Curation | `GET /api/v1/curation/stats` | `stats` | missing | Staging |  |  |
| approval | `GET /api/v1/approval/history` | `list_approval_history` | missing | Staging |  |  |
| approval | `GET /api/v1/approval/pending` | `list_pending_approvals` | missing | Staging |  |  |
| approval | `POST /api/v1/approval/request` | `request_approval` | missing | Staging |  |  |
| approval | `GET /api/v1/approval/{id}` | `get_approval` | missing | Staging |  |  |
| approval | `POST /api/v1/approval/{id}/approve` | `approve_promotion` | missing | Staging |  |  |
| approval | `POST /api/v1/approval/{id}/reject` | `reject_promotion` | missing | Staging |  |  |
| promotion | `GET /api/v1/promotion-rules` | `list_rules` | missing | Staging |  |  |
| promotion | `POST /api/v1/promotion-rules` | `create_rule` | missing | Staging |  |  |
| promotion | `DELETE /api/v1/promotion-rules/{id}` | `delete_rule` | missing | Staging |  |  |
| promotion | `GET /api/v1/promotion-rules/{id}` | `get_rule` | missing | Staging |  |  |
| promotion | `PUT /api/v1/promotion-rules/{id}` | `update_rule` | missing | Staging |  |  |
| promotion | `POST /api/v1/promotion-rules/{id}/evaluate` | `evaluate_rule` | missing | Staging |  |  |
| promotion | `POST /api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/promote` | `promote_artifact` | exists | Staging | `app/src/main/java/com/artifactkeeper/android/ui/screens/staging/PromotionDialog.kt` |  |
| promotion | `POST /api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/reject` | `reject_artifact` | missing | Staging |  |  |
| promotion | `POST /api/v1/promotion/repositories/{key}/promote` | `promote_artifacts_bulk` | exists | Staging | `app/src/main/java/com/artifactkeeper/android/ui/screens/staging/StagingListScreen.kt` |  |
| promotion | `GET /api/v1/promotion/repositories/{key}/promotion-history` | `promotion_history` | exists | Staging | `app/src/main/java/com/artifactkeeper/android/ui/screens/staging/PromotionHistoryScreen.kt` |  |
| promotion | `GET /api/v1/promotion/repositories/{key}/release-target` | `get_release_target` | missing | Staging |  |  |
| promotion | `PUT /api/v1/promotion/repositories/{key}/release-target` | `set_release_target` | missing | Staging |  |  |
| quarantine | `GET /api/v1/quarantine/{artifact_id}` | `get_quarantine_status` | missing | Staging |  |  |
| quarantine | `POST /api/v1/quarantine/{artifact_id}/reject` | `reject_quarantined_artifact` | missing | Staging |  |  |
| quarantine | `POST /api/v1/quarantine/{artifact_id}/release` | `release_artifact` | missing | Staging |  |  |

## Integration

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| migration | `GET /api/v1/migrations` | `list_migrations` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations` | `create_migration` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/connections` | `list_connections` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/connections` | `create_connection` | missing | Integration |  |  |
| migration | `DELETE /api/v1/migrations/connections/{id}` | `delete_connection` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/connections/{id}` | `get_connection` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/connections/{id}/repositories` | `list_source_repositories` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/connections/{id}/test` | `test_connection` | missing | Integration |  |  |
| migration | `DELETE /api/v1/migrations/{id}` | `delete_migration` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/{id}` | `get_migration` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/{id}/assess` | `run_assessment` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/{id}/assessment` | `get_assessment` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/{id}/cancel` | `cancel_migration` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/{id}/items` | `list_migration_items` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/{id}/pause` | `pause_migration` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/{id}/report` | `get_migration_report` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/{id}/resume` | `resume_migration` | missing | Integration |  |  |
| migration | `POST /api/v1/migrations/{id}/start` | `start_migration` | missing | Integration |  |  |
| migration | `GET /api/v1/migrations/{id}/stream` | `stream_migration_progress` | missing | Integration |  |  |
| peer-instance-labels | `GET /api/v1/peers/{id}/labels` | `list_labels` | missing | Integration |  |  |
| peer-instance-labels | `PUT /api/v1/peers/{id}/labels` | `set_labels` | missing | Integration |  |  |
| peer-instance-labels | `DELETE /api/v1/peers/{id}/labels/{label_key}` | `delete_label` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| peer-instance-labels | `POST /api/v1/peers/{id}/labels/{label_key}` | `add_label` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/artifacts/ArtifactDetailScreen.kt` |  |
| peers | `GET /api/v1/peers` | `list_peers` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/ReplicationScreen.kt` |  |
| peers | `POST /api/v1/peers` | `register_peer` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/PeersScreen.kt` |  |
| peers | `POST /api/v1/peers/announce` | `announce_peer` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/identity` | `get_identity` | missing | Integration |  |  |
| peers | `DELETE /api/v1/peers/{id}` | `unregister_peer` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/PeersScreen.kt` |  |
| peers | `GET /api/v1/peers/{id}` | `get_peer` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/chunks/{artifact_id}` | `get_chunk_availability` | missing | Integration |  |  |
| peers | `PUT /api/v1/peers/{id}/chunks/{artifact_id}` | `update_chunk_availability` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/chunks/{artifact_id}/peers` | `get_peers_with_chunks` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/chunks/{artifact_id}/scored-peers` | `get_scored_peers` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/connections` | `list_peer_connections` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/ReplicationScreen.kt` |  |
| peers | `GET /api/v1/peers/{id}/connections/discover` | `discover_peers` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/connections/probe` | `probe_peer` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/connections/{target_id}/unreachable` | `mark_unreachable` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/heartbeat` | `heartbeat` | missing | Integration |  |  |
| peers | `PUT /api/v1/peers/{id}/network-profile` | `update_network_profile` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/repositories` | `get_assigned_repos` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/ReplicationScreen.kt` |  |
| peers | `POST /api/v1/peers/{id}/repositories` | `assign_repo` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/ReplicationScreen.kt` |  |
| peers | `DELETE /api/v1/peers/{id}/repositories/{repo_id}` | `unassign_repo` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/repositories/{repo_id}` | `get_subscription` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/repositories/{repo_id}/sync` | `run_subscription_now` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/sync` | `trigger_sync` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/sync/tasks` | `get_sync_tasks` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/init` | `init_transfer` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/transfer/{session_id}` | `get_session` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/complete` | `complete_chunk` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/fail` | `fail_chunk` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/retry` | `retry_chunk` | missing | Integration |  |  |
| peers | `GET /api/v1/peers/{id}/transfer/{session_id}/chunks` | `get_chunk_manifest` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/{session_id}/complete` | `complete_session` | missing | Integration |  |  |
| peers | `POST /api/v1/peers/{id}/transfer/{session_id}/fail` | `fail_session` | missing | Integration |  |  |
| peers | `GET /api/v1/sync-policies` | `list_sync_policies` | missing | Integration |  |  |
| peers | `POST /api/v1/sync-policies` | `create_sync_policy` | missing | Integration |  |  |
| peers | `POST /api/v1/sync-policies/evaluate` | `evaluate_policies` | missing | Integration |  |  |
| peers | `POST /api/v1/sync-policies/preview` | `preview_sync_policy` | missing | Integration |  |  |
| peers | `DELETE /api/v1/sync-policies/{id}` | `delete_sync_policy` | missing | Integration |  |  |
| peers | `GET /api/v1/sync-policies/{id}` | `get_sync_policy` | missing | Integration |  |  |
| peers | `PUT /api/v1/sync-policies/{id}` | `update_sync_policy` | missing | Integration |  |  |
| peers | `POST /api/v1/sync-policies/{id}/toggle` | `toggle_policy` | missing | Integration |  |  |
| webhooks | `GET /api/v1/webhooks` | `list_webhooks` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |
| webhooks | `POST /api/v1/webhooks` | `create_webhook` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |
| webhooks | `DELETE /api/v1/webhooks/{id}` | `delete_webhook` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |
| webhooks | `GET /api/v1/webhooks/{id}` | `get_webhook` | missing | Integration |  |  |
| webhooks | `GET /api/v1/webhooks/{id}/deliveries` | `list_deliveries` | missing | Integration |  |  |
| webhooks | `POST /api/v1/webhooks/{id}/deliveries/{delivery_id}/redeliver` | `redeliver` | missing | Integration |  |  |
| webhooks | `POST /api/v1/webhooks/{id}/disable` | `disable_webhook` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |
| webhooks | `POST /api/v1/webhooks/{id}/enable` | `enable_webhook` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |
| webhooks | `POST /api/v1/webhooks/{id}/rotate-secret` | `rotate_webhook_secret` | missing | Integration |  |  |
| webhooks | `POST /api/v1/webhooks/{id}/test` | `test_webhook` | exists | Integration | `app/src/main/java/com/artifactkeeper/android/ui/screens/integration/WebhooksScreen.kt` |  |

## Security

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| quality | `GET /api/v1/quality/checks` | `list_checks` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `POST /api/v1/quality/checks/trigger` | `trigger_checks` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `GET /api/v1/quality/checks/{id}` | `get_check` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `GET /api/v1/quality/checks/{id}/issues` | `list_check_issues` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `GET /api/v1/quality/gates` | `list_gates` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityHealthScreen.kt` |  |
| quality | `POST /api/v1/quality/gates` | `create_gate` | missing | Security |  | gate authoring deferred to Administration pass |
| quality | `POST /api/v1/quality/gates/evaluate/{artifact_id}` | `evaluate_gate` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `DELETE /api/v1/quality/gates/{id}` | `delete_gate` | missing | Security |  | gate authoring deferred to Administration pass |
| quality | `GET /api/v1/quality/gates/{id}` | `get_gate` | missing | Security |  |  |
| quality | `PUT /api/v1/quality/gates/{id}` | `update_gate` | missing | Security |  | gate authoring deferred to Administration pass |
| quality | `GET /api/v1/quality/health/artifacts/{artifact_id}` | `get_artifact_health` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `GET /api/v1/quality/health/dashboard` | `get_health_dashboard` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityHealthScreen.kt` |  |
| quality | `GET /api/v1/quality/health/repositories/{key}` | `get_repo_health` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityHealthScreen.kt` |  |
| quality | `DELETE /api/v1/quality/issues/{id}/suppress` | `unsuppress_issue` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| quality | `POST /api/v1/quality/issues/{id}/suppress` | `suppress_issue` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/QualityArtifactScreen.kt` |  |
| sbom | `GET /api/v1/sbom` | `list_sboms` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SbomScreen.kt` |  |
| sbom | `POST /api/v1/sbom` | `generate_sbom` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SbomScreen.kt` |  |
| sbom | `GET /api/v1/sbom/by-artifact/{artifact_id}` | `get_sbom_by_artifact` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ArtifactSecurityScreen.kt` |  |
| sbom | `POST /api/v1/sbom/check-compliance` | `check_license_compliance` | missing | Security |  |  |
| sbom | `GET /api/v1/sbom/cve/history/by-artifact/{artifact_id}` | `get_cve_history_by_artifact` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SbomScreen.kt` |  |
| sbom | `GET /api/v1/sbom/cve/history/by-cve/{cve_id}` | `get_cve_history_by_cve` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/CveTrackingScreen.kt` |  |
| sbom | `GET /api/v1/sbom/cve/history/{id}` | `get_cve_history` | missing | Security |  |  |
| sbom | `POST /api/v1/sbom/cve/status/by-artifact/{artifact_id}/by-cve/{cve_id}` | `update_cve_status_by_artifact_cve` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/CveTrackingScreen.kt` |  |
| sbom | `POST /api/v1/sbom/cve/status/{id}` | `update_cve_status` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/CveTrackingScreen.kt` |  |
| sbom | `GET /api/v1/sbom/cve/trends` | `get_cve_trends` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| sbom | `GET /api/v1/sbom/license-policies` | `list_license_policies` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/LicensePoliciesScreen.kt` |  |
| sbom | `POST /api/v1/sbom/license-policies` | `upsert_license_policy` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/LicensePoliciesScreen.kt` |  |
| sbom | `DELETE /api/v1/sbom/license-policies/{id}` | `delete_license_policy` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/LicensePoliciesScreen.kt` |  |
| sbom | `GET /api/v1/sbom/license-policies/{id}` | `get_license_policy` | missing | Security |  |  |
| sbom | `DELETE /api/v1/sbom/{id}` | `delete_sbom` | missing | Security |  |  |
| sbom | `GET /api/v1/sbom/{id}` | `get_sbom` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SbomScreen.kt` |  |
| sbom | `GET /api/v1/sbom/{id}/components` | `get_sbom_components` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SbomScreen.kt` |  |
| sbom | `POST /api/v1/sbom/{id}/convert` | `convert_sbom` | missing | Security |  |  |
| security | `PUT /api/v1/dependency-track/analysis` | `update_analysis` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/metrics/portfolio` | `get_portfolio_metrics` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| security | `GET /api/v1/dependency-track/policies` | `list_dependency_track_policies` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects` | `list_projects` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}` | `get_project` | missing | Security |  | SDK duplicate of get_project_findings; not wired |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}/components` | `get_project_components` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}/findings` | `get_project_findings` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}/metrics` | `get_project_metrics` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}/metrics/history` | `get_project_metrics_history` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/projects/{project_uuid}/violations` | `get_project_violations` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/DependencyTrackScreen.kt` |  |
| security | `GET /api/v1/dependency-track/status` | `dt_status` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| security | `GET /api/v1/repositories/{key}/security` | `get_repo_security` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/RepoSecurityScreen.kt` |  |
| security | `PUT /api/v1/repositories/{key}/security` | `update_repo_security` | missing | Security |  |  |
| security | `GET /api/v1/repositories/{key}/security/scans` | `list_repo_scans` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/RepoSecurityScreen.kt` |  |
| security | `GET /api/v1/security/artifacts/{artifact_id}/scans` | `list_artifact_scans` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ArtifactSecurityScreen.kt` |  |
| security | `GET /api/v1/security/configs` | `list_scan_configs` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| security | `GET /api/v1/security/dashboard` | `get_dashboard` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| security | `DELETE /api/v1/security/findings/{id}/acknowledge` | `revoke_acknowledgment` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ScanDetailScreen.kt` |  |
| security | `POST /api/v1/security/findings/{id}/acknowledge` | `acknowledge_finding` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ScanDetailScreen.kt` |  |
| security | `GET /api/v1/security/policies` | `list_policies` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/PoliciesScreen.kt` |  |
| security | `POST /api/v1/security/policies` | `create_policy` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/PoliciesScreen.kt` |  |
| security | `DELETE /api/v1/security/policies/{id}` | `delete_policy` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/PoliciesScreen.kt` |  |
| security | `GET /api/v1/security/policies/{id}` | `get_policy` | missing | Security |  |  |
| security | `PUT /api/v1/security/policies/{id}` | `update_policy` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/PoliciesScreen.kt` |  |
| security | `POST /api/v1/security/scan` | `trigger_scan` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ScansScreen.kt` |  |
| security | `GET /api/v1/security/scans` | `list_scans` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ScansScreen.kt` |  |
| security | `GET /api/v1/security/scans/{id}` | `get_scan` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ArtifactSecurityScreen.kt` |  |
| security | `GET /api/v1/security/scans/{id}/findings` | `list_findings` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/ScanFindingsScreen.kt` |  |
| security | `GET /api/v1/security/scores` | `get_all_scores` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SecurityScreen.kt` |  |
| signing | `GET /api/v1/signing/keys` | `list_keys` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `POST /api/v1/signing/keys` | `create_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `DELETE /api/v1/signing/keys/{key_id}` | `delete_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `GET /api/v1/signing/keys/{key_id}` | `get_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `GET /api/v1/signing/keys/{key_id}/public` | `get_public_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `POST /api/v1/signing/keys/{key_id}/revoke` | `revoke_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `POST /api/v1/signing/keys/{key_id}/rotate` | `rotate_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/SigningKeysScreen.kt` |  |
| signing | `GET /api/v1/signing/repositories/{repo_id}/config` | `get_repo_signing_config` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/RepoSecurityScreen.kt` |  |
| signing | `POST /api/v1/signing/repositories/{repo_id}/config` | `update_repo_signing_config` | missing | Security |  | signing config write deferred to Administration pass |
| signing | `GET /api/v1/signing/repositories/{repo_id}/public-key` | `get_repo_public_key` | exists | Security | `app/src/main/java/com/artifactkeeper/android/ui/screens/security/RepoSecurityScreen.kt` |  |

## Operations

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| analytics | `GET /api/v1/admin/analytics/artifacts/stale` | `get_stale_artifacts` | missing | Operations |  |  |
| analytics | `GET /api/v1/admin/analytics/downloads/trend` | `get_download_trends` | missing | Operations |  |  |
| analytics | `GET /api/v1/admin/analytics/repositories/{id}/trend` | `get_repository_trend` | missing | Operations |  |  |
| analytics | `POST /api/v1/admin/analytics/snapshot` | `capture_snapshot` | missing | Operations |  |  |
| analytics | `GET /api/v1/admin/analytics/storage/breakdown` | `get_storage_breakdown` | exists | Operations | `app/src/main/java/com/artifactkeeper/android/ui/screens/operations/AnalyticsScreen.kt` |  |
| analytics | `GET /api/v1/admin/analytics/storage/growth` | `get_growth_summary` | missing | Operations |  |  |
| analytics | `GET /api/v1/admin/analytics/storage/trend` | `get_storage_trend` | missing | Operations |  |  |
| health | `GET /api/v1/admin/metrics` | `metrics` | missing | Operations |  |  |
| health | `GET /health` | `health_check` | exists | Operations | `app/src/main/java/com/artifactkeeper/android/ui/screens/welcome/WelcomeScreen.kt` |  |
| health | `GET /livez` | `liveness_check` | missing | Operations |  |  |
| health | `GET /readyz` | `readiness_check` | missing | Operations |  |  |
| monitoring | `GET /api/v1/admin/monitoring/alerts` | `get_alert_states` | exists | Operations | `app/src/main/java/com/artifactkeeper/android/ui/screens/operations/MonitoringScreen.kt` |  |
| monitoring | `POST /api/v1/admin/monitoring/alerts/suppress` | `suppress_alert` | missing | Operations |  |  |
| monitoring | `POST /api/v1/admin/monitoring/check` | `run_health_check` | missing | Operations |  |  |
| monitoring | `GET /api/v1/admin/monitoring/health-log` | `get_health_log` | exists | Operations | `app/src/main/java/com/artifactkeeper/android/ui/screens/operations/MonitoringScreen.kt` |  |
| system | `GET /api/v1/system/config` | `get_system_config` | missing | Operations |  |  |
| telemetry | `GET /api/v1/admin/telemetry/crashes` | `list_crashes` | missing | Operations |  |  |
| telemetry | `GET /api/v1/admin/telemetry/crashes/pending` | `list_pending_crashes` | missing | Operations |  |  |
| telemetry | `POST /api/v1/admin/telemetry/crashes/submit` | `submit_crashes` | missing | Operations |  |  |
| telemetry | `DELETE /api/v1/admin/telemetry/crashes/{id}` | `delete_crash` | missing | Operations |  |  |
| telemetry | `GET /api/v1/admin/telemetry/crashes/{id}` | `get_crash` | missing | Operations |  |  |
| telemetry | `GET /api/v1/admin/telemetry/settings` | `get_telemetry_settings` | missing | Operations |  |  |
| telemetry | `POST /api/v1/admin/telemetry/settings` | `update_telemetry_settings` | missing | Operations |  |  |

## Administration

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| admin | `GET /api/v1/admin/backups` | `list_backups` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/backups` | `create_backup` | missing | Administration |  |  |
| admin | `DELETE /api/v1/admin/backups/{id}` | `delete_backup` | missing | Administration |  |  |
| admin | `GET /api/v1/admin/backups/{id}` | `get_backup` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/backups/{id}/cancel` | `cancel_backup` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/backups/{id}/execute` | `execute_backup` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/backups/{id}/restore` | `restore_backup` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/cleanup` | `run_cleanup` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/reindex` | `trigger_reindex` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/rescan-for-inventory` | `rescan_for_inventory` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/search/reindex` | `trigger_search_reindex` | missing | Administration |  |  |
| admin | `GET /api/v1/admin/settings` | `get_settings` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/settings` | `update_settings` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/smtp/test` | `send_test_email` | missing | Administration |  |  |
| admin | `GET /api/v1/admin/stats` | `get_system_stats` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/operations/AnalyticsScreen.kt` |  |
| admin | `GET /api/v1/admin/storage-backends` | `list_storage_backends` | missing | Administration |  |  |
| admin | `POST /api/v1/admin/storage-gc` | `run_storage_gc` | missing | Administration |  |  |
| admin | `GET /api/v1/admin/storage-gc/oci-blob-report` | `oci_blob_report` | missing | Administration |  |  |
| admin | `GET /api/v1/instances` | `list_instances` | missing | Administration |  |  |
| admin | `POST /api/v1/instances` | `create_instance` | missing | Administration |  |  |
| admin | `DELETE /api/v1/instances/{id}` | `delete_instance` | missing | Administration |  |  |
| admin | `DELETE /api/v1/instances/{id}/proxy/{path}` | `proxy_delete` | missing | Administration |  |  |
| admin | `GET /api/v1/instances/{id}/proxy/{path}` | `proxy_get` | missing | Administration |  |  |
| admin | `POST /api/v1/instances/{id}/proxy/{path}` | `proxy_post` | missing | Administration |  |  |
| admin | `PUT /api/v1/instances/{id}/proxy/{path}` | `proxy_put` | missing | Administration |  |  |
| email_subscriptions | `GET /api/v1/repositories/{key}/email-subscriptions` | `list_subscriptions` | missing | Administration |  |  |
| email_subscriptions | `POST /api/v1/repositories/{key}/email-subscriptions` | `create_subscription` | missing | Administration |  |  |
| email_subscriptions | `DELETE /api/v1/repositories/{key}/email-subscriptions/{subscription_id}` | `delete_subscription` | missing | Administration |  |  |
| groups | `GET /api/v1/groups` | `list_groups` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/admin/GroupsScreen.kt` |  |
| groups | `POST /api/v1/groups` | `create_group` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/admin/GroupsScreen.kt` |  |
| groups | `DELETE /api/v1/groups/{id}` | `delete_group` | missing | Administration |  |  |
| groups | `GET /api/v1/groups/{id}` | `get_group` | missing | Administration |  |  |
| groups | `PUT /api/v1/groups/{id}` | `update_group` | missing | Administration |  |  |
| groups | `DELETE /api/v1/groups/{id}/members` | `remove_members` | missing | Administration |  |  |
| groups | `POST /api/v1/groups/{id}/members` | `add_members` | missing | Administration |  |  |
| lifecycle | `GET /api/v1/admin/lifecycle` | `list_lifecycle_policies` | missing | Administration |  |  |
| lifecycle | `POST /api/v1/admin/lifecycle` | `create_lifecycle_policy` | N/A-on-mobile | Administration |  | lifecycle policy authoring; desktop admin |
| lifecycle | `POST /api/v1/admin/lifecycle/execute-all` | `execute_all_policies` | missing | Administration |  |  |
| lifecycle | `DELETE /api/v1/admin/lifecycle/{id}` | `delete_lifecycle_policy` | N/A-on-mobile | Administration |  | lifecycle policy authoring; desktop admin |
| lifecycle | `GET /api/v1/admin/lifecycle/{id}` | `get_lifecycle_policy` | missing | Administration |  |  |
| lifecycle | `PATCH /api/v1/admin/lifecycle/{id}` | `update_lifecycle_policy` | N/A-on-mobile | Administration |  | lifecycle policy authoring; desktop admin |
| lifecycle | `POST /api/v1/admin/lifecycle/{id}/execute` | `execute_policy` | missing | Administration |  |  |
| lifecycle | `POST /api/v1/admin/lifecycle/{id}/preview` | `preview_policy` | missing | Administration |  |  |
| permissions | `GET /api/v1/permissions` | `list_permissions` | missing | Administration |  |  |
| permissions | `POST /api/v1/permissions` | `create_permission` | missing | Administration |  |  |
| permissions | `DELETE /api/v1/permissions/{id}` | `delete_permission` | missing | Administration |  |  |
| permissions | `GET /api/v1/permissions/{id}` | `get_permission` | missing | Administration |  |  |
| permissions | `PUT /api/v1/permissions/{id}` | `update_permission` | missing | Administration |  |  |
| plugins | `GET /api/v1/formats` | `list_format_handlers` | missing | Administration |  |  |
| plugins | `GET /api/v1/formats/{format_key}` | `get_format_handler` | missing | Administration |  |  |
| plugins | `POST /api/v1/formats/{format_key}/disable` | `disable_format_handler` | missing | Administration |  |  |
| plugins | `POST /api/v1/formats/{format_key}/enable` | `enable_format_handler` | missing | Administration |  |  |
| plugins | `POST /api/v1/formats/{format_key}/test` | `test_format_handler` | missing | Administration |  |  |
| plugins | `GET /api/v1/plugins` | `list_plugins` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins` | `install_plugin` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/install/git` | `install_from_git` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/install/local` | `install_from_local` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/install/zip` | `install_from_zip` | missing | Administration |  |  |
| plugins | `DELETE /api/v1/plugins/{id}` | `uninstall_plugin` | missing | Administration |  |  |
| plugins | `GET /api/v1/plugins/{id}` | `get_plugin` | missing | Administration |  |  |
| plugins | `GET /api/v1/plugins/{id}/config` | `get_plugin_config` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/{id}/config` | `update_plugin_config` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/{id}/disable` | `disable_plugin` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/{id}/enable` | `enable_plugin` | missing | Administration |  |  |
| plugins | `GET /api/v1/plugins/{id}/events` | `get_plugin_events` | missing | Administration |  |  |
| plugins | `POST /api/v1/plugins/{id}/reload` | `reload_plugin` | missing | Administration |  |  |
| service_accounts | `GET /api/v1/service-accounts` | `list_service_accounts` | missing | Administration |  |  |
| service_accounts | `POST /api/v1/service-accounts` | `create_service_account` | missing | Administration |  |  |
| service_accounts | `POST /api/v1/service-accounts/repo-selector/preview` | `preview_repo_selector` | missing | Administration |  |  |
| service_accounts | `DELETE /api/v1/service-accounts/{id}` | `delete_service_account` | missing | Administration |  |  |
| service_accounts | `GET /api/v1/service-accounts/{id}` | `get_service_account` | missing | Administration |  |  |
| service_accounts | `PATCH /api/v1/service-accounts/{id}` | `update_service_account` | missing | Administration |  |  |
| service_accounts | `GET /api/v1/service-accounts/{id}/tokens` | `list_tokens` | missing | Administration |  |  |
| service_accounts | `POST /api/v1/service-accounts/{id}/tokens` | `create_token` | missing | Administration |  |  |
| service_accounts | `DELETE /api/v1/service-accounts/{id}/tokens/{token_id}` | `revoke_token` | missing | Administration |  |  |
| sso | `GET /api/v1/admin/sso/ldap` | `list_ldap` | missing | Administration |  |  |
| sso | `POST /api/v1/admin/sso/ldap` | `create_ldap` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `DELETE /api/v1/admin/sso/ldap/{id}` | `delete_ldap` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `GET /api/v1/admin/sso/ldap/{id}` | `get_ldap` | missing | Administration |  |  |
| sso | `PUT /api/v1/admin/sso/ldap/{id}` | `update_ldap` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `POST /api/v1/admin/sso/ldap/{id}/test` | `test_ldap` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `PATCH /api/v1/admin/sso/ldap/{id}/toggle` | `toggle_ldap` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `GET /api/v1/admin/sso/oidc` | `list_oidc` | missing | Administration |  |  |
| sso | `POST /api/v1/admin/sso/oidc` | `create_oidc` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `DELETE /api/v1/admin/sso/oidc/{id}` | `delete_oidc` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `GET /api/v1/admin/sso/oidc/{id}` | `get_oidc` | missing | Administration |  |  |
| sso | `PUT /api/v1/admin/sso/oidc/{id}` | `update_oidc` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `PATCH /api/v1/admin/sso/oidc/{id}/toggle` | `toggle_oidc` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `GET /api/v1/admin/sso/providers` | `list_sso_providers_admin` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/admin/SSOScreen.kt` |  |
| sso | `GET /api/v1/admin/sso/saml` | `list_saml` | missing | Administration |  |  |
| sso | `POST /api/v1/admin/sso/saml` | `create_saml` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `DELETE /api/v1/admin/sso/saml/{id}` | `delete_saml` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `GET /api/v1/admin/sso/saml/{id}` | `get_saml` | missing | Administration |  |  |
| sso | `PUT /api/v1/admin/sso/saml/{id}` | `update_saml` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `PATCH /api/v1/admin/sso/saml/{id}/toggle` | `toggle_saml` | N/A-on-mobile | Administration |  | raw SSO/OIDC/LDAP/SAML provider authoring; desktop admin |
| sso | `POST /api/v1/auth/sso/exchange` | `exchange_code` | missing | Administration |  |  |
| sso | `POST /api/v1/auth/sso/ldap/{id}/login` | `ldap_login` | missing | Administration |  |  |
| sso | `GET /api/v1/auth/sso/oidc/{id}/callback` | `oidc_callback` | missing | Administration |  |  |
| sso | `GET /api/v1/auth/sso/oidc/{id}/login` | `oidc_login` | missing | Administration |  |  |
| sso | `GET /api/v1/auth/sso/providers` | `list_providers` | missing | Administration |  |  |
| sso | `POST /api/v1/auth/sso/saml/{id}/acs` | `saml_acs` | missing | Administration |  |  |
| sso | `GET /api/v1/auth/sso/saml/{id}/login` | `saml_login` | missing | Administration |  |  |
| users | `GET /api/v1/users` | `list_users` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/admin/UsersScreen.kt` |  |
| users | `POST /api/v1/users` | `create_user` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/admin/UsersScreen.kt` |  |
| users | `DELETE /api/v1/users/{id}` | `delete_user` | missing | Administration |  |  |
| users | `GET /api/v1/users/{id}` | `get_user` | missing | Administration |  |  |
| users | `PATCH /api/v1/users/{id}` | `update_user` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ProfileScreen.kt` |  |
| users | `POST /api/v1/users/{id}/force-password-change` | `force_password_change` | missing | Administration |  |  |
| users | `POST /api/v1/users/{id}/password` | `change_password` | exists | Administration | `app/src/main/java/com/artifactkeeper/android/ui/screens/auth/ChangePasswordScreen.kt` |  |
| users | `POST /api/v1/users/{id}/password/reset` | `reset_password` | missing | Administration |  |  |
| users | `GET /api/v1/users/{id}/roles` | `get_user_roles` | missing | Administration |  |  |
| users | `POST /api/v1/users/{id}/roles` | `assign_role` | missing | Administration |  |  |
| users | `DELETE /api/v1/users/{id}/roles/{role_id}` | `revoke_role` | missing | Administration |  |  |
| users | `GET /api/v1/users/{id}/tokens` | `list_user_tokens` | missing | Administration |  |  |
| users | `POST /api/v1/users/{id}/tokens` | `create_user_api_token` | missing | Administration |  |  |
| users | `DELETE /api/v1/users/{id}/tokens/{token_id}` | `revoke_user_api_token` | missing | Administration |  |  |

## Cross-cutting

| Tag | Method+Path | operationId | Status | Section | Target file | Notes |
|-----|-------------|-------------|--------|---------|-------------|-------|
| auth | `POST /api/v1/auth/login` | `login` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/settings/SettingsScreen.kt` |  |
| auth | `POST /api/v1/auth/logout` | `logout` | missing | Cross-cutting |  |  |
| auth | `GET /api/v1/auth/me` | `get_current_user` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/settings/SettingsScreen.kt` |  |
| auth | `POST /api/v1/auth/refresh` | `refresh_token` | missing | Cross-cutting |  |  |
| auth | `POST /api/v1/auth/ticket` | `create_download_ticket` | missing | Cross-cutting |  |  |
| auth | `POST /api/v1/auth/tokens` | `create_api_token` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ApiTokensScreen.kt` |  |
| auth | `DELETE /api/v1/auth/tokens/{token_id}` | `revoke_api_token` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ApiTokensScreen.kt` |  |
| auth | `POST /api/v1/auth/totp/disable` | `disable_totp` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ProfileScreen.kt` |  |
| auth | `POST /api/v1/auth/totp/enable` | `enable_totp` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ProfileScreen.kt` |  |
| auth | `POST /api/v1/auth/totp/setup` | `setup_totp` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/screens/profile/ProfileScreen.kt` |  |
| auth | `POST /api/v1/auth/totp/verify` | `verify_totp` | missing | Cross-cutting |  |  |
| auth | `GET /api/v1/setup/status` | `setup_status` | exists | Cross-cutting | `app/src/main/java/com/artifactkeeper/android/ui/components/AccountMenu.kt` |  |

