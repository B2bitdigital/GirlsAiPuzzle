package game.android.ads

import android.app.Activity
import android.view.Gravity
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(
    private val activity: Activity,
    private val bannerId: String,
    private val interstitialId: String
) {
    private var interstitialAd: InterstitialAd? = null
    private var bannerView: AdView? = null

    fun initialize() {
        MobileAds.initialize(activity)
        loadInterstitial()
    }

    private fun loadInterstitial() {
        val req = AdRequest.Builder().build()
        InterstitialAd.load(activity, interstitialId, req, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null }
        })
    }

    fun showInterstitialIfReady() {
        val ad = interstitialAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    fun createBannerAndAttach(layout: FrameLayout) {
        bannerView?.destroy()
        val banner = AdView(activity).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = bannerId
            loadAd(AdRequest.Builder().build())
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        layout.addView(banner, params)
        bannerView = banner
    }

    fun destroyBanner() {
        bannerView?.destroy()
        bannerView = null
    }

    fun pauseBanner() = bannerView?.pause()
    fun resumeBanner() = bannerView?.resume()
}
