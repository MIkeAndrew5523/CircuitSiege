package com.example.circuitcity_2.model

import android.content.Context
import kotlin.math.floor

class Level(
    val width: Int,
    val height: Int,
    private val grid: Array<CharArray>,
    val playerSpawnX: Int,
    val playerSpawnY: Int,
    val exitX: Int?,
    val exitY: Int?
) {
    // ---- Coordinate scale (world units -> tiles) ----
    // If your world positions are already expressed in tiles, keep this = 1f.
    // If they are pixels, set to your pixel tile size (e.g., 32f).
    private val TILE_SIZE: Float = 1f

    // ---- Tile accessors / queries ----
    fun tileAt(tx: Int, ty: Int): Tile {
        if (ty !in 0 until height || tx !in 0 until width) return Tile('#') // out-of-bounds as wall
        return Tile(grid[ty][tx])
    }

    fun hasExit(): Boolean = exitX != null && exitY != null

    fun isHazard(tx: Int, ty: Int)     = tileAt(tx, ty).hazard
    fun isKey(tx: Int, ty: Int)        = tileAt(tx, ty).key
    fun isCheckpoint(tx: Int, ty: Int) = tileAt(tx, ty).checkpoint
    fun isLockedDoor(tx: Int, ty: Int) = tileAt(tx, ty).lockedDoor
    fun isEnemySpawn(tx: Int, ty: Int) = tileAt(tx, ty).enemySpawn

    fun isSolid(tx: Int, ty: Int): Boolean {
        // Treat out-of-bounds as solid to avoid falling through edges
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true
        return tileAt(tx, ty).solid
    }

    /** Clear K, C, or E at (tx,ty) and return that char; else '\u0000'. */
    fun consumeAt(tx: Int, ty: Int): Char {
        if (ty !in 0 until height || tx !in 0 until width) return '\u0000'
        val c = grid[ty][tx]
        if (c == 'K' || c == 'C' || c == 'E') {
            grid[ty][tx] = '.'
            return c
        }
        return '\u0000'
    }

    /** If (tx,ty) is a locked door 'd', open it (set to '.') and report success. */
    fun unlockDoorAt(tx: Int, ty: Int): Boolean {
        if (ty !in 0 until height || tx !in 0 until width) return false
        if (grid[ty][tx] == 'd') {
            grid[ty][tx] = '.'
            return true
        }
        return false
    }

    // ---- World<->Tile helpers ----
    private fun worldToTileX(x: Float): Int = floor(x / TILE_SIZE).toInt()
    private fun worldToTileY(y: Float): Int = floor(y / TILE_SIZE).toInt()

    // ---- Collision helpers used by Physics.kt ----
    fun collidesHorizontally(x: Float, y: Float): Boolean {
        val tx = worldToTileX(x)
        val ty = worldToTileY(y)
        return isSolid(tx, ty)
    }

    fun collidesVertically(x: Float, y: Float): Boolean {
        val tx = worldToTileX(x)
        val ty = worldToTileY(y)
        return isSolid(tx, ty)
    }

    companion object {
        fun fromAsset(context: Context, path: String): Level {
            val lines = context.assets.open(path)
                .bufferedReader()
                .use { it.readLines().filter { s -> s.isNotEmpty() } }

            val h = lines.size
            val w = lines.maxOf { it.length }

            var spawnX = 1
            var spawnY = 1
            var ex: Int? = null
            var ey: Int? = null

            val grid = Array(h) { y ->
                val row = lines[y].padEnd(w, '#').toCharArray()
                for (x in row.indices) {
                    when (row[x]) {
                        '@' -> { spawnX = x; spawnY = y; row[x] = '.' }
                        'D' -> { ex = x; ey = y; /* keep 'D' so it can render; non-solid in Tile */ }
                        // 'd' (locked door) remains 'd' and is solid via Tile.solid
                    }
                }
                row
            }

            return Level(w, h, grid, spawnX, spawnY, ex, ey)
        }
    }
}
