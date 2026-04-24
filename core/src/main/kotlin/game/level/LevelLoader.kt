package game.level

import com.google.gson.Gson

object LevelLoader {
    private val gson = Gson()

    fun fromJson(json: String): LevelData = gson.fromJson(json, LevelData::class.java)
}
