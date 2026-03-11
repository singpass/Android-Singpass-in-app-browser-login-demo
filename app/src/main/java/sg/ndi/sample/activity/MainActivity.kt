package sg.ndi.sample.activity

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.singpass.login.model.LoginParams
import com.singpass.login.util.SingpassLoginActivityContract
import kotlinx.coroutines.launch
import sg.ndi.sample.R
import sg.ndi.sample.model.AUTH_TYPE
import sg.ndi.sample.model.LaunchAuthState
import sg.ndi.sample.model.ListItems
import sg.ndi.sample.ui.theme.NDIRpSampleTheme
import sg.ndi.sample.ui.theme.singpassred
import sg.ndi.sample.viewmodel.MainActivityViewModel

class MainActivity : ComponentActivity() {

    private lateinit var singpassLoginActivityResultLauncher: ActivityResultLauncher<LoginParams>

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        Log.d("onDestroy", this::class.java.simpleName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets -> insets }

        singpassLoginActivityResultLauncher = registerForActivityResult(
            SingpassLoginActivityContract()
        ) {
            if (it.error.isNullOrBlank()) {
                if (it.error == "Result intent is null") {
                    viewModel.updateAuthCode(MainActivityViewModel.ERROR_AUTH_CODE_TEXT.format(it.error))
                    viewModel.enableBackButtons()
                } else {
                    viewModel.updateAuthCode(MainActivityViewModel.AUTH_CODE_OBTAINED_TEXT.format(it.code))
                    viewModel.sendAuthCodeToBackend(state = it.state, code = it.code)
                }
            } else {
                viewModel.updateAuthCode(MainActivityViewModel.ERROR_AUTH_CODE_TEXT.format(it.error))
                viewModel.enableBackButtons()
            }
        }

        lifecycleScope.launch {
            viewModel.authState
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect {
                    when(it) {
                        is LaunchAuthState.LOGGED_OUT,
                        is LaunchAuthState.LOGIN_SUCCESS,
                        LaunchAuthState.INITIAL -> Unit
                        is LaunchAuthState.LAUNCH -> {
                            if (!it.launched) {
                                viewModel.consumeAuthorizationWebPageTrigger()
                                singpassLoginActivityResultLauncher.launch(it.singpassLoginParam)
                            }
                        }
                    }
                }
        }

