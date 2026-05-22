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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentHomeBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
                        item.put("titulo", "${document.getString("marca") ?: "Coche"} ${document.getString("modelo") ?: ""}".trim())
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
        val imageBase64 = document.getString("imageBase64") ?: ""

        val contenedor = LinearLayout(context)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(36, 20, 36, 10)

        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            520
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        if (imageBase64.isNotEmpty()) {
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

        val descripcion = TextView(context)
        descripcion.text = dato
        descripcion.textSize = 15f
        descripcion.gravity = Gravity.CENTER

        contenedor.addView(imageView)
        contenedor.addView(titulo)
        contenedor.addView(year)
        contenedor.addView(descripcion)

        AlertDialog.Builder(context)
            .setTitle("Spot")
            .setView(contenedor)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
