package com.example.weatherapp.Activity

import android.os.Bundle
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Adapter.CityAdapter
import com.example.weatherapp.Model.CityApi
import com.example.weatherapp.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CityToBeChosenActivity : AppCompatActivity() {
    private val cityAdapter by lazy { CityAdapter() }
    private val cityAdapterForFavouritesCities by lazy { CityAdapter() }
    private lateinit var cityRecyclerView: RecyclerView
    private lateinit var favCitiesRecyclerView: RecyclerView
    private lateinit var newCityTextHolder: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_city_to_be_chosen)
        cityRecyclerView = findViewById(R.id.cityList)
        newCityTextHolder = findViewById(R.id.newCityTextHolder)
        favCitiesRecyclerView = findViewById(R.id.favouriteCityList)

        newCityTextHolder.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length!! >= 3) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val gson = Gson()
                            var weatherObject: List<CityApi.CityApiItem>? = null

                            val url =
                                URL("https://api.openweathermap.org/geo/1.0/direct?q=$s&limit=4&appid=$API_KEY")
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

                                weatherObject = gson.fromJson(response, Array<CityApi.CityApiItem>::class.java).toList()

                                weatherObject.let {
                                    val list = it.toMutableList()
                                    if (list != null) {
                                        runOnUiThread {
                                            cityAdapter.differ.submitList(list)
                                            val context = this@CityToBeChosenActivity
                                            cityRecyclerView.apply {
                                                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                                                adapter = cityAdapter
                                            }
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@CityToBeChosenActivity,
                                        "Failed to connect",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@CityToBeChosenActivity,
                                    e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        })

        val file = File(filesDir, "fav_cities.txt")
        if (file.exists()) {
            val reader = BufferedReader(file.reader())
            val cities = reader.readLines()
            val list = mutableListOf<CityApi.CityApiItem>()
            for (line in cities) {
                val city = line.split(" ")[0]
                val country = line.split(" ")[1]
                list.add(CityApi.CityApiItem(country, 0.0, null, 0.0, city, null))
            }

            runOnUiThread {
                cityAdapterForFavouritesCities.differ.submitList(list)
                val context = this@CityToBeChosenActivity
                favCitiesRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = cityAdapterForFavouritesCities
                }
            }
        }

        cityAdapterForFavouritesCities.onCityLongClickListener = { cityItem ->
            val updatedList = cityAdapterForFavouritesCities.differ.currentList.toMutableList()
            updatedList.remove(cityItem)

            runOnUiThread {
                cityAdapterForFavouritesCities.differ.submitList(updatedList)
            }

            val favFile = File(filesDir, "fav_cities.txt")
            if (favFile.exists()) {
                favFile.writeText(updatedList.joinToString("\n") { "${it.name} ${it.country}" })
            }

            Toast.makeText(this@CityToBeChosenActivity, "Usunięto: ${cityItem.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
