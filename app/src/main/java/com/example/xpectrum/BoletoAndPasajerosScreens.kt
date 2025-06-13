package com.example.xpectrum

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import androidx.compose.ui.platform.LocalContext
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.itextpdf.layout.properties.HorizontalAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.xpectrum.R
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Rect


// Pantalla para escanear o cargar QR de boleto (stub funcional)
// Función para formatear fechas en formato "13 Jun 2025"
fun formatearFecha(fechaStr: String?): String? {
    if (fechaStr.isNullOrBlank()) return null
    
    return try {
        // Extraer solo la parte de la fecha (ignorar la hora si existe)
        val fechaParte = fechaStr.split("T")[0]
        val partes = fechaParte.split("-")
        
        if (partes.size == 3) {
            val anio = partes[0]
            val mes = when (partes[1].toInt()) {
                1 -> "Ene"
                2 -> "Feb"
                3 -> "Mar"
                4 -> "Abr"
                5 -> "May"
                6 -> "Jun"
                7 -> "Jul"
                8 -> "Ago"
                9 -> "Sep"
                10 -> "Oct"
                11 -> "Nov"
                12 -> "Dic"
                else -> partes[1]
            }
            val dia = partes[2].toInt().toString() // Eliminar ceros iniciales
            
            "$dia $mes $anio"
        } else {
            fechaStr
        }
    } catch (e: Exception) {
        android.util.Log.e("FormatoFecha", "Error al formatear fecha: ${e.message}")
        fechaStr // En caso de error, devolver la fecha original
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletoScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear/Cargar Boleto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var qrValue by remember { mutableStateOf("") }
            var ticketInfo by remember { mutableStateOf<TicketInfo?>(null) }
            var cargando by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            // Lanzador para seleccionar imagen
            val context = LocalContext.current
            val selectImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    cargando = true
                    error = null
                    ticketInfo = null
                    scope.launch {
                        try {
                            // Decodificar QR desde la imagen seleccionada
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            val intArray = IntArray(bitmap.width * bitmap.height)
                            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                            val reader = com.google.zxing.MultiFormatReader()
                            val result = try { reader.decode(binaryBitmap) } catch (e: Exception) { null }
                            val qrContent = result?.text
                            if (!qrContent.isNullOrBlank()) {
                                qrValue = qrContent
                                val info = obtenerTicketInfoPorCodigoVuelo(qrContent)
                                ticketInfo = info
                                if (info == null) error = "No se encontró información para este QR"
                                else navController.navigate("ticketInfo/${info.nombre}/${info.email}/${info.telefono}/${info.codigoVuelo}/${info.fechaReserva}/${info.fechaSalida}/${info.horaSalida}/${info.fechaLlegada}/${info.horaLlegada}/${info.precioUSD}/${info.precioPEN}/${info.tipoPago}")
                            } else {
                                error = "No se pudo leer el QR de la imagen"
                            }
                        } catch (e: Exception) {
                            error = "Error al procesar la imagen"
                        } finally {
                            cargando = false
                        }
                    }
                }
            }

            OutlinedTextField(
                value = qrValue,
                onValueChange = {}, // No editable manualmente
                label = { Text("Código QR extraído") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ZXing QR Scan launcher
            val activity = LocalContext.current as? Activity
            val qrScanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    val contents = intent?.getStringExtra("SCAN_RESULT")
                    if (!contents.isNullOrBlank()) {
                        qrValue = contents
                        cargando = true
                        error = null
                        ticketInfo = null
                        scope.launch {
                            try {
                                val info = obtenerTicketInfoPorCodigoVuelo(contents)
                                ticketInfo = info
                                if (info == null) error = "No se encontró información para este QR"
                                else navController.navigate("ticketInfo/${info.nombre}/${info.email}/${info.telefono}/${info.codigoVuelo}/${info.fechaReserva}/${info.fechaSalida}/${info.horaSalida}/${info.fechaLlegada}/${info.horaLlegada}/${info.precioUSD}/${info.precioPEN}/${info.tipoPago}")
                            } catch (e: Exception) {
                                error = "Error al consultar la API"
                            } finally {
                                cargando = false
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectImageLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cargar QR desde imagen")
                }
                Button(
                    onClick = {
                        activity?.let {
                            val integrator = IntentIntegrator(activity)
                            integrator.setOrientationLocked(false)
                            integrator.setPrompt("Escanea el código QR del boleto")
                            qrScanLauncher.launch(integrator.createScanIntent())
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Escanear QR")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (cargando) {
                CircularProgressIndicator()
            } else if (!error.isNullOrBlank()) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
            // Ya no mostramos TicketInfoResult aquí, la info va a otra pantalla
        }
    }
}

// Nueva pantalla para mostrar el resultado del ticket
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketInfoScreen(
    navController: NavHostController,
    nombre: String?,
    email: String?,
    telefono: String?,
    codigoVuelo: String?,
    fechaReserva: String?,
    fechaSalida: String?,
    horaSalida: String?,
    fechaLlegada: String?,
    horaLlegada: String?,
    precioUSD: Double?,
    precioPEN: Double?,
    tipoPago: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Boleto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nombre: ${nombre ?: "-"}", style = MaterialTheme.typography.titleMedium)
                    Text("Email: ${email ?: "-"}")
                    Text("Teléfono: ${telefono ?: "-"}")
                    Text("Código Vuelo: ${codigoVuelo ?: "-"}")
                    Text("Salida: ${formatearFecha(fechaSalida) ?: "-"} ${horaSalida?.substringBeforeLast(":") ?: ""}")
                    Text("Llegada: ${formatearFecha(fechaLlegada) ?: "-"} ${horaLlegada?.substringBeforeLast(":") ?: ""}")
                    Text("Precio USD: ${precioUSD ?: "-"}")
                    Text("Precio PEN: ${precioPEN ?: "-"}")
                    Text("Tipo de Pago: ${tipoPago ?: "-"}")
                    Text("Fecha Reserva: ${formatearFecha(fechaReserva) ?: "-"}")
                    Spacer(modifier = Modifier.height(16.dp))
                    val context = LocalContext.current
                    var pdfMessage by remember { mutableStateOf("") }
                    Button(onClick = {
                        try {
                            val fileName = "BoardingPass-${nombre}-${codigoVuelo}.pdf"
                            val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val file = java.io.File(downloads, fileName)
                            val outputStream = java.io.FileOutputStream(file)
                            val writer = com.itextpdf.kernel.pdf.PdfWriter(outputStream)
                            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(writer)
                            val document = com.itextpdf.layout.Document(pdfDoc)

                            // Fuentes
                            val bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
                            val normal = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)

                            // Logo desde drawable
                            try {
                                // Usar BitmapFactory directamente
                                // Cargar y ajustar el logo
                                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
                                val stream = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
                                val logoImageData = com.itextpdf.io.image.ImageDataFactory.create(stream.toByteArray())
                                
                                // Función para redondear bordes de la imagen
                                fun getRoundedBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
                                    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(output)
                                    val color = -0xbdbdbe
                                    val paint = Paint()
                                    val rect = Rect(0, 0, bitmap.width, bitmap.height)
                                    val rectF = RectF(rect)
                                    val roundPx = pixels.toFloat()
                                    
                                    paint.isAntiAlias = true
                                    canvas.drawARGB(0, 0, 0, 0)
                                    paint.color = color
                                    canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
                                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                                    canvas.drawBitmap(bitmap, rect, rect, paint)
                                    return output
                                }
                                
                                // Aplicar bordes redondeados al logo
                                val roundedBitmap = getRoundedBitmap(bitmap, 200)
                                val roundedStream = java.io.ByteArrayOutputStream()
                                roundedBitmap.compress(Bitmap.CompressFormat.PNG, 100, roundedStream)
                                val roundedLogoData = com.itextpdf.io.image.ImageDataFactory.create(roundedStream.toByteArray())
                                
                                // Crear el logo con posición absoluta
                                val pageSize = pdfDoc.defaultPageSize
                                val logoWidth = 300f  // Ancho del logo
                                val logoImage = Image(roundedLogoData)
                                    .setWidth(logoWidth)
                                    .setFixedPosition(
                                        pageSize.width - logoWidth - 50f,  // Posición X (desde la izquierda)
                                        pageSize.height - 350f,  // Posición Y (desde abajo)
                                        logoWidth  // Ancho
                                    )
                                
                                // Agregar el logo al documento
                                document.add(logoImage)
                                
                                // Agregar el título debajo del logo
                                val titleParagraph = Paragraph("XPECTRUM")
                                    .setFont(bold)
                                    .setFontSize(28f)
                                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK)
                                    .setMarginTop(20f)  // Espacio después del logo
                                
                                val subtitleParagraph = Paragraph("Operated by Expectrum Peru")
                                    .setFont(normal)
                                    .setFontSize(10f)
                                    .setMarginBottom(20f)
                                
                                document.add(titleParagraph)
                                document.add(subtitleParagraph)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Si falla, mostrar solo el texto
                                document.add(Paragraph("XPECTRUM")
                                    .setFont(bold)
                                    .setFontSize(28f)
                                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLUE))
                                document.add(Paragraph("Operated by Expectrum Peru")
                                    .setFont(normal)
                                    .setFontSize(10f))
                            }

                            // Encabezado y recuadro principal
                            document.add(Paragraph("Pase de abordar").setFont(bold).setFontSize(16f))
document.add(Paragraph("Online boarding pass").setFont(normal))
document.add(Paragraph("\n"))

document.add(Paragraph("Nombre de pasajero/Name of passenger").setFont(normal).setFontSize(10f))
val nombrePasajero = nombre ?: "-"
document.add(Paragraph(nombrePasajero.uppercase()).setFont(bold).setFontSize(12f))
val vuelo = codigoVuelo ?: "-"
val asiento = "6C" // Puedes parametrizar si tienes el dato
val grupo = "4" // Puedes parametrizar si tienes el dato
val booking = "W6LTWP 2017-07-13" // Puedes parametrizar si tienes el dato
val origen = "LIMA" // Parametriza si tienes el dato
val aeropuerto = "NUEVO AEROPUERTO INTERNACIONAL JORGE CHAVEZ" // Parametriza si tienes el dato
val fechaSalidaStr = fechaSalida ?: "-"
val horaSalidaStr = horaSalida ?: "-"
val fechaReservaStr = fechaReserva ?: "-"
val fechaLlegadaStr = fechaLlegada ?: "-"
val horaLlegadaStr = horaLlegada ?: "-"
val precioUSDStr = precioUSD?.toString() ?: "-"
val precioPENStr = precioPEN?.toString() ?: "-"
val tipoPagoStr = tipoPago ?: "-"
document.add(Paragraph("Vuelo No./Flight #: $vuelo").setFont(bold).setFontSize(12f))
document.add(Paragraph("Grupo de abordaje: $grupo").setFont(normal).setFontSize(10f))
document.add(Paragraph("Seat: $asiento").setFont(normal).setFontSize(10f))
document.add(Paragraph("Booking: $booking").setFont(normal).setFontSize(10f))
document.add(Paragraph("\n"))

// Código de barras (booking)
val barcodeFormat = com.google.zxing.BarcodeFormat.CODE_128
val bitMatrix = com.google.zxing.MultiFormatWriter().encode(booking, barcodeFormat, 300, 80)
val width = bitMatrix.width
val height = bitMatrix.height
val pixels = IntArray(width * height)
for (y in 0 until height) {
    for (x in 0 until width) {
        pixels[y * width + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
}
val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
bmp.setPixels(pixels, 0, width, 0, 0, width, height)
val stream = java.io.ByteArrayOutputStream()
bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
val imageData = com.itextpdf.io.image.ImageDataFactory.create(stream.toByteArray())
document.add(Image(imageData).setWidth(200f).setHeight(50f))

// Formatear la fecha para el PDF
val fechaSalidaFormateada = formatearFecha(fechaSalidaStr) ?: ""
val fechaLlegadaFormateada = formatearFecha(fechaLlegadaStr) ?:""
val horaSalidaCorta = horaSalidaStr?.substringBeforeLast(":") ?: ""
val horaLlegadaCorta = horaLlegadaStr?.substringBeforeLast(":") ?: ""

document.add(Paragraph("$origen - $aeropuerto").setFont(bold).setFontSize(20f))
document.add(Paragraph("\n"))
document.add(Paragraph("Salida: $fechaSalidaFormateada $horaSalidaCorta").setFont(bold).setFontSize(14f))
document.add(Paragraph("Llegada: $fechaLlegadaFormateada $horaLlegadaCorta").setFont(bold).setFontSize(14f))
document.add(Paragraph("\n"))

                            document.add(Paragraph("Recuerda que el artículo personal permitido sin costo por Viva Air es una única pieza de máximo 6 kg y 40x35x25 cm. Exceder las medidas o peso tendrá un costo adicional.").setFont(normal).setFontSize(8f))
                            document.add(Paragraph("\nAcércate al counter para reclamar el pase de abordar y entregar el equipaje, está disponible entre 2 horas y 45 minutos antes de la salida programada para vuelos nacionales. Todos los pasajeros deben presentarse en la sala de espera a más tardar 45 minutos antes de la salida programada del vuelo.").setFont(normal).setFontSize(8f))
                            document.add(Paragraph("\nEl equipaje en cabina, y en general cualquier pieza, que exceda los 55x45x25 cm y 12 kg, deberá ser entregado en el counter de Viva Air antes de ingresar a la espera y dentro de los tiempos mencionados en el punto anterior.").setFont(normal).setFontSize(8f))
                            document.add(Paragraph("\n#YoSoyXPECTRUM").setFont(bold).setFontSize(12f))

                            document.close()
                            pdfMessage = "PDF guardado en Descargas: $fileName"
                        } catch (e: Exception) {
                            pdfMessage = "Error al generar PDF: ${e.message}"
                        }
                    }) {
                        Text("Descargar PDF")
                    }
                    if (pdfMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pdfMessage, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// Composable para mostrar los datos del ticket
@Composable
fun TicketInfoResult(ticket: TicketInfo) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nombre: ${ticket.nombre}", style = MaterialTheme.typography.titleMedium)
            Text("Email: ${ticket.email}")
            Text("Teléfono: ${ticket.telefono}")
            Text("Código Vuelo: ${ticket.codigoVuelo}")
            Text("Fecha Reserva: ${ticket.fechaReserva ?: "-"}")
            Text("Fecha Salida: ${ticket.fechaSalida ?: "-"}")
            Text("Hora Salida: ${ticket.horaSalida ?: "-"}")
            Text("Fecha Llegada: ${ticket.fechaLlegada ?: "-"}")
            Text("Hora Llegada: ${ticket.horaLlegada ?: "-"}")
            Text("Precio USD: ${ticket.precioUSD ?: "-"}")
            Text("Precio PEN: ${ticket.precioPEN ?: "-"}")
            Text("Tipo de Pago: ${ticket.tipoPago ?: "-"}")
        }
    }
}

// Pantalla para mostrar los pasajeros del vuelo (stub funcional)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasajerosScreen(navController: NavHostController, codigoVuelo: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pasajeros Vuelo $codigoVuelo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pantalla de pasajeros para el vuelo $codigoVuelo (aquí irá la consulta real)")
        }
    }
}
