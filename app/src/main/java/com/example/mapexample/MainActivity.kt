package com.example.mapexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var btnCalculate: Button

    private var startLatLng: LatLng? = null
    private var endLatLng: LatLng? = null

    private var poly: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar el botón
        btnCalculate = findViewById(R.id.btnCalculateRoute)

        // Listener del botón
        btnCalculate.setOnClickListener {
            Log.d("MainActivity", "Botón presionado")
            Toast.makeText(this, "Botón presionado", Toast.LENGTH_SHORT).show()

            if (startLatLng != null && endLatLng != null) {
                createRoute()
            } else {
                Toast.makeText(this, "Selecciona origen y destino en el mapa primero", Toast.LENGTH_LONG).show()
            }
        }

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap

        Toast.makeText(this, "Toca 2 puntos en el mapa", Toast.LENGTH_LONG).show()

        // Listener para seleccionar puntos en el mapa
        map.setOnMapClickListener { latLng ->
            if (startLatLng == null) {
                startLatLng = latLng
                map.addMarker(MarkerOptions().position(latLng).title("Origen"))
                Toast.makeText(this, "Origen seleccionado", Toast.LENGTH_SHORT).show()
            } else if (endLatLng == null) {
                endLatLng = latLng
                map.addMarker(MarkerOptions().position(latLng).title("Destino"))
                Toast.makeText(this, "Destino seleccionado. Presiona el botón", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Ya tienes 2 puntos. Presiona el botón o reinicia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createRoute() {
        if (startLatLng == null || endLatLng == null) return

        val start = "${startLatLng!!.longitude},${startLatLng!!.latitude}"
        val end = "${endLatLng!!.longitude},${endLatLng!!.latitude}"

        Toast.makeText(this, "Calculando ruta...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = getRetrofit().create(ApiService::class.java)
                    .getRoute("eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjFhN2FiYzExNDdmODQ5ZTk5OGM0NmRlZWI2NTc4MzQ4IiwiaCI6Im11cm11cjY0In0=", start, end)

                if (call.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        drawRoute(call.body())
                        Toast.makeText(this@MainActivity, "¡Ruta calculada!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error al calcular ruta: ${call.code()}", Toast.LENGTH_LONG).show()
                    }
                    Log.e("MainActivity", "Error: ${call.code()} - ${call.message()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("MainActivity", "Exception: ${e.message}", e)
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        poly?.remove()

        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.firstOrNull()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        poly = map.addPolyline(polyLineOptions)
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}