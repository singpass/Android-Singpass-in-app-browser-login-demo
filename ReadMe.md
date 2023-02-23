# Migrating away from [WebView](https://developer.android.com/reference/android/webkit/WebView) for Android Mobile app Singpass Logins

Usage of WebViews for web logins are not recommended due to security and usability reasons documented in [RFC8252](https://www.rfc-editor.org/rfc/rfc8252). Google has done the [same](https://developers.googleblog.com/2021/06/upcoming-security-changes-to-googles-oauth-2.0-authorization-endpoint.html) for Google Sign-in in 2021.

> This best current practice requires that only external user-agents
like the browser are used for OAuth by native apps.  It documents how
native apps can implement authorization flows using the browser as
the preferred external user-agent as well as the requirements for
authorization servers to support such usage.

*Quoted from RFC8252.*

This repository has codes for a sample Android application implementing the recommended [Proof Key for Code Exchange (PKCE)](https://www.rfc-editor.org/rfc/rfc7636) for Singpass logins. The application will demonstrate the Singpass login flow while utilizing [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/#:~:text=Custom%20Tabs%20is%20a%20browser,to%20resort%20to%20a%20WebView.) or external mobile web browser along with PKCE leveraging on the Android [AppAuth](https://github.com/openid/AppAuth-Android) library.

# Sequence Diagram
![Sequence Diagram](pkce_sequence_diagram.png)

- 1a) Call **RP Backend** to obtain backend generate `code_challenge`, `code_challenge_method` along with `state` and `nonce` if required. #
<br><br>
- 1b) **RP Backend** responds with the requested parameters. (`code_challenge`, `code_challenge_method`, `state`, `nonce`) #
  <br><br>
- 2a) Open the Authorization endpoint in web browser via [AppAuth](https://github.com/openid/AppAuth-Android) providing query params of `redirect_uri`*, `client_id`, `scope`, `code_challenge`, `code_challenge_method` along with `state` and `nonce` if required. There can be other query params provided if needed. e.g. (`purpose_id` for myInfo use cases)
  <br><br>
- 2b) The `authorization code` will be delivered back to **RP Mobile App**.
<br><br>
- 3a) **RP Mobile App** Upon reception of `authorization code`, proceed to relay the Authorization code back to **RP Backend**. #
  <br><br>
- 3b) **RP Backend** will use the `authorization code` along with the generated `code_verifier` along with `state` and `nonce` if required, and do client assertion to call the token endpoint to obtain ID/access tokens.
<br><br>
- 3c) Token endpoint responds with the token payload to **RP Backend**.
  <br><br>
- 3d) **RP Backend** process the token payload and does its required operations and responds to **RP Mobile App** with the appropriate session state tokens or data. #
  <br><br>

&#8203;* - Take note that the `redirect_uri` should be a non-https url that represents the app link of the **RP Mobile App** as configured in the [AppAuth](https://github.com/openid/AppAuth-Android) library as shown in the [AndroidManifest.xml](#In-the-AndroidManifest.xml) implementation.

&#8203;# - It is up to the RP to secure the connection between **RP Mobile App** and **RP Backend**

# Potential changes/enhancements for RP Backend
1. Implement endpoint to serve `code_challenge`, `code_challenge_method`, `state`, `nonce` and other parameters needed for **RP Mobile App** to initiate the login flow.
   <br><br>
2. Implement endpoint in receive `authorization code`, `state` and other required parameters.

