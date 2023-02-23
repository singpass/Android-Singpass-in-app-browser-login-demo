package sg.ndi.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import sg.ndi.sample.MainActivityViewModel.Companion.AUTH_CODE_OBTAINED_TEXT
import sg.ndi.sample.MainActivityViewModel.Companion.ERROR_AUTH_CODE_TEXT
import sg.ndi.sample.MainActivityViewModel.Companion.WAITING_AUTH_CODE_TEXT
import sg.ndi.sample.ui.theme.NDIRpSampleTheme

class MainActivity : ComponentActivity() {

    private lateinit var authActivityLauncher: ActivityResultLauncher<Intent>

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        authActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                val resp = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                if (ex != null) {
                    viewModel.updateAuthCode(ERROR_AUTH_CODE_TEXT.format(ex.errorDescription))
                    viewModel.enableBackButtons()
                    return@registerForActivityResult
                }

                if (resp != null) {
                    viewModel.updateAuthCode(AUTH_CODE_OBTAINED_TEXT.format(resp.authorizationCode))
                    
                    viewModel.sendAuthCodeToBackend(
                        code = resp.authorizationCode ?: "",
                        state = resp.state
                    )
                }
            }
        }

        lifecycleScope.launch {
            viewModel.launchAuthorizationWebPage
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect {
                    if (it) {
                        doAuthorization()
                    }
                }
        }

        setContent {
            NDIRpSampleTheme {
                MainScreen(
                    authCodeState = viewModel.authCodeState,
                    idTokenState = viewModel.idTokenState,
                    buttonEnabledState = viewModel.buttonEnabledState,
                    spmInstalledState = viewModel.spmInstalledState,
                    refreshButtonClick = {
                        viewModel.reset()
                    },
                    loginButtonClick = {
                        viewModel.createAuthorizationServiceIntent(it)
                    }
                )
            }
        }
    }

    private fun doAuthorization() {
        viewModel.authIntent?.run {
            viewModel.consumeAuthorizationWebPageTrigger()
            viewModel.updateAuthCode(WAITING_AUTH_CODE_TEXT)
            authActivityLauncher.launch(this)
        } ?: viewModel.updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Authorization intent is not initialized!"))
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateSpmInstalledState()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    authCodeState: State<String>,
    idTokenState: State<String>,
    buttonEnabledState: State<Boolean>,
    spmInstalledState: State<Boolean>,
    refreshButtonClick: () -> Unit,
    loginButtonClick: (Boolean) -> Unit
) {

    val systemUiController = rememberSystemUiController()
    val authCode = remember { authCodeState }
    val idToken = remember { idTokenState }

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top).asPaddingValues(),
                actions = {
                    IconButton(
                        enabled = buttonEnabledState.value,
                        onClick = refreshButtonClick
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "reset")
                    }
                }
            )
        }
    ) { contentPadding ->

        Log.d("SCAFFOLD", "contentpadding bottom inset = ${WindowInsets.safeDrawing.getBottom(LocalDensity.current)}")
        Log.d("SCAFFOLD", "contentpadding bottom = ${contentPadding.calculateBottomPadding()}")
        Log.d("SCAFFOLD", "contentpadding top = ${contentPadding.calculateTopPadding()}")
        Log.d("SCAFFOLD", "contentpadding start = ${contentPadding.calculateStartPadding(LocalLayoutDirection.current)}")
        Log.d("SCAFFOLD", "contentpadding end = ${contentPadding.calculateEndPadding(LocalLayoutDirection.current)}")
        Log.d("SCAFFOLD", "---------------------------------------")

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            color = MaterialTheme.colors.background
        ) {
            LazyColumn(
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = ListItems.INSTRUCTION) {
                    Text(
                        text = if (spmInstalledState.value) "Tap on button to start login with SPM" else "Tap on button to start login",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h3,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                            .animateContentSize()
                            .animateItemPlacement()
                    )
                }

                item(key = ListItems.SINGPASS_BUTTON) {
                    LoginButton(
                        Modifier.animateItemPlacement(),
                        buttonEnabledState = buttonEnabledState,
                        buttonImage = R.drawable.neutral_login_button,
                        onClick = { loginButtonClick(false) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item(key = ListItems.MYINFO_BUTTON) {
                    LoginButton(
                        Modifier.animateItemPlacement(),
                        buttonEnabledState = buttonEnabledState,
                        buttonImage = R.drawable.myinfo_logo,
                        onClick = { loginButtonClick(true) }
                    )
                }

                item(key = ListItems.AUTHCODE) {

                    Spacer(
                        Modifier
                            .height(12.dp)
                            .fillMaxWidth()
                    )

                    Text(
                        text = authCode.value,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                            .animateItemPlacement()
                    )
                }

                item(key = ListItems.IDTOKEN) {
                    SelectionContainer {
                        Text(
                            text = idToken.value,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 24.dp)
                                .animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}

enum class ListItems {
    SINGPASS_BUTTON, MYINFO_BUTTON, INSTRUCTION, AUTHCODE, IDTOKEN
}

@Composable
private fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun LoginButton(
    modifier: Modifier = Modifier,
    buttonEnabledState: State<Boolean>,
    @DrawableRes buttonImage: Int,
    onClick: () -> Unit
) {

    val alpha: Float by animateFloatAsState(if (buttonEnabledState.value) 1f else 0.5f)

    Button(
        modifier = Modifier
            .graphicsLayer(alpha = alpha)
            .then(modifier),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.surface,
            disabledBackgroundColor = MaterialTheme.colors.surface
        ),
        enabled = buttonEnabledState.value,
        onClick = onClick
    ) {

        val painter = painterResource(id = buttonImage)
        Image(
            painter = painter,
            contentDescription = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NDIRpSampleTheme {
        val authCodeState = remember { mutableStateOf("Sample auth code!") }
        val idTokenState = remember { mutableStateOf("Sample idtoken code!") }
        val buttonEnabledState = remember { mutableStateOf(true) }
        val spmInstalledState = remember { mutableStateOf(false) }

        MainScreen(
            authCodeState = authCodeState,
            idTokenState = idTokenState,
            buttonEnabledState = buttonEnabledState,
            spmInstalledState = spmInstalledState,
            refreshButtonClick = { }
        ) {
            spmInstalledState.value = spmInstalledState.value.not()
        }
    }
}
