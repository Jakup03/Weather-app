package com.example.weatherapp.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.Activity.MainActivity2
import com.example.weatherapp.Model.CityApi
import com.example.weatherapp.databinding.CityViewholderBinding

class CityAdapter : RecyclerView.Adapter<CityAdapter.ViewHolder>() {

    private lateinit var binding: CityViewholderBinding
    var onCityLongClickListener: ((CityApi.CityApiItem) -> Unit)? = null

    inner class ViewHolder : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = CityViewholderBinding.inflate(inflater, parent, false)
        return ViewHolder()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = CityViewholderBinding.bind(holder.itemView)
        val cityItem = differ.currentList[position]

        binding.cityName.text = cityItem.name
        binding.countryName.text = cityItem.country

        binding.root.setOnClickListener {
            val cityName = differ.currentList[position].name
            val countryName = differ.currentList[position].country

            val fosCity = binding.root.context.openFileOutput("city.txt", Context.MODE_PRIVATE)
            fosCity.write(cityName?.toByteArray() ?: byteArrayOf())
            fosCity.close()

            val fosCountry = binding.root.context.openFileOutput("country.txt", Context.MODE_PRIVATE)
            fosCountry.write(countryName?.toByteArray() ?: byteArrayOf())
            fosCountry.close()
            val intent = Intent(binding.root.context, MainActivity2::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            binding.root.context.startActivity(intent)
        }

        binding.root.setOnLongClickListener {
            onCityLongClickListener?.invoke(cityItem)
            true
        }
    }

    override fun getItemCount() = differ.currentList.size

    private val diffCallback = object : DiffUtil.ItemCallback<CityApi.CityApiItem>() {
        override fun areItemsTheSame(oldItem: CityApi.CityApiItem, newItem: CityApi.CityApiItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: CityApi.CityApiItem, newItem: CityApi.CityApiItem): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)
}
