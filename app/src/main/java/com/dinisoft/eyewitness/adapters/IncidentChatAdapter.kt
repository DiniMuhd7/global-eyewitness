package com.dinisoft.eyewitness.adapters

import android.content.Context
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.dinisoft.eyewitness.R
import com.dinisoft.eyewitness.Utterance
import com.dinisoft.eyewitness.UtteranceFrom
import kotlinx.android.synthetic.main.user_card_incident_chat.view.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class IncidentChatAdapter(private val utteranceList: List<Utterance>, private val ctx: Context, private val menuInflater: MenuInflater) : RecyclerView.Adapter<IncidentChatAdapter.UtteranceViewHolder>() {
    var onLongClickListener: OnLongItemClickListener? = null

    interface OnLongItemClickListener {
        fun itemLongClicked(v: View, position: Int)
    }

    fun setOnLongItemClickListener(listener: OnLongItemClickListener) {
        onLongClickListener = listener
    }

    override fun getItemCount(): Int {
        return utteranceList.size
    }

    override fun onBindViewHolder(utteranceViewHolder: UtteranceViewHolder, i: Int) {
        utteranceViewHolder.vUtterance.text = utteranceList[i].utterance
        utteranceViewHolder.vUsername.text = utteranceList[i].username
        utteranceViewHolder.vDate.text = utteranceList[i].cdate
        //Glide.with(ctx).load(utteranceList[i].photoURL).into(utteranceViewHolder.vPhotoURL)
        utteranceViewHolder.itemView.setOnLongClickListener {v ->
            onLongClickListener?.itemLongClicked(v, i)
            true
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): UtteranceViewHolder {
        val itemView = when (i) {
            //UtteranceFrom.ABOKI.id -> LayoutInflater.from(viewGroup.context).inflate(R.layout.iwitness_card_layout, viewGroup, false)
            UtteranceFrom.USER.id -> LayoutInflater.from(viewGroup.context).inflate(R.layout.user_card_incident_chat, viewGroup, false)
            else -> throw IndexOutOfBoundsException("No such view id $i")
        }

        return UtteranceViewHolder(itemView, menuInflater, i)
    }

    override fun getItemViewType(position: Int): Int {
        val message = utteranceList[position]
        return message.from.id
    }

    class UtteranceViewHolder(v: View, private val menuInflater: MenuInflater, private val i: Int) : RecyclerView.ViewHolder(v), View.OnCreateContextMenuListener {
        val vUtterance = v.cutterance
        val vUsername = v.ctxt_username
        val vDate = v.ctxt_date


        init {
            v.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            when (i) {
                UtteranceFrom.USER.id -> menuInflater.inflate(R.menu.menu_user_utterance_context, menu)
                //UtteranceFrom.ABOKI.id -> menuInflater.inflate(R.menu.menu_iwitness_utterance_context, menu)
            }
        }
    }
}