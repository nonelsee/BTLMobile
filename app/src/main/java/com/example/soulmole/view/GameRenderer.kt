package com.example.soulmole.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.example.soulmole.activity.GameActivity
import com.example.soulmole.model.BlockType
import com.example.soulmole.model.GameState
import com.example.soulmole.model.Item
import com.example.soulmole.model.ItemType
import com.example.soulmole.model.Trap
import com.example.soulmole.model.TrapType
import java.util.Random

class GameRenderer(private val context: Context) {
    private val paint = Paint()

    fun drawGame(canvas: Canvas, gameState: GameState) {
        if (!gameState.isInitialized || gameState.isLevelTransitioning) return
        drawDungeonBackground(canvas, gameState)
        drawRestrictedArea(canvas, gameState)
        drawBlocks(canvas, gameState)
        drawItems(canvas, gameState.items, gameState.blockSize)
        gameState.activeTrap?.let { trap ->
            drawTrap(canvas, trap, gameState.blockSize)
        }
        drawMolePixelArt(canvas, gameState)
    }
    private fun drawDungeonBackground(canvas: Canvas, gameState: GameState) {
        val blockSize = gameState.blockSize
        val width = canvas.width
        val height = canvas.height

        val tileDark = Color.rgb(40, 40, 40)   // Màu tối
        val tileLight = Color.rgb(50, 50, 50) // Màu sáng hơn
        val crackColor = Color.rgb(30, 30, 30) // Màu cho các vết nứt

        val random = Random()

        for (y in 0..((height / blockSize).toInt())) {
            for (x in 0..((width / blockSize).toInt())) {
                paint.color = if ((x + y) % 2 == 0) tileDark else tileLight
                val left = x * blockSize
                val top = y * blockSize
                canvas.drawRect(
                    left,
                    top,
                    (left + blockSize),
                    (top + blockSize),
                    paint
                )

                if (random.nextFloat() < 0.3) { // 30% cơ hội xuất hiện vết nứt
                    drawCracks(canvas, left.toInt(), top.toInt(), blockSize.toInt(), crackColor)
                }
            }
        }
    }

