package de.hhn.gnsstrackingapp.ui.painting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import de.hhn.gnsstrackingapp.R

@Composable
fun FloatingToolbar(
    onCircleClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.3f)
            .background(Color.hsv(306f, 0.1f, 0.98f)),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onCircleClick) {
            Icon(
                painter = painterResource(R.drawable.circle),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight(0.8f).aspectRatio(1f)
            )
        }
    }
}