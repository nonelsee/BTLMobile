package com.example.soulmole.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.soulmole.R
import com.example.soulmole.controller.GameSessionDAO
import com.example.soulmole.db.DatabaseHelper
import com.example.soulmole.model.GameSession
import com.example.soulmole.model.Player
import com.example.soulmole.view.GameView

class GameActivity : AppCompatActivity() {

    private var isDigMode = false
    private lateinit var dbHelper: DatabaseHelper
    lateinit var gameView: GameView
    lateinit var player: Player

    private lateinit var buttonUp: Button
    private lateinit var buttonDown: Button
    private lateinit var buttonLeft: Button
    private lateinit var buttonRight: Button
    private lateinit var buttonDig: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        dbHelper = DatabaseHelper(this)

        gameView = findViewById<GameView>(R.id.gameView)
        showNameInputDialog()

        buttonDig = findViewById(R.id.buttonDig)
        buttonUp = findViewById(R.id.buttonUp)
        buttonDown = findViewById(R.id.buttonDown)
        buttonLeft = findViewById(R.id.buttonLeft)
        buttonRight = findViewById(R.id.buttonRight)

        val handler = Handler(Looper.getMainLooper())
        val updateButtonsRunnable = object : Runnable {
            override fun run() {
                updateButtonStates()
                handler.postDelayed(this, 100) // Kiểm tra mỗi 100ms
            }
        }
        handler.post(updateButtonsRunnable)

        buttonDig.setOnClickListener {
            isDigMode = true
        }

        buttonUp.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(0, -1)
                updateButtonStates()
            } else if(gameView.isRainbowBananaEffectActive()) {
                gameView.digBlock(0, -1)
                gameView.movePlayer(0f, -1f)
            } else {
                gameView.movePlayer(0f, -1f)
            }
            if (!gameView.isRainbowBananaEffectActive()) {
                isDigMode = false
            }
            updateButtonStates()
        }

        buttonDown.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(0, 1)
                updateButtonStates()
            } else if(gameView.isRainbowBananaEffectActive()) {
                gameView.digBlock(0, 1)
                gameView.movePlayer(0f, 1f)
            } else {
                gameView.movePlayer(0f, 1f)
            }
            if (!gameView.isRainbowBananaEffectActive()) {
                isDigMode = false
            }
            updateButtonStates()
        }

        buttonLeft.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(-1, 0)
                updateButtonStates()
            } else if(gameView.isRainbowBananaEffectActive()) {
                gameView.digBlock(-1, 0)
                gameView.movePlayer(-1f, 0f)
            } else {
                gameView.movePlayer(-1f, 0f)
            }
            if (!gameView.isRainbowBananaEffectActive()) {
                isDigMode = false
            }
            updateButtonStates()
        }

        buttonRight.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(1, 0)
                updateButtonStates()
            } else if(gameView.isRainbowBananaEffectActive()) {
                gameView.digBlock(1, 0)
                gameView.movePlayer(1f, 0f)
            } else {
                gameView.movePlayer(1f, 0f)
            }
            if (!gameView.isRainbowBananaEffectActive()) {
                isDigMode = false
            }
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        val stoneDirections = gameView.checkStoneBlocks()
        buttonDig.apply {
            isEnabled = !gameView.isRainbowBananaEffectActive()
            alpha = if (isEnabled) 1.0f else 0.2f
        }

        if (gameView.isRainbowBananaEffectActive()) {
            isDigMode = false
        }

        buttonUp.apply {
            isEnabled = !stoneDirections["up"]!!
            alpha = if (isEnabled) 1.0f else 0.2f
        }

        buttonDown.apply {
            isEnabled = !stoneDirections["down"]!!
            alpha = if (isEnabled) 1.0f else 0.2f
        }

        buttonLeft.apply {
            isEnabled = !stoneDirections["left"]!!
            alpha = if (isEnabled) 1.0f else 0.2f
        }

        buttonRight.apply {
            isEnabled = !stoneDirections["right"]!!
            alpha = if (isEnabled) 1.0f else 0.2f
        }
    }

    private fun showNameInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_name_input, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.edit_text_name)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Nhập tên người chơi")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Xác nhận", null)
            .setNegativeButton("Hủy") { _, _ -> finish() }
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val username = editTextName.text.toString().trim()

                if (username.isNotEmpty()) {
                    val existingPlayer = dbHelper.getPlayerByUsername(username)
                    if (existingPlayer == null) {
                        // Tạo Player mới với username và các giá trị mặc định
                        val newPlayer = Player(username = username)

                        // Chèn Player vào cơ sở dữ liệu và lấy playerId tự động tăng
                        val playerId = dbHelper.insertPlayer(newPlayer)
                        if (playerId != -1L) {
                            player = newPlayer.copy(playerId = playerId.toInt())

                            // Khởi tạo gameView với player sau khi tên đã nhập
                            gameView.initializePlayer(player)
                            gameView.playerManager.onPlayerHealthDepleted = { showGameOverDialog() }

                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Lỗi khi lưu người chơi, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Tên người dùng đã tồn tại, vui lòng nhập lại",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }
    private fun showGameOverDialog() {
        val gameSession = GameSession(
            player = player  // Truyền trực tiếp đối tượng player hiện tại
        )

        val gameSessionDAO = GameSessionDAO(dbHelper)
        gameSessionDAO.insertGameSession(gameSession)

        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Score: ${gameSession.player.score}\nDepth: ${gameSession.player.depth}")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
}

