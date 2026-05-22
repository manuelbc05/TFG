package com.example.tfg

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentSearchBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var binding: FragmentSearchBinding

    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    private val todosLosSpots = mutableListOf<DocumentSnapshot>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchBinding.bind(view)

        binding.txtDiscoverInfo.text = "Busca por persona, marca, modelo o año."

        binding.btnBuscar.setOnClickListener {
            filtrarSpots(binding.searchEditText.text.toString())
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filtrarSpots(binding.searchEditText.text.toString())
                true
            } else {
                false
            }
        }

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            filtrarSpots(text?.toString() ?: "")
        }

        cargarSpots()
    }

    private fun cargarSpots() {
        binding.txtDiscoverInfo.text = "Cargando spots..."
        binding.resultsGrid.removeAllViews()

        db.collection("spots")
            .get()
            .addOnSuccessListener { documents ->
                todosLosSpots.clear()

                for (document in documents) {
                    todosLosSpots.add(document)
                }

                binding.txtDiscoverInfo.text = "Busca por persona, marca, modelo, año."
                filtrarSpots(binding.searchEditText.text.toString())
            }
            .addOnFailureListener { e ->
                binding.txtDiscoverInfo.text = "No se pudieron cargar los spots"
                Toast.makeText(requireContext(), "Error cargando spots: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filtrarSpots(textoBusqueda: String) {
        if (!::binding.isInitialized) return

        val busqueda = textoBusqueda.trim().lowercase(Locale.getDefault())

        val resultados = if (busqueda.isEmpty()) {
            emptyList()
        } else {
            todosLosSpots.filter { document ->
                val marca = document.getString("marca") ?: ""
                val modelo = document.getString("modelo") ?: ""
                val anio = document.getString("anio") ?: ""
                val persona = document.getString("userEmail") ?: ""

                val textoCompleto = "$marca $modelo $anio $persona".lowercase(Locale.getDefault())
                textoCompleto.contains(busqueda)
            }
        }

        pintarResultados(resultados)
    }

    private fun pintarResultados(resultados: List<DocumentSnapshot>) {
        binding.resultsGrid.removeAllViews()

        binding.txtResultsCount.text = when {
            binding.searchEditText.text.toString().isBlank() -> "Busca un coche o una persona"
            resultados.isEmpty() -> "No se han encontrado resultados"
            resultados.size == 1 -> "1 resultado encontrado"
            else -> "${resultados.size} resultados encontrados"
        }

        for (document in resultados) {
            crearCuadradoResultado(document)
        }
    }

    private fun crearCuadradoResultado(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca"
        val modelo = document.getString("modelo") ?: "Modelo"
        val anio = document.getString("anio") ?: "Año"
        val imageBase64 = document.getString("imageBase64") ?: ""

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(6, 6, 6, 6)

        val size = resources.displayMetrics.widthPixels / 2

        val cardParams = GridLayout.LayoutParams()
        cardParams.width = size
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        card.layoutParams = cardParams

        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            size
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
        titulo.textSize = 13f
        titulo.gravity = Gravity.CENTER
        titulo.maxLines = 1
        titulo.setPadding(4, 6, 4, 0)

        val year = TextView(context)
        year.text = anio
        year.textSize = 12f
        year.gravity = Gravity.CENTER
        year.maxLines = 1

        card.addView(imageView)
        card.addView(titulo)
        card.addView(year)

        card.setOnClickListener {
            mostrarDialogSpot(document)
        }

        binding.resultsGrid.addView(card)
    }

    private fun mostrarDialogSpot(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca no detectada"
        val modelo = document.getString("modelo") ?: "Modelo no detectado"
        val anio = document.getString("anio") ?: "Año no detectado"
        val dato = document.getString("dato") ?: "Sin descripción"
        val persona = document.getString("userEmail") ?: "Usuario desconocido"
        val fecha = formatearFecha(document.getTimestamp("createdAt"))
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

        val infoSpot = TextView(context)
        infoSpot.text = "Spoteado por: $persona\nFecha: $fecha"
        infoSpot.textSize = 14f
        infoSpot.gravity = Gravity.CENTER
        infoSpot.setPadding(0, 0, 0, 18)

        val descripcion = TextView(context)
        descripcion.text = if (dato.isNotBlank()) dato else "Sin descripción"
        descripcion.textSize = 15f
        descripcion.gravity = Gravity.CENTER

        contenedor.addView(imageView)
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

    private fun formatearFecha(timestamp: Timestamp?): String {
        if (timestamp == null) return "Fecha no disponible"

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formato.format(timestamp.toDate())
    }
}
