package com.example.tfg

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.FragmentResultadoIaBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultadoIAFragment : Fragment(R.layout.fragment_resultado_ia) {

    private lateinit var binding: FragmentResultadoIaBinding

    private var posicionActual = 0
    private val imagenes = SpotImageStorage.imagenes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentResultadoIaBinding.bind(view)

        if (imagenes.isEmpty()) {
            Toast.makeText(requireContext(), "No hay imágenes seleccionadas", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        mostrarImagenActual()
        analizarImagenActual()

        binding.btnPrev.setOnClickListener {
            if (posicionActual > 0) {
                posicionActual--
                mostrarImagenActual()
                analizarImagenActual()
            }
        }

        binding.btnNextImage.setOnClickListener {
            if (posicionActual < imagenes.size - 1) {
                posicionActual++
                mostrarImagenActual()
                analizarImagenActual()
            }
        }

        binding.btnPublicar.setOnClickListener {
            Toast.makeText(requireContext(), "Publicar aún no está implementado", Toast.LENGTH_SHORT).show()
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

    private fun analizarImagenActual() {
        binding.txtMarca.text = "Analizando..."
        binding.txtModelo.text = "Analizando..."
        binding.txtAnio.text = "Analizando..."
        binding.txtDato.text = "Analizando imagen..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = convertirABitmap(imagenes[posicionActual])

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
                    pintarResultado(texto)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtMarca.text = "Error"
                    binding.txtModelo.text = "Error"
                    binding.txtAnio.text = "Error"
                    binding.txtDato.text = e.message ?: "Error al analizar la imagen"
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

    private fun pintarResultado(texto: String) {
        val marca = texto.substringAfter("Marca:", "No detectado")
            .substringBefore("Modelo:")
            .trim()

        val modelo = texto.substringAfter("Modelo:", "No detectado")
            .substringBefore("Año:")
            .trim()

        val anio = texto.substringAfter("Año:", "No detectado")
            .substringBefore("Dato:")
            .trim()

        val dato = texto.substringAfter("Dato:", texto)
            .trim()

        binding.txtMarca.text = marca.ifEmpty { "No detectado" }
        binding.txtModelo.text = modelo.ifEmpty { "No detectado" }
        binding.txtAnio.text = anio.ifEmpty { "No detectado" }
        binding.txtDato.text = dato.ifEmpty { texto }
    }
}