package com.gitofy.native.auth

/**
 * Ported from the web app's src/auth/config.ts.
 * The client id is PUBLIC (device flow needs no secret) — same value the web app
 * bakes in for the Gitufy GitHub App.
 */
object AuthConfig {
    const val CLIENT_ID = "Iv23liLESLFaJa3yzCdE"
    const val APP_ID = "5182766"
    const val REDIRECT_URI = "gitofy://callback"
    const val CALLBACK_SCHEME = "gitofy"
    const val CALLBACK_HOST = "callback"

    const val DEVICE_CODE_URL = "https://github.com/login/device/code"
    const val ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
    const val DEVICE_VERIFICATION = "https://github.com/login/device"

    /** OAuth App device flow documented defaults. */
    const val DEFAULT_EXPIRES_IN = 900
    const val DEFAULT_INTERVAL = 5
}
