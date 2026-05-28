package com.example.tfg

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentProfileBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
            binding.postsDivider.visibility = View.VISIBLE
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
            binding.postsDivider.visibility = View.GONE
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
        binding.postsTitle.text = "Spots"

        db.collection("spots")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                binding.postsGrid.removeAllViews()

                for (document in documents) {
                    crearCuadradoPublicacion(document)
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

    private fun crearCuadradoPublicacion(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca"
        val modelo = document.getString("modelo") ?: "Modelo"
        val anio = document.getString("anio") ?: "Año"

        val imagenesBase64 = obtenerImagenes(document)
        val imageBase64 = imagenesBase64.firstOrNull().orEmpty()

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.setPadding(10, 0, 10, 34)

        val screenWidth = resources.displayMetrics.widthPixels
        val size = (screenWidth - 74) / 2

        val cardParams = GridLayout.LayoutParams()
        cardParams.width = size
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        cardParams.setMargins(0, 0, 0, 0)
        card.layoutParams = cardParams

        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            size
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        ponerImagen(imageView, imageBase64)

        val titulo = TextView(context)
        titulo.text = "$marca $modelo"
        titulo.textSize = 16f
        titulo.gravity = Gravity.CENTER
        titulo.maxLines = 1
        titulo.setPadding(0, 10, 0, 0)

        val year = TextView(context)
        year.text = anio
        year.textSize = 16f
        year.gravity = Gravity.CENTER
        year.maxLines = 1

        card.addView(imageView)
        card.addView(titulo)
        card.addView(year)

        card.setOnClickListener {
            mostrarDialogSpot(document)
        }

        binding.postsGrid.addView(card)
    }

    private fun mostrarDialogSpot(document: DocumentSnapshot) {
        val context = requireContext()

        val marca = document.getString("marca") ?: "Marca no detectada"
        val modelo = document.getString("modelo") ?: "Modelo no detectado"
        val anio = document.getString("anio") ?: "Año no detectado"
        val dato = document.getString("dato") ?: "Sin descripción"
        val fecha = formatearFecha(document.getTimestamp("createdAt"))

        val imagenesBase64 = obtenerImagenes(document)
        var imagenActual = 0

        val contenedor = LinearLayout(context)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(36, 20, 36, 10)

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

        val fechaSpot = TextView(context)
        fechaSpot.text = "Fecha: $fecha"
        fechaSpot.textSize = 14f
        fechaSpot.gravity = Gravity.CENTER
        fechaSpot.setPadding(0, 0, 0, 18)

        val descripcion = TextView(context)
        descripcion.text = if (dato.isNotBlank()) dato else "Sin descripción"
        descripcion.textSize = 15f
        descripcion.gravity = Gravity.CENTER

        contenedor.addView(zonaFoto)
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