package com.example.tfg

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.example.tfg.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth  // Variable para manejar la autenticación de Firebase
    private lateinit var binding: FragmentProfileBinding  // Variable para el ViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout utilizando ViewBinding
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa la instancia de FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario está autenticado
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Si hay un usuario autenticado, mostrar el perfil y el botón de logout
            binding.profileTextView.text = "Tu perfil: ${currentUser.email}"  // Mostrar el correo del usuario autenticado
            binding.logoutButton.visibility = View.VISIBLE
            binding.loginButton.visibility = View.GONE
            binding.registerButton.visibility = View.GONE
            binding.emailEditText.visibility = View.GONE  // Ocultar el campo de correo
            binding.passwordEditText.visibility = View.GONE  // Ocultar el campo de contraseña
        } else {
            // Si no hay usuario autenticado, mostrar los botones de login y registro
            binding.profileTextView.text = "No has iniciado sesión"
            binding.logoutButton.visibility = View.GONE
            binding.loginButton.visibility = View.VISIBLE
            binding.registerButton.visibility = View.VISIBLE
            binding.emailEditText.visibility = View.VISIBLE  // Mostrar el campo de correo
            binding.passwordEditText.visibility = View.VISIBLE  // Mostrar el campo de contraseña
        }

        // Configura el botón de "registro"
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()  // Obtiene el email ingresado
            val password = binding.passwordEditText.text.toString()  // Obtiene la contraseña ingresada

            // Validación de campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Por favor, ingresa ambos campos.", Toast.LENGTH_SHORT).show()
            } else {
                // Llama a Firebase para crear un nuevo usuario con email y contraseña
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->  // Escucha si la tarea fue exitosa o no
                        if (task.isSuccessful) {  // Si la tarea fue exitosa (registro exitoso)
                            val user = auth.currentUser  // Obtiene el usuario actual autenticado
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de éxito
                        } else {  // Si hubo un error en el registro
                            Toast.makeText(context, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de error
                        }
                    }
            }
        }

        // Configura el botón de "login" (iniciar sesión)
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()  // Obtiene el email ingresado
            val password = binding.passwordEditText.text.toString()  // Obtiene la contraseña ingresada

            // Validación de campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Por favor, ingresa ambos campos.", Toast.LENGTH_SHORT).show()
            } else {
                // Llama a Firebase para autenticar al usuario con el email y la contraseña
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->  // Escucha si la tarea fue exitosa o no
                        if (task.isSuccessful) {  // Si la tarea fue exitosa (login exitoso)
                            val user = auth.currentUser  // Obtiene el usuario actual autenticado
                            Toast.makeText(context, "Bienvenido ${user?.email}", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de bienvenida

                            // Redirigir a la pantalla principal
                            val intent = Intent(context, MainActivity::class.java) // Aquí rediriges a tu actividad principal
                            startActivity(intent)
                        } else {  // Si hubo un error en el login
                            Toast.makeText(context, "Error en el login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de error
                        }
                    }
            }
        }

        // Configura el botón de "cerrar sesión"
        binding.logoutButton.setOnClickListener {
            auth.signOut()  // Cierra la sesión del usuario actual
            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de éxito
            // Oculta el botón de logout y muestra los de login/registro nuevamente
            binding.profileTextView.text = "No has iniciado sesión"
            binding.logoutButton.visibility = View.GONE
            binding.loginButton.visibility = View.VISIBLE
            binding.registerButton.visibility = View.VISIBLE
            binding.emailEditText.visibility = View.VISIBLE  // Mostrar los campos de email y password
            binding.passwordEditText.visibility = View.VISIBLE  // Mostrar los campos de email y password
        }
    }
}