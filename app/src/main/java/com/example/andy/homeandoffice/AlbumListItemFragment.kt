package com.example.andy.homeandoffice


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

private const val ALBUMS_KEY = "ALBUMS_KEY"

class AlbumListItemFragment : Fragment() {

    private var albums = ArrayList<Album>()
    private var listener: OpenListener? = null

    companion object {
        fun newInstance(albums: ArrayList<Album>): AlbumListItemFragment {
            val args = Bundle()
            args.putParcelableArrayList(ALBUMS_KEY, albums)
            val fragment = AlbumListItemFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            albums = it.getParcelableArrayList(ALBUMS_KEY)
        }
        retainInstance = true

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_album_list, container, false)

        //Build the RecyclerView
        if (view is RecyclerView) {
            val context = view.context
            view.layoutManager = LinearLayoutManager(context)
            view.adapter = AlbumRecyclerViewAdapter(albums)
        }
        return view
    }

    //Set context (main activity) as the listener
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OpenListener)
            listener = context
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.setAlbumsScreenOpen(false)
    }


}
