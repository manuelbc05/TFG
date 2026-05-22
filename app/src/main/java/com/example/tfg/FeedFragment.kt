package com.example.tfg

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class FeedFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    private lateinit var scrollView: ScrollView
    private lateinit var contenedor: LinearLayout

    private var ultimoDocumento: DocumentSnapshot? = null
    private var cargando = false
    private var noHayMas = false

    private val limiteCarga = 5

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        scrollView = ScrollView(requireContext())
        scrollView.isFillViewport = true

        contenedor = LinearLayout(requireContext())
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(20, 20, 20, 30)

        scrollView.addView(contenedor)

        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarMasSpots()

        scrollView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val scroll = v as ScrollView
            val child = scroll.getChildAt(0)

            if (child != null) {
                val diferencia = child.bottom - (scroll.height + scrollY)

                if (diferencia < 300 && !cargando && !noHayMas) {
                    cargarMasSpots()
                }
            }
        }
    }

    private fun cargarMasSpots() {
        if (cargando || noHayMas) return

        cargando = true

        var consulta = db.collection("spots")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limiteCarga.toLong())

        ultimoDocumento?.let {
            consulta = consulta.startAfter(it)
        }

        consulta.get()
            .addOnSuccessListener { documentos ->

                if (documentos.isEmpty) {
                    noHayMas = true

                    if (contenedor.childCount == 0) {
                        mostrarTextoVacio()
                    }

                    cargando = false
                    return@addOnSuccessListener
                }

                for (documento in documentos) {
                    crearSpotFeed(documento)
                }

                ultimoDocumento = documentos.documents.lastOrNull()

                if (documentos.size() < limiteCarga) {
                    noHayMas = true
                }

                cargando = false
            }
            .addOnFailureListener { e ->
                cargando = false
                Toast.makeText(
                    requireContext(),
                    "Error cargando el feed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarTextoVacio() {
        val texto = TextView(requireContext())
        texto.text = "Todavía no hay spots publicados"
        texto.textSize = 18f
        texto.gravity = Gravity.CENTER
        texto.setPadding(0, 120, 0, 0)

        contenedor.addView(texto)
    }

    private fun crearSpotFeed(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca no detectada"
        val modelo = document.getString("modelo") ?: "Modelo no detectado"
        val anio = document.getString("anio") ?: "Año no detectado"
        val dato = document.getString("dato") ?: "Sin descripción"
        val persona = document.getString("userEmail") ?: "Usuario desconocido"
        val fecha = formatearFecha(document.getTimestamp("createdAt"))

        val imagenesBase64 = obtenerImagenes(document)
        var imagenActual = 0

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(0, 20, 0, 45)

        val nombrePersona = TextView(context)
        nombrePersona.text = persona
        nombrePersona.textSize = 18f
        nombrePersona.gravity = Gravity.CENTER
        nombrePersona.setPadding(0, 0, 0, 14)

        val zonaFoto = FrameLayout(context)

        val anchoPantalla = resources.displayMetrics.widthPixels
        val altoFoto = (resources.displayMetrics.heightPixels * 0.58).toInt()

        zonaFoto.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            altoFoto
        )

        val imageView = ImageView(context)
        val paramsFoto = FrameLayout.LayoutParams(
            (anchoPantalla * 0.68).toInt(),
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        paramsFoto.gravity = Gravity.CENTER

        imageView.layoutParams = paramsFoto
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setBackgroundResource(android.R.color.darker_gray)

        ponerImagen(imageView, imagenesBase64.firstOrNull())

        zonaFoto.addView(imageView)

        if (imagenesBase64.size > 1) {
            val flechaIzquierda = TextView(context)
            flechaIzquierda.text = "‹"
            flechaIzquierda.textSize = 60f
            flechaIzquierda.gravity = Gravity.CENTER
            flechaIzquierda.setPadding(20, 0, 20, 0)

            val paramsIzquierda = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            paramsIzquierda.gravity = Gravity.CENTER_VERTICAL or Gravity.START

            val flechaDerecha = TextView(context)
            flechaDerecha.text = "›"
            flechaDerecha.textSize = 60f
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

        val tituloCoche = TextView(context)
        tituloCoche.text = "$marca $modelo · $anio"
        tituloCoche.textSize = 17f
        tituloCoche.gravity = Gravity.CENTER
        tituloCoche.setPadding(0, 16, 0, 6)

        val fechaSpot = TextView(context)
        fechaSpot.text = fecha
        fechaSpot.textSize = 13f
        fechaSpot.gravity = Gravity.CENTER
        fechaSpot.setPadding(0, 0, 0, 10)

        val descripcion = TextView(context)
        descripcion.text = dato
        descripcion.textSize = 16f
        descripcion.gravity = Gravity.CENTER
        descripcion.setPadding(20, 0, 20, 0)

        card.addView(nombrePersona)
        card.addView(zonaFoto)
        card.addView(tituloCoche)
        card.addView(fechaSpot)
        card.addView(descripcion)

        card.setOnClickListener {
            mostrarDialogSpot(document)
        }

        contenedor.addView(card)
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

        val contenedorDialog = LinearLayout(context)
        contenedorDialog.orientation = LinearLayout.VERTICAL
        contenedorDialog.setPadding(36, 20, 36, 10)

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

        contenedorDialog.addView(zonaFoto)
        contenedorDialog.addView(titulo)
        contenedorDialog.addView(year)
        contenedorDialog.addView(infoSpot)
        contenedorDialog.addView(descripcion)

        AlertDialog.Builder(context)
            .setTitle("Spot")
            .setView(contenedorDialog)
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