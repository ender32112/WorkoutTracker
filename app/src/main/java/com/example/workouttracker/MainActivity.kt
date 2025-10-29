package com.example.workouttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.viewModels
import com.example.workouttracker.ui.navigation.MainScreen
import com.example.workouttracker.ui.navigation.WorkoutNavGraph
import com.example.workouttracker.viewmodel.TrainingViewModel
import com.example.workouttracker.ui.theme.WorkoutTrackerTheme
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.ArticleViewModelFactory

class MainActivity : ComponentActivity() {

    private val trainingViewModel: TrainingViewModel by viewModels()
    private val articleViewModel: ArticleViewModel by viewModels {
        ArticleViewModelFactory(trainingViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutTrackerTheme {
                WorkoutNavGraph()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkoutTrackerTheme {
        Greeting("Android")
    }
}