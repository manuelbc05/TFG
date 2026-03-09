package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentProcesadoSpotBinding

class ProcesadoSpotFragment : Fragment() {

    private var _binding: FragmentProcesadoSpotBinding? = null
    private val binding get() = _binding!!

    // Launcher para elegir imagen de galería o tomar foto
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            // Si es cámara, la imagen vendrá en "data" extras como bitmap
            if (imageUri != null) {
                binding.imageView.setImageURI(imageUri)
            } else {
                val bitmap = result.data?.extras?.get("data")
                binding.imageView.setImageBitmap(bitmap as? android.graphics.Bitmap)
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
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pickImageLauncher.launch(intent)
        }

        binding.btnBack.setOnClickListener {
            // Esto hará que el fragment vuelva al anterior en el back stack
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}