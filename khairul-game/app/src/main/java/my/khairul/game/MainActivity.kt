package my.khairul.game

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var board: GridLayout
    private lateinit var status: TextView
    private lateinit var movesText: TextView
    private var tiles = (1..15).toMutableList().apply { add(0) }
    private var moves = 0
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private lateinit var banner: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        buildScreen()
        shuffleBoard()
        loadInterstitial()
        loadRewarded()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(18, 24, 18, 12)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        val title = TextView(this).apply {
            text = "KHAIRUL GAME"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "PUZZLE 15 • Susun nombor 1 hingga 15"
            textSize = 14f
            setTextColor(Color.rgb(203, 213, 225))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 18)
        }
        status = TextView(this).apply {
            text = "Gerakkan jubin ke ruang kosong"
            textSize = 16f
            setTextColor(Color.rgb(125, 211, 252))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 6)
        }
        movesText = TextView(this).apply {
            text = "Langkah: 0"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 14)
        }
        board = GridLayout(this).apply {
            columnCount = 4
            rowCount = 4
            useDefaultMargins = true
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        val boardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { gravity = Gravity.CENTER }
        root.addView(title, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(subtitle, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(status, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(movesText, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(board, boardParams)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 8)
        }
        buttons.addView(makeActionButton("Kocok Semula") { shuffleBoard() }, LinearLayout.LayoutParams(0, 52, 1f))
        buttons.addView(makeActionButton("Ganjaran ▶") { showRewarded() }, LinearLayout.LayoutParams(0, 52, 1f).apply { leftMargin = 8 })
        root.addView(buttons, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        banner = AdView(this).apply {
            adUnitId = "ca-app-pub-6620088049494024/1757609187"
            setAdSize(AdSize.BANNER)
        }
        root.addView(banner, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        setContentView(root)
        banner.loadAd(AdRequest.Builder().build())
    }

    private fun makeActionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 13f
            setOnClickListener { action() }
        }

    private fun shuffleBoard() {
        do {
            tiles = (1..15).toMutableList().apply { add(0) }
            repeat(250) {
                val empty = tiles.indexOf(0)
                val options = mutableListOf<Int>()
                if (empty % 4 > 0) options.add(empty - 1)
                if (empty % 4 < 3) options.add(empty + 1)
                if (empty >= 4) options.add(empty - 4)
                if (empty < 12) options.add(empty + 4)
                val target = options[Random.nextInt(options.size)]
                tiles[empty] = tiles[target]
                tiles[target] = 0
            }
        } while (isSolved())
        moves = 0
        status.text = "Gerakkan jubin ke ruang kosong"
        updateBoard()
    }

    private fun updateBoard() {
        board.removeAllViews()
        val side = (resources.displayMetrics.widthPixels - (18 * 2 + 24)) / 4
        tiles.forEachIndexed { index, value ->
            val cell = Button(this).apply {
                text = if (value == 0) "" else value.toString()
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                setTextColor(if (value == 0) Color.TRANSPARENT else Color.WHITE)
                setBackgroundColor(
                    if (value == 0) Color.rgb(30, 41, 59)
                    else Color.rgb(37, 99, 235)
                )
                setOnClickListener { moveTile(index) }
            }
            board.addView(cell, GridLayout.LayoutParams().apply {
                width = side
                height = side
                setMargins(3, 3, 3, 3)
            })
        }
        movesText.text = "Langkah: $moves"
    }

    private fun moveTile(index: Int) {
        val empty = tiles.indexOf(0)
        val adjacent = (kotlin.math.abs(index / 4 - empty / 4) +
                kotlin.math.abs(index % 4 - empty % 4)) == 1
        if (!adjacent) return
        tiles[empty] = tiles[index]
        tiles[index] = 0
        moves++
        updateBoard()
        if (isSolved()) {
            status.text = "Tahniah! Puzzle selesai!"
            Toast.makeText(this, "Hebat! Anda berjaya dalam $moves langkah.", Toast.LENGTH_LONG).show()
            interstitial?.show(this)
            loadInterstitial()
        }
    }

    private fun isSolved(): Boolean = tiles == (1..15).toList() + 0

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            "ca-app-pub-6620088049494024/3461573098",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            }
        )
    }

    private fun loadRewarded() {
        RewardedAd.load(
            this,
            "ca-app-pub-3940256099942544/5224354917",
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
            }
        )
    }

    private fun showRewarded() {
        val ad = rewarded
        if (ad == null) {
            Toast.makeText(this, "Iklan ganjaran sedang dimuatkan. Cuba lagi sebentar.", Toast.LENGTH_SHORT).show()
            loadRewarded()
            return
        }
        ad.show(this) {
            Toast.makeText(this, "Ganjaran diterima! Terima kasih.", Toast.LENGTH_LONG).show()
            loadRewarded()
        }
    }

    override fun onDestroy() {
        banner.destroy()
        super.onDestroy()
    }
}
