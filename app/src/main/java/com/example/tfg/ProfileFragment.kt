package com.example.tfg

import android.content.Intent
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
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
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
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa ambos campos.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    actualizarVistaUsuario()
                } else {
                    Toast.makeText(context, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun iniciarSesion() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa ambos campos.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Bienvenido ${auth.currentUser?.email}", Toast.LENGTH_SHORT).show()
                    actualizarVistaUsuario()

                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)

                } else {
                    Toast.makeText(context, "Error en el login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun cargarPublicaciones() {
        val user = auth.currentUser ?: return

        binding.postsGrid.removeAllViews()

        db.collection("spots")
            .whereEqualTo("userId", user.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->

                binding.postsGrid.removeAllViews()

                if (documents.isEmpty) {
                    binding.postsTitle.text = "Publicaciones"
                    return@addOnSuccessListener
                }

                binding.postsTitle.text = "Publicaciones (${documents.size()})"

                for (document in documents) {
                    val marca = document.getString("marca") ?: "Marca"
                    val modelo = document.getString("modelo") ?: "Modelo"
                    val anio = document.getString("anio") ?: "Año"
                    val dato = document.getString("dato") ?: ""
                    val imageBase64 = document.getString("imageBase64") ?: ""

                    crearCuadradoPublicacion(
                        marca = marca,
                        modelo = modelo,
                        anio = anio,
                        dato = dato,
                        imageBase64 = imageBase64
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error cargando publicaciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun crearCuadradoPublicacion(
        marca: String,
        modelo: String,
        anio: String,
        dato: String,
        imageBase64: String
    ) {
        val context = requireContext()

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(6, 6, 6, 6)

        val cardParams = GridLayout.LayoutParams()
        cardParams.width = resources.displayMetrics.widthPixels / 3
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        card.layoutParams = cardParams

        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.displayMetrics.widthPixels / 3
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

        val title = TextView(context)
        title.text = "$marca $modelo"
        title.textSize = 12f
        title.gravity = Gravity.CENTER
        title.maxLines = 1

        val year = TextView(context)
        year.text = anio
        year.textSize = 11f
        year.gravity = Gravity.CENTER
        year.maxLines = 1

        card.addView(imageView)
        card.addView(title)
        card.addView(year)

        card.setOnClickListener {
            Toast.makeText(context, dato, Toast.LENGTH_LONG).show()
        }

        binding.postsGrid.addView(card)
    }
}