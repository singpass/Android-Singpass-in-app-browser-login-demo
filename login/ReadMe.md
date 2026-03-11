# Singpass Login Android Library

This library provides a simplified and secure way to integrate Singpass login into your Android application. It handles the complexities of launching in-app browsers (Chrome Custom Tabs, Auth Tab) and managing redirects, ensuring compliance with modern security standards like **RFC8252** and the updated **FAPI2** standard.

## Table of Contents
1. [Key Features](#key-features)
2. [Getting Started](#getting-started)
3. [Conventional OIDC (PKCE) vs. FAPI2 Migration](#conventional-oidc-pkce-vs-fapi2-migration)
4. [Library Usage](#library-usage)
5. [Configuration](#configuration)
6. [Known Issues](#known-issues)
7. [FAQ & Troubleshooting](#faq--troubleshooting)

---

## Key Features
- **[Chrome Auth Tab Support](https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab)**: Provides the best user experience with a modern, trusted UI.
- **AppAuth Integration**: Seamlessly handles Chrome Custom Tabs and external browser fallbacks.
- **Sealed Login Parameters**: Supports both conventional OIDC and FAPI2 flows.
- **Headless Activity**: Minimal impact on your app's UI structure; uses the Activity Result API.

---

## Getting Started

### 1. Add the Library Module
Ensure the `:login` module is included in your project's `settings.gradle` and added as a dependency in your app's `build.gradle`:

```gradle
dependencies {
    implementation project(':login')
}
```

### 2. Configure Redirect URI
The library uses manifest placeholders to configure the redirect URI. Define these in your app's `build.gradle`:

```gradle
android {
    defaultConfig {
        manifestPlaceholders = [
            'host': "your.redirect.host", // e.g., "app.singpass.gov.sg"
            'path': "/your/redirect/path"  // e.g., "/rp/sample"
        ]
    }
}
```

---

## Conventional OIDC (PKCE) vs. FAPI2 Migration

Singpass is transitioning from the conventional OIDC flow (RFC8252) to the **FAPI2** standard to enhance security.

### Conventional OIDC (RFC8252)
- **Flow**: Uses PKCE. Authorization parameters (`client_id`, `scope`, `code_challenge` and `state`) are passed directly as query parameters in the authorization URL.
- **Usage**: Use `LoginParams.SingpassLoginParam` in the library.

### FAPI2 Standard (The New Way)
- **Flow**: Uses **Pushed Authorization Requests (PAR)**.
- **Process**:
  1. Your app calls your backend to initiate login.
  2. Your backend pushes all authorization parameters to the Singpass PAR endpoint.
  3. Singpass returns a `request_uri`.
  4. Your app launches the browser using only the `request_uri`.
- **Benefit**: Sensitive parameters are never exposed in the browser URL.
- **Usage**: Use `LoginParams.SingpassFapiLoginParam` in the library.

### Comparison Table

| Feature | Conventional (RFC8252) | FAPI2 (Updated) |
|---|---|---|
| **Parameters** | Passed in URL query | Passed via PAR (`request_uri`) |
| **Security** | High (PKCE) | Critical (PAR + FAPI2 compliance) |
| **Exposure** | Parameters visible in browser logs | Parameters hidden |
| **Library Class** | `SingpassLoginParam` | `SingpassFapiLoginParam` |

---

## Library Usage

The library simplifies integration using the `SingpassLoginActivityContract`.

### 1. Register the Launcher
In your Activity or Fragment:

```kotlin
val singpassLoginLauncher = registerForActivityResult(SingpassLoginActivityContract()) { result ->
    if (result.error != null) {
        // Handle error (e.g., result.error or result.userinfoError)
        Toast.makeText(context, "Login Failed: ${result.error}", Toast.LENGTH_LONG).show()
    } else {
        // Successful login
        val code = result.code
        val state = result.state
        // Relay code and state to your RP Backend
        viewModel.sendAuthCodeToBackend(code, state)
    }
}
```

### 2. Launch the Login Flow

#### Using FAPI2 (New Way)
```kotlin
val fapiParams = LoginParams.SingpassFapiLoginParam(
    clientId = "YOUR_CLIENT_ID",
    requestUri = "urn:ietf:params:oauth:request_uri:YOUR_UNIQUE_REQUEST_URI",
    redirectUri = "https://your.redirect.host/your/redirect/path".toUri()
)
singpassLoginLauncher.launch(fapiParams)
```

#### Using Conventional OIDC (Old Way)
```kotlin
val legacyParams = LoginParams.SingpassLoginParam(
    clientId = "YOUR_CLIENT_ID",
    redirectUri = "https://your.redirect.host/your/redirect/path".toUri(),
    scope = "openid",
    nonce = "GENERATED_NONCE",
    state = "GENERATED_STATE",
    codeChallenge = "GENERATED_CODE_CHALLENGE",
    codeChallengeMethod = "S256"
)
singpassLoginLauncher.launch(legacyParams)
```

---

## Configuration

### Customizing the UI
You can customize the appearance of the Chrome Custom Tabs / Auth Tab:

```kotlin
val params = LoginParams.SingpassFapiLoginParam(
    // ... other params
    customTabAppBarColor = Color.RED,
    customTabAppBarColorDark = Color.RED,
    customTabNavigationBarColor = Color.GREEN,
    customTabNavigationBarColorDark = Color.GREEN
)
```

### Browser Denylist
The library internally handles known issues with specific browsers (e.g., older versions of Samsung Internet or Microsoft Edge) by using a denylist to ensure a smooth redirect experience.

---

## Known Issues

- **Microsoft Edge App Linking Bug**: As of May 2023, `Microsoft Edge v113.0.1774.63` and certain versions have shown issues with app linking where the fallback URL is opened mistakenly during Singpass app launch. The library includes a denylist to handle this, but RPs should be aware of this browser behavior.
  <br><br>
  <img src="edge_browser_issue.gif" alt="Edge browser issue" width="300px" height="480px"></img>
  <br><br>
- **Samsung Internet Browser Redirect Issue**: As of June 2023, `Samsung Internet Browser v21.0.0.41` has an issue where custom tabs may close prematurely when launching the Singpass app. This is also mitigated via the library's browser matcher configuration.
  <br><br>
  <img src="samsung_internet_browser_issue.gif" alt="Samsung browser issue" width="300px" height="480px"></img>

---

## FAQ & Troubleshooting

- **Why use the Library instead of a WebView?**
  WebViews are strictly prohibited for Singpass login due to security risks defined in [RFC8252](https://www.rfc-editor.org/rfc/rfc8252). This library uses external user-agents (CCT/Auth Tab) as mandated.

- **How do I handle the redirect back to my app?**
  The library handles this automatically via `SingpassLoginHeadlessActivity`. Ensure your `manifestPlaceholders` match the `redirect_uri` registered with Singpass.

- **What if the user doesn't have a supported browser?**
  The library includes a fallback mechanism to open the system's default browser if Chrome Custom Tabs are not supported.

---

*RP stands for **Relying Party** (Your App/Backend). For more details on the Singpass ecosystem, visit the official developer documentation.*
