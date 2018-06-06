package com.example.andy.homeandoffice

import android.content.ComponentName
import android.media.AudioManager
import android.media.MediaMetadata
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player_view.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), OpenListener {

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var playerFragment: PlayerViewFragment
    private lateinit var playerModel: PlayerModel

    private var albumsScreenOpen = false
    private var infoScreenOpen = false

    private lateinit var mMediaBrowser: MediaBrowserCompat
    private val controllerCallback =
            object: MediaControllerCompat.Callback() {

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    updateUIText(metadata)
                }

                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    if (state?.state == PlaybackStateCompat.STATE_PLAYING)
                        playButton.imageResource = R.drawable.pause_selector
                    else
                        playButton.imageResource = R.drawable.play_selector
                }
            }

    private val mConnectionCallbacks =
            object: MediaBrowserCompat.ConnectionCallback() {

                override fun onConnected() {
                    super.onConnected()
                    val token = mMediaBrowser.sessionToken
                    val mediaController = MediaControllerCompat(this@MainActivity, token)
                    MediaControllerCompat.setMediaController(
                            this@MainActivity, mediaController)
                    buildTransportControls()
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize MediaBrowser and MediaPlaybackService
        mMediaBrowser = MediaBrowserCompat(this,
                ComponentName(this, MediaPlaybackService::class.java),
                mConnectionCallbacks, null)

        //Media Player View Fragment
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        playerFragment = PlayerViewFragment()
        fragmentTransaction
                .add(R.id.content_frame, playerFragment)
                .commit()

        //Fragments for album select and album info screens
        var albumListFragment: AlbumListItemFragment? = null
        var infoFragment: InfoFragment? = null
        doAsync {
            playerModel = PlayerModel
            activityUiThread {
                albumListFragment = AlbumListItemFragment.newInstance(playerModel.albums)
                infoFragment = InfoFragment.newInstance(playerModel.currentAlbum)
            }
        }

        //Toolbar
        val toolbar: Toolbar = toolbar
        setSupportActionBar(toolbar)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        //Nav drawer
        mDrawerLayout = drawer_layout
        val navigationView: NavigationView = nav_view

        //Listener for menu item select
        navigationView.setNavigationItemSelectedListener { menuItem ->
            mDrawerLayout.closeDrawers()

            //Album Select Screen
            if (menuItem.itemId == R.id.nav_albums) {
                if (albumListFragment != null && !albumsScreenOpen) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, albumListFragment)
                            .addToBackStack(null)
                            .commit()
                    albumsScreenOpen = true
                }
            //Album info screen
            } else {
                if (infoFragment != null && !infoScreenOpen) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, infoFragment)
                            .addToBackStack(null)
                            .commit()
                    infoScreenOpen = true
                }
            }
            true
        }

        //Update UI here in case we're returning from an exited state while the Player Model still
        //exists in memory. (
        updateUIText()

    }

    //Listener functions to keep tabs on back stack (not repeatedly add fragment views to stack)
    override fun setAlbumsScreenOpen(open: Boolean) { albumsScreenOpen = open }
    override fun setInfoScreenOpen(open: Boolean) { infoScreenOpen = open }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    //Connect to MediaBrowser
    override fun onStart() {
        super.onStart()
        mMediaBrowser.connect()
    }

    //Set volume control stream
    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    //Disconnect from MediaBrowser
    override fun onStop() {
        super.onStop()
        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback)
        }
        mMediaBrowser.disconnect()
    }

    //Call onStop() of MediaSessionCallback to clean everything up
    override fun onDestroy() {
        super.onDestroy()
        mediaController.transportControls.stop()

    }

    fun buildTransportControls() {

        //Play/pause button listener
        playButton.setOnClickListener {
            val pbState = MediaControllerCompat.getMediaController(this).playbackState.state
            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                MediaControllerCompat.getMediaController(this).transportControls.pause()
                playButton.imageResource = R.drawable.play_selector
            } else {
                MediaControllerCompat.getMediaController(this).transportControls.play()
                playButton.imageResource = R.drawable.pause_selector
            }
        }

        //Skip button
        skipButton.setOnClickListener {
            MediaControllerCompat.getMediaController(this)
                    .transportControls.skipToNext()
        }

        //Previous track button
        backButton.setOnClickListener {
            MediaControllerCompat.getMediaController(this)
                    .transportControls.skipToPrevious()
        }

        val mediaController = MediaControllerCompat.getMediaController(this)
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState
        mediaController.registerCallback(controllerCallback)
    }

    //UI Update used by the ControllerCallback in the event of metadata change
    private fun updateUIText(metadata: MediaMetadataCompat?) {
        titleText.text = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        albumText.text = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        albumArt.imageBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    }

    //UI update used by onCreate() in the event of a return from exit state
    private fun updateUIText() {
        doAsync {
            val pm = PlayerModel
            activityUiThread {
                titleText.text = pm.currentTrack.title
                albumText.text = pm.currentAlbum.albumTitle
                albumArt.imageBitmap = pm.currentArt
            }
        }


    }
}
