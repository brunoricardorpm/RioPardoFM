package br.com.britoinformatica.www.riopardofm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import br.com.britoinformatica.www.riopardofm.ui.theme.RadioRioPardoTheme
import br.com.britoinformatica.www.radioriopardomg.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) { 
            Log.d("AdMob", "SDK Inicializado")
        }

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controller ?: return@addListener
            if (controller.mediaItemCount == 0) {
                val streamUrl = "https://stm25.srvstm.com:7488/stream"
                val mediaItem = MediaItem.fromUri(streamUrl)
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.playWhenReady = true
            }
        }, MoreExecutors.directExecutor())

        enableEdgeToEdge()
        setContent {
            RadioRioPardoTheme {
                val controllerState = remember { mutableStateOf<Player?>(null) }
                
                DisposableEffect(Unit) {
                    controllerFuture?.addListener({
                        controllerState.value = controller
                    }, MoreExecutors.directExecutor())
                    onDispose {}
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    controllerState.value?.let { player ->
                        RadioPlayerScreen(
                            player = player,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }
}

@OptIn(UnstableApi::class)
@Composable
fun RadioPlayerScreen(player: Player, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val logoSize = if (screenHeight < 600.dp) 160.dp else 240.dp
    val playerButtonSize = if (screenHeight < 600.dp) 100.dp else 120.dp
    
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasInternet by remember { mutableStateOf(isNetworkAvailable(context)) }
    var currentTitle by remember { mutableStateOf("") }
    var currentArtist by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while(true) {
            hasInternet = isNetworkAvailable(context)
            delay(3000)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                currentTitle = mediaMetadata.title?.toString() ?: ""
                currentArtist = mediaMetadata.artist?.toString() ?: ""
                coverUrl = mediaMetadata.artworkUri?.toString()
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        isLoading = player.playbackState == Player.STATE_BUFFERING
        currentTitle = player.mediaMetadata.title?.toString() ?: ""
        currentArtist = player.mediaMetadata.artist?.toString() ?: ""
        coverUrl = player.mediaMetadata.artworkUri?.toString()
        
        onDispose { player.removeListener(listener) }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !hasInternet -> Color(0xFFD32F2F) 
            isPlaying -> Color(0xFF388E3C)    
            else -> Color(0xFF1976D2)         
        },
        animationSpec = tween(durationMillis = 500),
        label = "bgColorAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radioWaves")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.backgraund),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.4f))
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (coverUrl != null && isPlaying) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Capa da Música",
                        modifier = Modifier
                            .size(logoSize)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_radio),
                        placeholder = painterResource(id = R.drawable.logo_radio)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_radio),
                        contentDescription = "Logo Rádio Rio Pardo",
                        modifier = Modifier
                            .size(logoSize)
                            .padding(bottom = 16.dp)
                    )
                }

                if (currentTitle.isNotEmpty() && isPlaying) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = currentArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    Text(
                        text = when {
                            !hasInternet -> "Sem Conexão com a Internet"
                            isPlaying -> stringResource(R.string.radio_status_playing)
                            else -> stringResource(R.string.radio_status_stopped)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(playerButtonSize)
                                .scale(waveScale)
                                .background(Color.White.copy(alpha = waveAlpha), CircleShape)
                        )
                    }

                    if (isLoading && hasInternet) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(80.dp))
                    } else {
                        Button(
                            onClick = {
                                if (isPlaying) player.pause() else player.play()
                            },
                            modifier = Modifier.size(playerButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = backgroundColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(playerButtonSize / 2)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = {
                        player.stop()
                        context.stopService(Intent(context, PlaybackService::class.java))
                        (context as? MainActivity)?.finish()
                    },
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val phoneNumber = "5538991393210"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$phoneNumber")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("WhatsApp", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val email = "norteriofm@gmail.com"
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp).clip(MaterialTheme.shapes.medium).background(Color.White)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black)
                    }

                    IconButton(
                        onClick = {
                            val url = "https://www.youtube.com/@RIOPARDOFM."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFFFF0000))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            val url = "https://www.instagram.com/riopardofm/"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp).clip(MaterialTheme.shapes.medium).background(Color.White)
                    ) {
                        Image(painter = painterResource(id = R.drawable.ic_instagram), contentDescription = "Instagram", modifier = Modifier.size(30.dp))
                    }

                    IconButton(
                        onClick = {
                            val url = "https://www.facebook.com/riopardofm"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFF1877F2))
                    ) {
                        Icon(Icons.Default.Facebook, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // BANNER ADMOB FINAL (COM SEU ID REAL)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            AdView(context).apply {
                                setAdSize(AdSize.BANNER)
                                // SEU ID REAL
                                adUnitId = "ca-app-pub-3327624911875523/1690797329"
                                adListener = object : AdListener() {
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        Log.e("AdMob", "Falha: ${error.message} (Código: ${error.code})")
                                    }
                                    override fun onAdLoaded() {
                                        Log.d("AdMob", "Banner carregado com sucesso!")
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
