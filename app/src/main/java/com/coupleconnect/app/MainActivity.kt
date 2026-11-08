package com.coupleconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

private val Ink = Color(0xFF172033)
private val Blue = Color(0xFF3978F6)
private val Pale = Color(0xFFF5F7FC)
private val Muted = Color(0xFF7B8496)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CoupleConnectApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoupleConnectApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("couple_connect_profile", 0) }
    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var mobile by remember { mutableStateOf(prefs.getString("mobile", "") ?: "") }
    var photoUri by remember { mutableStateOf(prefs.getString("photo", null)) }
    var page by remember { mutableStateOf(if (name.isBlank()) "profile" else "connect") }
    var inviteCode by remember { mutableStateOf("") }
    var partnerCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        photoUri = it?.toString()
        prefs.edit().putString("photo", photoUri).apply()
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            background = Pale,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Scaffold(containerColor = Pale) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "profile") {
                        IconButton(onClick = { page = "profile" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("COUPLE CONNECT", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(48.dp))
                }
                Spacer(Modifier.height(34.dp))
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Blue, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(14.dp))

                if (page == "profile") {
                    Text("Let’s set up your profile", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Only the details needed to connect you", color = Muted)
                    Spacer(Modifier.height(28.dp))
                    Surface(
                        shape = CircleShape, color = Color(0xFFE8EEFF),
                        modifier = Modifier.size(92.dp).clickable { photoPicker.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Choose profile photo", tint = Blue, modifier = Modifier.size(42.dp))
                        }
                    }
                    Text("Add profile photo (optional)", color = Blue, modifier = Modifier.clickable { photoPicker.launch("image/*") }.padding(10.dp))
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it.filter(Char::isDigit).take(15) }, label = { Text("Mobile number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || mobile.length < 8) notice = "Enter your name and a valid mobile number."
                            else {
                                prefs.edit().putString("name", name.trim()).putString("mobile", mobile).apply()
                                notice = ""
                                page = "connect"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Continue") }
                } else {
                    Text("Connect with your partner", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("A private space for two", color = Muted)
                    Spacer(Modifier.height(28.dp))
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your invite code", color = Muted)
                            Text(if (inviteCode.isBlank()) "------" else inviteCode, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Blue, letterSpacing = 5.sp)
                            Button(onClick = {
                                inviteCode = Random.nextInt(100000, 1000000).toString()
                                notice = "Demo code only. Configure the secure backend before sharing it."
                            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Text("Generate 6-digit code")
                            }
                            Text("or enter your partner’s code", color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                            OutlinedTextField(value = partnerCode, onValueChange = { partnerCode = it.filter(Char::isDigit).take(6) }, label = { Text("Partner code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                            OutlinedButton(onClick = { notice = "Pairing backend is not connected yet. This screen is a Phase 1 UI scaffold." }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(14.dp)) {
                                Text("Connect partner")
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Signed in locally as $name", color = Muted, fontSize = 13.sp)
                    Text("Profile settings", color = Blue, modifier = Modifier.clickable { page = "profile" }.padding(12.dp))
                }
                if (notice.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(notice, color = Color(0xFF9A5A00), fontSize = 13.sp)
                }
                Spacer(Modifier.height(32.dp))
                Text("Private by design • Pair only with someone you trust", color = Muted, fontSize = 12.sp)
            }
        }
    }
}
