package com.example.andy.homeandoffice

import com.google.gson.Gson
import java.net.URL

class AlbumsRequest(val albumsPath: String) {

    //Read list of albums from JSON
    fun execute(): AlbumsResult {
        val albumsUrl = URL(albumsPath)
        val reader = albumsUrl.openStream().reader()
        return Gson().fromJson(reader, AlbumsResult::class.java)
    }
}