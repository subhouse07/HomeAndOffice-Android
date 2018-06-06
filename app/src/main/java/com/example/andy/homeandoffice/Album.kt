package com.example.andy.homeandoffice

import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

class Album(val albumTitle: String, val albumArt: String, val songs: ArrayList<Song>,
            val albumInfo: String): Parcelable {
    constructor(parcel: Parcel) : this(
            albumTitle = parcel.readString(),
            albumArt = parcel.readString(),
            songs = parcel.readArrayList(null) as ArrayList<Song>,
            albumInfo = parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(albumTitle)
        parcel.writeString(albumArt)
        parcel.writeList(songs)
        parcel.writeString(albumInfo)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Album> {
        override fun createFromParcel(parcel: Parcel) = Album(parcel)

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }
}