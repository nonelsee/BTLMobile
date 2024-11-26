package com.example.soulmole.controller

import android.os.Handler
import com.example.soulmole.model.Block
import com.example.soulmole.model.BlockType
import com.example.soulmole.model.Player
import com.example.soulmole.view.GameView

class PlayerManager(private var gameView: GameView, val player: Player) {
    private val healthHandler = Handler()
    var onPlayerHealthDepleted: (() -> Unit)? = null
    private val initialRow = ((gameView.maxY - gameView.playerY - gameView.pixelSize) / gameView.blockSize).toInt()
    var isMovingUp = false  // Cờ kiểm tra di chuyển lên
    var freezeRow = -1
    private var healthDecrementMultiplier = 1f
    private val healthDecrementRunnable = object : Runnable {
        override fun run() {
            if (player.health > 0) {
                // Sử dụng multiplier để điều chỉnh tốc độ giảm máu
                healthHandler.postDelayed(this, (500 / healthDecrementMultiplier).toLong())
                player.health--
                gameView.drawGame(gameView.holder)
            } else {
                healthHandler.removeCallbacks(this)
                onPlayerHealthDepleted?.invoke()
            }
        }
    }

    fun setHealthDecrementMultiplier(multiplier: Float) {
        healthDecrementMultiplier = multiplier
    }

    fun digBlock(dx: Int, dy: Int) {
        val col = (gameView.playerX / gameView.blockSize).toInt()
        val row = ((gameView.maxY - gameView.playerY - gameView.pixelSize) / gameView.blockSize).toInt()

        val targetCol = col + dx
        val targetRow = row - dy

        if (targetRow in gameView.blocks.indices && targetCol in gameView.blocks[targetRow].indices) {
            val targetBlock = gameView.blocks[targetRow][targetCol]
            if (targetBlock.type == BlockType.STONE) {
                return
            }
            if (targetBlock.type != BlockType.EMPTY) {
                // Kiểm tra hiệu ứng chuối cầu vồng
                if (gameView.isRainbowBananaEffectActive()) {
                    gameView.blocks[targetRow][targetCol] = Block(BlockType.EMPTY, 0)
                } else {
                    targetBlock.hitsRemaining -= 1
                    if (targetBlock.hitsRemaining <= 0) {
                        gameView.blocks[targetRow][targetCol] = Block(BlockType.EMPTY, 0)
                    }
                }
                gameView.drawGame(gameView.holder)
                gameView.makeWoodBlocksFall()
            }
        }
    }

    fun movePlayer(dx: Float, dy: Float) {
        val col = (gameView.playerX / gameView.blockSize).toInt()
        val row = ((gameView.maxY - gameView.playerY - gameView.pixelSize) / gameView.blockSize).toInt()

        val newX = gameView.playerX + dx * gameView.blockSize
        val newY = gameView.playerY + dy * gameView.blockSize

        if (dy < 0 && row >= initialRow) {
            return  // Không cho phép di chuyển lên trên hàng khởi tạo
        }

        if (dy < 0) {  // Di chuyển lên
            if (!isMovingUp) {
                // Bắt đầu đi lên, lưu lại hàng hiện tại làm mốc dừng tính điểm
                isMovingUp = true
                freezeRow = row
            }
        } else if (dy > 0) {  // Di chuyển xuống
            val rowBelow = row - 1
            if (rowBelow >= 0 && rowBelow < gameView.blocks.size &&
                col in gameView.blocks[rowBelow].indices &&
                gameView.blocks[rowBelow][col].type != BlockType.EMPTY) {
                return
            }

            // Kiểm tra nếu người chơi đã di chuyển xuống qua hàng freezeRow
            if (isMovingUp && row <= freezeRow) {
                isMovingUp = false  // Reset cờ khi người chơi đã di chuyển xuống qua hàng freezeRow
            }

            // Chỉ tính điểm khi người chơi không đang di chuyển lên
            if (!isMovingUp) {
                player.depth += 5
                player.score += 20
            }
        }

        // Kiểm tra va chạm với các khối không phải EMPTY khi di chuyển theo chiều X
        if (dx < 0) { // Di chuyển sang trái
            val colLeft = col - 1
            if (colLeft >= 0 && row in gameView.blocks.indices &&
                gameView.blocks[row][colLeft].type != BlockType.EMPTY) {
                return
            }
        } else if (dx > 0) { // Di chuyển sang phải
            val colRight = col + 1
            if (colRight < gameView.numColumns && row in gameView.blocks.indices &&
                gameView.blocks[row][colRight].type != BlockType.EMPTY) {
                return
            }
        }

        // Cập nhật vị trí người chơi nếu tọa độ nằm trong giới hạn cho phép
        if (newX in gameView.minX..(gameView.maxX - gameView.blockSize)) {
            gameView.playerX = newX
        }
        if (newY in gameView.minY..(gameView.maxY - gameView.blockSize)) {
            gameView.playerY = newY
        }

        gameView.drawGame(gameView.holder)
        gameView.checkItemPickup();
        if (gameView.isPlayerAtBottomRow()) {
            gameView.goToNextLevel()
        }
    }


    fun startHealthDecrement() {
        healthHandler.post(healthDecrementRunnable)
    }

    fun stopHealthDecrement() {
        healthHandler.removeCallbacks(healthDecrementRunnable)
    }
}