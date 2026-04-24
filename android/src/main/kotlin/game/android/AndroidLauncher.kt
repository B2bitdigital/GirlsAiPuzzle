package game.android

import android.os.Bundle
import android.widget.FrameLayout
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import game.GirlsPanicGame
import game.android.ads.AdManager
import game.android.persistence.AndroidGamePrefs
import game.girlsaipanic.BuildConfig

class AndroidLauncher : AndroidApplication() {

    private lateinit var adManager: AdManager
    private lateinit var gameInstance: GirlsPanicGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = AndroidGamePrefs(this)
        adManager = AdManager(this, BuildConfig.ADMOB_BANNER_ID, BuildConfig.ADMOB_INTERSTITIAL_ID)
        adManager.initialize()

        gameInstance = GirlsPanicGame(prefs)

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
        }

        val layout = initializeForView(gameInstance, config) as FrameLayout
        adManager.createBannerAndAttach(layout)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        adManager.resumeBanner()
    }

    override fun onPause() {
        super.onPause()
        adManager.pauseBanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        adManager.destroyBanner()
    }
}
