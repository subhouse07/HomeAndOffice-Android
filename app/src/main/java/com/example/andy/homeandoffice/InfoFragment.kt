package com.example.andy.homeandoffice


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.support.v4.supportFragmentUiThread
import java.net.URL

const val KEY = "KEY"

class InfoFragment : Fragment() {

    private lateinit var album: Album
    private lateinit var listener: OpenListener

    companion object {
        fun newInstance(album: Album): InfoFragment {
            val args = Bundle()
            args.putParcelable(KEY, album)
            val fragment = InfoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            album = it.getParcelable(KEY)
        }
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        //Get album art and album description and apply to this view
        val v = inflater.inflate(R.layout.fragment_info, container, false)
        v.findViewById<TextView>(R.id.infoText).text = album.albumInfo
        doAsync {
            val bitmap = bitmapFromUrl(album.albumArt)
            supportFragmentUiThread {
                v.findViewById<ImageView>(R.id.infoArt).imageBitmap = bitmap
            }
        }
        return v
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OpenListener)
            listener = context
    }

    override fun onDestroy() {
        super.onDestroy()
        listener.setInfoScreenOpen(false)
    }

    private fun bitmapFromUrl(urlString: String): Bitmap {
        val url = URL(urlString)
        val stream = url.openStream()
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        return bitmap
    }


}
