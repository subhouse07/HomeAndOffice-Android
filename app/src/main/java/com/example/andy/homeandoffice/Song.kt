package com.example.andy.homeandoffice

import android.os.Parcel
import android.os.Parcelable

class Song(val title: String, val path: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            title = parcel.readString(),
            path = parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(path)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song = Song(parcel)

        override fun newArray(size: Int): Array<Song?> = arrayOfNulls(size)
    }
}