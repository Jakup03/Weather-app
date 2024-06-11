package com.example.weatherapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Model.ForecastWeatherApi
import com.example.weatherapp.R
import com.example.weatherapp.databinding.SingleDayWeatherBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.roundToInt

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.ViewHolder>(){

    private lateinit var binding: SingleDayWeatherBinding

    inner class ViewHolder : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = SingleDayWeatherBinding.inflate(inflater, parent, false)
        return ViewHolder()
    }

    override fun onBindViewHolder(holder: ForecastAdapter.ViewHolder, position: Int) {
        val binding = SingleDayWeatherBinding.bind(holder.itemView)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(differ.currentList[position].dtTxt.toString())
        val calendar = Calendar.getInstance()
        calendar.time = date

        val dayOfWeek = when(calendar.get(Calendar.DAY_OF_WEEK)){
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            7 -> "Saturday"
            else -> ""
        }

        binding.dayForecast.text = dayOfWeek
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val time = if(hour > 12) "${hour - 12} PM" else "$hour AM"
        binding.hourForecast.text = time
        binding.temperatureForecast.text = differ.currentList[position].main?.temp?.roundToInt()?.minus(273).toString() + "°C"

        when(differ.currentList[position].weather?.get(0)?.icon) {
            "01d", "01n" -> binding.imageView.setImageResource(R.drawable.sunny)
            "02d" -> binding.imageView.setImageResource(R.drawable.cloudy_sunny)
            "02n" -> binding.imageView.setImageResource(R.drawable.cloudy_sunny)
            "03d", "03n", "04d", "04n" -> binding.imageView.setImageResource(R.drawable.cloudy)
            "09d", "09n", "10d", "10n" -> binding.imageView.setImageResource(R.drawable.rainy)
            "11d", "11n" -> binding.imageView.setImageResource(R.drawable.storm)
            "13d", "13n" -> binding.imageView.setImageResource(R.drawable.snowy)
            "50d", "50n" -> binding.imageView.setImageResource(R.drawable.windy)
            else -> binding.imageView.setImageResource(R.drawable.sunny)
        }
    }

    override fun getItemCount() = differ.currentList.size

    private val diffCallback = object : DiffUtil.ItemCallback<ForecastWeatherApi.data>() {
        override fun areItemsTheSame(oldItem: ForecastWeatherApi.data, newItem: ForecastWeatherApi.data): Boolean {
            return oldItem.dt == newItem.dt
        }

        override fun areContentsTheSame(oldItem: ForecastWeatherApi.data, newItem: ForecastWeatherApi.data): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)


}