package com.example.ejemplo_reproductor

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    val cancionesImagenes = arrayOf(
        R.mipmap.beach,
        R.mipmap.francis,
        R.mipmap.hole,
        R.mipmap.island,
        R.mipmap.maddona,
        R.mipmap.mistki,
        R.mipmap.monsta,
        R.mipmap.oasis,
        R.mipmap.playdate,
        R.mipmap.west
    )

    val cancionesRecursos = arrayOf(
        R.raw.beach,
        R.raw.francis,
        R.raw.hole,
        R.raw.island,
        R.raw.maddona,
        R.raw.mitski,
        R.raw.monsta,
        R.raw.oasis,
        R.raw.playdate,
        R.raw.west
    )

    val cancionesNombres = arrayOf(
        "Beach Weather - Sex, Drugs, Etc.",
        "Mitski - Francis Forever",
        "Supermassive black hole - Muse",
        "Weezer - Island In The Sun",
        "Cage The Elephant – Black Madonna",
        "Mitski – My Love Mine All Mine",
        "MONSTA X – MIDDLE OF THE NIGHT",
        "Oasis – Stop Crying Your Heart Out ",
        "Play Date  - Melanie Martinez ",
        "Lana Del Rey – West Coast "
    )

    val coloresFondo = arrayOf(
        Color.parseColor("#FEF9E7"),
        Color.parseColor("#E5E7E9"),
        Color.parseColor("#E59866"),
        Color.parseColor("#2ECC71"),
        Color.parseColor("#85C1E9"),
        Color.parseColor("#FADBD8"),
        Color.parseColor("#F1948A"),
        Color.parseColor("#99A3A4"),
        Color.parseColor("#3498DB"),
        Color.parseColor("#808B96")
    )

    lateinit var player: MediaPlayer
    lateinit var btnPlay: ImageView
    lateinit var btnStop: ImageView
    lateinit var btnPausa: ImageView
    lateinit var siguiente: ImageView
    lateinit var atras: ImageView
    lateinit var img: ImageView
    lateinit var txt: TextView
    lateinit var btn: Button
    lateinit var switch: Switch
    private lateinit var seekBar: SeekBar

    lateinit var repo1: TextView
    lateinit var repo2: TextView

    private var job: Job? = null
    private var userIsSeeking = false
    private var currentSongIndex = 0

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isShakeEnabled = false
    private var lastShakeTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        switch = findViewById(R.id.switch1)
        btnPlay = findViewById(R.id.Play)
        btnPausa = findViewById(R.id.Pause)
        btnStop = findViewById(R.id.Stop)
        siguiente = findViewById(R.id.Siguiente)
        atras = findViewById(R.id.Atras)
        seekBar = findViewById(R.id.seek1)
        img = findViewById(R.id.imagenes)
        txt = findViewById(R.id.titulo)
        btn = findViewById(R.id.btnSeleccionarCancion)

        repo1 = findViewById(R.id.tiempo1)
        repo2 = findViewById(R.id.tiempo2)

        player = MediaPlayer.create(this@MainActivity, cancionesRecursos[0])
        setupSongInfo()

        repo2.text = formatTime(player.duration)
        seekBar.max = player.duration

        btnPlay.setOnClickListener {
            player.seekTo(avance)
            player.start()
            startUpdatingTime()
        }

        btnPausa.setOnClickListener {
            avance = player.currentPosition
            player.pause()
            stopUpdatingTime()
        }

        btnStop.setOnClickListener {
            player.pause()
            avance = 0
            player.seekTo(avance)
            repo1.text = formatTime(avance)
            seekBar.progress = avance
            stopUpdatingTime()
        }

        siguiente.setOnClickListener {
            cambiarCancion(1)
        }

        atras.setOnClickListener {
            cambiarCancion(-1)
        }

        btn.setOnClickListener {
            mostrarDialogoSeleccionCancion()
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            isShakeEnabled = isChecked
            if (isChecked) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                sensorManager.unregisterListener(this)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    repo1.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userIsSeeking = false
                player.seekTo(seekBar?.progress ?: 0)
                repo1.text = formatTime(player.currentPosition)
            }
        })
    }

    private fun startUpdatingTime() {
        job = CoroutineScope(Dispatchers.Main).launch {
            while (player.isPlaying) {
                if (!userIsSeeking) {
                    repo1.text = formatTime(player.currentPosition)
                    seekBar.progress = player.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun stopUpdatingTime() {
        job?.cancel()
    }

    private fun formatTime(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun setupSongInfo() {
        repo2.text = formatTime(player.duration)
        seekBar.max = player.duration
        txt.text = cancionesNombres[currentSongIndex]
        img.setImageResource(cancionesImagenes[currentSongIndex])
        findViewById<View>(R.id.main).setBackgroundColor(coloresFondo[currentSongIndex])
    }

    private fun cambiarCancion(direccion: Int) {
        player.stop()
        player.reset()
        stopUpdatingTime()

        currentSongIndex = (currentSongIndex + direccion + cancionesRecursos.size) % cancionesRecursos.size

        player = MediaPlayer.create(this@MainActivity, cancionesRecursos[currentSongIndex])
        setupSongInfo()
        player.start()
        startUpdatingTime()
    }

    private fun mostrarDialogoSeleccionCancion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar Canción")
        builder.setItems(cancionesNombres) { dialog, which ->
            cambiarCancionPorIndice(which)
        }
        builder.create().show()
    }

    private fun cambiarCancionPorIndice(indice: Int) {
        player.stop()
        player.reset()
        stopUpdatingTime()

        currentSongIndex = indice

        player = MediaPlayer.create(this@MainActivity, cancionesRecursos[currentSongIndex])
        setupSongInfo()
        player.start()
        startUpdatingTime()
    }

    companion object {
        var avance = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && isShakeEnabled) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            if (acceleration > 12) { // Adjust this threshold as needed
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 2000) { // 2 seconds cooldown
                    lastShakeTime = currentTime
                    cambiarCancion(1)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onResume() {
        super.onResume()
        if (isShakeEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
