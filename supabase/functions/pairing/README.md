# Pairing Edge Function – deployment checklist

This folder is reserved for the secure pairing endpoint.

Required behavior before enabling pairing:
1. Require a valid Supabase Auth user JWT.
2. Generate codes using a cryptographically secure random generator.
3. Store only a server-keyed hash of each code, never plaintext.
4. Expire codes after 5 minutes.
5. Enforce per-user and per-IP attempt limits.
6. Reject self-pairing, consumed codes, expired codes, and users already paired.
7. Require both partners to confirm the pairing.
8. Keep the Supabase service-role key exclusively in Edge Function secrets.

The Android demo currently generates a local display-only code. It is NOT a real pairing code.
