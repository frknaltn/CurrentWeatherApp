package com.frkn.currentweather.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.frkn.currentweather.Model.WeatherModel
import com.frkn.currentweather.R
import com.frkn.currentweather.Util.ApiUtilities
import com.frkn.currentweather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    private lateinit var currentLocation : Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101
    private val apiKey = "03060da3cecc818e129e2230bcac08ce"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        binding.citySearch.setOnEditorActionListener { _, actionId, _ ->

            if(actionId == EditorInfo.IME_ACTION_SEARCH){

                getCityWeather(binding.citySearch.text.toString())

                val view = this.currentFocus

                if(view != null){
                    val imm:InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken,0)

                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            }else{
                return@setOnEditorActionListener false
            }

        }

        binding.currentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCityWeather(city:String){

        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCityWeatherData(city,apiKey)?.enqueue(
            object :Callback<WeatherModel>{
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if(response.isSuccessful){
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                        }
                    }else{
                        Toast.makeText(this@MainActivity,"No City Found",Toast.LENGTH_LONG)
                        binding.progressBar.visibility = View.GONE
                    }


                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }

            }
        )
    }

    private fun fetchCurrentLocationWeather(latitude:String, longitude:String){
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKey)
            ?.enqueue(object:Callback<WeatherModel>{
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if (response.isSuccessful){
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                        }
                    }

                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }

            })
    }


    private fun getCurrentLocation(){

        if (checkPermission()){

            if (isLocationEnable()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )!=PackageManager.PERMISSION_GRANTED
                ){
                    requestPermission()

                    return
                }

                fusedLocationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        if(location != null){
                            currentLocation = location
                            binding.progressBar.visibility = View.VISIBLE

                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )


                        }
                    }
            }else{
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                startActivity(intent)
            }

        }

        else {
            requestPermission()
        }
    }

    private fun requestPermission(){

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )

    }

    private fun isLocationEnable():Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermission():Boolean{
        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )==PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode==LOCATION_REQUEST_CODE){

            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()

            }

        }
    }


    private fun setData(body:WeatherModel){
        binding.apply {

            val currentDate = SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())

            dateTime.text=currentDate.toString()
            maxTemp.text ="Max " + tempC(body.main.tempMax)+"°C"
            minTemp.text= "Min " + tempC(body.main.tempMin)+"°C"
            temp.text = " " + tempC(body.main.temp)+"°"
            weatherTitle.text = body.weather[0].main
            sunriseValue.text = timeLocal(body.sys.sunrise.toLong())
            sunsetValue.text = timeLocal(body.sys.sunset.toLong())
            pressureValue.text = body.main.pressure.toString()
            humidityValue.text =body.main.humidity.toString()+"%"
            tempFValue.text = ""+tempC(body.main.temp).times(1.8).plus(32).roundToInt()
            citySearch.setText(body.name)
            feelsLike.text = "Feels Like "+tempC(body.main.feelsLike)+"°"
            windValue.text = body.wind.speed.toString()+"m/s"
            groundValue.text = body.main.grndLevel.toString()
            seaValue.text = body.main.seaLevel.toString()
            countryValue.text=body.sys.country
        }

        updateUI(body.weather[0].id)
    }

    private fun updateUI(id: Int) {

        binding.apply {

            when(id){
             in 200..232->{
                 weatherImg.setImageResource(R.drawable.ic_storm_weather)
                 mainLayout.background=ContextCompat
                     .getDrawable(this@MainActivity,R.drawable.thunderstrom_bg)

                 optionsLayout.background=ContextCompat
                     .getDrawable(this@MainActivity,R.drawable.thunderstrom_bg)
                }

                in 300..321->{
                    weatherImg.setImageResource(R.drawable.ic_few_clouds)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                }

                in 500..531->{
                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.rain_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.rain_bg)
                }

                in 600..622->{
                    weatherImg.setImageResource(R.drawable.ic_snow_weather)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)
                }

                in 701..781->{
                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)
                }

                800->{
                    weatherImg.setImageResource(R.drawable.ic_clear_day)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)
                }

                in 801..804->{
                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                }

                else ->{
                    weatherImg.setImageResource(R.drawable.ic_unknown)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.unknown_bg)

                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.unknown_bg)
                }

                }

            }

        }


    private fun timeLocal(t: Long): String {
        val localTime = t.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }

    private fun tempC(t: Double): Double {
        var intTemp = t
        intTemp = intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1,RoundingMode.UP).toDouble()
    }

    }




