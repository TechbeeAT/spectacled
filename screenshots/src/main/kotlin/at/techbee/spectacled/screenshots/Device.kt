package at.techbee.spectacled.screenshots

/** Which system bars a device draws on top of the app, see SystemBars.kt. */
internal enum class SystemBarsStyle { IOS, ANDROID, NONE }

/**
 * The screen sizes to render. Pixel sizes follow the store requirements; the density decides how
 * many dp the layout gets, and with it whether the app shows its two-pane layout (see [isWide]).
 * [topInsetDp] and [bottomInsetDp] are the system bar insets the device reports to the app, so
 * the app leaves room for the status bar and home indicator exactly as it does on the device.
 */
internal enum class Device(
    val folder: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val systemBars: SystemBarsStyle,
    val topInsetDp: Int,
    val bottomInsetDp: Int
) {
    IOS_PHONE("ios-phone-6.9", 1320, 2868, 3f, SystemBarsStyle.IOS, 62, 34),             // App Store iPhone 6.9", 440 x 956 dp
    IOS_TABLET("ios-tablet-13", 2064, 2752, 2f, SystemBarsStyle.IOS, 24, 20),            // App Store iPad 13", 1032 x 1376 dp
    IOS_DUO_OPEN("ios-duo-open", 2007, 2853, 3f, SystemBarsStyle.IOS, 44, 20),           // iPhone Duo unfolded, 669 x 951 dp (insets estimated)
    MACBOOK("macbook", 2880, 1880, 2f, SystemBarsStyle.NONE, 0, 0),                      // Mac App Store, 1440 x 940 dp
    ANDROID_PHONE("android-phone", 1080, 1920, 2.625f, SystemBarsStyle.ANDROID, 32, 24), // Play 9:16, 411 x 731 dp
    ANDROID_TABLET_7("android-tablet-7", 1200, 1920, 2f, SystemBarsStyle.ANDROID, 24, 24),   // 600 x 960 dp
    ANDROID_TABLET_10("android-tablet-10", 1600, 2560, 2f, SystemBarsStyle.ANDROID, 24, 24); // 800 x 1280 dp

    /** Mirrors rememberIsLandscape() in SpectacledApp: wide screens get the two-pane layout. */
    val isWide: Boolean
        get() = widthPx > heightPx || widthPx / density > 700f

    val isTablet: Boolean
        get() = widthPx / density >= 600f
}
