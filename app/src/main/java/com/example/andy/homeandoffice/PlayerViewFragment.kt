package com.example.andy.homeandoffice



import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import android.util.DisplayMetrics
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player_view.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.textColor
import java.util.*


class PlayerViewFragment : Fragment() {

    private var originalView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        //Keep a copy of the original view so it doesn't return a new instance each time we return
        //to this fragment view
        if (originalView == null) {
            originalView = inflater.inflate(R.layout.fragment_player_view, container, false)
        }
        return originalView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Start background text animation and lift the player view above it
        animateText(context)
        playerLayout.translationZ = 1F
    }

    private fun animateText(context: Context?) {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val devHeight = displayMetrics.heightPixels
        val devHeightIncr = devHeight / 10
        val yVals = MutableList(10, {it * devHeightIncr.toFloat()})
        val offsets = longArrayOf(0L, 2200L, 5500L, 3300L, 6600L, 7700L, 1100L, 11100L, 4400L, 9900L  )
        val xStart = -400F
        val xEnd = (displayMetrics.widthPixels + 400).toFloat()

        for (i in 0 until 10) {
            val textView = TextView(context)
            textView.text = getString(R.string.bg_text)
            textView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
            textView.rotation = 345F
            textView.textColor = Color.WHITE

            val index = Random().nextInt(yVals.size)
            val anim = TranslateAnimation(
                    xStart, xEnd, yVals[index], yVals[index]-devHeightIncr)

            anim.startOffset = offsets[i]
            anim.repeatMode = Animation.RESTART
            anim.repeatCount = Animation.INFINITE
            anim.duration = 15000
            yVals.removeAt(index)
            relativeLayout.addView(textView)
            textView.startAnimation(anim)
        }
    }
}
