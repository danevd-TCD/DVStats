package com.example.dvstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dvstats.ui.theme.DVStatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp()
{
    DVStatsTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            NewsStory()
        }
    }

}


@Composable
fun NewsStory()
{
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.italy),
            contentDescription = null,
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Initial Jetpack Compose work: Here's some header text",
            style = typography.h6
        )

        Text("Here's a line. \nHere's a newline. Lorem ipsum dolor sit amet.",
        style = typography.body2)

        //confusing checkbox syntax
        var firstCheck: Boolean = true
        Checkbox(
            checked = firstCheck,
            onCheckedChange = {firstCheck = false}
        )

        

    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DVStatsTheme {
        MainApp()
    }
}