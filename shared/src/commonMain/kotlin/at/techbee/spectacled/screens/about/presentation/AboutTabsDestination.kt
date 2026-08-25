package at.techbee.spectacled.screens.about.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about
import spectacled.shared.generated.resources.about_contributors
import spectacled.shared.generated.resources.about_libraries
import spectacled.shared.generated.resources.about_release_notes
import spectacled.shared.generated.resources.about_sponsor


sealed class AboutTabDestination (
    val titleResource: StringResource,
    val icon: ImageVector,
    //val badgeCount: Int?
) {
    data object Jtx: AboutTabDestination(
        titleResource = Res.string.about,
        icon = Icons.Outlined.Info,
    )
    data object Sponsor: AboutTabDestination(
        titleResource = Res.string.about_sponsor,
        icon = Icons.Outlined.VolunteerActivism,
    )
    /*
    data object JtxBoardPro: AboutTabDestination(
        titleResource = R.string.buypro_initial_dialog_title,
        icon = Icons.Outlined.CardGiftcard,
    )

     */
    data object ReleaseNotes: AboutTabDestination(
        titleResource = Res.string.about_release_notes,
        icon = Icons.Outlined.NewReleases,
    )
    data object Libraries: AboutTabDestination(
        titleResource = Res.string.about_libraries,
        icon = Icons.Outlined.DataObject,
    )
    data object Contributors: AboutTabDestination(
        titleResource = Res.string.about_contributors,
        icon = Icons.Outlined.VerifiedUser,
    )
    /*
    data object Translations: AboutTabDestination(
        titleResource = R.string.about_tabitem_translations,
        icon = Icons.Outlined.Translate,
    )


     */
}
