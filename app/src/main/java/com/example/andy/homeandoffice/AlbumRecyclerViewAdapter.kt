package com.example.andy.homeandoffice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.uiThread
import java.net.URL

class AlbumRecyclerViewAdapter(private val mValues: ArrayList<Album>) :
        RecyclerView.Adapter<AlbumRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var albumTitle: TextView = view.findViewById<TextView>(R.id.recycler_albumTitle)
        var artView: ImageView = view.findViewById<ImageView>(R.id.recycler_albumArt)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int):
            AlbumRecyclerViewAdapter.ViewHolder {

        val view = LayoutInflater.from(parent?.context)
                .inflate(R.layout.fragment_album_list_item, parent, false) as View
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.albumTitle?.text = mValues[position].albumTitle
        var art: Bitmap
        doAsync {
            art = bitmapFromUrl(mValues[position].albumArt)
            uiThread {
                holder?.artView?.imageBitmap = art
            }
        }
    }

    override fun getItemCount() = mValues.size

    private fun bitmapFromUrl(urlString: String): Bitmap {
        val url = URL(urlString)
        val stream = url.openStream()
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        return bitmap
    }
}
