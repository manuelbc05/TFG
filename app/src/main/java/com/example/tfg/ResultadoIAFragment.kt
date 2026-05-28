package com.example.tfg

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.FragmentResultadoIaBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

class ResultadoIAFragment : Fragment(R.layout.fragment_resultado_ia) {

    private lateinit var binding: FragmentResultadoIaBinding

    private var posicionActual = 0
    private val imagenes = SpotImageStorage.imagenes

    private var marcaDetectada = ""
    private var modeloDetectado = ""
    private var anioDetectado = ""
    private var datoDetectado = ""

    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentResultadoIaBinding.bind(view)

        if (auth.currentUser == null) {
            Toast.makeText(requireContext(), "Necesitas una cuenta para esto", Toast.LENGTH_SHORT).show()

            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ProfileFragment())
                .addToBackStack(null)
                .commit()

            return
        }

        if (imagenes.isEmpty()) {
            Toast.makeText(requireContext(), "No hay imágenes seleccionadas", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        binding.btnPublicar.isEnabled = false
        binding.txtUbicacion.text = "Ubicación no seleccionada"

        mostrarImagenActual()
        analizarSoloPrimeraImagen()

        binding.btnPrev.setOnClickListener {
            if (posicionActual > 0) {
                posicionActual--
                mostrarImagenActual()
                mostrarResultadoGuardado()
            }
        }

        binding.btnNextImage.setOnClickListener {
            if (posicionActual < imagenes.size - 1) {
                posicionActual++
                mostrarImagenActual()
                mostrarResultadoGuardado()
            }
        }

        binding.btnSeleccionarUbicacion.setOnClickListener {
            abrirSelectorUbicacion()
        }

        binding.btnPublicar.setOnClickListener {
            publicarSpot()
        }
    }

    private fun mostrarImagenActual() {
        when (val imagen = imagenes[posicionActual]) {
            is Uri -> binding.imageCar.setImageURI(imagen)
            is Bitmap -> binding.imageCar.setImageBitmap(imagen)
        }

        binding.btnPrev.visibility =
            if (posicionActual == 0) View.INVISIBLE else View.VISIBLE

        binding.btnNextImage.visibility =
            if (posicionActual == imagenes.size - 1) View.INVISIBLE else View.VISIBLE
    }

    private fun analizarSoloPrimeraImagen() {
        binding.txtMarca.text = "Analizando..."
        binding.txtModelo.text = "Analizando..."
        binding.txtAnio.text = "Analizando..."
        binding.txtDato.text = "Analizando imagen..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = convertirABitmap(imagenes[0])

                val generativeModel = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = getString(R.string.gemini_api_key)
                )

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)

                        text(
                            """
                            Analiza el coche de la imagen.
                            Respóndeme exactamente con este formato:
                            Marca:
                            Modelo:
                            Año:
                            Dato:

                            El dato debe ser corto, interesante y relacionado con ese coche.
                            Si no estás seguro, pon "No detectado".
                            """.trimIndent()
                        )
                    }
                )

                val texto = response.text ?: "No detectado"

                withContext(Dispatchers.Main) {
                    guardarResultado(texto)
                    mostrarResultadoGuardado()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    marcaDetectada = "Error"
                    modeloDetectado = "Error"
                    anioDetectado = "Error"
                    datoDetectado = "No se pudo analizar la imagen"

                    mostrarResultadoGuardado()
                }
            }
        }
    }

    private fun guardarResultado(texto: String) {
        marcaDetectada = texto.substringAfter("Marca:", "No detectado")
            .substringBefore("Modelo:")
            .trim()
            .ifEmpty { "No detectado" }

        modeloDetectado = texto.substringAfter("Modelo:", "No detectado")
            .substringBefore("Año:")
            .trim()
            .ifEmpty { "No detectado" }

        anioDetectado = texto.substringAfter("Año:", "No detectado")
            .substringBefore("Dato:")
            .trim()
            .ifEmpty { "No detectado" }

        datoDetectado = texto.substringAfter("Dato:", texto)
            .trim()
            .ifEmpty { texto }
    }

    private fun mostrarResultadoGuardado() {
        binding.txtMarca.text = marcaDetectada
        binding.txtModelo.text = modeloDetectado
        binding.txtAnio.text = anioDetectado
        binding.txtDato.text = datoDetectado
    }

    private fun abrirSelectorUbicacion() {
        val webView = WebView(requireContext())

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        var dialog: AlertDialog? = null

        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun onLocationSelected(lat: Double, lng: Double) {
                    requireActivity().runOnUiThread {
                        latitudSeleccionada = lat
                        longitudSeleccionada = lng

                        binding.txtUbicacion.text = String.format(
                            Locale.US,
                            "Ubicación seleccionada: %.5f, %.5f",
                            lat,
                            lng
                        )

                        binding.btnPublicar.isEnabled = true
                        dialog?.dismiss()
                    }
                }
            },
            "AndroidLocation"
        )

        val latInicial = latitudSeleccionada ?: 40.4168
        val lngInicial = longitudSeleccionada ?: -3.7038

        webView.loadDataWithBaseURL(
            "https://unpkg.com/",
            crearHtmlSelectorUbicacion(latInicial, lngInicial),
            "text/html",
            "UTF-8",
            null
        )

        dialog = AlertDialog.Builder(requireContext())
            .setTitle("Selecciona la ubicación del spot")
            .setMessage("Pulsa una vez sobre el mapa para poner la chincheta.")
            .setView(webView)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun crearHtmlSelectorUbicacion(latInicial: Double, lngInicial: Double): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
                <style>
                    html, body, #mapid {
                        width: 100%;
                        height: 420px;
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>
            <body>
                <div id="mapid"></div>
                <script>
                    var map = L.map('mapid').setView([$latInicial, $lngInicial], 13);
                    var marker = null;

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    function setMarker(lat, lng) {
                        if (marker !== null) {
                            map.removeLayer(marker);
                        }

                        marker = L.marker([lat, lng]).addTo(map);
                        AndroidLocation.onLocationSelected(lat, lng);
                    }

                    map.on('click', function(e) {
                        setMarker(e.latlng.lat, e.latlng.lng);
                    });

                    ${
            if (latitudSeleccionada != null && longitudSeleccionada != null) {
                "marker = L.marker([$latInicial, $lngInicial]).addTo(map);"
            } else {
                ""
            }
        }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun publicarSpot() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "Necesitas una cuenta para esto", Toast.LENGTH_SHORT).show()
            return
        }

        if (marcaDetectada.isEmpty() || modeloDetectado.isEmpty() || anioDetectado.isEmpty()) {
            Toast.makeText(requireContext(), "Espera a que termine el análisis", Toast.LENGTH_SHORT).show()
            return
        }

        val latitud = latitudSeleccionada
        val longitud = longitudSeleccionada

        if (latitud == null || longitud == null) {
            Toast.makeText(requireContext(), "Selecciona una ubicación antes de publicar", Toast.LENGTH_SHORT).show()
            binding.btnPublicar.isEnabled = false
            return
        }

        binding.btnPublicar.isEnabled = false
        binding.btnPublicar.text = "Publicando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imagenesBase64 = mutableListOf<String>()

                for (imagen in imagenes) {
                    val bitmap = convertirABitmap(imagen)
                    val base64 = bitmapToBase64(bitmap)
                    imagenesBase64.add(base64)
                }

                val imagenPrincipal = imagenesBase64.firstOrNull().orEmpty()

                val spot = hashMapOf(
                    "userId" to user.uid,
                    "userEmail" to user.email,
                    "spottedBy" to (user.email ?: "Usuario desconocido"),
                    "marca" to marcaDetectada,
                    "modelo" to modeloDetectado,
                    "anio" to anioDetectado,
                    "dato" to datoDetectado,
                    "imageBase64" to imagenPrincipal,
                    "imagenesBase64" to imagenesBase64,
                    "latitud" to latitud,
                    "longitud" to longitud,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                withContext(Dispatchers.Main) {
                    db.collection("spots")
                        .add(spot)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Spot publicado", Toast.LENGTH_SHORT).show()

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.frameLayout, HomeFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        .addOnFailureListener { e ->
                            binding.btnPublicar.isEnabled = true
                            binding.btnPublicar.text = "Publicar"
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnPublicar.isEnabled = true
                    binding.btnPublicar.text = "Publicar"
                    Toast.makeText(requireContext(), "Error al publicar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun convertirABitmap(imagen: Any): Bitmap {
        return when (imagen) {
            is Bitmap -> imagen

            is Uri -> {
                val inputStream = requireContext().contentResolver.openInputStream(imagen)
                BitmapFactory.decodeStream(inputStream)
            }

            else -> {
                BitmapFactory.decodeResource(resources, R.drawable.cochedesconocido)
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resizedBitmap = redimensionarBitmap(bitmap, 800)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

        val bytes = outputStream.toByteArray()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun redimensionarBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}