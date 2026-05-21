package com.example.tfg

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
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

class ResultadoIAFragment : Fragment(R.layout.fragment_resultado_ia) {

    private lateinit var binding: FragmentResultadoIaBinding

    private var posicionActual = 0
    private val imagenes = SpotImageStorage.imagenes

    private var marcaDetectada = ""
    private var modeloDetectado = ""
    private var anioDetectado = ""
    private var datoDetectado = ""

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentResultadoIaBinding.bind(view)

        if (imagenes.isEmpty()) {
            Toast.makeText(requireContext(), "No hay imágenes seleccionadas", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

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

    private fun publicarSpot() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para publicar", Toast.LENGTH_SHORT).show()
            return
        }

        if (marcaDetectada.isEmpty() || modeloDetectado.isEmpty() || anioDetectado.isEmpty()) {
            Toast.makeText(requireContext(), "Espera a que termine el análisis", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPublicar.isEnabled = false
        binding.btnPublicar.text = "Publicando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = convertirABitmap(imagenes[0])
                val imageBase64 = bitmapToBase64(bitmap)

                val spot = hashMapOf(
                    "userId" to user.uid,
                    "userEmail" to user.email,
                    "marca" to marcaDetectada,
                    "modelo" to modeloDetectado,
                    "anio" to anioDetectado,
                    "dato" to datoDetectado,
                    "imageBase64" to imageBase64,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                withContext(Dispatchers.Main) {
                    db.collection("spots")
                        .add(spot)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Spot publicado", Toast.LENGTH_SHORT).show()

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.frameLayout, ProfileFragment())
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