        setContent {
            NDIRpSampleTheme {

                var useAppAuthOnlyChecked by remember { mutableStateOf(false) }
                var useFapi2 by remember { mutableStateOf(false) }
                val screenState = viewModel.authState.collectAsStateWithLifecycle()

                MainScreen(
                    authCode = viewModel.authCodeState.value,
                    idToken = viewModel.idTokenState.value,
                    buttonEnabled = viewModel.buttonEnabledState.value,
                    spmInstalled = viewModel.spmInstalledState.value,
                    useAppAuthOnly = useAppAuthOnlyChecked,
                    useFapi2 = useFapi2,
                    screenState = screenState.value,
                    refreshButtonClick = {
                        viewModel.reset()
                    },
                    loginButtonClick = {
                        if (useFapi2) {
                            viewModel.getFapiRequestUri(AUTH_TYPE.SINGPASS, useAppAuthOnlyChecked)
                        } else {
                            viewModel.getPkceParams(AUTH_TYPE.SINGPASS, useAppAuthOnlyChecked)
                        }
                    },
                    userinfoButtonClick = {
                        if (useFapi2) {
                            viewModel.getFapiRequestUri(AUTH_TYPE.USERINFO, useAppAuthOnlyChecked)
                        } else {
                            viewModel.getPkceParams(AUTH_TYPE.USERINFO, useAppAuthOnlyChecked)
                        }
                    },
                    sfvButtonClick = {
                        if (useFapi2) {
                            viewModel.getFapiRequestUri(AUTH_TYPE.SFV, useAppAuthOnlyChecked)
                        } else {
                            viewModel.getPkceParams(AUTH_TYPE.SFV, useAppAuthOnlyChecked)
                        }
                    },
                    useAppAuthOnlyCheckChange = {
                        useAppAuthOnlyChecked = it
                    },
                    useFapi2CheckChange = {
                        useFapi2 = it
                    },
                    logout = { viewModel.logout() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateSpmInstalledState()
    }
}

@Composable
private fun MainScreen(
    authCode: String,
    idToken: String,
    buttonEnabled: Boolean,
    spmInstalled: Boolean,
    useAppAuthOnly: Boolean,
    useFapi2: Boolean,
    screenState: LaunchAuthState,
    refreshButtonClick: () -> Unit,
    loginButtonClick: () -> Unit,
    userinfoButtonClick: () -> Unit,
    sfvButtonClick: () -> Unit,
    useAppAuthOnlyCheckChange: (Boolean) -> Unit,
    useFapi2CheckChange: (Boolean) -> Unit,
    logout: () -> Unit
) {

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
                modifier = Modifier.systemGestureExclusion(),
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    when (screenState) {
                        LaunchAuthState.INITIAL -> Unit
                        is LaunchAuthState.LAUNCH,
                        LaunchAuthState.LOGGED_OUT -> {
                            IconButton(
                                enabled = buttonEnabled,
                                onClick = refreshButtonClick
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "reset")
                            }
                        }
                        is LaunchAuthState.LOGIN_SUCCESS -> {
                            IconButton(
                                enabled = true,
                                onClick = logout
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "logout")
                            }
                        }
                    }
                }
            )
        }
    ) { contentPadding ->

        Log.d(
            "SCAFFOLD",
            "contentpadding bottom inset = ${
                WindowInsets.safeDrawing.getBottom(LocalDensity.current)
            }"
        )
        Log.d("SCAFFOLD", "contentpadding bottom = ${contentPadding.calculateBottomPadding()}")
        Log.d("SCAFFOLD", "contentpadding top = ${contentPadding.calculateTopPadding()}")
        Log.d(
            "SCAFFOLD",
            "contentpadding start = ${contentPadding.calculateStartPadding(LocalLayoutDirection.current)}"
        )
        Log.d(
            "SCAFFOLD",
            "contentpadding end = ${contentPadding.calculateEndPadding(LocalLayoutDirection.current)}"
        )
        Log.d("SCAFFOLD", "---------------------------------------")

        AnimatedContent(screenState) {
            when (it) {
                is LaunchAuthState.INITIAL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is LaunchAuthState.LAUNCH,
                is LaunchAuthState.LOGGED_OUT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    ) {
                        LazyColumn(
                            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                .asPaddingValues(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item(key = ListItems.INSTRUCTION) {
                                Text(
                                    text = if (spmInstalled) "Tap button to start login with Singpass app" else "Tap on button to start login",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.h3,
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 24.dp)
                                        .animateContentSize()
                                        .animateItem()
                                )
                            }

                            item(key = ListItems.APPAUTH_CHECKBOX) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Use AppAuth only"
                                        )
                                        Checkbox(
                                            checked = useAppAuthOnly,
                                            onCheckedChange = useAppAuthOnlyCheckChange,
                                        )
                                    }
                                }
                            }

                            item(key = ListItems.FAPI_CHECKBOX) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "FAPI2"
                                    )
                                    Checkbox(
                                        checked = useFapi2,
                                        onCheckedChange = useFapi2CheckChange,
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            item(key = ListItems.SINGPASS_BUTTON) {
                                LoginButton(
                                    Modifier.animateItem(),
                                    buttonEnabledState = buttonEnabled,
                                    buttonImage = R.drawable.singpass_logo,
                                    onClick = loginButtonClick
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = ListItems.SFV_BUTTON) {
                                SfvButton(
                                    Modifier.animateItem(),
                                    buttonEnabledState = buttonEnabled,
                                    buttonImage = R.drawable.singpass_logo,
                                    onClick = sfvButtonClick
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = ListItems.USERINFO_BUTTON) {
                                UserInfoButton(
                                    Modifier.animateItem(),
                                    buttonEnabledState = buttonEnabled,
                                    buttonImage = R.drawable.singpass_logo,
                                    onClick = userinfoButtonClick
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = ListItems.AUTHCODE) {

                                Spacer(
                                    Modifier
                                        .height(12.dp)
                                        .fillMaxWidth()
                                )

                                Text(
                                    text = authCode,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 24.dp)
                                        .animateItem()
                                )
                            }

                            item(key = ListItems.IDTOKEN) {
                                SelectionContainer {
                                    Text(
                                        text = idToken,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onBackground,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 24.dp)
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
                is LaunchAuthState.LOGIN_SUCCESS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                style = MaterialTheme.typography.h2,
                                text = "Welcome ${it.uid}",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginButton(
    modifier: Modifier = Modifier,
    buttonEnabledState: Boolean,
    @DrawableRes buttonImage: Int,
    onClick: () -> Unit
) {

    val buttonColor = if(MaterialTheme.colors.isLight) {
        Color.White
    } else {
        singpassred
    }

    val alpha: Float by animateFloatAsState(if (buttonEnabledState) 1f else 0.5f)

    Button(
        modifier = Modifier
            .graphicsLayer(alpha = alpha)
            .then(modifier),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonColor,
            disabledBackgroundColor = MaterialTheme.colors.surface
        ),
        border = BorderStroke(1.dp, colorResource(R.color.singpass_btn_border)),
        enabled = buttonEnabledState,
        onClick = onClick
    ) {
        val painter = painterResource(id = buttonImage)

        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Log in with", color = colorResource(R.color.singpass_text))
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painter,
                contentDescription = null,
                modifier
                    .height(getButtonFontHeight())
                    .padding(top = 2.dp)
            )
        }
    }
}

/**
 * Used for setting dp to font size to make it unscalable
 */
@Composable
fun Float.dpToSp() = with(LocalDensity.current) {
    this@dpToSp.dp.toSp()
}

@Composable
private fun getButtonFontHeight(isBtnFixedSize: Boolean = false): Dp {
    return with(LocalDensity.current) {

        val lineHeight = if (isBtnFixedSize) MaterialTheme.typography.button.lineHeight.value.dpToSp()
        else MaterialTheme.typography.button.lineHeight

        with(lineHeight) {
            when {
                isSp -> toDp()

                isEm -> {
                    lineHeight.toPx().toDp()
                }

                else -> 20.dp
            }
        }
    }
}

@Composable
private fun UserInfoButton(
    modifier: Modifier = Modifier,
    buttonEnabledState: Boolean,
    @DrawableRes buttonImage: Int,
    onClick: () -> Unit
) {

    val alpha: Float by animateFloatAsState(if (buttonEnabledState) 1f else 0.5f)

    val buttonColor = if (MaterialTheme.colors.isLight) {
        Color.White
    } else {
        singpassred
    }
    Column {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "MyInfo V5"
        )
        Button(
            modifier = Modifier
                .graphicsLayer(alpha = alpha)
                .then(modifier),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = buttonColor,
                disabledBackgroundColor = MaterialTheme.colors.surface
            ),
            border = BorderStroke(1.dp, colorResource(R.color.singpass_btn_border)),
            enabled = buttonEnabledState,
            onClick = onClick
        ) {
            val painter = painterResource(id = buttonImage)
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Log in with")
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = modifier
                        .height(getButtonFontHeight())
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SfvButton(
    modifier: Modifier = Modifier,
    buttonEnabledState: Boolean,
    @DrawableRes buttonImage: Int,
    onClick: () -> Unit
) {

    val alpha: Float by animateFloatAsState(if (buttonEnabledState) 1f else 0.5f)

    val buttonColor = if(MaterialTheme.colors.isLight) {
        Color.White
    } else {
        singpassred
    }

    Column {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Login (SFV)"
        )
        Button(
            modifier = Modifier
                .graphicsLayer(alpha = alpha)
                .then(modifier),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = buttonColor,
                disabledBackgroundColor = MaterialTheme.colors.surface
            ),
            border = BorderStroke(1.dp, colorResource(R.color.singpass_btn_border)),
            enabled = buttonEnabledState,
            onClick = onClick
        ) {
            val painter = painterResource(id = buttonImage)
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Login with")
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = modifier
                        .height(getButtonFontHeight())
                        .padding(top = 2.dp)

                )
            }
        }
    }


}

