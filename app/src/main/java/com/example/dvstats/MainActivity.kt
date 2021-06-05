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
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.launch
//import com.example.dvstats.MyApplication.Companion.appContext
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Composable
fun MainApp(names: List<String> = List(15) {"Hello user #$it"}, i_userFiles: List<String>? = getDataDirFiles( MainActivity.instance ))
{
    //val clickCount = remember { mutableStateOf(0)}
    val initialResponse = remember  { mutableStateOf("Initial")}
    //val curFileName = remember { mutableStateOf("DEFAULT_FILENAME") }
    var curFileName: MutableList<String> = remember { mutableListOf() }

    DVStatsTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            Column {
                //NewsStory()
                //ButtonCounter(count = clickCount.value, updateCount = {newCount -> clickCount.value = newCount})
                //Divider()
                //NameList(names, Modifier.weight(1f))
                ButtonCall(text_Reponse = initialResponse.value, updateAPIString = {newString -> initialResponse.value = newString})
                //WriteCall(text = curFileName.value, updateFileText = { newFileName -> curFileName.value = newFileName;writeTestText(curFileName.value)})
                WriteCall(text = curFileName, updateFileText = { newFileName -> curFileName =
                    newFileName as MutableList<String>;writeTestText(
                    curFileName
                )})
                if (i_userFiles != null) {
                    DataList(userFiles = i_userFiles)
                }
            }
        }
    }
}



class MainActivity : ComponentActivity() {
    companion object {
        lateinit var instance: MainActivity
    }
    //override to provide app-wide context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContent {
            MainApp()
        }
    }
}

// ############################ FILE SCAN AND DISPLAY #######################################

//write filename to file
fun writeTestText(inputFieldList:MutableList<String>){
    //inputFieldList is the mutable list passed by data fields from compose function
    val filename = inputFieldList[0] + "_config.json" //N.B: hardcoding filename based on field loc

    val file = File(MainActivity.instance.filesDir, filename)
    file.createNewFile()
    //writing json data here, code in syntax by hand or use gson/etc?
    //file.writeText("{\n"+ "\"Name\":"+ "\"" + filename + "\",\n"+ "\"URL\":" + "\""+inputFieldList[1] +"\"" + "\n}")
    file.writeText(varsToJson(inputFieldList[0],inputFieldList[1]))
}

//https://github.com/Kotlin/kotlinx.serialization#android for original

//define data class for json writing here
@Serializable
data class API_Json(val Name: String, val URL: String)

//serialise input data from API_Json format into json data and return as string to print/write/etc
fun varsToJson(name: String, url: String): String {
    // Pass args and set up as data class for encoding
    val data = API_Json(Name = name,URL = url)
    val string = Json.encodeToString(data)

    return string
}

//read a specified json file, return string? or maybe multiple vars? I dunno yet
//n.b we decode using the same data class we encoded with in varsToJson

fun readJson(fileLoc: String): API_Json {
    val jsonFile = File(fileLoc).readText(Charsets.UTF_8) //read entire json file into a string
    val JsonAsString = Json.decodeFromString<API_Json>(jsonFile) //parse json file
    return JsonAsString
    //can then access elements with JsonAsString.name or JsonAsString.language, i think?
}


//get all files in app filesDir
fun getDataDirFiles(getFilesContext: Context): List<String> {
    val dataDir = File(getFilesContext.filesDir, "")
    if (!dataDir.exists()){
        dataDir.mkdir()
    }
    val fileList = mutableListOf<String>()
    //walk each file in dir, and look for .json files by reversing each string and scanning the last 5
    //chars for ".json" backwards... there's got to be a more elegant way, but hey, it works for now
    dataDir.walk().forEach { if (it.toString().reversed().substring(0,5) == "nosj.") {fileList.add(it.toString()) }}

    return fileList
}

//get all files in filesDir and make lazy columns
@Composable
fun DataList(userFiles: List<String>, modifier: Modifier = Modifier)
{
    //val coroutineScope = rememberCoroutineScope()

    LazyColumn(modifier = modifier) {
        items(items = userFiles) { data ->
            //Text(text = data)
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                val tempJsonObject = readJson(data)

                //

                //

                Text(text = "Name: " + tempJsonObject.Name + "\nURL: " + tempJsonObject.URL)
                Button(onClick = { }) {
                    Text(text = "Query ${tempJsonObject.Name}")
                }
            }

            Divider(color = Color.Black)
        }

    }
}

@Composable
fun WriteCall(text: List<String>, updateFileText: (List<String>) -> Unit)
{
    val coroutineScope = rememberCoroutineScope()

    /*n.b: will special characters (/ , . ;) etc. cause issues w/ saving filename?
    might need to sanitise/check input if so.
    /parameters for writing to JSON config file:
     */
    var text by remember { mutableStateOf("")}
    var API_name by remember { mutableStateOf("")} //api name, e.g "danev.xyz" or "google sheets"
    var API_url by remember { mutableStateOf("")} //api URL, e.g "danev.xyz/status/all"

    val fieldList = mutableListOf<String>()

    Column(modifier = Modifier.padding(16.dp)) {
        //original
        /*
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Filename") }
        )
         */
        //API name
        TextField(
            value = API_name,
            onValueChange = { API_name = it },
            label = { Text("API name") }
        )
        //API URL
        TextField(
            value = API_url,
            onValueChange = { API_url = it },
            label = { Text("API URL") }
        )
        //eventually add more fields for e.g arguments or authorisation

        //there's gotta be a better way to add the field elements to the fieldList list before
        //sending off to updateFileText function
        Button(onClick = { fieldList.add(API_name); fieldList.add(API_url); updateFileText(fieldList) }) {
            Text("Save file")
        }

    }

}

// ############################ API CALL #######################################

suspend fun fuelCall(userURL:String) : String {
    //var responseString = "No response"
    val (request, response, result) =
        userURL.httpGet().awaitStringResponse()
    //issues present in .toString() method of fuel
    //https://github.com/kittinunf/fuel/issues/742
    return result
}

//api call button
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


// ############################ OLD/OTHER #######################################
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

@Composable
fun NameList(names: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(items = names) { name ->
            Greeting(name = name)
            Divider(color = Color.Black)
        }
    }
}

//basic counter button
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

    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DVStatsTheme {
        MainApp()
    }
}

