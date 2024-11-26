package com.example.soulmole.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.soulmole.activity.GameActivity
import com.example.soulmole.controller.PlayerManager
import com.example.soulmole.model.Block
import com.example.soulmole.model.BlockType
import com.example.soulmole.model.GameState
import com.example.soulmole.model.Item
import com.example.soulmole.model.ItemType
import com.example.soulmole.model.Player
import com.example.soulmole.model.Trap
import com.example.soulmole.model.TrapType
import java.util.Random
import kotlin.math.abs

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val renderer = GameRenderer(context)
    var playerX = 0f
    var playerY = 0f
    var pixelSize = 20f
    private var currentLevel = 1

    val numColumns = 5
    var blockSize = 0f
    val blocks = mutableListOf<MutableList<Block>>()
    private val items = mutableListOf<Item>()

    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f

    var isLevelTransitioning = false
    lateinit var playerManager: PlayerManager
    private var isInitialized = false

    private var isRainbowBananaActive = false
    private val rainbowBananaHandler = Handler(Looper.getMainLooper())

    private var currentTrap: Trap? = null
    private val laserHandler = Handler(Looper.getMainLooper())
    private val laserDuration = 2000L // Laser tồn tại 2 giây
    private val laserDamageDelay = 1000L
    private val laserRunnable = object : Runnable {
        override fun run() {
            if (!isLevelTransitioning) {
                generateNewLaser()
            }
            // Sử dụng thời gian delay được tính toán dựa trên level
            laserHandler.postDelayed(this, calculateLaserDelay())
        }
    }
    private fun calculateLaserDelay(): Long {
        if (currentLevel < 2) return 6000L // Level 1 không có laser

        // Bắt đầu từ 6 giây ở level 2, mỗi level giảm 1 giây
        val delayInSeconds = (7 - currentLevel).coerceAtLeast(2)
        return delayInSeconds * 1000L
    }
    private fun calculateLaserProbability(): Int {
        if (currentLevel < 2) return 0  // Level 1 không có laser

        // Bắt đầu từ 50% ở level 2, mỗi level tăng 10%
        val probability = 50 + (currentLevel - 2) * 10

        // Giới hạn tối đa là 90%
        return probability.coerceAtMost(90)
    }
    private fun generateNewLaser() {
        val probability = calculateLaserProbability()

        // Nếu level 1 hoặc không đạt ngưỡng xác suất, không tạo laser
        if (probability == 0 || (1..100).random() > probability) {
            currentTrap = null
            drawGame(holder)
            return
        }

        // Xác định bắn ngang (horizontal) hoặc dọc (vertical)
        val isHorizontal = Random().nextBoolean()

        val startPosition = if (isHorizontal) {
            playerY + pixelSize / 2
        } else {
            playerX + pixelSize / 2
        }

        // Tạo laser với phase ban đầu là 0
        currentTrap = Trap(
            type = TrapType.LASER,
            startX = if (!isHorizontal) startPosition else 0f,
            startY = if (isHorizontal) startPosition else 0f,
            isHorizontal = isHorizontal,
            damagePhase = 0  // Đảm bảo luôn bắt đầu từ phase 0
        )

        // Vẽ laser sau khi tạo
        drawGame(holder)

        // Delay chuyển sang phase gây sát thương
        laserHandler.postDelayed({
            currentTrap = currentTrap?.copy(damagePhase = 1)
            checkTrapCollision()
            drawGame(holder)
        }, laserDamageDelay)

        // Xóa laser sau khi kết thúc
        laserHandler.postDelayed({
            currentTrap = null
            drawGame(holder)
        }, laserDuration)
    }
    private fun checkTrapCollision() {
        currentTrap?.let { trap ->
            when (trap.type) {
                TrapType.LASER -> {
                    val playerCenterX = playerX + pixelSize / 2
                    val playerCenterY = playerY + pixelSize / 2

                    if (trap.isHorizontal) {
                        if (abs(trap.startY - playerCenterY) < pixelSize / 2) {
                            playerManager.player.health -= 20
                        }
                    } else {
                        if (abs(trap.startX - playerCenterX) < pixelSize / 2) {
                            playerManager.player.health -= 20
                        }
                    }
                }
            }
        }
    }

    private val fallHandler = Handler(Looper.getMainLooper())
    private val fallRunnable = object : Runnable {
        override fun run() {
            makeWoodBlocksFall()
            fallHandler.postDelayed(this, 100)
        }
    }

    init {
        holder.addCallback(this)
    }

    fun initializePlayer(player: Player) {
        playerManager = PlayerManager(this, player)
        (context as GameActivity).player = player
        isInitialized = true
        if (holder.surface.isValid) {
            playerManager.startHealthDecrement()
            drawGame(holder)
        }
    }

    private fun calculateMovementBounds() {
        val screenWidth = width
        val screenHeight = height

        minX = 0f
        maxX = screenWidth * 5 / 7f
        minY = 0f
        maxY = screenHeight.toFloat()

        blockSize = maxX / numColumns
        pixelSize = blockSize
    }

    private fun initializeBlocks() {
        val numRows = (maxY / blockSize * 2 / 3).toInt()
        blocks.clear()
        items.clear()

        for (row in 0 until numRows) {
            val rowBlocks = mutableListOf<Block>()
            for (col in 0 until numColumns) {
                val randomValue = (0..99).random()
                val type = when {
                    randomValue < 70 -> BlockType.DIRT
                    randomValue < 90 -> BlockType.WOOD
                    else -> BlockType.STONE
                }
                val hitsRequired = if (type == BlockType.DIRT) 1 else 2
                rowBlocks.add(Block(type, hitsRequired))
            }
            blocks.add(rowBlocks)
        }
        val heartsToSpawn = if (currentLevel < 4) 2 else 1
        for (i in 1..heartsToSpawn) {
            placeHeartItem()
        }
        if ((0..100).random() < 50) {  // 50% cơ hội spawn
            placeRainbowBanana()
        }
    }

    private fun placeHeartItem() {
        var placed = false
        while (!placed) {
            val randomRow = (0 until blocks.size).random()
            val randomCol = (0 until numColumns).random()

            val block = blocks[randomRow][randomCol]
            if (block.type != BlockType.EMPTY) {
                // Thay block hiện tại thành EMPTY
                blocks[randomRow][randomCol] = Block(BlockType.EMPTY, 0)

                // Thêm vật phẩm trái tim
                val itemX = randomCol * blockSize + blockSize / 4
                val itemY = maxY - (randomRow + 1) * blockSize + blockSize / 4
                items.add(Item(ItemType.HEART, itemX, itemY))

                placed = true
            }
        }
    }
    private fun placeRainbowBanana() {
        var placed = false
        while (!placed) {
            val randomRow = (0 until blocks.size).random()
            val randomCol = (0 until numColumns).random()

            val block = blocks[randomRow][randomCol]
            if (block.type != BlockType.EMPTY) {
                // Replace the current block with EMPTY
                blocks[randomRow][randomCol] = Block(BlockType.EMPTY, 0)

                // Place the rainbow banana in the middle of the block
                val itemX = randomCol * blockSize + blockSize / 18
                val itemY = maxY - (randomRow + 1) * blockSize + blockSize / 12
                items.add(Item(ItemType.RAINBOW_BANANA, itemX, itemY))

                placed = true
            }
        }
    }
    private fun initializePlayerPosition() {
        playerX = blockSize * 2
        playerY = maxY - blocks.size * blockSize - pixelSize - 1
    }

    private fun showCurrentLevel() {
        isLevelTransitioning = true
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            renderer.showCurrentLevel(canvas, currentLevel, width, height)
            holder.unlockCanvasAndPost(canvas)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            initializeBlocks()
            initializePlayerPosition()
            isLevelTransitioning = false
            drawGame(holder)
        }, 2000)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        calculateMovementBounds()
        isLevelTransitioning = true
        showCurrentLevel()
        initializeBlocks()
        initializePlayerPosition()

        if (isInitialized) {
            playerManager.startHealthDecrement()
            drawGame(holder)
            fallHandler.post(fallRunnable)
            if (currentLevel >= 2) {
                laserHandler.post(laserRunnable)
            }
        }
    }

    fun drawGame(holder: SurfaceHolder) {
        if (!isInitialized || isLevelTransitioning) return
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            val gameState = GameState(
                isInitialized = isInitialized,
                isLevelTransitioning = isLevelTransitioning,
                playerManager = playerManager,
                blocks = blocks,
                items = items,
                blockSize = blockSize,
                pixelSize = pixelSize,
                playerX = playerX,
                playerY = playerY,
                maxY = maxY,
                activeTrap = currentTrap
            )
            renderer.drawGame(canvas, gameState)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun goToNextLevel() {
        if (isLevelTransitioning) return
        currentLevel++
        playerManager.isMovingUp = false
        playerManager.freezeRow = -1
        playerManager.player.depth += 5
        playerManager.player.score += 20
        if (currentLevel >= 2) {
            laserHandler.removeCallbacks(laserRunnable)  // Xóa laser callback cũ nếu có
            laserHandler.post(laserRunnable)
        }
        showCurrentLevel()
    }

    fun isPlayerAtBottomRow(): Boolean {
        val playerRow = ((maxY - playerY - pixelSize) / blockSize).toInt()
        return playerRow == 0
    }

    fun makeWoodBlocksFall() {
        var hasFloatingBlocks = false

        for (row in 1 until blocks.size) {
            for (col in blocks[row].indices) {
                val block = blocks[row][col]

                if (block.type == BlockType.WOOD) {
                    var currentRow = row

                    while (currentRow - 1 >= 0 && blocks[currentRow - 1][col].type == BlockType.EMPTY) {
                        blocks[currentRow - 1][col] = block
                        blocks[currentRow][col] = Block(BlockType.EMPTY, 0)
                        currentRow--
                        hasFloatingBlocks = true
                    }
                }
            }
        }

        if (hasFloatingBlocks) {
            drawGame(holder)
        }
    }

    fun checkStoneBlocks(): Map<String, Boolean> {
        val col = (playerX / blockSize).toInt()
        val row = ((maxY - playerY - pixelSize) / blockSize).toInt()

        val directions = mutableMapOf(
            "up" to false,
            "down" to false,
            "left" to false,
            "right" to false
        )

        if (row - 1 >= 0 && col in blocks[row - 1].indices) {
            directions["down"] = blocks[row - 1][col].type == BlockType.STONE
        }

        if (row + 1 < blocks.size && col in blocks[row + 1].indices) {
            directions["up"] = blocks[row + 1][col].type == BlockType.STONE
        }

        if (col - 1 >= 0 && row in blocks.indices && col - 1 in blocks[row].indices) {
            directions["left"] = blocks[row][col - 1].type == BlockType.STONE
        }

        if (col + 1 < numColumns && row in blocks.indices && col + 1 in blocks[row].indices) {
            directions["right"] = blocks[row][col + 1].type == BlockType.STONE
        }

        return directions
    }

    fun checkItemPickup() {
        val playerCenterX = playerX + pixelSize / 2
        val playerCenterY = playerY + pixelSize / 2

        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val itemCenterX = item.x + blockSize / 2
            val itemCenterY = item.y + blockSize / 2

            if (abs(playerCenterX - itemCenterX) < blockSize / 2 &&
                abs(playerCenterY - itemCenterY) < blockSize / 2
            ) {
                when (item.type) {
                    ItemType.HEART -> {
                        playerManager.player.health += 15
                    }
                    ItemType.RAINBOW_BANANA -> {
                        activateRainbowBananaEffect()
                    }
                }
                iterator.remove()
            }
        }
    }

    private fun activateRainbowBananaEffect() {
        if (isRainbowBananaActive) {
            rainbowBananaHandler.removeCallbacksAndMessages(null)
        } else {
            // Nếu chưa có hiệu ứng, kích hoạt hiệu ứng
            isRainbowBananaActive = true
            playerManager.setHealthDecrementMultiplier(0.5f)  // Giảm tốc độ mất máu
        }

        // Đặt lại hẹn giờ để kết thúc hiệu ứng sau 10 giây
        rainbowBananaHandler.postDelayed({
            isRainbowBananaActive = false
            playerManager.setHealthDecrementMultiplier(1f)  // Trả lại tốc độ mất máu bình thường
        }, 10000)
    }

    fun isRainbowBananaEffectActive(): Boolean = isRainbowBananaActive

    fun movePlayer(dx: Float, dy: Float) {
        playerManager.movePlayer(dx, dy)
    }

    fun digBlock(dx: Int, dy: Int) {
        playerManager.digBlock(dx, dy)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playerManager.stopHealthDecrement()
        fallHandler.removeCallbacks(fallRunnable)
        laserHandler.removeCallbacks(laserRunnable)
    }
}