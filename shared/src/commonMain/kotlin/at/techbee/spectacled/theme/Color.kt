package at.techbee.spectacled.theme
import androidx.compose.ui.graphics.Color

/**
 * This function can be used to determine whether a color is perceived as dark or light
 * and can be useful to determine a contrast color for a background.
 * The algorithm is copied from https://stackoverflow.com/questions/1855884/determine-font-color-based-on-background-color
 * @param color as int for which it should be determined if a color should be seen as dark
 * @return true if the color is likely to be perceived as dark, otherwise false
 */
fun isDarkColor(color: Color): Boolean {
    // Counting the perceptive luminance - human eye favors green color...
    val a = 1 - ((0.299 * color.red + 0.586 * color.green + 0.115 * color.blue) / color.colorSpace.getMaxValue(0))
    println(color.toString() + " " + a*100)
    return a > 0.5
}