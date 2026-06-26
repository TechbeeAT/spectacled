package at.techbee.spectacled.theme


import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import spectacled.shared.generated.resources.ABeeZee_Regular
import spectacled.shared.generated.resources.NotoSerif
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.Roboto


/*
val robotoFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Roboto)
    )


val abeezeeFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.ABeeZee_Regular)
    )

val notoSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.NotoSerif)
    )

 */

val DefaultAppTypography = Typography()    // Default Material 3 typography values


//val baseline = Typography()

enum class ThemeFont(val fontName: String) {
    ROBOTO("Roboto"),
    ABEEZEE("ABeeZee"),
    NOTO_SERIF("Noto Serif");

    val themeFont: Font
        @Composable
        get() = when(this) {
            ROBOTO -> Font(Res.font.Roboto)
            ABEEZEE -> Font(Res.font.ABeeZee_Regular)
            NOTO_SERIF -> Font(Res.font.NotoSerif)
        }

    val themeFontFamily: FontFamily
        @Composable
        get() = FontFamily(this.themeFont)

    @Composable
    fun getTypography() = Typography(
        displayLarge = DefaultAppTypography.displayLarge.copy(fontFamily = themeFontFamily),
        displayMedium = DefaultAppTypography.displayMedium.copy(fontFamily = themeFontFamily),
        displaySmall = DefaultAppTypography.displaySmall.copy(fontFamily = themeFontFamily),
        headlineLarge = DefaultAppTypography.headlineLarge.copy(fontFamily = themeFontFamily),
        headlineMedium = DefaultAppTypography.headlineMedium.copy(fontFamily = themeFontFamily),
        headlineSmall = DefaultAppTypography.headlineSmall.copy(fontFamily = themeFontFamily),
        titleLarge = DefaultAppTypography.titleLarge.copy(fontFamily = themeFontFamily),
        titleMedium = DefaultAppTypography.titleMedium.copy(fontFamily = themeFontFamily),
        titleSmall = DefaultAppTypography.titleSmall.copy(fontFamily = themeFontFamily),
        bodyLarge = DefaultAppTypography.bodyLarge.copy(fontFamily = themeFontFamily),
        bodyMedium = DefaultAppTypography.bodyMedium.copy(fontFamily = themeFontFamily),
        bodySmall = DefaultAppTypography.bodySmall.copy(fontFamily = themeFontFamily),
        labelLarge = DefaultAppTypography.labelLarge.copy(fontFamily = themeFontFamily),
        labelMedium = DefaultAppTypography.labelMedium.copy(fontFamily = themeFontFamily),
        labelSmall = DefaultAppTypography.labelSmall.copy(fontFamily = themeFontFamily),
    )
}

/*



val AppTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )


 */

