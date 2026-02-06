package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.example.tfg.databinding.FragmentProfileBinding // Importa el ViewBinding generado

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

        // Configura el botón de "registro"
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()  // Obtiene el email ingresado
            val password = binding.passwordEditText.text.toString()  // Obtiene la contraseña ingresada

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

        // Configura el botón de "login" (iniciar sesión)
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()  // Obtiene el email ingresado
            val password = binding.passwordEditText.text.toString()  // Obtiene la contraseña ingresada

            // Llama a Firebase para autenticar al usuario con el email y la contraseña
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->  // Escucha si la tarea fue exitosa o no
                    if (task.isSuccessful) {  // Si la tarea fue exitosa (login exitoso)
                        val user = auth.currentUser  // Obtiene el usuario actual autenticado
                        Toast.makeText(context, "Bienvenido ${user?.email}", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de bienvenida
                    } else {  // Si hubo un error en el login
                        Toast.makeText(context, "Error en el login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()  // Muestra un mensaje de error
                    }
                }
        }
    }
}