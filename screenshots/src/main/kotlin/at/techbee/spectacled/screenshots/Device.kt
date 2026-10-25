package at.techbee.spectacled.screenshots

/**
 * The screen sizes to render. Pixel sizes follow the store requirements; the density decides how
 * many dp the layout gets, and with it whether the app shows its two-pane layout (see [isWide]).
 */
internal enum class Device(
    val folder: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float
) {
    IOS_PHONE("ios-phone-6.9", 1320, 2868, 3f),             // App Store iPhone 6.9", 440 x 956 dp
    IOS_TABLET("ios-tablet-13", 2064, 2752, 2f),            // App Store iPad 13", 1032 x 1376 dp
    ANDROID_PHONE("android-phone", 1080, 1920, 2.625f),     // Play 9:16, 411 x 731 dp
    ANDROID_TABLET_7("android-tablet-7", 1200, 1920, 2f),   // 600 x 960 dp
    ANDROID_TABLET_10("android-tablet-10", 1600, 2560, 2f); // 800 x 1280 dp

    /** Mirrors rememberIsLandscape() in SpectacledApp: wide screens get the two-pane layout. */
    val isWide: Boolean
        get() = widthPx > heightPx || widthPx / density > 700f
}
