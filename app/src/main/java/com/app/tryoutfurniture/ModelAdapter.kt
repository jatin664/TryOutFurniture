package com.app.tryoutfurniture

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_model.view.*

const val SELECTED_MODEL_COLOR = Color.YELLOW
const val UNSELECTED_MODEL_COLOR = Color.LTGRAY

class ModelAdapter(private val context: Context, private val dataList : MutableList<Model>) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    var selectedModel = MutableLiveData<Model>()
    private var selectedModelIndex =0

    class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        return ModelViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_model,
        parent,false))
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {

        //for selecting ones
        if(selectedModelIndex == holder.layoutPosition){
            holder.itemView.setBackgroundColor(SELECTED_MODEL_COLOR)
            selectedModel.value = dataList[holder.layoutPosition]
        }
        //for non selection
        else{
            holder.itemView.setBackgroundColor(UNSELECTED_MODEL_COLOR)
        }

        holder.itemView.apply {
            tvTitle?.text = dataList[position].title
            ivThumbnail?.setImageResource(dataList[position].imageResource)
        }

        holder.itemView.setOnClickListener {
            selectModel(holder)
        }

    }

    override fun getItemCount(): Int = dataList.size

    private fun selectModel(holder: ModelViewHolder){
        // to check if user not selecting already selected layout
        if(selectedModelIndex!=holder.layoutPosition){
            holder.itemView.setBackgroundColor(SELECTED_MODEL_COLOR)
            notifyItemChanged(selectedModelIndex)
            selectedModelIndex = holder.layoutPosition
            selectedModel.value = dataList[holder.layoutPosition]
        }
    }

}