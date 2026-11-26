package com.myrepo.oslauncher

enum class Shape { SQUIRCLE, CIRCLE, CARD }

enum class Style(val title: String, val desc: String) {
    IOS("iOS", "Классический экран: скруглённые иконки, плавающий док, точки страниц"),
    MACOS("macOS", "Launchpad: строка меню сверху, широкий док, иконки с тенью"),
    TVOS("tvOS", "Крупные широкие карточки, панель вкладок сверху, без дока"),
    VISIONOS("visionOS", "Круглые стеклянные иконки, стеклянная панель и док-капсула")
}

data class Spec(
    val cols: Int,
    val rows: Int,
    val icon: Int,
    val label: Int,
    val shape: Shape,
    val dock: Boolean,
    val dockMax: Int,
    val dockIcon: Int,
    val dockRadius: Int,
    val dockColor: Int,
    val dockStroke: Int,
    val dots: Boolean,
    val menuBar: Boolean,
    val tvBar: Boolean,
    val glass: Boolean,
    val scrim: Int,
    val shadow: Boolean
)

fun Style.spec(): Spec = when (this) {
    Style.IOS -> Spec(
        cols = 4, rows = 6, icon = 60, label = 11, shape = Shape.SQUIRCLE,
        dock = true, dockMax = 4, dockIcon = 58, dockRadius = 30,
        dockColor = 0x4DFFFFFF, dockStroke = 0x33FFFFFF,
        dots = true, menuBar = false, tvBar = false, glass = false,
        scrim = 0x00000000, shadow = false
    )
    Style.MACOS -> Spec(
        cols = 5, rows = 6, icon = 56, label = 11, shape = Shape.SQUIRCLE,
        dock = true, dockMax = 5, dockIcon = 48, dockRadius = 18,
        dockColor = 0x59FFFFFF, dockStroke = 0x40FFFFFF,
        dots = true, menuBar = true, tvBar = false, glass = false,
        scrim = 0x26000000, shadow = true
    )
    Style.TVOS -> Spec(
        cols = 2, rows = 4, icon = 84, label = 14, shape = Shape.CARD,
        dock = false, dockMax = 0, dockIcon = 0, dockRadius = 0,
        dockColor = 0, dockStroke = 0,
        dots = false, menuBar = false, tvBar = true, glass = false,
        scrim = 0x66000000, shadow = true
    )
    Style.VISIONOS -> Spec(
        cols = 4, rows = 5, icon = 58, label = 11, shape = Shape.CIRCLE,
        dock = true, dockMax = 4, dockIcon = 52, dockRadius = 44,
        dockColor = 0x40FFFFFF, dockStroke = 0x66FFFFFF,
        dots = true, menuBar = false, tvBar = false, glass = true,
        scrim = 0x26000000, shadow = false
    )
}
