package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tfg.databinding.BottomSheetSpotBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class UploadSpotBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSpotBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSpotBinding.inflate(inflater, container, false)

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(requireContext(), "Necesitas una cuenta para esto", Toast.LENGTH_SHORT).show()
            dismiss()
            return binding.root
        }

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