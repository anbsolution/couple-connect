# Couple Connect

Private, invite-only Android app for two mutually consenting partners.

## Phase 1 starter
- Native Android app using Kotlin + Jetpack Compose
- Clean Light, iOS-inspired UI
- Profile setup: name, mobile number, optional profile photo picker
- Pairing screen: generate / enter a six-digit partner code
- Supabase SQL and Edge Function starter
- GitHub Actions debug APK workflow

## Important implementation status
The Android screens are a client starter. Pairing becomes functional only after configuring and deploying the Supabase project and Edge Function. The app does not currently send real push notifications or establish a live partner connection.

## Build
Push to `main` to run `.github/workflows/android-build.yml`, or trigger it manually from GitHub Actions.

## Package
`com.coupleconnect.app`

## Required GitHub secrets (only after backend setup)
No Supabase service-role key belongs in the Android app. Configure backend secrets in Supabase Edge Function settings.
