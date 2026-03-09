package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Configura el WebView
        val webView = binding.webview
        webView.settings.javaScriptEnabled = true // Habilitar JavaScript
        webView.settings.domStorageEnabled = true // Habilitar almacenamiento local
        webView.settings.setSupportZoom(true) // Habilitar zoom
        webView.settings.useWideViewPort = true // Usar el viewport adecuado
        webView.settings.loadWithOverviewMode = true // Ajustar el contenido al viewport
        WebView.setWebContentsDebuggingEnabled(true)
        // Log para verificar si el WebView está siendo configurado correctamente
        Log.d("WebView", "Configuración de WebView: JavaScript habilitado y zoom habilitado")

        // Configura WebViewClient para capturar errores de carga
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                // Log para errores de carga
                Log.e("WebViewError", "Error al cargar el mapa: $description")
            }
        }


        // Verifica la URL que se está cargando
        Log.d("WebView", "Cargando URL: file:///android_asset/map.html")

        // Cargar el archivo HTML desde assets
        webView.loadUrl("file:///android_asset/map.html")

        // Log de verificación de carga
        Log.d("WebView", "Archivo map.html cargado en WebView")

        return binding.root
    }
}