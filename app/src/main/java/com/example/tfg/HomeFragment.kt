package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.tfg.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        val webView = binding.webview

        // Necesario habilitar JavaScript porque el html que vamos a cagar contiene también htrml
        // y si no pues no funciona

        webView.settings.javaScriptEnabled = true

        // Habilitado para guardar datos localmente en el navegador y funcionar correctamente
        webView.settings.domStorageEnabled = true

        // Le decimos al webView que cargue los enlaces del webView
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/map.html")

        return binding.root
    }
}