# Potential changes/enhancements for RP Mobile App
1. Integrate [AppAuth](https://github.com/openid/AppAuth-Android) library to handle launching of authorization endpoint webpage in a [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/#:~:text=Custom%20Tabs%20is%20a%20browser,to%20resort%20to%20a%20WebView.) or external mobile web browser.
   <br><br>
2. Implement api call to **RP Backend** to request for `code_challenge`, `code_challenge_method`, `state` and `nonce` if required and other parameters.
   <br><br>
3. Implement api call to send `authorization code`, `state` and other needed parameters. back to **RP Backend**.

# Other Notes
- Do **NOT** use the query param `app_launch_url` when opening the authorization endpoint webpage for Android as it will break the flow with [AppAuth](https://github.com/openid/AppAuth-Android) library.
  <br><br>
- Recommended to **NOT** use `redirect_uri` with a `https` scheme e.g. https://rp.redirectUri/callback due to potential UX issues when redirecting back to **RP Mobile App** from the Chrome Custom Tabs or external web browser. Use [Android DeepLinks](https://developer.android.com/training/app-links#deep-links) instead as the `redirect_uri`. e.g. sg.gov.singpass.app://ndisample.gov.sg/rp/sample

# Implementation Details

---

## Required dependencies

AppAuth Android Library
> implementation "net.openid:appauth:0.11.1"

Androidx Browser (Chrome Custom Tabs)
> implementation "androidx.browser:browser:1.5.0"

## Implementation

### In the AndroidManifest.xml

Configure [AppAuth](https://github.com/openid/AppAuth-Android) RedirectUriReceiverActivity's  `IntentFilter` in AndroidManifest which is also the `redirect_uri`. 
```xml
<activity
    android:name="net.openid.appauth.RedirectUriReceiverActivity"
    android:exported="true"
    tools:node="replace">
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="sg.gov.singpass.app"
            android:host="ndisample.gov.sg"
            android:path="/rp/sample"/>
    </intent-filter>

    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="sg.gov.singpass.app"
            android:host="ndisample.gov.sg"
            android:path="/rp/sample/"/>
    </intent-filter>
    
</activity>
```
<br>

### In the ViewModel

The below code snippets should be inside a ViewModel or any other component that survives an orientation change in an Android application.

<br>

Create the Oauth service configuration
```kotlin
  // This is the json string that describes the current Oauth service
  // This example is using the test environment for MyInfo Singpass login 
  // Todo: Modify these values for your use-case e.g. Singpass, MyInfo etc 
  val jsonConfig = "{" +
    "\"issuer\":\"https://test.api.myinfo.gov.sg\"," +
    "\"authorizationEndpoint\":\"https://test.api.myinfo.gov.sg/com/v4/authorize\"," +
    "\"tokenEndpoint\":\"https://test.api.myinfo.gov.sg/com/v4/token\"" +
  "}"
  
  val serviceConfig = AuthorizationServiceConfiguration.fromJson(jsonConfig)
```
<br>

Create the OAuth authorization request
```kotlin
val authRequest = AuthorizationRequest.Builder(
  serviceConfig, // from the above section
  client_id, // RP client_id
  ResponseTypeValues.CODE, // code
  Uri.parse(refirect_uri) // redirect_uri
).apply {
    
  val additionalParams = mutableMapOf<String, String>()

  // MyInfo Singpass login does not need nonce and state
  // It needs purpose_id and has different scope values
  if (isMyinfo) {
    setScope(app.getString(R.string.myinfo_scope))
    additionalParams.put("purpose_id", "demonstration")
    setNonce(null)
    setState(null)
  } else {
    setScope("openid")
    setState(state) // state generated from RP Backend
    setNonce(nonce) // nonce generated from RP Backend
  }

  // code_challenge and code_challenge_method generated from RP Backend
  // Set code_challenge for code_verifier as AppAuth library
  // does NOT natively support externally generated code_verifier
  // Set code_challenge as code_verifier as a hack       
  // as we are not calling token endpoint from the mobile app        
  setCodeVerifier(code_challenge, code_challenge, code_challenge_method)

  if (additionalParams.isNotEmpty()) {
    setAdditionalParameters(additionalParams)
  }
}.build()
```
<br>

Create the OAuth authorization service
```kotlin
val authService = AuthorizationService(applicationContext)
```
<br>

Create the Intent to launch the Authorization Endpoint in a Chrome Custom Tab or external web browser
```kotlin

// Todo: Modify to make the custom tabs fit your application theme for light mode
private val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
  val toolbarColor = ContextCompat.getColor(app, R.color.primary)
  setToolbarColor(toolbarColor)
  setSecondaryToolbarColor(toolbarColor)
}.build()

// Todo: Modify to make the custom tabs fit your application theme for dark mode
private val darkCustomTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
  val toolbarColor = ContextCompat.getColor(app, R.color.grey60)
  setToolbarColor(toolbarColor)
  setSecondaryToolbarColor(toolbarColor)
}.build()

// Create the custom tabs intent with CustomTabsIntent.Builder
// Modify how you want the custom tabs to look using the androidx.browser api
// This builder will also function to warm up the custom tabs in the background for faster custom tabs launching
val customTabsIntent = authService.customTabManager.createTabBuilder(authRequest.toUri()).apply {
  setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkCustomTabColorSchemeParams)
  setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, customTabColorSchemeParams)
  setShowTitle(true)
  setStartAnimations(app, android.R.anim.slide_in_left, android.R.anim.fade_out)
  setExitAnimations(app, android.R.anim.fade_in, android.R.anim.slide_out_right)
}.build()

try {
  authIntent = authService.getAuthorizationRequestIntent(authRequest, customTabsIntent)
} catch (e: ActivityNotFoundException) {
//  Todo: This toast here is just to indicate the error, please show your own error UI 
  Toast.makeText(app, "No suitable web browser found!", Toast.LENGTH_SHORT).show()
}
```

## In the UI Layer (Activity or Fragment)

<br>

Create an `authActivityLauncher` in your Activity or Fragment.
The `authActivityLauncher` will listen for the authorization code or any errors returned from the Chrome Custom Tabs or external web browser
```kotlin
val authActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    val data = it.data
    if (data != null) {
        val resp = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)

        if (ex != null) { 
            // Todo: This toast here is just to indicate the error, please show your own error UI 
            Toast.makeText(app, "Error occurred: ${ex.errorDescription}", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        if (resp != null) {
            // Todo: obtain the authorization code and state and send to RP Backend 
            viewModel.sendAuthCodeToBackend(
                code = resp.authorizationCode ?: "",
                state = resp.state
            )
        }
    }
}
```
<br>

Launch the authorization Intent created in the viewModel
```kotlin
viewModel.authIntent?.run {   
    authActivityLauncher.launch(this)
} ?:
// Todo: This toast here is just to indicate the error, please show your own error UI 
Toast.makeText(app, "Error occurred: Intent is null!", Toast.LENGTH_SHORT).show()
```

## Demo Video/s

MyInfo Mockpass Demo

<img src="MyInfo%20PKCE%20flow.gif" alt="Myinfo Mockpass flow video" width="300px" height="666.5px"></img>
