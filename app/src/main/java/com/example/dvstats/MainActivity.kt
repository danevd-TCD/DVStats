package com.example.dvstats

//https://developer.android.com/training/volley

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dvstats.ui.theme.DVStatsTheme
import androidx.compose.material.Button
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import android.app.Application
import androidx.compose.runtime.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.coroutines.awaitString
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.file.Path

/*
import okhttp3.*
import okio.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
 */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //########################

        //########################

        setContent {
            MainApp()
        }
    }
}



suspend fun fuelCall(userURL:String) : String {
    //var responseString = "No response"

    val (request, response, result) =
        userURL.httpGet().awaitStringResponse()
    /*
    try {
        responseString = (Fuel.get(userURL).awaitString()) // "{"origin":"127.0.0.1"}"
    } catch(exception: Exception) {
        println("A network request exception was thrown: ${exception.message}")
    }
     */

    //issues present in .toString() method of fuel
    //https://github.com/kittinunf/fuel/issues/742


    return result
}

@Composable
fun MainApp(names: List<String> = List(15) {"Hello user #$it"})
{
    val clickCount = remember { mutableStateOf(0)}
    val initialResponse = remember  { mutableStateOf("Initial")}

    DVStatsTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            Column {
                NewsStory()
                //ButtonCounter(count = clickCount.value, updateCount = {newCount -> clickCount.value = newCount})
                //Divider()
                //NameList(names, Modifier.weight(1f))
                ButtonCall(text_Reponse = initialResponse.value, updateAPIString = {newString -> initialResponse.value = newString})
            }
        }
    }
}

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
    companion object {
        var appContext: Context? = null
            private set
    }
}

@Composable
fun ButtonCall(text_Reponse: String, updateAPIString: (String) -> Unit)
{
    /*
    https://stackoverflow.com/questions/64116377/how-to-call-kotlin-coroutine-in-composable-function-callbacks
    need to define co-routines that last as long as a composable element is present in composition
    so that suspendable functions can be called; esp. important for network events that can
    no longer occur on main threads
    */
    // Returns a scope that's cancelled when F is removed from composition
    val coroutineScope = rememberCoroutineScope()

    val scopedUpdateAPIString: () -> Unit = {
        coroutineScope.launch {
            val scopedFuelVal = fuelCall("https://www.danev.xyz/status/all").toString()
            updateAPIString(scopedFuelVal)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        //Button(onClick = { updateAPIString(fuelCall("https://www.danev.xyz/status/all").toString()) })
        Button(onClick = scopedUpdateAPIString )
        {
            Text("Make API call")
        }
        Text(text = text_Reponse)
    }
}

@Composable
fun ButtonCounter(count: Int, updateCount: (Int) -> Unit)
{
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {updateCount(count+1)})
        {
            Text("Button click count: $count")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {updateCount(count - count)}) {
            Text("Reset counter")
        }
    }

}
@Composable
fun Greeting(name: String) {
    var isSelected by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(if (isSelected) Color.Cyan else Color.Transparent)

    Text(
        text = "Hello $name!",
        modifier = Modifier
            .padding(24.dp)
            .background(color = backgroundColor)
            .clickable(onClick = { isSelected = !isSelected })
    )
}




//lazy columns: essentially, scrollable columns that implement lazy loading for
//e.g large lists or other (potentially) large amounts of generated component elements
@Composable
fun NameList(names: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(items = names) { name ->
            Greeting(name = name)
            Divider(color = Color.Black)
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
        /*
        var firstCheck: Boolean = true
        Checkbox(
            checked = firstCheck,
            onCheckedChange = {firstCheck = false}
        )
         */

    }
}
/*
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DVStatsTheme {
        MainApp()
    }
}

 */