package com.example.soulmole.activity

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.soulmole.R
import com.example.soulmole.model.RankingItem

class RankingAdapter(
    private val context: Context,
    private val leaderboard: List<RankingItem>
) : BaseAdapter() {

    override fun getCount(): Int = leaderboard.size + 1 // +1 cho header

    override fun getItem(position: Int): Any =
        if (position == 0) HeaderItem else leaderboard[position - 1]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_ranking, parent, false)

        val rankIcon = view.findViewById<ImageView>(R.id.rankIcon)
        val rankTextView = view.findViewById<TextView>(R.id.rankTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val scoreTextView = view.findViewById<TextView>(R.id.scoreTextView)

        when (position) {
            0 -> bindHeaderRow(rankTextView, usernameTextView, scoreTextView, rankIcon)
            else -> bindDataRow(
                position,
                leaderboard[position - 1],
                rankIcon,
                rankTextView,
                usernameTextView,
                scoreTextView
            )
        }

        // Animation cho mỗi item
        view.animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)

        return view
    }

    private fun bindHeaderRow(
        rankView: TextView,
        usernameView: TextView,
        scoreView: TextView,
        rankIcon: ImageView
    ) {
        rankIcon.visibility = View.GONE // Không hiển thị icon ở hàng header

        rankView.apply {
            text = "RANKING"
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.header_text))
        }

        usernameView.apply {
            text = "NAME"
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.header_text))
        }

        scoreView.apply {
            text = "SCORE"
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.header_text))
        }
    }

    private fun bindDataRow(
        position: Int,
        item: RankingItem,
        rankIcon: ImageView,
        rankView: TextView,
        usernameView: TextView,
        scoreView: TextView
    ) {
        // Kiểm tra vị trí để hiển thị icon nếu cần
        when (position) {
            1 -> {
                rankIcon.setImageResource(R.drawable.ic_rank_gold)
                rankIcon.visibility = View.VISIBLE
            }
            2 -> {
                rankIcon.setImageResource(R.drawable.ic_rank_silver)
                rankIcon.visibility = View.VISIBLE
            }
            3 -> {
                rankIcon.setImageResource(R.drawable.ic_rank_bronze)
                rankIcon.visibility = View.VISIBLE
            }
            else -> rankIcon.visibility = View.INVISIBLE // Sử dụng INVISIBLE thay vì GONE
        }

        // Setup rank text
        rankView.text = position.toString()
        rankView.setTextColor(getRankColor(position))

        // Setup username
        usernameView.text = item.username
        usernameView.setTextColor(ContextCompat.getColor(context, R.color.normal_text))

        // Setup score
        scoreView.text = item.score.toString()
        scoreView.setTextColor(ContextCompat.getColor(context, R.color.score_text))
    }

    private fun getRankColor(position: Int): Int {
        val colorResId = when (position) {
            1 -> R.color.gold
            2 -> R.color.silver
            3 -> R.color.bronze
            else -> R.color.normal_text
        }
        return ContextCompat.getColor(context, colorResId)
    }

    private object HeaderItem
}
