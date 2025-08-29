package com.example.circuitcity_2.model

import android.content.Context

class Level(
    val width: Int,
    val height: Int,
    private val grid: Array<CharArray>,
    val playerSpawnX: Int,
    val playerSpawnY: Int,
    val exitX: Int?,
    val exitY: Int?
) {
    fun tileAt(tx: Int, ty: Int): Tile {
        if (ty !in 0 until height || tx !in 0 until width) return Tile('#')
        return Tile(grid[ty][tx])
    }

    fun hasExit(): Boolean = exitX != null && exitY != null

    // queries
    fun isHazard(tx: Int, ty: Int)     = tileAt(tx, ty).hazard
    fun isKey(tx: Int, ty: Int)        = tileAt(tx, ty).key
    fun isCheckpoint(tx: Int, ty: Int) = tileAt(tx, ty).checkpoint
    fun isLockedDoor(tx: Int, ty: Int) = tileAt(tx, ty).lockedDoor
    // Level.kt
    fun isSolid(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true // treat OOB as solid
        return tileAt(tx, ty).solid
    }
    fun isEnemySpawn(tx: Int, ty: Int): Boolean =
        tileAt(tx, ty).enemySpawn




    /** Clear K or C at (tx,ty) and return that char, else '\u0000'. */
        fun consumeAt(tx: Int, ty: Int): Char {
            if (ty !in 0 until height || tx !in 0 until width) return '\u0000'
            val c = grid[ty][tx]
            if (c == 'K' || c == 'C' || c == 'E') {
                grid[ty][tx] = '.'
                return c
            }
            return '\u0000'
        }

    // If (tx,ty) is a locked door 'd', open it (set to '.') and report success.
    fun unlockDoorAt(tx: Int, ty: Int): Boolean {
        if (ty !in 0 until height || tx !in 0 until width) return false
        if (grid[ty][tx] == 'd') {
            grid[ty][tx] = '.'
            return true
        }
        return false
    }


    companion object {
        fun fromAsset(context: Context, path: String): Level {
            val lines = context.assets.open(path).bufferedReader().use { it.readLines().filter { s -> s.isNotEmpty() } }
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
                        'D' -> { ex = x; ey = y; /* keep 'D' in grid so it can render; treat as non-solid */ }
                        // 'd' (locked door) stays as 'd' and is SOLID via Tile.solid
                    }
                }
                row
            }
            return Level(w, h, grid, spawnX, spawnY, ex, ey)
        }
    }
}