@Preview(showBackground = true)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun InitialPreview() {
    NDIRpSampleTheme {
        val authCodeState = remember { mutableStateOf("Sample auth code!") }
        val idTokenState = remember { mutableStateOf("Sample idtoken code!") }
        val buttonEnabledState = remember { mutableStateOf(true) }
        val spmInstalledState = remember { mutableStateOf(false) }

        MainScreen(
            authCode = authCodeState.value,
            idToken = idTokenState.value,
            buttonEnabled = buttonEnabledState.value,
            spmInstalled = spmInstalledState.value,
            useAppAuthOnly = false,
            useFapi2 = false,
            screenState = LaunchAuthState.INITIAL,
            refreshButtonClick = { },
            loginButtonClick = {
                spmInstalledState.value = spmInstalledState.value.not()
            },
            userinfoButtonClick = { },
            sfvButtonClick = { },
            useAppAuthOnlyCheckChange = { },
            useFapi2CheckChange = { },
            logout = { }
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoggedOutPreview() {
    NDIRpSampleTheme {
        val authCodeState = remember { mutableStateOf("Sample auth code!") }
        val idTokenState = remember { mutableStateOf("Sample idtoken code!") }
        val buttonEnabledState = remember { mutableStateOf(true) }
        val spmInstalledState = remember { mutableStateOf(false) }

        MainScreen(
            authCode = authCodeState.value,
            idToken = idTokenState.value,
            buttonEnabled = buttonEnabledState.value,
            spmInstalled = spmInstalledState.value,
            useAppAuthOnly = false,
            useFapi2 = false,
            screenState = LaunchAuthState.LOGGED_OUT,
            refreshButtonClick = { },
            loginButtonClick = {
                spmInstalledState.value = spmInstalledState.value.not()
            },
            userinfoButtonClick = { },
            sfvButtonClick = { },
            useAppAuthOnlyCheckChange = { },
            useFapi2CheckChange = { },
            logout = { }
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginPreview() {
    NDIRpSampleTheme {
        val authCodeState = remember { mutableStateOf("Sample auth code!") }
        val idTokenState = remember { mutableStateOf("Sample idtoken code!") }
        val buttonEnabledState = remember { mutableStateOf(true) }
        val spmInstalledState = remember { mutableStateOf(false) }

        MainScreen(
            authCode = authCodeState.value,
            idToken = idTokenState.value,
            buttonEnabled = buttonEnabledState.value,
            spmInstalled = spmInstalledState.value,
            useAppAuthOnly = false,
            useFapi2 = false,
            screenState = LaunchAuthState.LOGIN_SUCCESS("S1234567A"),
            refreshButtonClick = { },
            loginButtonClick = {
                spmInstalledState.value = spmInstalledState.value.not()
            },
            userinfoButtonClick = { },
            sfvButtonClick = { },
            useAppAuthOnlyCheckChange = { },
            useFapi2CheckChange = { },
            logout = { }
        )
    }
}