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

/**
 * Main activity for CircuitCity 2. Initializes the game engine, manages screen transitions,
 * and handles Android lifecycle events.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var engine: GameEngine
    private lateinit var sm: ScreenManager

    /**
     * Called when the activity is created. Sets up the game engine and initial screen.
     * @param savedInstanceState Saved instance state bundle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engine = GameEngine(this)
        setContentView(engine)

        sm = engine.screens()

        // Start at Title
        engine.setInitialScreen(makeTitle())
    }

    /** Called when the activity resumes. Starts the game engine. */
    override fun onResume() { super.onResume(); engine.start() }
    /** Called when the activity pauses. Stops the game engine. */
    override fun onPause()  { engine.stop(); super.onPause() }

    /** ---- Screen factories (functions avoid the self-reference issue) ---- **/

    /**
     * Factory for the title screen.
     * @return TitleScreen instance
     */
    private fun makeTitle(): Screen =
        TitleScreen(sm, ::makeBackstory)

    /**
     * Factory for the backstory screen.
     * @return BackstoryScreen instance
     */
    private fun makeBackstory(): Screen =
        BackstoryScreen(this, sm, ::makeLevel1)

    /**
     * Factory for level 1 screen.
     * @return GameScreen instance for level 1
     */
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

    /**
     * Factory for level 2 screen.
     * @return GameScreen instance for level 2
     */
    private fun makeLevel2(): Screen =
        GameScreen(
            context = this,
            sm = sm,
            levelPath = "levels/level_02.txt",
            onComplete = ::makeVictory
        )

    /**
     * Factory for the victory screen.
     * @return VictoryScreen instance
     */
    private fun makeVictory(): Screen =
        VictoryScreen(
            sm = sm,
            makeNext = null,                // or ::makeLevel3 later
            makeTitle = ::makeTitle,
            minShowSec = 3.0f,              // wait 3s before taps are accepted
            autoAdvanceSec = 10.0f          // optional: auto-return after 10s
        )
}
