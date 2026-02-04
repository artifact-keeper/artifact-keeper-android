# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-rc.1] - 2026-02-03

### Added
- Forced password change flow on first login
- Scan findings detail view with CVE information
- Password visibility toggle on login dialog
- Account menu in top-right of all section TopAppBars
- Adaptive layout for Samsung Fold/Flip foldable devices
- Network security config to trust self-signed certs in debug builds
- Nightly release with debug APK on every push to main
- Hide Security, Operations, Admin tabs when not logged in

### Fixed
- Clear auth credentials when removing active server
- Return to welcome screen when last server is removed
- Search rewritten to query both repos and artifacts
- SSO providers endpoint model updated to match backend array response
- SearchScreen rewritten to search repositories instead of empty packages table
- Artifact model updated to use repository_key instead of repository_id
- Unique prefixed keys in MonitoringScreen to prevent duplicate key crash
- HealthLogEntry, AlertState models updated to match backend response fields
- SSL-configured OkHttp client used for health and metrics endpoints
- Crashes from 401 errors and LoginResponse model mismatch
