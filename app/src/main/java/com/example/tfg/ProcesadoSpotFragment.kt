package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentProcesadoSpotBinding

class ProcesadoSpotFragment : Fragment() {

    private var _binding: FragmentProcesadoSpotBinding? = null
    private val binding get() = _binding!!

    private var currentPhotoSlot = 0

    private val imageUris = mutableListOf<Any>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            if (imageUris.size >= 4) {
                Toast.makeText(requireContext(), "Solo puedes subir 4 fotos", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val imageUri: Uri? = result.data?.data

            if (imageUri != null) {
                addImageToSlot(imageUri)
            } else {
                val bitmap = result.data?.extras?.get("data") as? Bitmap

                if (bitmap != null) {
                    addImageToSlot(bitmap)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProcesadoSpotBinding.inflate(inflater, container, false)

        binding.btnGallery.setOnClickListener {

            if (imageUris.size >= 4) {
                Toast.makeText(requireContext(), "Máximo 4 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }

            pickImageLauncher.launch(intent)
        }

        binding.btnCamera.setOnClickListener {

            if (imageUris.size >= 4) {
                Toast.makeText(requireContext(), "Máximo 4 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pickImageLauncher.launch(intent)
        }

        binding.btnSubmitSpot.setOnClickListener {

            if (imageUris.size > 4) {
                Toast.makeText(requireContext(), "No puedes subir más de 4 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageUris.isEmpty()) {
                Toast.makeText(requireContext(), "Añade al menos una foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Spot enviado correctamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    private fun addImageToSlot(image: Any) {

        when (currentPhotoSlot) {

            0 -> {
                if (image is Uri) {
                    binding.imageView1.setImageURI(image)
                } else {
                    binding.imageView1.setImageBitmap(image as Bitmap)
                }
            }

            1 -> {
                if (image is Uri) {
                    binding.imageView2.setImageURI(image)
                } else {
                    binding.imageView2.setImageBitmap(image as Bitmap)
                }
            }

            2 -> {
                if (image is Uri) {
                    binding.imageView3.setImageURI(image)
                } else {
                    binding.imageView3.setImageBitmap(image as Bitmap)
                }
            }

            3 -> {
                if (image is Uri) {
                    binding.imageView4.setImageURI(image)
                } else {
                    binding.imageView4.setImageBitmap(image as Bitmap)
                }
            }
        }

        imageUris.add(image)
        currentPhotoSlot++
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}