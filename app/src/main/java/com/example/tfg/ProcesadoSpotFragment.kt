package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentProcesadoSpotBinding

class ProcesadoSpotFragment : Fragment(R.layout.fragment_procesado_spot) {

    private lateinit var binding: FragmentProcesadoSpotBinding

    private val imagenes = mutableListOf<Any>()
    private var posicionActual = 0

    private val launcherImagen = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            if (imagenes.size >= 4) {
                Toast.makeText(requireContext(), "Máximo 4 fotos", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val uri: Uri? = result.data?.data

            if (uri != null) {
                añadirImagen(uri)
            } else {
                val bitmap = result.data?.extras?.get("data") as? Bitmap

                if (bitmap != null) {
                    añadirImagen(bitmap)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProcesadoSpotBinding.bind(view)

        binding.btnNext.visibility = View.GONE

        binding.btnGallery.setOnClickListener {

            if (imagenes.size >= 4) {
                Toast.makeText(requireContext(), "Máximo 4 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }

            launcherImagen.launch(intent)
        }

        binding.btnCamera.setOnClickListener {

            if (imagenes.size >= 4) {
                Toast.makeText(requireContext(), "Máximo 4 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcherImagen.launch(intent)
        }

        binding.btnNext.setOnClickListener {

            SpotImageStorage.imagenes.clear()
            SpotImageStorage.imagenes.addAll(imagenes)

            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ResultadoIAFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun añadirImagen(imagen: Any) {

        when (posicionActual) {
            0 -> mostrarImagen(binding.imageView1, imagen)
            1 -> mostrarImagen(binding.imageView2, imagen)
            2 -> mostrarImagen(binding.imageView3, imagen)
            3 -> mostrarImagen(binding.imageView4, imagen)
        }

        imagenes.add(imagen)
        posicionActual++

        binding.btnNext.visibility = View.VISIBLE
    }

    private fun mostrarImagen(imageView: ImageView, imagen: Any) {
        when (imagen) {
            is Uri -> imageView.setImageURI(imagen)
            is Bitmap -> imageView.setImageBitmap(imagen)
        }
    }
}