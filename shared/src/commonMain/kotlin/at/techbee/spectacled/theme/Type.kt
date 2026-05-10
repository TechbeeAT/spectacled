package at.techbee.spectacled.theme


import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import spectacled.shared.generated.resources.ABeeZee_Regular
import spectacled.shared.generated.resources.Nunito_Bold
import spectacled.shared.generated.resources.Nunito_ExtraBold
import spectacled.shared.generated.resources.Nunito_Regular
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.Roboto


val nunitoFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Nunito_Regular, weight = FontWeight.Normal),
        Font(Res.font.Nunito_Bold, weight = FontWeight.Bold),
        Font(Res.font.Nunito_ExtraBold, weight = FontWeight.ExtraBold)
    )


val bodyFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Roboto)
    )


val displayFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.ABeeZee_Regular)
    )

// Default Material 3 typography values
val baseline = Typography()

val AppTypography: Typography
    @Composable
    get() = baseline.copy(
        //displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
        //displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
        //displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
        //headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
        //headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
        headlineSmall = baseline.headlineSmall.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 20.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            ),
            fontFamily = nunitoFontFamily
        ),
        //titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
        //titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
        //titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
        //bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        //bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        //bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        //labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        //labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        //labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )



