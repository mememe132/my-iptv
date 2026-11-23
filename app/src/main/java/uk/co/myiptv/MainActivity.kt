
package uk.co.myiptv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.text.InputType
import android.widget.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.Executors

private data class Channel(
    val name: String,
    val url: String
)

class MainActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val channels = mutableListOf<Channel>()
    private val channelNames = mutableListOf<String>()

    private var player: ExoPlayer? = null
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var status: TextView
    private lateinit var playlistInput: EditText

    private val navy = Color.rgb(8, 18, 42)
    private val blue = Color.rgb(20, 110, 220)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLibrary()
    }

    private fun showLibrary() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 18)
            setBackgroundColor(navy)
        }

        val heading = TextView(this).apply {
            text = "MY IPTV"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 18)
        }
        root.addView(heading)

        playlistInput = EditText(this).apply {
            hint = "Paste your M3U playlist URL"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
inputType = InputType.TYPE_CLASS_TEXT or
    InputType.TYPE_TEXT_VARIATION_URI
        }
        root.addView(playlistInput)

        val loadButton = Button(this).apply {
            text = "LOAD PLAYLIST"
            setBackgroundColor(blue)
            setTextColor(Color.WHITE)
        }
        root.addView(loadButton)

        status = TextView(this).apply {
            text = "Enter your playlist address to begin"
            textSize = 14f
            setTextColor(Color.CYAN)
            setPadding(4, 14, 4, 14)
        }
        root.addView(status)

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            channelNames
        )

        val list = ListView(this).apply {
            this.adapter = this@MainActivity.adapter
            setBackgroundColor(navy)
            dividerHeight = 1
        }

        list.setOnItemClickListener { _, _, position, _ ->
            playChannel(channels[position])
        }

        root.addView(
            list,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        loadButton.setOnClickListener {
            val keyboard = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            keyboard.hideSoftInputFromWindow(
                playlistInput.windowToken, 0
            )

            loadPlaylist(playlistInput.text.toString().trim())
        }

        setContentView(root)
    }

    private fun loadPlaylist(address: String) {

        if (!address.startsWith("http://") &&
            !address.startsWith("https://")) {
            status.text = "Please enter a valid playlist URL"
            return
        }

        status.text = "Loading playlist..."

        executor.execute {
            try {
                val connection =
                    URL(address).openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.requestMethod = "GET"

                val contents = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val found = parsePlaylist(contents)

                runOnUiThread {
                    channels.clear()
                    channels.addAll(found)

                    channelNames.clear()
                    channelNames.addAll(found.map { it.name })

                    adapter.notifyDataSetChanged()

                    status.text =
                        if (found.isEmpty()) {
                            "No channels found in playlist"
                        } else {
                            "${found.size} channels loaded"
                        }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text =
                        "Playlist error: ${e.message}"
                }
            }
        }
    }

    private fun parsePlaylist(contents: String): List<Channel> {

        val result = mutableListOf<Channel>()
        var pendingName: String? = null

        contents.lineSequence().forEach { rawLine ->

            val line = rawLine.trim()

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val comma = line.indexOf(',')

                pendingName =
                    if (comma >= 0) {
                        line.substring(comma + 1).trim()
                    } else {
                        "Live Channel"
                    }

            } else if (
                line.isNotEmpty() &&
                !line.startsWith("#") &&
                pendingName != null
            ) {
                result.add(
                    Channel(
                        pendingName!!.ifBlank { "Live Channel" },
                        line
                    )
                )
                pendingName = null
            }
        }

        return result
    }

    
private fun playChannel(channel: Channel) {
    try {
        player?.release()
        player = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
        }

        val backButton = Button(this).apply {
            text = "← BACK TO CHANNELS"
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            setOnClickListener {
                player?.release()
                player = null
                showLibrary()
            }
        }
        root.addView(backButton)

        val title = TextView(this).apply {
            text = channel.name
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(10, 12, 10, 12)
        }
        root.addView(title)

        val message = TextView(this).apply {
            text = "Starting stream..."
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        root.addView(message)

        val video = PlayerView(this).apply {
            useController = true
        }

        root.addView(
            video,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        val newPlayer = ExoPlayer.Builder(this).build()
        player = newPlayer
        video.player = newPlayer

        newPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                message.text =
                    "Playback error: ${error.errorCodeName}"
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    message.text = ""
                } else if (state == Player.STATE_BUFFERING) {
                    message.text = "Buffering..."
                }
            }
        })

        newPlayer.setMediaItem(MediaItem.fromUri(channel.url))
        newPlayer.prepare()
        newPlayer.playWhenReady = true

    } catch (e: Exception) {
        player?.release()
        player = null
        showLibrary()
        status.text =
            "Player error: ${e.javaClass.simpleName}: ${e.message}"
    }
}


    override fun onDestroy() {
        player?.release()
        player = null
        executor.shutdownNow()
        super.onDestroy()
    }
}
