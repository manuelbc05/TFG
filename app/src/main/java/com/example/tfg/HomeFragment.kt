package com.example.tfg

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentHomeBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    private val spots = mutableMapOf<String, DocumentSnapshot>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = binding.webview

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun openSpot(spotId: String) {
                    requireActivity().runOnUiThread {
                        abrirSpot(spotId)
                    }
                }
            },
            "AndroidSpot"
        )

        webView.loadUrl("file:///android_asset/map.html")

        binding.btnAction.setOnClickListener {
            val bottomSheet = UploadSpotBottomSheet()
            bottomSheet.show(parentFragmentManager, "UploadSpot")
        }

        cargarSpotsEnMapa()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            binding.webview.stopLoading()
            binding.webview.loadUrl("about:blank")
            binding.webview.clearHistory()
            binding.webview.removeAllViews()
            binding.webview.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarSpotsEnMapa() {
        db.collection("spots")
            .get()
            .addOnSuccessListener { documents ->
                spots.clear()

                val array = JSONArray()

                for (document in documents) {
                    val latitud = document.getDouble("latitud")
                    val longitud = document.getDouble("longitud")

                    if (latitud != null && longitud != null) {
                        spots[document.id] = document

                        val item = JSONObject()
                        item.put("id", document.id)
                        item.put("lat", latitud)
                        item.put("lng", longitud)
                        item.put(
                            "titulo",
                            "${document.getString("marca") ?: "Coche"} ${document.getString("modelo") ?: ""}".trim()
                        )
                        array.put(item)
                    }
                }

                pintarMarcadores(array.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error cargando spots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pintarMarcadores(spotsJson: String) {
        val javascript = """
            javascript:(function() {
                if (typeof window.spotMarkers === 'undefined') {
                    window.spotMarkers = [];
                }

                for (var i = 0; i < window.spotMarkers.length; i++) {
                    map.removeLayer(window.spotMarkers[i]);
                }

                window.spotMarkers = [];

                var spots = $spotsJson;

                for (var j = 0; j < spots.length; j++) {
                    var spot = spots[j];
                    var marker = L.marker([spot.lat, spot.lng]).addTo(map);
                    marker.bindPopup(spot.titulo);

                    marker.on('click', (function(id) {
                        return function() {
                            AndroidSpot.openSpot(id);
                        };
                    })(spot.id));

                    window.spotMarkers.push(marker);
                }
            })();
        """.trimIndent()

        binding.webview.postDelayed({
            binding.webview.evaluateJavascript(javascript, null)
        }, 700)
    }

    private fun abrirSpot(spotId: String) {
        val document = spots[spotId]

        if (document != null) {
            mostrarDialogSpot(document)
            return
        }

        db.collection("spots")
            .document(spotId)
            .get()
            .addOnSuccessListener { nuevoDocumento ->
                if (nuevoDocumento.exists()) {
                    spots[spotId] = nuevoDocumento
                    mostrarDialogSpot(nuevoDocumento)
                } else {
                    Toast.makeText(requireContext(), "No se ha encontrado el spot", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error abriendo spot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogSpot(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca no detectada"
        val modelo = document.getString("modelo") ?: "Modelo no detectado"
        val anio = document.getString("anio") ?: "Año no detectado"
        val dato = document.getString("dato") ?: "Sin descripción"
        val persona = document.getString("userEmail") ?: "Usuario desconocido"
        val fecha = formatearFecha(document.getTimestamp("createdAt"))

        val imagenesBase64 = obtenerImagenes(document)
        var imagenActual = 0

        val contenedor = LinearLayout(context)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(36, 20, 36, 10)

        val zonaFoto = FrameLayout(context)

        zonaFoto.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            520
        )

        val imageView = ImageView(context)
        imageView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        ponerImagen(imageView, imagenesBase64.firstOrNull())

        zonaFoto.addView(imageView)

        if (imagenesBase64.size > 1) {
            val flechaIzquierda = TextView(context)
            flechaIzquierda.text = "‹"
            flechaIzquierda.textSize = 56f
            flechaIzquierda.gravity = Gravity.CENTER
            flechaIzquierda.setPadding(20, 0, 20, 0)

            val paramsIzquierda = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            paramsIzquierda.gravity = Gravity.CENTER_VERTICAL or Gravity.START

            val flechaDerecha = TextView(context)
            flechaDerecha.text = "›"
            flechaDerecha.textSize = 56f
            flechaDerecha.gravity = Gravity.CENTER
            flechaDerecha.setPadding(20, 0, 20, 0)

            val paramsDerecha = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            paramsDerecha.gravity = Gravity.CENTER_VERTICAL or Gravity.END

            flechaIzquierda.setOnClickListener {
                imagenActual--
                if (imagenActual < 0) {
                    imagenActual = imagenesBase64.size - 1
                }

                ponerImagen(imageView, imagenesBase64[imagenActual])
            }

            flechaDerecha.setOnClickListener {
                imagenActual++
                if (imagenActual >= imagenesBase64.size) {
                    imagenActual = 0
                }

                ponerImagen(imageView, imagenesBase64[imagenActual])
            }

            zonaFoto.addView(flechaIzquierda, paramsIzquierda)
            zonaFoto.addView(flechaDerecha, paramsDerecha)
        }

        val titulo = TextView(context)
        titulo.text = "$marca $modelo"
        titulo.textSize = 22f
        titulo.gravity = Gravity.CENTER
        titulo.setPadding(0, 24, 0, 4)

        val year = TextView(context)
        year.text = anio
        year.textSize = 18f
        year.gravity = Gravity.CENTER
        year.setPadding(0, 0, 0, 18)

        val infoSpot = TextView(context)
        infoSpot.text = "Spoteado por: $persona\nFecha: $fecha"
        infoSpot.textSize = 14f
        infoSpot.gravity = Gravity.CENTER
        infoSpot.setPadding(0, 0, 0, 18)

        val descripcion = TextView(context)
        descripcion.text = dato
        descripcion.textSize = 15f
        descripcion.gravity = Gravity.CENTER

        contenedor.addView(zonaFoto)
        contenedor.addView(titulo)
        contenedor.addView(year)
        contenedor.addView(infoSpot)
        contenedor.addView(descripcion)

        AlertDialog.Builder(context)
            .setTitle("Spot")
            .setView(contenedor)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun obtenerImagenes(document: DocumentSnapshot): List<String> {
        val lista = document.get("imagenesBase64") as? List<String>

        if (!lista.isNullOrEmpty()) {
            return lista
        }

        val imagenPrincipal = document.getString("imageBase64") ?: ""

        return if (imagenPrincipal.isNotEmpty()) {
            listOf(imagenPrincipal)
        } else {
            emptyList()
        }
    }

    private fun ponerImagen(imageView: ImageView, imageBase64: String?) {
        if (!imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.cochedesconocido)
            }
        } else {
            imageView.setImageResource(R.drawable.cochedesconocido)
        }
    }

    private fun formatearFecha(timestamp: Timestamp?): String {
        if (timestamp == null) return "Fecha no disponible"

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formato.format(timestamp.toDate())
    }
}