package com.example.circuitcity_2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.circuitcity_2.engine.GameEngine
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager
import com.example.circuitcity_2.engine.GameScreen
import com.example.circuitcity_2.ui.BackstoryScreen
import com.example.circuitcity_2.ui.TitleScreen
import com.example.circuitcity_2.ui.VictoryScreen
import com.example.circuitcity_2.ui.theme.TransitionScreen   // ✅ import added

class MainActivity : AppCompatActivity() {

    private lateinit var engine: GameEngine
    private lateinit var sm: ScreenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engine = GameEngine(this)
        setContentView(engine)

        sm = engine.screens()

        // Start at Title
        engine.setInitialScreen(makeTitle())
    }

    override fun onResume() { super.onResume(); engine.start() }
    override fun onPause()  { engine.stop(); super.onPause() }

    /** ---- Screen factories (functions avoid the self-reference issue) ---- **/

    private fun makeTitle(): Screen =
        TitleScreen(sm, ::makeBackstory)

    private fun makeBackstory(): Screen =
        BackstoryScreen(this, sm, ::makeLevel1)

    private fun makeLevel1(): Screen =
        GameScreen(
            context = this,
            sm = sm,
            levelPath = "levels/level_01.txt",
            onComplete = {
                TransitionScreen(
                    context = this,
                    sm = sm,
                    title = "Mission Update",
                    bodyLines = listOf(
                        "Well done infiltrating Sector 1!",
                        "Intel suggests the next zone is heavily guarded.",
                        "Prepare yourself..."
                    ),
                    nextScreen = { makeLevel2() }
                )
            }
        )

    private fun makeLevel2(): Screen =
        GameScreen(
            context = this,
            sm = sm,
            levelPath = "levels/level_02.txt",
            onComplete = ::makeVictory
        )

    private fun makeVictory(): Screen =
        VictoryScreen(
            sm = sm,
            makeNext = null,                // or ::makeLevel3 later
            makeTitle = ::makeTitle,
            minShowSec = 3.0f,              // wait 3s before taps are accepted
            autoAdvanceSec = 10.0f          // optional: auto-return after 10s
        )
}
