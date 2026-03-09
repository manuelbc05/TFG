package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tfg.databinding.BottomSheetSpotBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UploadSpotBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSpotBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout con ViewBinding
        _binding = BottomSheetSpotBinding.inflate(inflater, container, false)

        // Aquí va tu listener para abrir ProcesadoSpotFragment
        binding.btnUploadSpot.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ProcesadoSpotFragment())
                .addToBackStack(null)
                .commit()
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}