    private fun drawCracks(canvas: Canvas, left: Int, top: Int, blockSize: Int, color: Int) {
        val random = Random()
        paint.color = color
        paint.strokeWidth = 2f
        for (i in 0..random.nextInt(3)) { // Vẽ 1-3 đường nứt trên mỗi tile
            val startX = left + random.nextInt(blockSize)
            val startY = top + random.nextInt(blockSize)
            val endX = startX + random.nextInt(blockSize / 2) - blockSize / 4
            val endY = startY + random.nextInt(blockSize / 2) - blockSize / 4
            canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), paint)
        }
    }

    fun showCurrentLevel(canvas: Canvas, level: Int, width: Int, height: Int) {
        canvas.drawColor(Color.WHITE)
        paint.color = Color.BLACK
        paint.textSize = 100f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Level $level", width / 2f, height / 2f, paint)
    }

    private fun drawRestrictedArea(canvas: Canvas, gameState: GameState) {
        val width = canvas.width
        val height = canvas.height

        // Tạo màu gradient
        val topColor = Color.rgb(178, 190, 211)
        val middleColor = Color.rgb(223, 212, 191)
        val bottomColor = Color.rgb(46, 39, 75)

        val gradient = LinearGradient(
            width * 5 / 7f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(topColor, middleColor, bottomColor),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(width * 5 / 7f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Vẽ đường kẻ màu đen
        paint.color = Color.BLACK
        canvas.drawLine(width * 5 / 7f, 0f, width * 5 / 7f, height.toFloat(), paint)

        // Vẽ chỉ số máu
        paint.color = Color.RED
        val centerX = width * 6 / 7f
        val centerY = height / 6f
        canvas.drawCircle(centerX, centerY, 50f, paint)

        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(gameState.playerManager.player.health.toString(), centerX, centerY + 15f, paint)

        // Vẽ DEPTH
        paint.color = Color.DKGRAY
        val rectLeft = width * 5.5f / 7f
        val rectTop = height / 3f
        val rectRight = width * 6.5f / 7f
        val rectBottom = rectTop + 100f
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)

        paint.color = Color.WHITE
        paint.textSize = 30f
        canvas.drawText("DEPTH", (rectLeft + rectRight) / 2, rectTop + 30f, paint)
        canvas.drawText(gameState.playerManager.player.depth.toString(), (rectLeft + rectRight) / 2, rectTop + 70f, paint)

        // Vẽ SCORE
        val scoreRectTop = rectTop + 120f
        val scoreRectBottom = scoreRectTop + 100f
        paint.color = Color.DKGRAY
        canvas.drawRect(rectLeft, scoreRectTop, rectRight, scoreRectBottom, paint)

        paint.color = Color.WHITE
        canvas.drawText("SCORE", (rectLeft + rectRight) / 2, scoreRectTop + 30f, paint)
        canvas.drawText(gameState.playerManager.player.score.toString(), (rectLeft + rectRight) / 2, scoreRectTop + 70f, paint)
    }

    private fun drawBlocks(canvas: Canvas, gameState: GameState) {
        for (row in gameState.blocks.indices) {
            for (col in gameState.blocks[row].indices) {
                val block = gameState.blocks[row][col]
                val x = col * gameState.blockSize
                val y = gameState.maxY - (row + 1) * gameState.blockSize

                when (block.type) {
                    BlockType.DIRT -> drawDirtBlock(canvas, x, y, gameState.blockSize)
                    BlockType.STONE -> drawStoneBlock(canvas, x, y, gameState.blockSize)
                    BlockType.WOOD -> drawWoodBlock(canvas, x, y, gameState.blockSize)
                    BlockType.EMPTY -> {} // Do nothing for empty blocks
                }
            }
        }
    }

    private fun drawDirtBlock(canvas: Canvas, x: Float, y: Float, blockSize: Float) {
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

        paint.color = Color.rgb(120, 60, 15)
        for (i in 0 until 5) {
            val dotX = x + (Math.random() * blockSize).toFloat()
            val dotY = y + (Math.random() * blockSize).toFloat()
            canvas.drawCircle(dotX, dotY, 6f, paint)
        }
    }

    private fun drawStoneBlock(canvas: Canvas, x: Float, y: Float, blockSize: Float) {
        paint.color = Color.DKGRAY
        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

        paint.color = Color.rgb(169, 169, 169)
        for (i in 0 until 5) {
            val lineX1 = x + (Math.random() * blockSize / 2).toFloat()
            val lineY1 = y + (Math.random() * blockSize).toFloat()
            val lineX2 = lineX1 + (Math.random() * blockSize / 2).toFloat()
            val lineY2 = lineY1 + (Math.random() * 5).toFloat()
            canvas.drawLine(lineX1, lineY1, lineX2, lineY2, paint)
        }
    }

    private fun drawWoodBlock(canvas: Canvas, x: Float, y: Float, blockSize: Float) {
        paint.color = Color.parseColor("#deb887")
        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

        paint.color = Color.rgb(139, 69, 19)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE

        val centerX = x + blockSize / 2
        val centerY = y + blockSize / 2
        for (i in 1..3) {
            val radius = (blockSize / 4 * i) / 2f
            canvas.drawCircle(centerX, centerY, radius, paint)
        }

        for (i in 0 until 3) {
            val startX = x + (Math.random() * blockSize).toFloat()
            val startY = y + (Math.random() * blockSize).toFloat()
            val endX = startX + (Math.random() * blockSize / 4).toFloat()
            val endY = startY + (Math.random() * blockSize / 4).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        paint.style = Paint.Style.FILL
    }

    private fun drawItems(canvas: Canvas, items: List<Item>, blockSize: Float) {
        for (item in items) {
            when (item.type) {
                ItemType.HEART -> drawHeart(canvas, item.x, item.y, blockSize)
                ItemType.RAINBOW_BANANA -> drawRainbowBanana(canvas, item.x, item.y, blockSize)
            }
        }
    }

    private fun drawTrap(canvas: Canvas, trap: Trap, blockSize: Float) {
        when (trap.type) {
            TrapType.LASER -> {
                val paint = Paint().apply {
                    strokeWidth = blockSize * 2/3
                    alpha = 200

                    // Determine color based on trap's damage phase
                    color = if (trap.damagePhase == 0) {
                        // First second: Cyan color
                        Color.CYAN
                    } else {
                        // Second second: Original red/yellow gradient
                        Color.RED
                    }
                    if (trap.damagePhase == 0) {
                        // First second: Cyan gradient with multiple shades
                        shader = LinearGradient(
                            0f, 0f, 50f, 50f,
                            intArrayOf(
                                Color.rgb(64, 224, 208),   // Turquoise
                                Color.rgb(0, 206, 209),    // Dark Turquoise
                                Color.rgb(32, 178, 170),   // Light Sea Green
                                Color.rgb(72, 209, 204)    // Medium Turquoise
                            ),
                            floatArrayOf(0f, 0.33f, 0.66f, 1f),
                            Shader.TileMode.MIRROR
                        )
                    }
                    // Only apply shader for the second phase
                    else {
                        shader = LinearGradient(
                            0f, 0f, 50f, 50f,
                            intArrayOf(Color.RED, Color.YELLOW, Color.RED),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.MIRROR
                        )
                    }
                }

                if (trap.isHorizontal) {
                    val centerY = trap.startY + blockSize / 6
                    canvas.drawLine(0f, centerY, canvas.width.toFloat(), centerY, paint)

                    // Only draw sparks in the damage phase
                    if (trap.damagePhase > 0) {
                        val random = Random()
                        val sparkPaint = Paint().apply {
                            color = Color.YELLOW
                            alpha = 200
                        }
                        for (i in 0..15) {
                            val x = random.nextFloat() * canvas.width
                            val y = centerY + (random.nextFloat() - 0.5f) * blockSize/3
                            canvas.drawCircle(x, y, blockSize/10, sparkPaint)
                        }
                    }
                } else {
                    val centerX = trap.startX + blockSize / 6
                    canvas.drawLine(centerX, 0f, centerX, canvas.height.toFloat(), paint)

                    // Only draw sparks in the damage phase
                    if (trap.damagePhase > 0) {
                        val random = Random()
                        val sparkPaint = Paint().apply {
                            color = Color.YELLOW
                            alpha = 200
                        }
                        for (i in 0..15) {
                            val x = centerX + (random.nextFloat() - 0.5f) * blockSize/3
                            val y = random.nextFloat() * canvas.height
                            canvas.drawCircle(x, y, blockSize/10, sparkPaint)
                        }
                    }
                }
            }
        }
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, blockSize: Float) {
        val heartSize = blockSize / 2 // Kích thước trái tim
        val centerX = x + heartSize / 2
        val centerY = y + heartSize / 2

        paint.color = Color.RED
        paint.style = Paint.Style.FILL

        // Sử dụng Path để tạo hình trái tim
        val path = Path()

        // Bắt đầu từ đỉnh trái tim (trên cùng giữa)
        path.moveTo(centerX, centerY - heartSize / 4)

        // Vẽ nửa bên trái của trái tim bằng đường cong Bézier
        path.cubicTo(
            centerX - heartSize / 2, centerY - heartSize / 2, // Điểm điều khiển đầu tiên
            centerX - heartSize / 2, centerY + heartSize / 3, // Điểm điều khiển thứ hai
            centerX, centerY + heartSize / 2 // Điểm cuối (chóp dưới)
        )

        // Vẽ nửa bên phải của trái tim bằng đường cong Bézier
        path.cubicTo(
            centerX + heartSize / 2, centerY + heartSize / 3, // Điểm điều khiển đầu tiên
            centerX + heartSize / 2, centerY - heartSize / 2, // Điểm điều khiển thứ hai
            centerX, centerY - heartSize / 4 // Điểm quay lại đỉnh
        )

        path.close() // Đóng đường dẫn
        canvas.drawPath(path, paint)
    }
    private fun drawRainbowBanana(canvas: Canvas, x: Float, y: Float, blockSize: Float) {
        val bananaSize = blockSize * 1f
        val centerX = x + blockSize / 2
        val centerY = y + blockSize / 2

        // Vẽ thân chuối với đường cong kép
        val bananaPath = Path()

        // Điểm bắt đầu - mở rộng phần trên
        bananaPath.moveTo(centerX - bananaSize * 0.25f, centerY - bananaSize * 0.3f)

        // Đường cong chính bên ngoài - điều chỉnh để phần dưới to hơn
        bananaPath.quadTo(
            centerX + bananaSize * 0.6f,  // Giữ nguyên điểm điều khiển X
            centerY - bananaSize * 0.1f,  // Giữ nguyên điểm điều khiển Y
            centerX - bananaSize * 0.2f,  // Di chuyển điểm cuối ra xa hơn
            centerY + bananaSize * 0.4f   // Giữ nguyên độ cao
        )

        // Đường cong ngược lại - điều chỉnh để tạo phần lõm cân đối
        bananaPath.quadTo(
            centerX + bananaSize * 0.15f, // Giảm độ lõm một chút
            centerY + bananaSize * 0.2f,  // Giữ nguyên điểm điều khiển Y
            centerX - bananaSize * 0.25f, // Khớp với điểm bắt đầu
            centerY - bananaSize * 0.3f   // Khớp với điểm bắt đầu
        )

        bananaPath.close()

        // Gradient cầu vồng với màu rõ hơn và transition mượt hơn
        val rainbowShader = LinearGradient(
            centerX - bananaSize / 2,
            centerY - bananaSize / 2,
            centerX + bananaSize / 2,
            centerY + bananaSize / 2,
            intArrayOf(
                Color.rgb(255, 0, 0),     // Đỏ tươi
                Color.rgb(255, 165, 0),   // Cam
                Color.rgb(255, 255, 0),   // Vàng
                Color.rgb(0, 255, 0),     // Lục
                Color.rgb(0, 150, 255),   // Lam
                Color.rgb(75, 0, 130),    // Chàm
                Color.rgb(238, 130, 238)  // Tím
            ),
            floatArrayOf(0f, 0.17f, 0.34f, 0.51f, 0.68f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )

        // Thiết lập paint với shader mới
        paint.shader = rainbowShader
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = bananaSize * 0.15f

        // Vẽ viền trắng
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = bananaSize * 0.08f
        canvas.drawPath(bananaPath, paint)

        // Vẽ phần filled
        paint.style = Paint.Style.FILL
        paint.shader = rainbowShader
        canvas.drawPath(bananaPath, paint)

        paint.shader = null
    }

    private fun drawRainbowBananaHalo(canvas: Canvas, playerX: Float, playerY: Float, pixelSize: Float) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = pixelSize * 0.1f

        // Create rainbow gradient
        val haloRadius = pixelSize * 0.6f
        val centerX = playerX + pixelSize * 0.5f
        val centerY = playerY + pixelSize * 0.5f

        val haloShader = LinearGradient(
            centerX - haloRadius,
            centerY - haloRadius,
            centerX + haloRadius,
            centerY + haloRadius,
            intArrayOf(
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.BLUE,
                Color.MAGENTA
            ),
            null,
            Shader.TileMode.REPEAT
        )

        paint.shader = haloShader
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = pixelSize * 0.1f

        // Draw multiple rings with decreasing opacity
        for (i in 1..3) {
            val ringRadius = haloRadius * (1f - i * 0.1f)
            paint.alpha = 255 - (i * 60)
            canvas.drawCircle(centerX, centerY, ringRadius, paint)
        }

        paint.shader = null
    }

    private fun drawMolePixelArt(canvas: Canvas, gameState: GameState) {
        val moleBodyColor = Color.rgb(102, 51, 0)
        val moleEyeColor = Color.BLACK
        val moleNoseColor = Color.rgb(255, 100, 100)
        val moleHandColor = Color.rgb(153, 76, 0)
        val pickaxeHandleColor = Color.rgb(139, 69, 19)
        val pickaxeHeadColor = Color.DKGRAY
        val moleToothColor = Color.WHITE

        val pixelSize = gameState.pixelSize
        val playerX = gameState.playerX
        val playerY = gameState.playerY

        // Body
        paint.color = moleBodyColor
        val bodyLeft = playerX + pixelSize * 0.15f
        val bodyRight = playerX + pixelSize * 0.85f
        val bodyTop = playerY + pixelSize * 0.4f
        val bodyBottom = playerY + pixelSize * 1.1f
        canvas.drawOval(bodyLeft, bodyTop, bodyRight, bodyBottom, paint)

        // Head
        val headRadius = pixelSize * 0.3f
        val headCenterX = playerX + pixelSize * 0.5f
        val headCenterY = playerY + pixelSize * 0.35f
        canvas.drawCircle(headCenterX, headCenterY, headRadius, paint)

        // Ears
        val leftEarCenterX = playerX + pixelSize * 0.35f
        val rightEarCenterX = playerX + pixelSize * 0.65f
        val earCenterY = playerY + pixelSize * 0.15f
        val earRadius = pixelSize * 0.1f
        canvas.drawCircle(leftEarCenterX, earCenterY, earRadius, paint)
        canvas.drawCircle(rightEarCenterX, earCenterY, earRadius, paint)

        // Eyes
        paint.color = moleEyeColor
        val eyeRadius = pixelSize * 0.05f
        canvas.drawCircle(playerX + pixelSize * 0.4f, playerY + pixelSize * 0.3f, eyeRadius, paint)
        canvas.drawCircle(playerX + pixelSize * 0.6f, playerY + pixelSize * 0.3f, eyeRadius, paint)

        // Nose
        paint.color = moleNoseColor
        canvas.drawCircle(playerX + pixelSize * 0.5f, playerY + pixelSize * 0.45f, pixelSize * 0.07f, paint)

        // Teeth
        paint.color = moleToothColor
        canvas.drawRect(
            playerX + pixelSize * 0.45f,
            playerY + pixelSize * 0.55f,
            playerX + pixelSize * 0.48f,
            playerY + pixelSize * 0.65f,
            paint
        )
        canvas.drawRect(
            playerX + pixelSize * 0.52f,
            playerY + pixelSize * 0.55f,
            playerX + pixelSize * 0.55f,
            playerY + pixelSize * 0.65f,
            paint
        )

        // Hands
        paint.color = moleHandColor
        // Left hand
        canvas.drawOval(
            playerX + pixelSize * 0.15f,
            playerY + pixelSize * 0.6f,
            playerX + pixelSize * 0.3f,
            playerY + pixelSize * 0.75f,
            paint
        )
        // Right hand
        canvas.drawOval(
            playerX + pixelSize * 0.7f,
            playerY + pixelSize * 0.6f,
            playerX + pixelSize
                    * 0.85f,
            playerY + pixelSize * 0.75f,
            paint
        )

        // Pickaxe
        paint.color = pickaxeHandleColor
        canvas.drawRect(
            playerX + pixelSize * 0.2f,
            playerY + pixelSize * 0.4f,
            playerX + pixelSize * 0.35f,
            playerY + pixelSize * 0.8f,
            paint
        )

        paint.color = pickaxeHeadColor
        canvas.drawRect(
            playerX + pixelSize * 0.15f,
            playerY + pixelSize * 0.35f,
            playerX + pixelSize * 0.4f,
            playerY + pixelSize * 0.45f,
            paint
        )
        if ((context as? GameActivity)?.gameView?.isRainbowBananaEffectActive() == true) {
            drawRainbowBananaHalo(canvas, playerX, playerY, pixelSize)
        }
    }
}