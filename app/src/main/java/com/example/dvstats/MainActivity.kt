package com.example.dvstats

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dvstats.ui.theme.DVStatsTheme
import androidx.compose.material.Button
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
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
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import androidx.compose.runtime.snapshots.SnapshotStateList

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

@Composable
fun MainApp()
{
    val initialResponse = remember  { mutableStateOf("Initial")} //initial API string
    var curFileName = remember {mutableStateListOf<String>()} //

    DVStatsTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                ButtonCall( text_Reponse = initialResponse.value,
                            updateAPIString = {newString -> initialResponse.value = newString})
                WriteCall(  text = curFileName,
                            updateFileText =    {
                                    newFileName -> curFileName = newFileName as SnapshotStateList<String>;
                                    writeTestText(curFileName)
                                                })
                DataList(   userFiles = getDataDirFiles( MainActivity.instance ),
                            updateAPIString = { newString -> initialResponse.value = newString},
                            text_Reponse = initialResponse.value)
            }
        }
    }
}

// ############################ FILE SCAN AND DISPLAY #######################################

//write filename to file
fun writeTestText(inputFieldList:SnapshotStateList<String>){
    //inputFieldList is the mutable list passed by data fields from compose function
    val filename = inputFieldList[0] + "_config.json" //N.B: hardcoding filename based on field loc
    val file = File(MainActivity.instance.filesDir, filename)
    file.createNewFile()
    file.writeText(varsToJson(inputFieldList[0],inputFieldList[1]))
}

//define data class for json writing here
@Serializable
data class API_Json(val Name: String, val URL: String)

//serialise input data from API_Json format into json data and return as string to print/write/etc
fun varsToJson(name: String, url: String): String
{
    // Pass args and set up as data class for encoding
    val data = API_Json(Name = name, URL = url)
    return Json.encodeToString(data)
}

//read a specified json file, return string
fun readJson(fileLoc: String): API_Json
{
    val jsonFile = File(fileLoc).readText(Charsets.UTF_8) //read entire json file into a string
    val JsonAsString = Json.decodeFromString<API_Json>(jsonFile) //parse json file
    return JsonAsString
}

//get all files in app filesDir
fun getDataDirFiles(getFilesContext: Context): List<String>
{
    val dataDir = File(getFilesContext.filesDir, "")
    if (!dataDir.exists()){ dataDir.mkdir() } //necessary?
    val fileList = mutableListOf<String>()
    /*walk each file in dir, and look for .json files by reversing each string and scanning the last 5
    chars for ".json" backwards... there's got to be a more elegant way, but hey, it works for now */
    dataDir.walk().forEach { if (it.toString().reversed().substring(0,5) == "nosj.") {fileList.add(it.toString()) }}
    return fileList
}


//get all files in filesDir and make lazy columns
@Composable
fun DataList(userFiles: List<String>, modifier: Modifier = Modifier, updateAPIString: (String) -> Unit, text_Reponse: String)
{

    val lazyElements = remember{ mutableStateListOf<String>() }
    for (i in getDataDirFiles(MainActivity.instance))
    {
        lazyElements.add(i)
    }

    LazyColumn(modifier = modifier) {
        items(items = lazyElements) { data ->

            val coroutineScope = rememberCoroutineScope()

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                val tempJsonObject = readJson(data)

                val scopedUpdateAPIString: () -> Unit = {
                    coroutineScope.launch {
                        val scopedFuelVal = fuelCall("https://"+tempJsonObject.URL)
                        updateAPIString(scopedFuelVal)
                    }
                }

                Text(text = "Name: " + tempJsonObject.Name + "\nURL: " + tempJsonObject.URL)
                Button(onClick = scopedUpdateAPIString) {
                    Text(text = "Query ${tempJsonObject.Name}")

                }
                Text(text = text_Reponse)
            }

            Divider(color = Color.Black)
        }
    }
    
}

//input fields to write file to .json
@Composable
fun WriteCall(text: List<String>, updateFileText: (List<String>) -> Unit)
{
    var text by remember { mutableStateOf("")}
    var API_name by remember { mutableStateOf("")} //api name, e.g "danev.xyz" or "google sheets"
    var API_url by remember { mutableStateOf("")} //api URL, e.g "danev.xyz/status/all"

    val fieldList = remember{mutableStateListOf<String>()}

    Column(modifier = Modifier.padding(16.dp))
    {
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

        Button(onClick = {
            fieldList.add(API_name);
            fieldList.add(API_url);
            updateFileText(fieldList) })
            { Text("Save file") }
    }

}

// ############################ API CALL #######################################
//HTTPGet using Fuel at specified URL
suspend fun fuelCall(userURL:String) : String
{
    val (request, response, result) = //request, response are probably not necessary
        userURL.httpGet().awaitStringResponse()
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
        Button(onClick = scopedUpdateAPIString )
        {
            Text("Make API call")
        }
        Text(text = text_Reponse)
    }
}

// ############################ OLD/OTHER #######################################
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DVStatsTheme {
        MainApp()
    }
}

