package com.example.greetingcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.greetingcard.ui.theme.GreetingCardTheme

@Composable
fun NewScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Hello World2")
    }
}

@Preview(showBackground = true)
@Composable
fun NewScreenPreview() {
    GreetingCardTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NewScreen()
        }
    }
}
