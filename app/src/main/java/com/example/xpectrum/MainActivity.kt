package com.example.xpectrum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp // Import necesario para usar dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.xpectrum.ui.theme.XpectrumTheme
import com.example.xpectrum.BoletoScreen
import com.example.xpectrum.PasajerosScreen
import kotlinx.coroutines.launch
import com.example.xpectrum.Vuelo
import com.example.xpectrum.obtenerVuelos
import com.example.xpectrum.Pasajero
import com.example.xpectrum.obtenerPasajerosPorCodigoVuelo
import androidx.compose.runtime.rememberCoroutineScope

// Actividad principal de la app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita el modo edge-to-edge para la UI
        setContent {
            // Aplicamos el tema personalizado
            XpectrumTheme {
                // Controlador de navegación para cambiar de pantalla
                val navController = rememberNavController()
                // Definimos el grafo de navegación con dos pantallas
                NavHost(navController = navController, startDestination = "bienvenida") {
                    // Pantalla de bienvenida
                    composable("bienvenida") { BienvenidaScreen(navController) }
                    // Pantalla de lista de vuelos
                    composable("vuelos") { ListaVuelosScreen(navController) }
                    // Pantalla de boleto (QR)
                    composable("boleto") { BoletoScreen(navController) }
                    // Pantalla de pasajeros por código de vuelo
                    composable("pasajeros/{codigoVuelo}") { backStackEntry ->
                        val codigoVuelo = backStackEntry.arguments?.getString("codigoVuelo") ?: ""
                        PasajerosScreen(navController, codigoVuelo)
                    }
                    // Pantalla de detalle de boleto
                    composable(
                        "ticketInfo/{nombre}/{email}/{telefono}/{codigoVuelo}/{fechaReserva}/{fechaSalida}/{horaSalida}/{fechaLlegada}/{horaLlegada}/{precioUSD}/{precioPEN}/{tipoPago}"
                    ) { backStackEntry ->
                        TicketInfoScreen(
                            navController = navController,
                            nombre = backStackEntry.arguments?.getString("nombre"),
                            email = backStackEntry.arguments?.getString("email"),
                            telefono = backStackEntry.arguments?.getString("telefono"),
                            codigoVuelo = backStackEntry.arguments?.getString("codigoVuelo"),
                            fechaSalida = backStackEntry.arguments?.getString("fechaSalida"),
                            horaSalida = backStackEntry.arguments?.getString("horaSalida"),
                            fechaLlegada = backStackEntry.arguments?.getString("fechaLlegada"),
                            horaLlegada = backStackEntry.arguments?.getString("horaLlegada"),
                            precioUSD = backStackEntry.arguments?.getString("precioUSD")?.toDoubleOrNull(),
                            precioPEN = backStackEntry.arguments?.getString("precioPEN")?.toDoubleOrNull(),
                            tipoPago = backStackEntry.arguments?.getString("tipoPago"),
                            fechaReserva = backStackEntry.arguments?.getString("fechaReserva")
                        )
                    }
                }
            }
        }
    }
}

// Pantalla de bienvenida con el diseño de la imagen
@Composable
fun BienvenidaScreen(navController: NavHostController) {
    // Definición del degradado de fondo
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2C3E50),
            Color(0xFF1A2530)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 40.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Ilustración (usando un Box con fondo blanco y esquinas redondeadas)
            Box(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .size(200.dp)
                    .background(Color.White, shape = RoundedCornerShape(20.dp))
                    .padding(0.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                // Aquí iría la imagen del logo/ilustración
                // Por ahora mostramos un texto de marcador de posición
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo de Xpectrum",

                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
            }

            // Título de bienvenida
            Text(
                text = "¡Bienvenido a Xpectrum!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 60.dp, bottom = 16.dp)
            )

            // Subtítulo
            Text(
                text = "Tu aplicación para gestionar vuelos y\nescanear boletos de forma rápida y segura",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            // Botón de continuar
            Button(
                onClick = { navController.navigate("vuelos") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF08121E),  // Color #08121e
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continuar",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// Pantalla que muestra la lista de vuelos obtenidos desde la API
@Composable
fun ListaVuelosScreen(navController: NavHostController) {
    // CoroutineScope para lanzar tareas asíncronas
    val scope = rememberCoroutineScope()
    // Estado que almacena la lista de vuelos
    var vuelos by remember { mutableStateOf<List<Vuelo>>(emptyList()) }
    // Estado de carga
    var cargando by remember { mutableStateOf(true) }
    // Estado para mensajes de error
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { navController.navigate("boleto") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Boleto")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LaunchedEffect(Unit) {
                cargando = true
                error = null
                try {
                    vuelos = obtenerVuelos()
                } catch (e: Exception) {
                    error = "Error al cargar vuelos"
                } finally {
                    cargando = false
                }
            }

            // Título de la pantalla
            Text(
                "Lista de Vuelos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            // Indicador de carga
            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                // Muestra el mensaje de error si ocurrió alguno
                Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else if (vuelos.isEmpty()) {
                // Si la lista está vacía, mostramos un mensaje
                Text(
                    "No se encontraron vuelos. Verifica la respuesta de la API en Logcat.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                // (Opcional) Mostrar un ejemplo de vuelo para debug visual
                VueloItem(
                    vuelo = Vuelo(
                        codigoVuelo = "DEMO123",
                        fechaSalida = "2025-06-10T10:00:00",
                        horaSalida = "10:00:00",
                        fechaLlegada = "2025-06-10T12:00:00",
                        horaLlegada = "12:00:00",
                        aeropuertoOrigen = "Aeropuerto Demo",
                        paisOrigen = "País Demo",
                        estadoVuelo = "Disponible"
                    )
                )
            } else {
                // Lista de vuelos usando LazyColumn
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vuelos.size) { idx ->
                        VueloItem(vuelo = vuelos[idx])
                    }
                }
            }
        }
    }
}

// Componente que muestra los datos de un vuelo en una tarjeta
@Composable
fun VueloItem(vuelo: Vuelo) {
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Código del vuelo
            Text("Código: ${vuelo.codigoVuelo}", style = MaterialTheme.typography.titleMedium)
            // Fecha y hora de salida
            Text("Salida: ${vuelo.fechaSalidaFormateada} ${vuelo.horaSalida}")
            // Fecha y hora de llegada
            Text("Llegada: ${vuelo.fechaLlegadaFormateada} ${vuelo.horaLlegada}")
            // Origen (aeropuerto y país)
            Text("Origen: ${vuelo.aeropuertoOrigen} (${vuelo.paisOrigen})")
            // Estado del vuelo
            Text("Estado: ${vuelo.estadoVuelo}")
        }
    }
}

// Vista previa para la pantalla de bienvenida (solo para diseño en el IDE)
@Preview(showBackground = true)
@Composable
fun BienvenidaPreview() {
    XpectrumTheme {
        // No navigation en preview
        BienvenidaScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}