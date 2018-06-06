package com.example.andy.homeandoffice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import java.net.URI
import java.net.URL

private const val JSON_URL = "http://addyt.hopto.org/albums.json"

object PlayerModel {

    var albumsResult: AlbumsResult = AlbumsRequest(JSON_URL).execute()
    var albums: ArrayList<Album> = albumsResult.albums
    var currentAlbum: Album = albums[0]
    var currentTrack: Song = currentAlbum.songs[0]
    var currentArt: Bitmap = getBitmapFromAlbumArt(currentAlbum)
    var trackNum = 0
    var playing = false


    private fun getBitmapFromAlbumArt(album: Album): Bitmap {
        val artUrl = URL(album.albumArt)
        val stream = artUrl.openStream()
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        return bitmap
    }

    fun nextTrack() {
        trackNum++
        if (trackNum == currentAlbum.songs.size)
            trackNum = 0
        currentTrack = currentAlbum.songs[trackNum]
    }

    fun prevTrack() {
        trackNum--
        if (trackNum < 0)
            trackNum = 0
        currentTrack = currentAlbum.songs[trackNum]
    }

    fun albumToMediaItem(position: Int): MediaBrowserCompat.MediaItem {
        val item: MediaBrowserCompat.MediaItem
        val builder = MediaDescriptionCompat.Builder()
        builder
                .setTitle(albums[position].albumTitle)
                .setDescription(albums[position].albumInfo)
                .setIconUri(Uri.parse(albums[position].albumArt))
        item = MediaBrowserCompat.MediaItem.fromMediaItem(builder.build())
        return item
    }
}