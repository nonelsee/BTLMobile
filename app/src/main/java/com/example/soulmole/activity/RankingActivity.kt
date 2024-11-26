package com.example.soulmole.activity

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.soulmole.R
import com.example.soulmole.db.DatabaseHelper
import com.example.soulmole.model.RankingItem

class RankingActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listViewRanking: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        dbHelper = DatabaseHelper(this)
        listViewRanking = findViewById(R.id.listViewRanking)

        setupRankingList()
    }

    private fun setupRankingList() {
        // Lấy top 5 người chơi từ cơ sở dữ liệu
        val leaderboard = dbHelper.getTop5PlayersWithScores().map {
            RankingItem(it.first, it.second)
        }

        val adapter = RankingAdapter(this, leaderboard)
        listViewRanking.adapter = adapter

        // Thêm animation cho ListView
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        listViewRanking.layoutAnimation = animation
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
