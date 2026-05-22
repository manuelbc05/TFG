package com.example.tfg

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance(
        FirebaseFirestore.getInstance().app,
        "tfg-base-datos"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        actualizarVistaUsuario()

        binding.registerButton.setOnClickListener {
            registrarUsuario()
        }

        binding.loginButton.setOnClickListener {
            iniciarSesion()
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            actualizarVistaUsuario()
        }
    }

    private fun actualizarVistaUsuario() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            binding.profileTextView.text = "Tu perfil: ${currentUser.email}"

            binding.logoutButton.visibility = View.VISIBLE
            binding.loginButton.visibility = View.GONE
            binding.registerButton.visibility = View.GONE
            binding.emailEditText.visibility = View.GONE
            binding.passwordEditText.visibility = View.GONE

            binding.postsTitle.visibility = View.VISIBLE
            binding.postsGrid.visibility = View.VISIBLE

            cargarPublicaciones()

        } else {
            binding.profileTextView.text = "No has iniciado sesión"

            binding.logoutButton.visibility = View.GONE
            binding.loginButton.visibility = View.VISIBLE
            binding.registerButton.visibility = View.VISIBLE
            binding.emailEditText.visibility = View.VISIBLE
            binding.passwordEditText.visibility = View.VISIBLE

            binding.postsTitle.visibility = View.GONE
            binding.postsGrid.visibility = View.GONE
            binding.postsGrid.removeAllViews()
        }
    }

    private fun registrarUsuario() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, ingresa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_SHORT).show()
                actualizarVistaUsuario()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun iniciarSesion() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, ingresa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Bienvenido ${auth.currentUser?.email}", Toast.LENGTH_SHORT).show()
                actualizarVistaUsuario()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarPublicaciones() {
        val user = auth.currentUser ?: return

        binding.postsGrid.removeAllViews()
        binding.postsTitle.text = "Publicaciones"

        db.collection("spots")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->

                binding.postsGrid.removeAllViews()

                binding.postsTitle.text = "Publicaciones (${documents.size()})"

                for (document in documents) {
                    val marca = document.getString("marca") ?: "Marca"
                    val modelo = document.getString("modelo") ?: "Modelo"
                    val anio = document.getString("anio") ?: "Año"
                    val dato = document.getString("dato") ?: ""
                    val fecha = formatearFecha(document.getTimestamp("createdAt"))
                    val imageBase64 = document.getString("imageBase64") ?: ""

                    crearCuadradoPublicacion(
                        marca = marca,
                        modelo = modelo,
                        anio = anio,
                        dato = dato,
                        fecha = fecha,
                        imageBase64 = imageBase64
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error cargando publicaciones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun crearCuadradoPublicacion(
        marca: String,
        modelo: String,
        anio: String,
        dato: String,
        fecha: String,
        imageBase64: String
    ) {
        val context = requireContext()

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(6, 6, 6, 6)

        val size = resources.displayMetrics.widthPixels / 3

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
        titulo.textSize = 12f
        titulo.gravity = Gravity.CENTER
        titulo.maxLines = 1

        val year = TextView(context)
        year.text = anio
        year.textSize = 11f
        year.gravity = Gravity.CENTER
        year.maxLines = 1

        card.addView(imageView)
        card.addView(titulo)
        card.addView(year)

        card.setOnClickListener {
            mostrarDialogSpot(
                marca = marca,
                modelo = modelo,
                anio = anio,
                dato = dato,
                fecha = fecha,
                imageBase64 = imageBase64
            )
        }

        binding.postsGrid.addView(card)
    }

    private fun mostrarDialogSpot(
        marca: String,
        modelo: String,
        anio: String,
        dato: String,
        fecha: String,
        imageBase64: String
    ) {
        val context = requireContext()

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

        val fechaSpot = TextView(context)
        fechaSpot.text = "Fecha: $fecha"
        fechaSpot.textSize = 14f
        fechaSpot.gravity = Gravity.CENTER
        fechaSpot.setPadding(0, 0, 0, 18)

        val descripcion = TextView(context)
        descripcion.text = if (dato.isNotBlank()) dato else "Sin descripción"
        descripcion.textSize = 15f
        descripcion.gravity = Gravity.CENTER

        contenedor.addView(imageView)
        contenedor.addView(titulo)
        contenedor.addView(year)
        contenedor.addView(fechaSpot)
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
