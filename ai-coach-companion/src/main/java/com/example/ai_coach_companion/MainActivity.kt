package com.example.ai_coach_companion

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()

        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MyApp() {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "AI Coach")
                },
                backgroundColor = Color(0xFFEB5E28)
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color(0xFF252422)) {
                // Add bottom navigation items here
            }
        }
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFCF2))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 56.dp, start = 10.dp, end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo")
            Column(modifier = Modifier.align(Alignment.Start)) {
                Text(text = "Heart Rate: ", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                Text(text = "Velocity: ", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                Text(text = "Rotation: ", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            }

        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApp()
}
