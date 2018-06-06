package com.example.andy.homeandoffice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.jetbrains.anko.*
import kotlin.concurrent.thread


private const val SESSION_TAG = "MEDIA SESSION"
private const val EMPTY_ROOT_ID = "EMPTY ROOT"
private const val ROOT_ID = "_ROOT_"

class MediaPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener {

    private lateinit var mMediaSession: MediaSessionCompat
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var afChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var am: AudioManager
    private lateinit var mPlaybackAttributes: AudioAttributes
    private lateinit var mFocusRequest: AudioFocusRequest
    private val noisyReceiver = BecomingNoisyReceiver()
    private var receiverRegistered = false
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val mHandler = Handler()
    private lateinit var playerModel: PlayerModel
    private val runnable = Runnable {
        MyMediaSessionCallback().onStop()
    }


    override fun onCreate() {
        super.onCreate()

        //Initialize MediaSession
        mMediaSession = MediaSessionCompat(applicationContext, SESSION_TAG)
        mMediaSession
                .setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS.or(
                        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                ))

        //Initialize PlaybackState builder
        mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY.or(
                                PlaybackState.ACTION_PLAY_PAUSE)
                )
        mMediaSession.setPlaybackState(mStateBuilder.build())
        mMediaSession.setCallback(MyMediaSessionCallback())
        sessionToken = mMediaSession.sessionToken

        //Audio focus change listener
        afChangeListener = AudioManager.OnAudioFocusChangeListener {
            when(it) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    mMediaPlayer?.setVolume(0.5F, 0.5F)
                    MyMediaSessionCallback().onPlay()
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ->
                    MyMediaSessionCallback().onPlay()
                AudioManager.AUDIOFOCUS_LOSS -> {
                    MyMediaSessionCallback().onPause()
//                    mHandler.postDelayed(runnable, 30000)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> MyMediaSessionCallback().onPause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    mMediaPlayer?.setVolume(0.1F, 0.1F)
            }
        }

        //Build focus request (for Android O only)
        am = this@MediaPlaybackService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mPlaybackAttributes  = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mPlaybackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(afChangeListener, mHandler)
                    .build()
        }

        //Initialize a player model and set metadata
        doAsync {
            playerModel = PlayerModel
            uiThread {
                mMediaSession.setMetadata(buildMetadata())
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) mMediaPlayer?.release()
    }

    //Send media list to any client connecting
    override fun onLoadChildren(
            parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (parentId.equals(EMPTY_ROOT_ID)) {
            result.sendResult(null)
            return
        } else {
            val items = MutableList(playerModel.albums.size, {
                playerModel.albumToMediaItem(it)
            })
            result.sendResult(items)
        }
    }

    //Return browser root to connecting client
    override fun onGetRoot(
            clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(ROOT_ID, null)
    }

    fun startPlayer() {
        thread(start = true) {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer?.setOnPreparedListener(this)
                mMediaPlayer?.setOnCompletionListener {

                    playerModel.nextTrack()
                    mMediaSession.setMetadata(buildMetadata())
                    mMediaPlayer?.reset()
                    mMediaPlayer?.setDataSource(playerModel.currentTrack.path)
                    mMediaPlayer?.prepareAsync()
                }

                if (!mMediaPlayer!!.isPlaying) {
                    mMediaPlayer?.setDataSource(playerModel.currentTrack.path)
                    mMediaPlayer?.prepareAsync()
                }

            } else {
                mMediaPlayer?.start()
                playerModel.playing = true
            }
        }
    }

    //Start player and update notifcation
    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        notificationManager.notify(1, buildNotification().build())
        playerModel.playing = true
    }

    //Skipping or going back to previous track
    fun playerChangeTrack() {
        mMediaPlayer?.stop()
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
        if (mMediaSession.isActive) {
            startPlayer()
        }
    }

    fun playerStop() {
        mMediaPlayer?.stop()
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
        playerModel.playing = false
    }

    //Pause if headphones removed or external speaker disconnected, etc
    inner class BecomingNoisyReceiver: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(p1?.action))
                MyMediaSessionCallback().onPause()
        }
    }

    //Main control callbacks
    inner class MyMediaSessionCallback: MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()

            //Request audio focus, and proceed if granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (am.requestAudioFocus(mFocusRequest) !=
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    MyMediaSessionCallback().onPause()
            } else {
                //For SDK 25 and under
                @Suppress("DEPRECATION")
                if (am.requestAudioFocus(
                                afChangeListener,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN) !=
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    MyMediaSessionCallback().onPause()
            }

            //Start service
            startService(Intent(
                    this@MediaPlaybackService, MediaPlaybackService::class.java))

            //Start player
            mMediaSession.isActive = true
            mStateBuilder.setState(PlaybackState.STATE_PLAYING, 0, 1F)
            mMediaSession.setPlaybackState(mStateBuilder.build())
            startPlayer()

            //Register BECOME_NOISY BroadcastReceiver
            if (!receiverRegistered) {
                registerReceiver(noisyReceiver, intentFilter)
                receiverRegistered = true
            }

            //Build notification and put in foreground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel
                val name = getString(R.string.channel_name)
                val description = getString(R.string.channel_description)
                val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NotificationManager.IMPORTANCE_LOW
                } else {
                    NotificationManager.IMPORTANCE_LOW
                }
                val id = getString(R.string.CHANNEL_ID)
                val mChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel(id, name, importance)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                mChannel.description = description
                notificationManager.createNotificationChannel(mChannel)
            }
            startForeground(1, buildNotification().build())
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            playerModel.prevTrack()
            mMediaSession.setMetadata(buildMetadata())
            playerChangeTrack()
            notificationManager.notify(1, buildNotification().build())
        }

        override fun onStop() {
            super.onStop()

            //Abandon focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest(mFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(afChangeListener)
            }

            //Unregister Noisy Receiver
            if (receiverRegistered) {
                unregisterReceiver(noisyReceiver)
                receiverRegistered = false
            }

            //Stop player
            playerStop()
            mMediaSession.isActive = false

            //Stop service and notification
            stopForeground(true)
            stopSelf()

        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            playerModel.nextTrack()
            mMediaSession.setMetadata(buildMetadata())
            playerChangeTrack()
            notificationManager.notify(1, buildNotification().build())
        }

        override fun onPause() {
            super.onPause()

            //Update state and pause
            mMediaSession.isActive = false
            mStateBuilder.setState(
                    PlaybackState.STATE_PAUSED, mMediaPlayer?.currentPosition!!.toLong(), 0F)
            mMediaSession.setPlaybackState(mStateBuilder.build())
            mMediaPlayer?.pause()
            playerModel.playing = false

            //Unregister become noisy receiver
            if (receiverRegistered) {
                unregisterReceiver(noisyReceiver)
                receiverRegistered = false
            }

            //Service out of foreground
            stopForeground(false)
        }
    }

    fun buildMetadata(): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_ALBUM,
                        playerModel.currentAlbum.albumTitle)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, playerModel.currentArt)
                .putString(MediaMetadata.METADATA_KEY_TITLE, playerModel.currentTrack.title)
                .build()
    }

    fun buildNotification(): NotificationCompat.Builder {

        val controller = mMediaSession.controller
        val builder = NotificationCompat.Builder(this@MediaPlaybackService, getString(R.string.CHANNEL_ID))
        builder
                .setContentTitle(playerModel.currentAlbum.albumTitle)
                .setContentText(playerModel.currentTrack.title)
                .setLargeIcon(playerModel.currentArt)
                .setContentIntent(controller.sessionActivity)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this@MediaPlaybackService,
                        PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ho_icon_small)
                .setColor(ContextCompat.getColor(this, R.color.darkNotify))
                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.sessionToken)
                        .setShowActionsInCompactView(0)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this@MediaPlaybackService, PlaybackStateCompat.ACTION_STOP)))
        if (mMediaSession.isActive)
                builder.addAction(NotificationCompat.Action(
                        R.drawable.notification_pause, getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this@MediaPlaybackService,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        else
            builder.addAction(NotificationCompat.Action(
                    R.drawable.notification_play, getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this@MediaPlaybackService,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE)))

        return builder
    }
}
