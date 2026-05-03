package at.techbee.spectacled.screens.core.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun SpecialRoundedCard(
    isFirst: Boolean = true,
    isLast: Boolean = true,
    overrideTopRoundedCornerSize: Dp? = null,
    overrideBottomRoundedCornerSize: Dp? = null,
    isSelected: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = overrideTopRoundedCornerSize?: if(isFirst) 16.dp else 4.dp,
            topEnd = overrideTopRoundedCornerSize?: if(isFirst) 16.dp else 4.dp,
            bottomStart = overrideBottomRoundedCornerSize?: if(isLast) 16.dp else 4.dp,
            bottomEnd = overrideBottomRoundedCornerSize?: if(isLast) 16.dp else 4.dp
        ),
        colors = colors,
        border = if(isSelected) BorderStroke(3.dp, colors.disabledContentColor) else null,
        interactionSource = interactionSource
    ) {
        content()
    }
}

@Preview
@Composable
private fun SpecialRoundedCard_single_Preview() {
    SpecialRoundedCard(
        isFirst = true,
        isLast = true,
        isSelected = false,
        onClick = {},
        content = {
            Text(
                text = "Hello World",
                modifier = Modifier.padding(16.dp)
            ) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun SpecialRoundedCard_first_Preview() {
    SpecialRoundedCard(
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        content = {
            Text(
                text = "Hello World",
                modifier = Modifier.padding(16.dp)
            ) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun SpecialRoundedCard_middle_Preview() {
    SpecialRoundedCard(
        isFirst = false,
        isLast = false,
        isSelected = false,
        onClick = {},
        content = {
            Text(
                text = "Hello World",
                modifier = Modifier.padding(16.dp)
            ) },
        modifier = Modifier.fillMaxWidth()
    )
}


@Preview
@Composable
private fun SpecialRoundedCard_last_selected_Preview() {
    SpecialRoundedCard(
        isFirst = false,
        isLast = true,
        isSelected = true,
        onClick = {},
        content = {
            Text(
                text = "Hello World",
                modifier = Modifier.padding(16.dp)
            ) },
        modifier = Modifier.fillMaxWidth()
    )
}