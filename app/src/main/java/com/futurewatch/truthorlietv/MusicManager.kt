package com.futurewatch.truthorlietv

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context

object MusicManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentGenre = "Chill Lounge"
    private var isEnabled = true
    private var isPaused = false
    private var isStarted = false
    private var appContext: android.content.Context? = null

    private val musicResources = mapOf(
        "Party Vibes" to R.raw.music_party,
        "Chill Lounge" to R.raw.music_chill,
        "Retro Arcade" to R.raw.music_retro,
        "Epic Adventure" to R.raw.music_epic,
        "Funky Groove" to R.raw.music_funky
    )

    fun init(context: Context) {
        if (appContext != null) return

        appContext = context.applicationContext

        val prefs = appContext?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        isEnabled = prefs?.getBoolean("music_enabled", true) ?: true
        currentGenre = prefs?.getString("music_genre", "Chill Lounge") ?: "Chill Lounge"

        Log.d("MusicManager", "Initialized")

        // ✅ App lifecycle listener (THIS is the important part)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onStop(owner: LifecycleOwner) {
                Log.d("MusicManager", "App in background → stopping music")
                stopMusic()
            }

            override fun onStart(owner: LifecycleOwner) {
                Log.d("MusicManager", "App in foreground → starting music")
                if (isEnabled) {
                    startMusic()
                }
            }
        })
    }

    fun startMusic() {
        if (!isEnabled) {
            Log.d("MusicManager", "Music is disabled in settings")
            return
        }

        if (isStarted && mediaPlayer?.isPlaying == true) {
            Log.d("MusicManager", "Music already playing")
            return
        }

        val context = appContext
        if (context == null) {
            Log.e("MusicManager", "Context is null, cannot start music")
            return
        }

        val musicResId = musicResources[currentGenre]
        if (musicResId == null) {
            Log.e("MusicManager", "No music resource found for genre: $currentGenre")
            return
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, musicResId).apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)

                    setOnErrorListener { _, what, extra ->
                        Log.e("MusicManager", "MediaPlayer error: $what, $extra")
                        false
                    }

                    setOnPreparedListener {
                        if (!isPaused && isEnabled) {
                            start()
                            isStarted = true
                            Log.d("MusicManager", "Started music: $currentGenre")
                        }
                    }
                }
            } else {
                if (!mediaPlayer!!.isPlaying && !isPaused && isEnabled) {
                    mediaPlayer!!.start()
                    isStarted = true
                    Log.d("MusicManager", "Resumed music: $currentGenre")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Failed to start music", e)
        }
    }

    fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
        isStarted = false
        isPaused = false
        Log.d("MusicManager", "Music stopped")
    }

    fun pauseMusic() {
        if (!isStarted) return

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
                Log.d("MusicManager", "Music paused (game/voting screen)")
            }
        }
    }

    fun resumeMusic() {
        if (!isStarted) {
            startMusic()
            return
        }

        if (isEnabled && isPaused) {
            isPaused = false
            mediaPlayer?.start()
            Log.d("MusicManager", "Music resumed (back to menu/settings)")
        }
    }

    fun changeGenre(genre: String) {
        if (currentGenre == genre) return

        currentGenre = genre
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("music_genre", genre).apply()
        }

        Log.d("MusicManager", "Changing music to: $genre")

        val wasPlaying = mediaPlayer?.isPlaying == true
        val wasPaused = isPaused

        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
            mediaPlayer = null
        }

        if ((wasPlaying || !wasPaused) && isEnabled) {
            startMusic()
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("music_enabled", enabled).apply()
        }

        if (enabled) {
            if (!isPaused && isStarted) {
                startMusic()
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }
        Log.d("MusicManager", "Music ${if (enabled) "enabled" else "disabled"}")
    }

    fun isEnabled(): Boolean = isEnabled
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun getCurrentGenre(): String = currentGenre

    fun onAppDestroy() {
        stopMusic()
    }
}