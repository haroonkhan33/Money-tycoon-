package com.example

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    enum class MissionType {
        CLICKS,
        TRADES,
        WORK_SHIFTS,
        BUSINESS_UPGRADES,
        VAULT_COLLECT,
        TAP_UPGRADE
    }

    data class DailyMission(
        val id: String,
        val title: String,
        val description: String,
        val type: MissionType,
        val targetCount: Int,
        var currentCount: Int = 0,
        val rewardCash: Double,
        var isClaimed: Boolean = false
    ) {
        val isCompleted: Boolean
            get() = currentCount >= targetCount
    }

    data class BusinessAsset(
        val id: Int,
        var name: String,
        var level: Int,
        var basePayout: Double,
        var upgradeCost: Double,
        var cycleProgress: Int = 0,
        val cycleSpeed: Int
    )

    data class CareerRank(
        val rank: Int,
        val title: String,
        val salaryPerShift: Double,
        val requiredShifts: Int,
        val promotionCost: Double,
        val requiresDegree: Boolean = false,
        val requiresMba: Boolean = false
    )

    data class StockAsset(
        val symbol: String,
        val name: String,
        val category: String,
        var currentPrice: Double,
        var changePercent: Double = 0.0,
        var high24h: Double,
        var low24h: Double,
        var ownedShares: Int = 0,
        var totalInvested: Double = 0.0,
        val priceHistory: MutableList<Double> = mutableListOf()
    ) {
        val avgBuyPrice: Double
            get() = if (ownedShares > 0) totalInvested / ownedShares else 0.0

        val currentValue: Double
            get() = ownedShares * currentPrice

        val unrealizedPnL: Double
            get() = currentValue - totalInvested

        val pnlPercent: Double
            get() = if (totalInvested > 0) (unrealizedPnL / totalInvested) * 100.0 else 0.0
    }

    // Player life state (Money Tycoon financial sim)
    private var totalNetWorth = 25.00
    private var uncollectedRevenue = 0.00
    private var totalManualClicks = 0
    private var playerAge = 21
    private var gameYear = 2026

    // Manual Clicker state (Starts at Level 1: 1 click = $1.00)
    private var clickLevel = 1
    private var clickValue = 1.00
    private var clickUpgradeCost = 20.00

    // Career System state
    private val careerRanks = listOf(
        CareerRank(1, "Dishwasher / Intern", 15.0, 5, 100.0),
        CareerRank(2, "Junior Developer", 45.0, 10, 500.0, requiresDegree = true),
        CareerRank(3, "Project Manager", 140.0, 15, 2000.0, requiresDegree = true),
        CareerRank(4, "VP of Engineering", 450.0, 25, 8000.0, requiresMba = true),
        CareerRank(5, "Chief Executive Officer", 1500.0, 50, 25000.0, requiresMba = true)
    )
    private var currentCareerRankIndex = 0
    private var completedShifts = 0
    private var hasCollegeDegree = false
    private var hasMba = false

    // Real Stock Market Assets (Simulated live exchange)
    private val stocks = listOf(
        StockAsset("NEON", "NEON Technologies", "Tech Equity", 120.00, 3.5, 128.50, 115.00).apply {
            listOf(112.0, 114.5, 113.0, 116.2, 115.0, 117.8, 119.0, 118.2, 120.5, 122.0, 119.5, 120.0).forEach { priceHistory.add(it) }
        },
        StockAsset("CYBER", "Cyber Coin", "Crypto Asset", 45.00, 8.2, 49.00, 38.50).apply {
            listOf(36.0, 37.5, 35.0, 39.0, 42.0, 40.5, 43.0, 41.2, 44.0, 46.5, 43.8, 45.0).forEach { priceHistory.add(it) }
        },
        StockAsset("VOLT", "Volt Electric Motors", "EV Auto", 85.00, -2.1, 92.00, 82.00).apply {
            listOf(90.0, 89.0, 91.5, 88.0, 87.2, 89.0, 86.5, 87.0, 85.5, 86.0, 84.0, 85.0).forEach { priceHistory.add(it) }
        },
        StockAsset("GLD", "Gold Reserve ETF", "Commodity", 210.00, 0.9, 215.00, 206.00).apply {
            listOf(205.0, 206.2, 207.0, 206.8, 208.5, 207.9, 209.0, 208.5, 210.0, 209.5, 210.5, 210.0).forEach { priceHistory.add(it) }
        }
    )

    private var selectedStockIndex = 0
    private var selectedOrderQty = 1 // 1, 5, 10, or -1 for MAX

    // Businesses
    private val businesses = mutableListOf(
        BusinessAsset(1, "Coffee Kiosk", 0, 5.0, 50.0, 0, 3),
        BusinessAsset(2, "Tech Agency", 0, 18.0, 200.0, 0, 2),
        BusinessAsset(3, "Real Estate Firm", 0, 65.0, 800.0, 0, 2),
        BusinessAsset(4, "Crypto Mining Rig", 0, 240.0, 3200.0, 0, 1),
        BusinessAsset(5, "BioTech Genetics", 0, 850.0, 12500.0, 0, 1),
        BusinessAsset(6, "Aerospace Corp", 0, 3200.0, 50000.0, 0, 1)
    )

    // Daily Missions Pool & Current Active Missions
    private val allMissionsPool = listOf(
        DailyMission("m_clicks_50", "Tap Master", "Make 50 manual clicks", MissionType.CLICKS, 50, 0, 250.0),
        DailyMission("m_clicks_100", "Click Champion", "Make 100 manual clicks", MissionType.CLICKS, 100, 0, 500.0),
        DailyMission("m_trades_3", "Market Operator", "Execute 3 stock or crypto trades", MissionType.TRADES, 3, 0, 450.0),
        DailyMission("m_trades_5", "Wall Street Pro", "Execute 5 stock or crypto trades", MissionType.TRADES, 5, 0, 800.0),
        DailyMission("m_shifts_3", "Corporate Hustle", "Complete 3 career work shifts", MissionType.WORK_SHIFTS, 3, 0, 350.0),
        DailyMission("m_shifts_5", "Executive Climber", "Complete 5 career work shifts", MissionType.WORK_SHIFTS, 5, 0, 650.0),
        DailyMission("m_upgrades_2", "Enterprise Mogul", "Upgrade any business 2 times", MissionType.BUSINESS_UPGRADES, 2, 0, 500.0),
        DailyMission("m_vault_2", "Vault Collector", "Collect cash from the vault 2 times", MissionType.VAULT_COLLECT, 2, 0, 200.0),
        DailyMission("m_tap_up_1", "Power Surge", "Upgrade your manual tap power", MissionType.TAP_UPGRADE, 1, 0, 300.0)
    )

    private val activeMissions = mutableListOf<DailyMission>()

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    private val compactFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    // Containers for bottom nav switching
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var layoutCash: LinearLayout
    private lateinit var layoutInvestments: LinearLayout
    private lateinit var layoutJobs: LinearLayout
    private lateinit var layoutTrading: LinearLayout

    // Touch coordinate tracker for business card clicks
    private val cardLastTouchCoords = Array(6) { FloatArray(2) { -1f } }

    // Top Header Views
    private lateinit var tvPlayerLevel: TextView
    private lateinit var btnTopMissions: View
    private lateinit var btnTopSettings: View
    private lateinit var tvMissionsBadge: TextView
    private lateinit var tvTotalNetWorth: TextView
    private lateinit var tvNetWorthGrowth: TextView
    private lateinit var tvMonthlyCashFlow: TextView
    private lateinit var tvDailyRate: TextView
    private lateinit var btnCollectCash: MaterialButton
    private lateinit var progressRevenueCycle: LinearProgressIndicator
    private lateinit var bottomNav: BottomNavigationView

    private var missionsBottomSheetDialog: BottomSheetDialog? = null
    private var settingsBottomSheetDialog: BottomSheetDialog? = null
    private var isAdminUnlocked = false

    // Manual Clicker UI
    private lateinit var containerTapArea: FrameLayout
    private lateinit var cardManualTapTarget: View
    private lateinit var imgTapIcon: ImageView
    private lateinit var tvTapPowerBadge: TextView
    private lateinit var tvTapStats: TextView
    private lateinit var btnUpgradeTapPower: MaterialButton

    // Business Card Views
    private lateinit var card1: MaterialCardView
    private lateinit var tvBiz1Lvl: TextView
    private lateinit var tvBiz1Income: TextView
    private lateinit var progBiz1: LinearProgressIndicator
    private lateinit var btnUpgradeBiz1: MaterialButton

    private lateinit var card2: MaterialCardView
    private lateinit var tvBiz2Lvl: TextView
    private lateinit var tvBiz2Income: TextView
    private lateinit var progBiz2: LinearProgressIndicator
    private lateinit var btnUpgradeBiz2: MaterialButton

    private lateinit var card3: MaterialCardView
    private lateinit var tvBiz3Lvl: TextView
    private lateinit var tvBiz3Income: TextView
    private lateinit var progBiz3: LinearProgressIndicator
    private lateinit var btnUpgradeBiz3: MaterialButton

    private lateinit var card4: MaterialCardView
    private lateinit var tvBiz4Lvl: TextView
    private lateinit var tvBiz4Income: TextView
    private lateinit var progBiz4: LinearProgressIndicator
    private lateinit var btnUpgradeBiz4: MaterialButton

    private lateinit var card5: MaterialCardView
    private lateinit var tvBiz5Lvl: TextView
    private lateinit var tvBiz5Income: TextView
    private lateinit var progBiz5: LinearProgressIndicator
    private lateinit var btnUpgradeBiz5: MaterialButton

    private lateinit var card6: MaterialCardView
    private lateinit var tvBiz6Lvl: TextView
    private lateinit var tvBiz6Income: TextView
    private lateinit var progBiz6: LinearProgressIndicator
    private lateinit var btnUpgradeBiz6: MaterialButton

    // Jobs Section UI
    private lateinit var tvCurrentJobTitle: TextView
    private lateinit var tvJobShiftSalary: TextView
    private lateinit var tvCareerLevelBadge: TextView
    private lateinit var btnDoWorkShift: MaterialButton
    private lateinit var tvCareerProgressLabel: TextView
    private lateinit var progCareerShift: LinearProgressIndicator
    private lateinit var btnApplyPromotion: MaterialButton
    private lateinit var tvEducationStatus: TextView
    private lateinit var btnEnrollDegree: MaterialButton

    // Trading Terminal UI
    private lateinit var tvPortfolioTotal: TextView
    private lateinit var tvPortfolioPnL: TextView
    private lateinit var tvPortfolioCashAvailable: TextView
    private lateinit var tvMarketNews: TextView

    private lateinit var chipStockNeon: TextView
    private lateinit var chipStockCyber: TextView
    private lateinit var chipStockVolt: TextView
    private lateinit var chipStockGold: TextView

    private lateinit var tvTerminalSymbol: TextView
    private lateinit var tvTerminalHolding: TextView
    private lateinit var tvTerminalPrice: TextView
    private lateinit var tvTerminalChange: TextView
    private lateinit var tvTerminalHighLow: TextView
    private lateinit var tvTerminalPositionValue: TextView
    private lateinit var chartTerminal: TradingChartView

    private lateinit var btnQty1: MaterialButton
    private lateinit var btnQty5: MaterialButton
    private lateinit var btnQty10: MaterialButton
    private lateinit var btnQtyMax: MaterialButton
    private lateinit var btnTerminalBuy: MaterialButton
    private lateinit var btnTerminalSell: MaterialButton

    // Watchlist Cards
    private lateinit var cardWatchlist1: MaterialCardView
    private lateinit var tvWatch1Price: TextView
    private lateinit var tvWatch1Change: TextView
    private lateinit var tvWatch1Sub: TextView

    private lateinit var cardWatchlist2: MaterialCardView
    private lateinit var tvWatch2Price: TextView
    private lateinit var tvWatch2Change: TextView
    private lateinit var tvWatch2Sub: TextView

    private lateinit var cardWatchlist3: MaterialCardView
    private lateinit var tvWatch3Price: TextView
    private lateinit var tvWatch3Change: TextView
    private lateinit var tvWatch3Sub: TextView

    private lateinit var cardWatchlist4: MaterialCardView
    private lateinit var tvWatch4Price: TextView
    private lateinit var tvWatch4Change: TextView
    private lateinit var tvWatch4Sub: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bindViews()
        setupWindowInsets()
        initDailyMissions()
        setupEventListeners()
        startIdleCycleSimulation()
        startStockMarketSimulation()

        updateDashboardDisplays()
        updateCareerDisplays()
        updateTradingTerminalDisplay()
        updateMissionsDisplay()
    }

    private fun bindViews() {
        // Layout Sections
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        layoutCash = findViewById(R.id.layoutCash)
        layoutInvestments = findViewById(R.id.layoutInvestments)
        layoutJobs = findViewById(R.id.layoutJobs)
        layoutTrading = findViewById(R.id.layoutTrading)

        // Top Header
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        btnTopMissions = findViewById(R.id.btnTopMissions)
        btnTopSettings = findViewById(R.id.btnTopSettings)
        tvMissionsBadge = findViewById(R.id.tvMissionsBadge)
        tvTotalNetWorth = findViewById(R.id.tvTotalNetWorth)
        tvNetWorthGrowth = findViewById(R.id.tvNetWorthGrowth)
        tvMonthlyCashFlow = findViewById(R.id.tvMonthlyCashFlow)
        tvDailyRate = findViewById(R.id.tvDailyRate)
        btnCollectCash = findViewById(R.id.btnCollectCash)
        progressRevenueCycle = findViewById(R.id.progressRevenueCycle)
        bottomNav = findViewById(R.id.bottomNavigationView)

        // Manual Clicker
        containerTapArea = findViewById(R.id.containerTapArea)
        cardManualTapTarget = findViewById(R.id.cardManualTapTarget)
        imgTapIcon = findViewById(R.id.imgTapIcon)
        tvTapPowerBadge = findViewById(R.id.tvTapPowerBadge)
        tvTapStats = findViewById(R.id.tvTapStats)
        btnUpgradeTapPower = findViewById(R.id.btnUpgradeTapPower)

        // Businesses
        card1 = findViewById(R.id.cardAsset1)
        tvBiz1Lvl = findViewById(R.id.tvBiz1Lvl)
        tvBiz1Income = findViewById(R.id.tvBiz1Income)
        progBiz1 = findViewById(R.id.progBiz1)
        btnUpgradeBiz1 = findViewById(R.id.btnUpgradeBiz1)

        card2 = findViewById(R.id.cardAsset2)
        tvBiz2Lvl = findViewById(R.id.tvBiz2Lvl)
        tvBiz2Income = findViewById(R.id.tvBiz2Income)
        progBiz2 = findViewById(R.id.progBiz2)
        btnUpgradeBiz2 = findViewById(R.id.btnUpgradeBiz2)

        card3 = findViewById(R.id.cardAsset3)
        tvBiz3Lvl = findViewById(R.id.tvBiz3Lvl)
        tvBiz3Income = findViewById(R.id.tvBiz3Income)
        progBiz3 = findViewById(R.id.progBiz3)
        btnUpgradeBiz3 = findViewById(R.id.btnUpgradeBiz3)

        card4 = findViewById(R.id.cardAsset4)
        tvBiz4Lvl = findViewById(R.id.tvBiz4Lvl)
        tvBiz4Income = findViewById(R.id.tvBiz4Income)
        progBiz4 = findViewById(R.id.progBiz4)
        btnUpgradeBiz4 = findViewById(R.id.btnUpgradeBiz4)

        card5 = findViewById(R.id.cardAsset5)
        tvBiz5Lvl = findViewById(R.id.tvBiz5Lvl)
        tvBiz5Income = findViewById(R.id.tvBiz5Income)
        progBiz5 = findViewById(R.id.progBiz5)
        btnUpgradeBiz5 = findViewById(R.id.btnUpgradeBiz5)

        card6 = findViewById(R.id.cardAsset6)
        tvBiz6Lvl = findViewById(R.id.tvBiz6Lvl)
        tvBiz6Income = findViewById(R.id.tvBiz6Income)
        progBiz6 = findViewById(R.id.progBiz6)
        btnUpgradeBiz6 = findViewById(R.id.btnUpgradeBiz6)

        // Career Views
        tvCurrentJobTitle = findViewById(R.id.tvCurrentJobTitle)
        tvJobShiftSalary = findViewById(R.id.tvJobShiftSalary)
        tvCareerLevelBadge = findViewById(R.id.tvCareerLevelBadge)
        btnDoWorkShift = findViewById(R.id.btnDoWorkShift)
        tvCareerProgressLabel = findViewById(R.id.tvCareerProgressLabel)
        progCareerShift = findViewById(R.id.progCareerShift)
        btnApplyPromotion = findViewById(R.id.btnApplyPromotion)
        tvEducationStatus = findViewById(R.id.tvEducationStatus)
        btnEnrollDegree = findViewById(R.id.btnEnrollDegree)

        // Trading Terminal Views
        tvPortfolioTotal = findViewById(R.id.tvPortfolioTotal)
        tvPortfolioPnL = findViewById(R.id.tvPortfolioPnL)
        tvPortfolioCashAvailable = findViewById(R.id.tvPortfolioCashAvailable)
        tvMarketNews = findViewById(R.id.tvMarketNews)

        chipStockNeon = findViewById(R.id.chipStockNeon)
        chipStockCyber = findViewById(R.id.chipStockCyber)
        chipStockVolt = findViewById(R.id.chipStockVolt)
        chipStockGold = findViewById(R.id.chipStockGold)

        tvTerminalSymbol = findViewById(R.id.tvTerminalSymbol)
        tvTerminalHolding = findViewById(R.id.tvTerminalHolding)
        tvTerminalPrice = findViewById(R.id.tvTerminalPrice)
        tvTerminalChange = findViewById(R.id.tvTerminalChange)
        tvTerminalHighLow = findViewById(R.id.tvTerminalHighLow)
        tvTerminalPositionValue = findViewById(R.id.tvTerminalPositionValue)
        chartTerminal = findViewById(R.id.chartTerminal)

        btnQty1 = findViewById(R.id.btnQty1)
        btnQty5 = findViewById(R.id.btnQty5)
        btnQty10 = findViewById(R.id.btnQty10)
        btnQtyMax = findViewById(R.id.btnQtyMax)
        btnTerminalBuy = findViewById(R.id.btnTerminalBuy)
        btnTerminalSell = findViewById(R.id.btnTerminalSell)

        // Watchlist
        cardWatchlist1 = findViewById(R.id.cardWatchlist1)
        tvWatch1Price = findViewById(R.id.tvWatch1Price)
        tvWatch1Change = findViewById(R.id.tvWatch1Change)
        tvWatch1Sub = findViewById(R.id.tvWatch1Sub)

        cardWatchlist2 = findViewById(R.id.cardWatchlist2)
        tvWatch2Price = findViewById(R.id.tvWatch2Price)
        tvWatch2Change = findViewById(R.id.tvWatch2Change)
        tvWatch2Sub = findViewById(R.id.tvWatch2Sub)

        cardWatchlist3 = findViewById(R.id.cardWatchlist3)
        tvWatch3Price = findViewById(R.id.tvWatch3Price)
        tvWatch3Change = findViewById(R.id.tvWatch3Change)
        tvWatch3Sub = findViewById(R.id.tvWatch3Sub)

        cardWatchlist4 = findViewById(R.id.cardWatchlist4)
        tvWatch4Price = findViewById(R.id.tvWatch4Price)
        tvWatch4Change = findViewById(R.id.tvWatch4Change)
        tvWatch4Sub = findViewById(R.id.tvWatch4Sub)
    }

    private fun setupWindowInsets() {
        val root = findViewById<View>(R.id.coordinatorLayout)
        val scrollView = findViewById<View>(R.id.nestedScrollView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            scrollView.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom + 90
            )
            bottomNav.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupEventListeners() {
        // MANUAL CLICK: 1 click gives $1 at Level 1, $2 at Level 2, etc.
        cardManualTapTarget.setOnClickListener {
            handleManualTap()
        }

        // UPGRADE MANUAL CLICK
        btnUpgradeTapPower.setOnClickListener {
            handleUpgradeTapPower()
        }

        // Collect cash button
        btnCollectCash.setOnClickListener {
            triggerHapticFeedback()
            pulseView(btnCollectCash)
            val collected = uncollectedRevenue
            totalNetWorth += collected
            uncollectedRevenue = 0.0

            updateMissionProgress(MissionType.VAULT_COLLECT, 1)

            showNeonSnackbar("Vault Collected: +${currencyFormatter.format(collected)}")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        }

        // Business clicks and upgrades
        btnUpgradeBiz1.setOnClickListener { upgradeBusiness(0, card1) }
        setupBusinessCardClick(0, card1)

        btnUpgradeBiz2.setOnClickListener { upgradeBusiness(1, card2) }
        setupBusinessCardClick(1, card2)

        btnUpgradeBiz3.setOnClickListener { upgradeBusiness(2, card3) }
        setupBusinessCardClick(2, card3)

        btnUpgradeBiz4.setOnClickListener { upgradeBusiness(3, card4) }
        setupBusinessCardClick(3, card4)

        btnUpgradeBiz5.setOnClickListener { upgradeBusiness(4, card5) }
        setupBusinessCardClick(4, card5)

        btnUpgradeBiz6.setOnClickListener { upgradeBusiness(5, card6) }
        setupBusinessCardClick(5, card6)

        // Career / Jobs Actions
        btnDoWorkShift.setOnClickListener { handleWorkShift() }
        btnApplyPromotion.setOnClickListener { handlePromotion() }
        btnEnrollDegree.setOnClickListener { handleEducation() }

        // Top Header Daily Missions & Settings Buttons
        btnTopMissions.setOnClickListener {
            showDailyMissionsBottomSheet()
        }

        btnTopSettings.setOnClickListener {
            showSettingsBottomSheet()
        }

        // Trading Terminal Asset Selection Chips
        chipStockNeon.setOnClickListener { selectAssetForTerminal(0) }
        chipStockCyber.setOnClickListener { selectAssetForTerminal(1) }
        chipStockVolt.setOnClickListener { selectAssetForTerminal(2) }
        chipStockGold.setOnClickListener { selectAssetForTerminal(3) }

        cardWatchlist1.setOnClickListener { selectAssetForTerminal(0) }
        cardWatchlist2.setOnClickListener { selectAssetForTerminal(1) }
        cardWatchlist3.setOnClickListener { selectAssetForTerminal(2) }
        cardWatchlist4.setOnClickListener { selectAssetForTerminal(3) }

        // Order Quantity Selection
        btnQty1.setOnClickListener { setOrderQuantity(1) }
        btnQty5.setOnClickListener { setOrderQuantity(5) }
        btnQty10.setOnClickListener { setOrderQuantity(10) }
        btnQtyMax.setOnClickListener { setOrderQuantity(-1) }

        // Buy / Sell Orders
        btnTerminalBuy.setOnClickListener { executeBuyOrder() }
        btnTerminalSell.setOnClickListener { executeSellOrder() }

        // Bottom Navigation Bar with Separate Interfaces
        bottomNav.selectedItemId = R.id.nav_cash
        bottomNav.setOnItemSelectedListener { item ->
            triggerHapticFeedback()
            when (item.itemId) {
                R.id.nav_cash -> {
                    layoutCash.visibility = View.VISIBLE
                    layoutInvestments.visibility = View.GONE
                    layoutTrading.visibility = View.GONE
                    layoutJobs.visibility = View.GONE
                    showNeonSnackbar("Cash & Vault • Tap to Mint or Upgrade Power")
                    true
                }
                R.id.nav_businesses -> {
                    layoutCash.visibility = View.GONE
                    layoutInvestments.visibility = View.VISIBLE
                    layoutTrading.visibility = View.GONE
                    layoutJobs.visibility = View.GONE
                    showNeonSnackbar("Businesses • Enterprises Production & Upgrades")
                    true
                }
                R.id.nav_trading -> {
                    layoutCash.visibility = View.GONE
                    layoutInvestments.visibility = View.GONE
                    layoutTrading.visibility = View.VISIBLE
                    layoutJobs.visibility = View.GONE
                    updateTradingTerminalDisplay()
                    showNeonSnackbar("Trading Desk • Live Market Fluctuations")
                    true
                }
                R.id.nav_jobs -> {
                    layoutCash.visibility = View.GONE
                    layoutInvestments.visibility = View.GONE
                    layoutTrading.visibility = View.GONE
                    layoutJobs.visibility = View.VISIBLE
                    showNeonSnackbar("Career Ladder • Work Shifts & Climb to C-Suite")
                    true
                }
                else -> false
            }
        }
    }

    // DAILY MISSIONS SYSTEM
    private fun initDailyMissions() {
        activeMissions.clear()
        // Randomly select 3 distinct mission types
        val shuffledPool = allMissionsPool.shuffled()
        val chosenTypes = mutableSetOf<MissionType>()

        for (mission in shuffledPool) {
            if (!chosenTypes.contains(mission.type)) {
                chosenTypes.add(mission.type)
                // Clone fresh instance
                activeMissions.add(
                    DailyMission(
                        mission.id,
                        mission.title,
                        mission.description,
                        mission.type,
                        mission.targetCount,
                        0,
                        mission.rewardCash,
                        false
                    )
                )
                if (activeMissions.size >= 3) break
            }
        }
    }

    private fun updateMissionProgress(type: MissionType, increment: Int) {
        var anyMissionCompletedNow = false
        for (mission in activeMissions) {
            if (mission.type == type && !mission.isCompleted) {
                val wasNotCompleted = !mission.isCompleted
                mission.currentCount = (mission.currentCount + increment).coerceAtMost(mission.targetCount)
                if (wasNotCompleted && mission.isCompleted) {
                    anyMissionCompletedNow = true
                }
            }
        }

        if (anyMissionCompletedNow) {
            triggerHapticFeedback()
            showNeonSnackbar("🎯 Mission Completed! Claim your reward in Daily Missions.")
        }

        updateMissionsDisplay()
    }

    private fun claimMissionReward(index: Int, button: MaterialButton, sheetView: View? = null) {
        if (index >= activeMissions.size) return
        val mission = activeMissions[index]

        if (mission.isCompleted && !mission.isClaimed) {
            mission.isClaimed = true
            totalNetWorth += mission.rewardCash

            triggerHapticFeedback()
            pulseView(button)
            spawnFloatingTapText(mission.rewardCash)
            showNeonSnackbar("🏆 Reward Claimed: +${currencyFormatter.format(mission.rewardCash)}!")

            updateDashboardDisplays()
            updateTradingTerminalDisplay()
            updateMissionsDisplay()
            if (sheetView != null) {
                updateSheetMissionViews(sheetView)
            }
        } else if (!mission.isCompleted) {
            val remaining = mission.targetCount - mission.currentCount
            showNeonSnackbar("Mission in progress! Need $remaining more to complete.")
            shakeView(button)
        } else {
            showNeonSnackbar("Reward already claimed!")
        }
    }

    private fun showSettingsBottomSheet() {
        triggerHapticFeedback()
        val dialog = BottomSheetDialog(this, R.style.Theme_NeonBottomSheetDialog)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        dialog.setContentView(sheetView)
        settingsBottomSheetDialog = dialog

        val btnClose = sheetView.findViewById<ImageView>(R.id.sheetSettingsBtnClose)
        val tvAdminStatusBadge = sheetView.findViewById<TextView>(R.id.tvAdminStatusBadge)
        val tvAdminDesc = sheetView.findViewById<TextView>(R.id.tvAdminDesc)
        val layoutSecretCodeInput = sheetView.findViewById<View>(R.id.layoutSecretCodeInput)
        val etSecretCode = sheetView.findViewById<EditText>(R.id.etSecretCode)
        val btnUnlockAdmin = sheetView.findViewById<MaterialButton>(R.id.btnUnlockAdmin)
        val layoutAdminControls = sheetView.findViewById<View>(R.id.layoutAdminControls)

        // Cheats
        val btnAdminAdd1M = sheetView.findViewById<MaterialButton>(R.id.btnAdminAdd1M)
        val btnAdminAdd1B = sheetView.findViewById<MaterialButton>(R.id.btnAdminAdd1B)
        val btnAdminMaxBiz = sheetView.findViewById<MaterialButton>(R.id.btnAdminMaxBiz)
        val btnAdminMaxTap = sheetView.findViewById<MaterialButton>(R.id.btnAdminMaxTap)
        val btnAdminMaxCareer = sheetView.findViewById<MaterialButton>(R.id.btnAdminMaxCareer)
        val btnAdminGiveStocks = sheetView.findViewById<MaterialButton>(R.id.btnAdminGiveStocks)
        val btnAdminCompleteMissions = sheetView.findViewById<MaterialButton>(R.id.btnAdminCompleteMissions)

        // Reset
        val btnReset = sheetView.findViewById<MaterialButton>(R.id.btnResetGameProgress)

        fun updateAdminStateInSheet() {
            if (isAdminUnlocked) {
                tvAdminStatusBadge.text = "ACTIVE ✔"
                tvAdminStatusBadge.setTextColor(getColor(R.color.neon_green))
                tvAdminStatusBadge.setBackgroundResource(R.drawable.bg_neon_pill)
                tvAdminDesc.text = "Admin God Mode Active! All cheat tools unlocked."
                layoutSecretCodeInput.visibility = View.GONE
                layoutAdminControls.visibility = View.VISIBLE
            } else {
                tvAdminStatusBadge.text = "LOCKED"
                tvAdminStatusBadge.setTextColor(getColor(R.color.neon_cyan))
                tvAdminStatusBadge.setBackgroundResource(R.drawable.bg_cyan_pill)
                tvAdminDesc.text = "Enter secret developer code to unlock admin mode."
                layoutSecretCodeInput.visibility = View.VISIBLE
                layoutAdminControls.visibility = View.GONE
            }
        }

        updateAdminStateInSheet()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnUnlockAdmin.setOnClickListener {
            val code = etSecretCode.text.toString().trim()
            if (code.equals("9ppp", ignoreCase = true)) {
                isAdminUnlocked = true
                triggerHapticFeedback()
                pulseView(btnUnlockAdmin)
                updateAdminStateInSheet()
                showNeonSnackbar("🔓 ADMIN ACCESS GRANTED! Welcome developer.")
            } else {
                shakeView(etSecretCode)
                shakeView(btnUnlockAdmin)
                showNeonSnackbar("❌ Incorrect Secret Code! Access Denied.")
            }
        }

        btnAdminAdd1M.setOnClickListener {
            totalNetWorth += 1_000_000.0
            triggerHapticFeedback()
            pulseView(btnAdminAdd1M)
            spawnFloatingTapText(1_000_000.0)
            showNeonSnackbar("💰 +$1,000,000 Cash Added!")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        }

        btnAdminAdd1B.setOnClickListener {
            totalNetWorth += 1_000_000_000.0
            triggerHapticFeedback()
            pulseView(btnAdminAdd1B)
            spawnFloatingTapText(1_000_000_000.0)
            showNeonSnackbar("💎 +$1,000,000,000 Cash Added!")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        }

        btnAdminMaxBiz.setOnClickListener {
            businesses.forEach { biz ->
                biz.level = 50
                biz.upgradeCost = biz.upgradeCost * 10
            }
            triggerHapticFeedback()
            pulseView(btnAdminMaxBiz)
            showNeonSnackbar("🚀 All 6 Businesses set to Level 50!")
            updateDashboardDisplays()
        }

        btnAdminMaxTap.setOnClickListener {
            clickLevel = 25
            clickValue = 5000.0
            clickUpgradeCost = 100_000_000.0
            triggerHapticFeedback()
            pulseView(btnAdminMaxTap)
            showNeonSnackbar("⚡ Click Power set to LVL 25 (+$5,000/tap)!")
            updateDashboardDisplays()
        }

        btnAdminMaxCareer.setOnClickListener {
            hasCollegeDegree = true
            hasMba = true
            currentCareerRankIndex = careerRanks.size - 1
            completedShifts = 100
            triggerHapticFeedback()
            pulseView(btnAdminMaxCareer)
            showNeonSnackbar("👔 Promoted to Chief Executive Officer (CEO)!")
            updateCareerDisplays()
            updateDashboardDisplays()
        }

        btnAdminGiveStocks.setOnClickListener {
            stocks.forEach { stock ->
                stock.ownedShares += 5000
                stock.totalInvested += (5000 * stock.currentPrice)
            }
            triggerHapticFeedback()
            pulseView(btnAdminGiveStocks)
            showNeonSnackbar("📈 +5,000 Shares added to NEON, CYBER, VOLT, GLD!")
            updateTradingTerminalDisplay()
        }

        btnAdminCompleteMissions.setOnClickListener {
            activeMissions.forEach { mission ->
                mission.currentCount = mission.targetCount
            }
            triggerHapticFeedback()
            pulseView(btnAdminCompleteMissions)
            showNeonSnackbar("🎯 All 3 Daily Missions marked completed!")
            updateMissionsDisplay()
        }

        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Reset Game Progress?")
                .setMessage("Are you sure you want to reset everything? Your cash, career, upgrades, businesses, and stocks will be reset to a brand new game.")
                .setPositiveButton("RESET ALL") { _, _ ->
                    resetAllGameProgress()
                    dialog.dismiss()
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }

        dialog.setOnDismissListener {
            settingsBottomSheetDialog = null
        }

        dialog.show()
    }

    private fun resetAllGameProgress() {
        totalNetWorth = 25.0
        uncollectedRevenue = 0.0
        totalManualClicks = 0
        playerAge = 21
        gameYear = 2026

        clickLevel = 1
        clickValue = 1.0
        clickUpgradeCost = 20.0

        currentCareerRankIndex = 0
        completedShifts = 0
        hasCollegeDegree = false
        hasMba = false

        val initialCosts = listOf(50.0, 200.0, 800.0, 3200.0, 12500.0, 50000.0)
        businesses.forEachIndexed { i, biz ->
            biz.level = 0
            biz.upgradeCost = initialCosts.getOrElse(i) { 50.0 }
            biz.cycleProgress = 0
        }

        stocks.forEach { stock ->
            stock.ownedShares = 0
            stock.totalInvested = 0.0
        }

        initDailyMissions()

        triggerHapticFeedback()
        showNeonSnackbar("🔄 Game Progress has been reset to default start.")

        updateDashboardDisplays()
        updateCareerDisplays()
        updateTradingTerminalDisplay()
        updateMissionsDisplay()
    }

    private fun showDailyMissionsBottomSheet() {
        triggerHapticFeedback()
        val dialog = BottomSheetDialog(this, R.style.Theme_NeonBottomSheetDialog)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_daily_missions, null)
        dialog.setContentView(sheetView)
        missionsBottomSheetDialog = dialog

        val btnClose = sheetView.findViewById<ImageView>(R.id.sheetBtnClose)
        val btnReroll = sheetView.findViewById<MaterialButton>(R.id.sheetBtnReroll)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnReroll.setOnClickListener {
            initDailyMissions()
            triggerHapticFeedback()
            showNeonSnackbar("🎯 New Daily Objectives Generated!")
            updateMissionsDisplay()
            updateSheetMissionViews(sheetView)
        }

        updateSheetMissionViews(sheetView)

        dialog.setOnDismissListener {
            missionsBottomSheetDialog = null
        }

        dialog.show()
    }

    private fun updateSheetMissionViews(sheetView: View) {
        if (activeMissions.size < 3) return
        val tvSubtitle = sheetView.findViewById<TextView>(R.id.sheetMissionsSubtitle)
        val claimedCount = activeMissions.count { it.isClaimed }
        tvSubtitle.text = "Complete objectives for bonus cash • $claimedCount/3 Claimed"

        val m1 = activeMissions[0]
        val m2 = activeMissions[1]
        val m3 = activeMissions[2]

        val btnClaim1 = sheetView.findViewById<MaterialButton>(R.id.sheetBtnClaimMission1)
        val btnClaim2 = sheetView.findViewById<MaterialButton>(R.id.sheetBtnClaimMission2)
        val btnClaim3 = sheetView.findViewById<MaterialButton>(R.id.sheetBtnClaimMission3)

        bindMissionRow(
            m1,
            sheetView.findViewById(R.id.sheetMission1Title),
            sheetView.findViewById(R.id.sheetMission1Reward),
            sheetView.findViewById(R.id.sheetMission1Desc),
            sheetView.findViewById(R.id.sheetMission1ProgressText),
            sheetView.findViewById(R.id.sheetProgMission1),
            btnClaim1
        )
        bindMissionRow(
            m2,
            sheetView.findViewById(R.id.sheetMission2Title),
            sheetView.findViewById(R.id.sheetMission2Reward),
            sheetView.findViewById(R.id.sheetMission2Desc),
            sheetView.findViewById(R.id.sheetMission2ProgressText),
            sheetView.findViewById(R.id.sheetProgMission2),
            btnClaim2
        )
        bindMissionRow(
            m3,
            sheetView.findViewById(R.id.sheetMission3Title),
            sheetView.findViewById(R.id.sheetMission3Reward),
            sheetView.findViewById(R.id.sheetMission3Desc),
            sheetView.findViewById(R.id.sheetMission3ProgressText),
            sheetView.findViewById(R.id.sheetProgMission3),
            btnClaim3
        )

        btnClaim1.setOnClickListener { claimMissionReward(0, btnClaim1, sheetView) }
        btnClaim2.setOnClickListener { claimMissionReward(1, btnClaim2, sheetView) }
        btnClaim3.setOnClickListener { claimMissionReward(2, btnClaim3, sheetView) }
    }

    private fun updateMissionsDisplay() {
        if (activeMissions.size < 3) return

        val claimedCount = activeMissions.count { it.isClaimed }
        val readyToClaimCount = activeMissions.count { it.isCompleted && !it.isClaimed }

        // Top bar badge
        if (readyToClaimCount > 0) {
            tvMissionsBadge.text = "🎯 $readyToClaimCount READY"
            tvMissionsBadge.setTextColor(getColor(R.color.neon_green))
        } else {
            tvMissionsBadge.text = "🎯 $claimedCount/3"
            tvMissionsBadge.setTextColor(getColor(R.color.neon_cyan))
        }

        missionsBottomSheetDialog?.findViewById<View>(android.R.id.content)?.let { root ->
            updateSheetMissionViews(root)
        }
    }

    private fun bindMissionRow(
        mission: DailyMission,
        tvTitle: TextView,
        tvReward: TextView,
        tvDesc: TextView,
        tvProgress: TextView,
        progressBar: LinearProgressIndicator,
        btnClaim: MaterialButton
    ) {
        tvTitle.text = mission.title
        tvReward.text = "+${currencyFormatter.format(mission.rewardCash)}"
        tvDesc.text = mission.description

        val percent = ((mission.currentCount.toFloat() / mission.targetCount) * 100).toInt().coerceIn(0, 100)
        progressBar.progress = percent
        tvProgress.text = "${mission.currentCount} / ${mission.targetCount} (${percent}%)"

        when {
            mission.isClaimed -> {
                btnClaim.text = "CLAIMED ✔"
                btnClaim.setBackgroundColor(getColor(R.color.bg_surface_elevated))
                btnClaim.setTextColor(getColor(R.color.text_muted))
                btnClaim.setIconResource(R.drawable.ic_check)
                btnClaim.iconTint = getColorStateList(R.color.neon_green)
                btnClaim.isEnabled = false
            }
            mission.isCompleted -> {
                btnClaim.text = "CLAIM!"
                btnClaim.setBackgroundColor(getColor(R.color.neon_green))
                btnClaim.setTextColor(getColor(R.color.text_on_neon))
                btnClaim.icon = null
                btnClaim.isEnabled = true
            }
            else -> {
                btnClaim.text = "PROGRESS"
                btnClaim.setBackgroundColor(getColor(R.color.bg_surface_elevated))
                btnClaim.setTextColor(getColor(R.color.text_secondary))
                btnClaim.icon = null
                btnClaim.isEnabled = true
            }
        }
    }

    private fun handleManualTap() {
        totalNetWorth += clickValue
        totalManualClicks++

        updateMissionProgress(MissionType.CLICKS, 1)

        triggerHapticFeedback()
        pulseView(cardManualTapTarget)
        pulseView(imgTapIcon)
        spawnFloatingTapText(clickValue)

        updateDashboardDisplays()
        updateTradingTerminalDisplay()
    }

    private fun handleUpgradeTapPower() {
        if (totalNetWorth >= clickUpgradeCost) {
            totalNetWorth -= clickUpgradeCost
            clickLevel++

            updateMissionProgress(MissionType.TAP_UPGRADE, 1)

            clickValue = when (clickLevel) {
                2 -> 2.00 // Level 1 (1 click = $1) -> Level 2 (1 click = $2)
                3 -> 5.00
                4 -> 12.00
                5 -> 30.00
                6 -> 75.00
                else -> Math.round(clickValue * 2.3 * 10.0) / 10.0
            }

            clickUpgradeCost = when (clickLevel) {
                2 -> 50.00
                3 -> 150.00
                4 -> 400.00
                5 -> 1000.00
                6 -> 2500.00
                else -> Math.round(clickUpgradeCost * 2.5).toDouble()
            }

            triggerHapticFeedback()
            pulseView(btnUpgradeTapPower)
            showNeonSnackbar("Tap Upgraded to Level $clickLevel! (1 click = ${currencyFormatter.format(clickValue)})")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        } else {
            val needed = clickUpgradeCost - totalNetWorth
            showNeonSnackbar("Need ${currencyFormatter.format(needed)} more to upgrade Click Power!")
            shakeView(btnUpgradeTapPower)
        }
    }

    private fun spawnFloatingTapText(amount: Double) {
        val floatingText = TextView(this).apply {
            text = "+${currencyFormatter.format(amount)}"
            setTextColor(getColor(R.color.neon_green))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, getColor(R.color.neon_green_glow))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                val offsetX = Random.nextInt(-60, 60)
                val offsetY = Random.nextInt(-20, 20)
                setMargins(offsetX, offsetY, 0, 0)
            }
        }

        containerTapArea.addView(floatingText)

        floatingText.animate()
            .translationY(-110f)
            .alpha(0f)
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(600)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    containerTapArea.removeView(floatingText)
                }
            })
            .start()
    }

    // REAL TRADING TERMINAL SYSTEM
    private fun selectAssetForTerminal(index: Int) {
        selectedStockIndex = index
        triggerHapticFeedback()

        // Update Chip UI
        val chips = listOf(chipStockNeon, chipStockCyber, chipStockVolt, chipStockGold)
        chips.forEachIndexed { i, chip ->
            if (i == index) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(getColor(R.color.text_on_neon))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(getColor(R.color.text_secondary))
            }
        }

        updateTradingTerminalDisplay()
    }

    private fun setOrderQuantity(qty: Int) {
        selectedOrderQty = qty
        triggerHapticFeedback()

        // Highlight selected qty button
        val buttons = listOf(btnQty1, btnQty5, btnQty10, btnQtyMax)
        buttons.forEach { btn ->
            btn.setBackgroundColor(getColor(R.color.bg_surface_elevated))
            btn.setTextColor(getColor(R.color.text_secondary))
        }

        when (qty) {
            1 -> {
                btnQty1.setBackgroundColor(getColor(R.color.neon_green))
                btnQty1.setTextColor(getColor(R.color.text_on_neon))
            }
            5 -> {
                btnQty5.setBackgroundColor(getColor(R.color.neon_green))
                btnQty5.setTextColor(getColor(R.color.text_on_neon))
            }
            10 -> {
                btnQty10.setBackgroundColor(getColor(R.color.neon_green))
                btnQty10.setTextColor(getColor(R.color.text_on_neon))
            }
            -1 -> {
                btnQtyMax.setBackgroundColor(getColor(R.color.neon_green))
                btnQtyMax.setTextColor(getColor(R.color.text_on_neon))
            }
        }

        updateTradingTerminalDisplay()
    }

    private fun getResolvedQuantity(stock: StockAsset, isBuy: Boolean): Int {
        return if (isBuy) {
            if (selectedOrderQty == -1) {
                val maxAffordable = (totalNetWorth / stock.currentPrice).toInt()
                maxAffordable.coerceAtLeast(1)
            } else {
                selectedOrderQty
            }
        } else {
            if (selectedOrderQty == -1) {
                stock.ownedShares.coerceAtLeast(1)
            } else {
                selectedOrderQty.coerceAtMost(stock.ownedShares.coerceAtLeast(1))
            }
        }
    }

    private fun executeBuyOrder() {
        val stock = stocks[selectedStockIndex]
        val qty = getResolvedQuantity(stock, isBuy = true)
        val totalCost = qty * stock.currentPrice

        if (totalNetWorth >= totalCost) {
            totalNetWorth -= totalCost
            stock.ownedShares += qty
            stock.totalInvested += totalCost

            updateMissionProgress(MissionType.TRADES, 1)

            triggerHapticFeedback()
            pulseView(btnTerminalBuy)
            showNeonSnackbar("EXECUTED: Bought $qty ${stock.symbol} for ${currencyFormatter.format(totalCost)}")

            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        } else {
            val needed = totalCost - totalNetWorth
            showNeonSnackbar("Insufficient Cash! Need ${currencyFormatter.format(needed)} more.")
            shakeView(btnTerminalBuy)
        }
    }

    private fun executeSellOrder() {
        val stock = stocks[selectedStockIndex]
        if (stock.ownedShares <= 0) {
            showNeonSnackbar("No shares of ${stock.symbol} to sell!")
            shakeView(btnTerminalSell)
            return
        }

        val qty = getResolvedQuantity(stock, isBuy = false).coerceAtMost(stock.ownedShares)
        val proceeds = qty * stock.currentPrice
        val proportion = qty.toDouble() / stock.ownedShares
        val costBasis = stock.totalInvested * proportion
        val realizedProfit = proceeds - costBasis

        totalNetWorth += proceeds
        stock.ownedShares -= qty
        stock.totalInvested = (stock.totalInvested - costBasis).coerceAtLeast(0.0)

        updateMissionProgress(MissionType.TRADES, 1)

        triggerHapticFeedback()
        pulseView(btnTerminalSell)
        val pnlText = if (realizedProfit >= 0) "+${currencyFormatter.format(realizedProfit)}" else "-${currencyFormatter.format(-realizedProfit)}"
        showNeonSnackbar("EXECUTED: Sold $qty ${stock.symbol} for ${currencyFormatter.format(proceeds)} (P&L: $pnlText)")

        updateDashboardDisplays()
        updateTradingTerminalDisplay()
    }

    private fun updateTradingTerminalDisplay() {
        // Portfolio summary
        val totalPortfolioValue = stocks.sumOf { it.currentValue }
        val totalInvested = stocks.sumOf { it.totalInvested }
        val netPnL = totalPortfolioValue - totalInvested
        val netPnLPercent = if (totalInvested > 0) (netPnL / totalInvested) * 100.0 else 0.0

        tvPortfolioTotal.text = currencyFormatter.format(totalPortfolioValue)
        val pnlSign = if (netPnL >= 0) "+" else ""
        tvPortfolioPnL.text = "Total Profit / Loss: $pnlSign${currencyFormatter.format(netPnL)} ($pnlSign${String.format(Locale.US, "%.1f", netPnLPercent)}%)"
        tvPortfolioPnL.setTextColor(if (netPnL >= 0) getColor(R.color.neon_green) else getColor(R.color.neon_red))
        tvPortfolioCashAvailable.text = "Cash: ${currencyFormatter.format(totalNetWorth)}"

        // Active Stock Details
        val stock = stocks[selectedStockIndex]
        tvTerminalSymbol.text = "${stock.name} (${stock.symbol})"
        tvTerminalHolding.text = "Position: ${stock.ownedShares} shares • Avg: ${currencyFormatter.format(stock.avgBuyPrice)}"
        tvTerminalPrice.text = currencyFormatter.format(stock.currentPrice)

        val changeSign = if (stock.changePercent >= 0) "+" else ""
        tvTerminalChange.text = "$changeSign${String.format(Locale.US, "%.1f", stock.changePercent)}%"
        val isBullish = stock.changePercent >= 0
        tvTerminalChange.setTextColor(if (isBullish) getColor(R.color.neon_green) else getColor(R.color.neon_red))
        tvTerminalPrice.setTextColor(if (isBullish) getColor(R.color.neon_green) else getColor(R.color.neon_red))

        tvTerminalHighLow.text = "24h High: ${currencyFormatter.format(stock.high24h)}  |  24h Low: ${currencyFormatter.format(stock.low24h)}"
        val posSign = if (stock.unrealizedPnL >= 0) "+" else ""
        tvTerminalPositionValue.text = "Val: ${currencyFormatter.format(stock.currentValue)} ($posSign${currencyFormatter.format(stock.unrealizedPnL)})"
        tvTerminalPositionValue.setTextColor(if (stock.unrealizedPnL >= 0) getColor(R.color.neon_green) else getColor(R.color.neon_red))

        // Update real chart
        chartTerminal.updatePrices(stock.priceHistory, isBullish)

        // Update Buy/Sell Buttons text
        val buyQty = getResolvedQuantity(stock, isBuy = true)
        val buyCost = buyQty * stock.currentPrice
        btnTerminalBuy.text = "BUY ($buyQty) • ${currencyFormatter.format(buyCost)}"

        val sellQty = getResolvedQuantity(stock, isBuy = false).coerceAtMost(stock.ownedShares)
        val sellProceeds = sellQty * stock.currentPrice
        btnTerminalSell.text = if (stock.ownedShares > 0) "SELL ($sellQty) • ${currencyFormatter.format(sellProceeds)}" else "SELL (0)"

        // Update Watchlist items
        updateWatchlistRow(stocks[0], tvWatch1Price, tvWatch1Change, tvWatch1Sub)
        updateWatchlistRow(stocks[1], tvWatch2Price, tvWatch2Change, tvWatch2Sub)
        updateWatchlistRow(stocks[2], tvWatch3Price, tvWatch3Change, tvWatch3Sub)
        updateWatchlistRow(stocks[3], tvWatch4Price, tvWatch4Change, tvWatch4Sub)
    }

    private fun updateWatchlistRow(stock: StockAsset, tvPrice: TextView, tvChange: TextView, tvSub: TextView) {
        tvPrice.text = currencyFormatter.format(stock.currentPrice)
        val sign = if (stock.changePercent >= 0) "+" else ""
        tvChange.text = "$sign${String.format(Locale.US, "%.1f", stock.changePercent)}%"
        tvChange.setTextColor(if (stock.changePercent >= 0) getColor(R.color.neon_green) else getColor(R.color.neon_red))
        tvSub.text = "${stock.symbol} • ${stock.category} • ${stock.ownedShares} owned"
    }

    private fun startStockMarketSimulation() {
        val marketNewsHeadlines = listOf(
            "MARKET BREAK: Tech Giants report surging cloud profits!",
            "FED UPDATE: Interest rate cut announced, markets surge!",
            "CRYPTO SURGE: Institutional buyers drive Cyber Coin volume!",
            "EV RALLY: Volt Electric breaks new battery range milestone!",
            "COMMODITY ALERT: Gold hits new monthly safety high!"
        )

        lifecycleScope.launch {
            var newsTick = 0
            while (isActive) {
                delay(2500)
                newsTick++

                for (stock in stocks) {
                    val volatility = when (stock.symbol) {
                        "CYBER" -> 6.5
                        "VOLT" -> 4.5
                        "NEON" -> 3.2
                        else -> 1.5
                    }

                    val deltaPercent = Random.nextDouble(-volatility, volatility + 0.3)
                    stock.changePercent = deltaPercent
                    val newPrice = (stock.currentPrice * (1.0 + (deltaPercent / 100.0))).coerceAtLeast(5.0)
                    stock.currentPrice = newPrice

                    if (newPrice > stock.high24h) stock.high24h = newPrice
                    if (newPrice < stock.low24h) stock.low24h = newPrice

                    stock.priceHistory.add(newPrice)
                    if (stock.priceHistory.size > 22) {
                        stock.priceHistory.removeAt(0)
                    }
                }

                if (newsTick % 6 == 0) {
                    tvMarketNews.text = marketNewsHeadlines.random()
                }

                if (layoutTrading.visibility == View.VISIBLE) {
                    updateTradingTerminalDisplay()
                }
            }
        }
    }

    // CAREER & JOBS LOGIC
    private fun handleWorkShift() {
        val currentRank = careerRanks[currentCareerRankIndex]
        totalNetWorth += currentRank.salaryPerShift
        completedShifts++

        updateMissionProgress(MissionType.WORK_SHIFTS, 1)

        triggerHapticFeedback()
        pulseView(btnDoWorkShift)
        showNeonSnackbar("Shift Completed: +${currencyFormatter.format(currentRank.salaryPerShift)}")

        updateDashboardDisplays()
        updateCareerDisplays()
        updateTradingTerminalDisplay()
    }

    private fun handlePromotion() {
        if (currentCareerRankIndex >= careerRanks.size - 1) {
            showNeonSnackbar("You have reached the maximum rank: CEO!")
            return
        }

        val nextRank = careerRanks[currentCareerRankIndex + 1]
        val reqShifts = nextRank.requiredShifts
        val cost = nextRank.promotionCost

        if (completedShifts < reqShifts) {
            showNeonSnackbar("Need $reqShifts shifts completed (You have: $completedShifts)")
            shakeView(btnApplyPromotion)
            return
        }

        if (nextRank.requiresDegree && !hasCollegeDegree) {
            showNeonSnackbar("Promotion requires a College Degree! Enroll in Education.")
            shakeView(btnApplyPromotion)
            return
        }

        if (nextRank.requiresMba && !hasMba) {
            showNeonSnackbar("Executive position requires an MBA! Enroll in Education.")
            shakeView(btnApplyPromotion)
            return
        }

        if (totalNetWorth < cost) {
            val needed = cost - totalNetWorth
            showNeonSnackbar("Need ${currencyFormatter.format(needed)} more for certification & promotion!")
            shakeView(btnApplyPromotion)
            return
        }

        totalNetWorth -= cost
        currentCareerRankIndex++
        triggerHapticFeedback()
        pulseView(btnApplyPromotion)
        showNeonSnackbar("PROMOTED TO ${nextRank.title.uppercase()}!")

        updateDashboardDisplays()
        updateCareerDisplays()
        updateTradingTerminalDisplay()
    }

    private fun handleEducation() {
        if (!hasCollegeDegree) {
            val cost = 250.00
            if (totalNetWorth >= cost) {
                totalNetWorth -= cost
                hasCollegeDegree = true
                triggerHapticFeedback()
                pulseView(btnEnrollDegree)
                showNeonSnackbar("College Degree Acquired! Unlocked Corporate Promotions.")
                updateDashboardDisplays()
                updateCareerDisplays()
                updateTradingTerminalDisplay()
            } else {
                val needed = cost - totalNetWorth
                showNeonSnackbar("Need ${currencyFormatter.format(needed)} more for College tuition!")
                shakeView(btnEnrollDegree)
            }
        } else if (!hasMba) {
            val cost = 1500.00
            if (totalNetWorth >= cost) {
                totalNetWorth -= cost
                hasMba = true
                triggerHapticFeedback()
                pulseView(btnEnrollDegree)
                showNeonSnackbar("Master of Business Administration (MBA) Acquired! C-Suite Unlocked.")
                updateDashboardDisplays()
                updateCareerDisplays()
                updateTradingTerminalDisplay()
            } else {
                val needed = cost - totalNetWorth
                showNeonSnackbar("Need ${currencyFormatter.format(needed)} more for MBA tuition!")
                shakeView(btnEnrollDegree)
            }
        } else {
            showNeonSnackbar("All executive qualifications completed!")
        }
    }

    private fun updateCareerDisplays() {
        val currentRank = careerRanks[currentCareerRankIndex]
        tvCurrentJobTitle.text = currentRank.title
        tvJobShiftSalary.text = "Salary: +${currencyFormatter.format(currentRank.salaryPerShift)} / shift"
        tvCareerLevelBadge.text = "RANK ${currentRank.rank}"

        if (currentCareerRankIndex < careerRanks.size - 1) {
            val nextRank = careerRanks[currentCareerRankIndex + 1]
            tvCareerProgressLabel.text = "Shifts Completed: $completedShifts / ${nextRank.requiredShifts}"
            val progressPercent = ((completedShifts.toFloat() / nextRank.requiredShifts) * 100).toInt().coerceIn(0, 100)
            progCareerShift.progress = progressPercent
            btnApplyPromotion.text = "PROMOTION: ${nextRank.title.uppercase()} • ${currencyFormatter.format(nextRank.promotionCost)}"
        } else {
            tvCareerProgressLabel.text = "Maximum Rank Attained • Highest Executive Level"
            progCareerShift.progress = 100
            btnApplyPromotion.text = "C-SUITE SUMMIT REACHED"
        }

        // Education status
        if (!hasCollegeDegree) {
            tvEducationStatus.text = "Current: High School Diploma"
            btnEnrollDegree.text = "ENROLL IN COLLEGE DEGREE • $250.00"
        } else if (!hasMba) {
            tvEducationStatus.text = "Current: Bachelor of Science (Degree Holder)"
            btnEnrollDegree.text = "ENROLL IN EXECUTIVE MBA • $1,500.00"
        } else {
            tvEducationStatus.text = "Current: MBA Master of Business Administration"
            btnEnrollDegree.text = "EDUCATION MAXIMIZED"
        }
    }

    // BUSINESS ENTERPRISES LOGIC
    private fun setupBusinessCardClick(index: Int, card: MaterialCardView) {
        val touchListener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    cardLastTouchCoords[index][0] = event.rawX
                    cardLastTouchCoords[index][1] = event.rawY
                }
            }
            false
        }
        card.setOnTouchListener(touchListener)
        for (i in 0 until card.childCount) {
            val child = card.getChildAt(i)
            if (child !is MaterialButton) {
                child.setOnTouchListener(touchListener)
            }
        }

        card.setOnClickListener {
            val rawX = cardLastTouchCoords[index][0]
            val rawY = cardLastTouchCoords[index][1]
            manualProduceBusiness(index, card, rawX, rawY)
            cardLastTouchCoords[index][0] = -1f
            cardLastTouchCoords[index][1] = -1f
        }
    }

    private fun manualProduceBusiness(index: Int, cardView: MaterialCardView, rawX: Float = -1f, rawY: Float = -1f) {
        val loc = IntArray(2)
        cardView.getLocationOnScreen(loc)
        val finalX = if (rawX > 0f) rawX else (loc[0] + cardView.width / 2f)
        val finalY = if (rawY > 0f) rawY else (loc[1] + cardView.height / 2f)

        val biz = businesses[index]
        if (biz.level > 0) {
            val payout = biz.basePayout * (1.0 + (biz.level * 0.25))
            totalNetWorth += payout
            totalManualClicks++
            updateMissionProgress(MissionType.CLICKS, 1)

            spawnFloatingTextAtLocation(
                finalX,
                finalY,
                "+${currencyFormatter.format(payout)}",
                colorRes = R.color.neon_green,
                glowColorRes = R.color.neon_green_glow
            )

            triggerHapticFeedback()
            pulseView(cardView)
            showNeonSnackbar("${biz.name} Produced: +${currencyFormatter.format(payout)}")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        } else {
            spawnFloatingTextAtLocation(
                finalX,
                finalY,
                "🔒 LOCKED",
                colorRes = R.color.neon_cyan,
                glowColorRes = R.color.neon_cyan_glow
            )
            showNeonSnackbar("Upgrade ${biz.name} to Level 1 to activate production!")
            shakeView(cardView)
        }
    }

    private fun spawnFloatingTextAtLocation(
        rawX: Float,
        rawY: Float,
        text: String,
        colorRes: Int = R.color.neon_green,
        glowColorRes: Int = R.color.neon_green_glow
    ) {
        val rootLoc = IntArray(2)
        coordinatorLayout.getLocationOnScreen(rootLoc)
        val localX = rawX - rootLoc[0]
        val localY = rawY - rootLoc[1]

        val floatingText = TextView(this).apply {
            this.text = text
            setTextColor(getColor(colorRes))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(12f, 0f, 0f, getColor(glowColorRes))
            isClickable = false
            isFocusable = false

            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        }

        val textWidth = floatingText.measuredWidth
        val textHeight = floatingText.measuredHeight

        val rootWidth = coordinatorLayout.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val startX = (localX - textWidth / 2f).toInt().coerceIn(16, (rootWidth - textWidth - 16).coerceAtLeast(16))
        val startY = (localY - textHeight / 2f).toInt()

        val lp = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = startX
            topMargin = startY
        }

        coordinatorLayout.addView(floatingText, lp)

        floatingText.alpha = 0f
        floatingText.scaleX = 0.5f
        floatingText.scaleY = 0.5f

        floatingText.animate()
            .alpha(1f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .translationY(-35f)
            .setDuration(130)
            .withEndAction {
                floatingText.animate()
                    .alpha(0f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .translationY(-120f)
                    .setDuration(600)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            coordinatorLayout.removeView(floatingText)
                        }
                    })
                    .start()
            }
            .start()
    }

    private fun upgradeBusiness(index: Int, cardView: MaterialCardView) {
        val biz = businesses[index]
        if (totalNetWorth >= biz.upgradeCost) {
            totalNetWorth -= biz.upgradeCost
            biz.level += 1
            biz.upgradeCost = (biz.upgradeCost * 1.45)

            updateMissionProgress(MissionType.BUSINESS_UPGRADES, 1)

            triggerHapticFeedback()
            pulseView(cardView)
            showNeonSnackbar("${biz.name} upgraded to LVL ${biz.level}!")
            updateDashboardDisplays()
            updateTradingTerminalDisplay()
        } else {
            val needed = biz.upgradeCost - totalNetWorth
            showNeonSnackbar("Need ${currencyFormatter.format(needed)} more to upgrade!")
            shakeView(cardView)
        }
    }

    private fun calculateTotalPassiveCashFlow(): Double {
        return businesses.filter { it.level > 0 }.sumOf { biz ->
            biz.basePayout * (biz.level * 0.15)
        }
    }

    private fun updateDashboardDisplays() {
        val passivePerSec = calculateTotalPassiveCashFlow()
        val passivePerMin = passivePerSec * 60.0

        tvTotalNetWorth.text = currencyFormatter.format(totalNetWorth)
        tvMonthlyCashFlow.text = "+${currencyFormatter.format(passivePerSec)} / sec"
        tvDailyRate.text = "+${currencyFormatter.format(passivePerMin)} / min"
        tvNetWorthGrowth.text = "Clicks: $totalManualClicks"

        // Auto-Clock Age & Year
        tvPlayerLevel.text = "YEAR $gameYear • AGE $playerAge"

        // Manual Clicker
        tvTapPowerBadge.text = "+${currencyFormatter.format(clickValue)} / TAP"
        tvTapStats.text = "Tap to earn cash • Level $clickLevel (1 click = ${currencyFormatter.format(clickValue)})"

        val nextLevel = clickLevel + 1
        val nextValue = when (nextLevel) {
            2 -> 2.00
            3 -> 5.00
            4 -> 12.00
            5 -> 30.00
            6 -> 75.00
            else -> Math.round(clickValue * 2.3 * 10.0) / 10.0
        }
        btnUpgradeTapPower.text = "UPGRADE CLICK: LVL $nextLevel (+$${nextValue.toInt()}/tap) • ${currencyFormatter.format(clickUpgradeCost)}"

        // Cards
        updateCardViews(0, tvBiz1Lvl, tvBiz1Income, btnUpgradeBiz1)
        updateCardViews(1, tvBiz2Lvl, tvBiz2Income, btnUpgradeBiz2)
        updateCardViews(2, tvBiz3Lvl, tvBiz3Income, btnUpgradeBiz3)
        updateCardViews(3, tvBiz4Lvl, tvBiz4Income, btnUpgradeBiz4)
        updateCardViews(4, tvBiz5Lvl, tvBiz5Income, btnUpgradeBiz5)
        updateCardViews(5, tvBiz6Lvl, tvBiz6Income, btnUpgradeBiz6)
    }

    private fun updateCardViews(
        index: Int,
        tvLvl: TextView,
        tvIncome: TextView,
        btnUpgrade: MaterialButton
    ) {
        val biz = businesses[index]
        tvLvl.text = "LVL ${biz.level}"
        if (biz.level == 0) {
            tvIncome.text = "LOCKED • Tap to Buy"
            tvIncome.setTextColor(getColor(R.color.text_muted))
            btnUpgrade.text = "BUY • ${formatShortCurrency(biz.upgradeCost)}"
        } else {
            val incomePerSec = biz.basePayout * (biz.level * 0.15)
            tvIncome.text = "+${currencyFormatter.format(incomePerSec)}/sec"
            tvIncome.setTextColor(getColor(R.color.neon_green))
            btnUpgrade.text = "UPGRADE • ${formatShortCurrency(biz.upgradeCost)}"
        }
    }

    private fun formatShortCurrency(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.US, "$%.1fM", amount / 1_000_000)
            amount >= 1_000 -> String.format(Locale.US, "$%.1fK", amount / 1_000)
            else -> compactFormatter.format(amount)
        }
    }

    private fun startIdleCycleSimulation() {
        lifecycleScope.launch {
            var globalCycle = 0
            while (isActive) {
                delay(100)
                globalCycle = (globalCycle + 1) % 100
                progressRevenueCycle.progress = globalCycle

                // Progress active meters
                for (biz in businesses) {
                    if (biz.level > 0) {
                        biz.cycleProgress = (biz.cycleProgress + biz.cycleSpeed) % 100
                    }
                }
                progBiz1.progress = businesses[0].cycleProgress
                progBiz2.progress = businesses[1].cycleProgress
                progBiz3.progress = businesses[2].cycleProgress
                progBiz4.progress = businesses[3].cycleProgress
                progBiz5.progress = businesses[4].cycleProgress
                progBiz6.progress = businesses[5].cycleProgress

                // Money Tycoon Auto-Clock: advance year & age every 60 seconds
                if (globalCycle == 0) {
                    playerAge++
                    gameYear++
                    tvPlayerLevel.text = "YEAR $gameYear • AGE $playerAge"
                }

                // Passive income tick every second
                if (globalCycle % 10 == 0) {
                    val passiveIncome = calculateTotalPassiveCashFlow()
                    if (passiveIncome > 0) {
                        uncollectedRevenue += passiveIncome
                        totalNetWorth += (passiveIncome * 0.5)
                        tvTotalNetWorth.text = currencyFormatter.format(totalNetWorth)
                    }
                }
            }
        }
    }

    private fun pulseView(view: View) {
        view.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(90)
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
            }
            .start()
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 16f, -16f, 10f, -10f, 5f, -5f, 0f).apply {
            duration = 320
            start()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            }
        } catch (_: Exception) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun showNeonSnackbar(message: String) {
        val snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), message, Snackbar.LENGTH_SHORT)
        snackbar.anchorView = bottomNav
        snackbar.setBackgroundTint(getColor(R.color.bg_surface_elevated))
        snackbar.setTextColor(getColor(R.color.neon_green))
        snackbar.show()
    }
}
