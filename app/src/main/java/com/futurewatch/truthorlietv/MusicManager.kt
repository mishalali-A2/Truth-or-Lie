package com.futurewatch.truthorlietv

import android.media.MediaPlayer

object MusicManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentGenre = "Chill Lounge"
    private var isEnabled = true
    private var isPaused = false

    private val musicResources = mapOf(
        "Party Vibes" to R.raw.music_party,
        "Chill Lounge" to R.raw.music_chill,
        "Retro Arcade" to R.raw.music_retro,
        "Epic Adventure" to R.raw.music_epic,
        "Funky Groove" to R.raw.music_funky
    )

    fun init(context: android.content.Context) {
        // Use application context - this is safe because it's a singleton
        val appContext = context.applicationContext

        //prev pref chosen
        val prefs = appContext.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean("music_enabled", true)
        currentGenre = prefs.getString("music_genre", "Chill Lounge") ?: "Chill Lounge"

        if (isEnabled && !isPaused) {
            startMusic(appContext)
        }
    }

    private fun startMusic(context: android.content.Context) {
        if (!isEnabled || isPaused) return

        val musicResId = musicResources[currentGenre]
        if (musicResId == null) {
            android.util.Log.e("MusicManager", "No music resource found for genre: $currentGenre")
            return
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, musicResId).apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)

                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("MusicManager", "MediaPlayer error: $what, $extra")
                        false
                    }

                    setOnPreparedListener {
                        if (!isPaused && isEnabled) {
                            start()
                            android.util.Log.d("MusicManager", "Started music: $currentGenre")
                        }
                    }
                }
            } else {
                if (!mediaPlayer!!.isPlaying && !isPaused && isEnabled) {
                    mediaPlayer!!.start()
                    android.util.Log.d("MusicManager", "Resumed music: $currentGenre")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicManager", "Failed to start music", e)
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
        isPaused = false
        android.util.Log.d("MusicManager", "Music stopped")
    }

    fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
                android.util.Log.d("MusicManager", "Music paused (game/voting screen)")
            }
        }
    }

    fun resumeMusic() {
        if (isEnabled && isPaused) {
            isPaused = false
            if (mediaPlayer == null) {
               //in order to restart
                val context = TruthOrLieApplication.instance
                startMusic(context)
            } else {
                mediaPlayer?.start()
                android.util.Log.d("MusicManager", "Music resumed (back to menu)")
            }
        }
    }

    fun changeGenre(genre: String) {
        if (currentGenre == genre) return

        currentGenre = genre
        val prefs = TruthOrLieApplication.prefs
        prefs.edit().putString("music_genre", genre).apply()

        android.util.Log.d("MusicManager", "Changing music to: $genre")

        // Restart music with new genre
        val wasPlaying = mediaPlayer?.isPlaying == true
        val wasPaused = isPaused

        // Stop current music
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }

        // Restart if it was playing (not paused)
        if (wasPlaying && isEnabled && !wasPaused) {
            val context = TruthOrLieApplication.instance
            startMusic(context)
        } else if (!wasPaused) {
            // If it wasn't paused, start playing
            val context = TruthOrLieApplication.instance
            startMusic(context)
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        val prefs = TruthOrLieApplication.prefs
        prefs.edit().putBoolean("music_enabled", enabled).apply()

        if (enabled) {
            if (!isPaused) {
                val context = TruthOrLieApplication.instance
                startMusic(context)
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }
        android.util.Log.d("MusicManager", "Music ${if (enabled) "enabled" else "disabled"}")
    }

    fun isEnabled(): Boolean = isEnabled
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun getCurrentGenre(): String = currentGenre

    fun onAppDestroy() {
        stopMusic()
    }
}