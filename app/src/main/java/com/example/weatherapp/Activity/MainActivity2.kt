package com.example.weatherapp.Activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Adapter.ForecastAdapter
import com.example.weatherapp.Model.CityApi
import com.example.weatherapp.Model.CurrentWeatherApiClass
import com.example.weatherapp.Model.ForecastWeatherApi
import com.example.weatherapp.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import android.os.Handler
import android.os.Looper

public const val API_KEY = "fe02a6b6389e2ba9aff21103d2dbe6fd"

class MainActivity2 : AppCompatActivity() {

    private lateinit var cityTextView: TextView

    private lateinit var temperatureTextView: TextView
    private lateinit var temperatureMinTextView: TextView
    private lateinit var temperatureMaxTextView: TextView

    private lateinit var humidityTextView: TextView
    private lateinit var pressureTextView: TextView
    private lateinit var windTextView: TextView
    private lateinit var weatherPic: ImageView
    private lateinit var cityIcon: ImageView
    private lateinit var fetchWeatherIcon: ImageView
    private lateinit var favouriteCityIcon: ImageView

    private lateinit var forecastView: RecyclerView

    private val forecastAdapter by lazy { ForecastAdapter() }

    private val handler = Handler(Looper.getMainLooper())
    private val refreshWeatherRunnable = object : Runnable {
        override fun run() {
            getWeather()
            makeText(this@MainActivity2, "Weather updated", Toast.LENGTH_SHORT).show()
            handler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        cityTextView = findViewById(R.id.city)

        temperatureTextView = findViewById(R.id.temperature)
        temperatureMinTextView = findViewById(R.id.temperatureMin)
        temperatureMaxTextView = findViewById(R.id.temperatureMax)

        humidityTextView = findViewById(R.id.humidity)
        pressureTextView = findViewById(R.id.pressureTextView)
        windTextView = findViewById(R.id.wind)
        weatherPic = findViewById(R.id.weatherPic)

        forecastView = findViewById(R.id.forecastList)
        cityIcon = findViewById(R.id.addCityIcon)
        fetchWeatherIcon = findViewById(R.id.fetchWeatherImage)
        favouriteCityIcon = findViewById(R.id.addToFavIcon)

        cityIcon.setOnClickListener{
            val intent = Intent(this, CityToBeChosenActivity::class.java)
            startActivity(intent)
        }

        favouriteCityIcon.setOnClickListener {
            val file = File(filesDir, "fav_cities.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            val cityName = cityTextView.text.toString()
            if (cityName != "No city selected") {
                val isCityFavourite = file.readText().contains(cityName)

                if (isCityFavourite) {
                    val bufferedReader = file.bufferedReader()
                    val country = bufferedReader.use { it.readText() }
                    val listOfNewFavoriteCities = mutableListOf<String>()
                    country.lines().forEach { line ->
                        if (!line.contains(cityName) && line.isNotBlank()) {
                            listOfNewFavoriteCities.add(line + "\n")
                        }
                    }
                    file.delete()
                    file.createNewFile()
                    val fos = openFileOutput("fav_cities.txt", Context.MODE_APPEND)
                    listOfNewFavoriteCities.forEach {
                        fos.write(it.toByteArray())
                    }

                    favouriteCityIcon.setImageResource(android.R.drawable.btn_star_big_off)
                    makeText(
                        this@MainActivity2,
                        "City removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val fos = openFileOutput("fav_cities.txt", Context.MODE_APPEND)

                    val file2 = File(filesDir, "country.txt")
                    val bufferedReader = file2.bufferedReader()
                    val country = bufferedReader.use { it.readText() }

                    fos.write(cityName.toByteArray())
                    fos.write(" ".toByteArray())
                    fos.write(country.toByteArray())
                    fos.write("\n".toByteArray())
                    fos.close()
                    favouriteCityIcon.setImageResource(android.R.drawable.btn_star_big_on)
                    makeText(
                        this@MainActivity2,
                        "City added to favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                makeText(
                    this@MainActivity2,
                    "No city selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }



        fetchWeatherIcon.setOnClickListener {
            getWeather()
            makeText(
                this@MainActivity2,
                "Weather updated",
                Toast.LENGTH_SHORT
            ).show()
        }

        getWeather()
    }

    override fun onStart() {
        super.onStart()
        handler.post(refreshWeatherRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(refreshWeatherRunnable)
    }

    private fun handleFavouriteCityIcon() {
        val file = File(filesDir, "fav_cities.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        val cityName = cityTextView.text.toString()
        if (cityName != "No city selected") {
            val isCityFavourite = file.readText().contains(cityName)

            if (isCityFavourite) {
                favouriteCityIcon.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                favouriteCityIcon.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }
    }

    private fun getWeather() {
        val file = File(filesDir, "city.txt")
        if(file.exists()) {
            val bufferedReader = file.bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            fetchWeatherData(inputString)
            cityTextView.setText(inputString)
            fetchWeatherForecast(inputString)
            handleFavouriteCityIcon()
        } else {
            cityTextView.setText("No city selected")
        }
    }

    private fun fetchWeatherForecast(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val file = File(filesDir, "$city.json")
                val gson = Gson()
                var weatherObject: ForecastWeatherApi? = null

                if (isNetworkAvailable(this@MainActivity2)) {
                    val url =
                        URL("https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$API_KEY")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val stream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(stream))
                        val buffer = StringBuffer()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            buffer.append(line + "\n")
                        }
                        val response = buffer.toString()

                        weatherObject = gson.fromJson(response, ForecastWeatherApi::class.java)

                        val weatherJson = gson.toJson(weatherObject)
                        val fos = openFileOutput("$city.json", Context.MODE_PRIVATE)
                        fos.write(weatherJson.toByteArray())
                        fos.close()


                    } else {
                        withContext(Dispatchers.Main) {
                            makeText(
                                this@MainActivity2,
                                "Failed to connect",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        makeText(
                            this@MainActivity2,
                            "No internet connection. Fetching data from cache.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if(file.exists()) {
                        val bufferedReader = file.bufferedReader()
                        val inputString = bufferedReader.use { it.readText() }

                        weatherObject = gson.fromJson(inputString, ForecastWeatherApi::class.java)
                    } else {
                        withContext(Dispatchers.Main) {
                            makeText(
                                this@MainActivity2,
                                "File from cache does not exist. Please connect to the internet to fetch data.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                weatherObject?.let {
                    val list = it.list?.toMutableList()
                    if (list != null) {
                        val filteredList = list.filterIndexed { index, _ -> (index + 1) % 3 == 0 }

                        runOnUiThread {
                            forecastAdapter.differ.submitList(filteredList)
                            val context = this@MainActivity2
                            forecastView.apply {
                                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                                adapter = forecastAdapter
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    makeText(
                        this@MainActivity2,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    private fun fetchWeatherData(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val file = File(filesDir, "forecast_$city.json")

                val gson = Gson()
                var weatherObject: CurrentWeatherApiClass? = null

                if (isNetworkAvailable(this@MainActivity2)) {

                    val url =
                        URL("https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$API_KEY")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val stream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(stream))
                        val buffer = StringBuffer()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            buffer.append(line + "\n")
                        }
                        val response = buffer.toString()

                        weatherObject = gson.fromJson(response, CurrentWeatherApiClass::class.java)

                        val weatherJson = gson.toJson(weatherObject)
                        val fos = openFileOutput("forecast_$city.json", Context.MODE_PRIVATE)
                        fos.write(weatherJson.toByteArray())
                        fos.close()
                    } else {
                        throw Exception("Failed to connect")
                    }
                } else {
                    if(file.exists()) {
                        val bufferedReader = file.bufferedReader()
                        val inputString = bufferedReader.use { it.readText() }

                        weatherObject = gson.fromJson(inputString, CurrentWeatherApiClass::class.java)
                    } else {
                        // nothing
                    }
                }

                val main = weatherObject?.main
                val tempC = main?.temp?.minus(273)?.roundToInt()
                val tempMin = main?.tempMin?.minus(273)?.roundToInt()
                val tempMax = main?.tempMax?.minus(273)?.roundToInt()
                val humidity = main?.humidity
                val pressure = main?.pressure
                val wind = weatherObject?.wind?.speed

                launch(Dispatchers.Main) {
                    temperatureTextView.text = "${tempC}°C"
                    temperatureMinTextView.text = "${tempMin}°C"
                    temperatureMaxTextView.text = "${tempMax}°C"
                    humidityTextView.text = "$humidity%"
                    pressureTextView.text = "$pressure hPa"
                    windTextView.text = "$wind km/h"

                    when(weatherObject?.weather?.get(0)?.main) {
                        "01d", "01n" -> weatherPic.setImageResource(R.drawable.sunny)
                        "02d" -> weatherPic.setImageResource(R.drawable.cloudy_sunny)
                        "02n" -> weatherPic.setImageResource(R.drawable.cloudy_sunny)
                        "03d", "03n", "04d", "04n" -> weatherPic.setImageResource(R.drawable.cloudy)
                        "09d", "09n", "10d", "10n" -> weatherPic.setImageResource(R.drawable.rainy)
                        "11d", "11n" -> weatherPic.setImageResource(R.drawable.storm)
                        "13d", "13n" -> weatherPic.setImageResource(R.drawable.snowy)
                        "50d", "50n" -> weatherPic.setImageResource(R.drawable.windy)
                        else -> weatherPic.setImageResource(R.drawable.sunny)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    makeText(
                        this@MainActivity2,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }
}