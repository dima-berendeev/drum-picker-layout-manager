package org.example.berendeev.drumlayoutmanager

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.dmitry_macpro.drumlayoutmanager.R


class MainActivity : AppCompatActivity() {
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.adapter = MyAdapter()
        recyclerView.layoutManager = PickerLayoutManager(this)
        val snapHelper = PickerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

}

class MyAdapter : RecyclerView.Adapter<MyHolder>() {

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val textView = holder.itemView as TextView
        textView.text = position.toString()
    }

    override fun getItemCount(): Int {
        return 40
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(parent)
    }

}

class MyHolder(parent: ViewGroup)
    : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
        .inflate(R.layout.item, parent, false)) {

    init {
        itemView.setTag(PickerLayoutManager.VIEW_TAG_DISTANCE_TO_FOCUS_LISTENER, DistanceToFocusListenerImpl())
    }

    inner class DistanceToFocusListenerImpl : PickerLayoutManager.DistanceToFocusListener {
        override fun onDistanceToFocusChanged(distance: Int) {
            val absDistance = Math.abs(distance)
            val total = 500
            val distanceCoef = if (absDistance <= total) {
                absDistance.toFloat() / total.toFloat()
            } else {
                1f
            }

            val scaleCoef = 1f - distanceCoef * 0.3f
            val alphaCoef = 1f - distanceCoef * 0.7f

            itemView.scaleY = scaleCoef
            itemView.scaleX = scaleCoef
            itemView.alpha = alphaCoef
        }
    }
}