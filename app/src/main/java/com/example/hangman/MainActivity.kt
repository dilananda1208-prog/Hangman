package com.example.hangman

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Data Permainan
    private lateinit var kataRahasia: String
    private lateinit var hintKata: String
    private val hurufBenar = mutableSetOf<Char>()
    private val hurufSalah = mutableSetOf<Char>()

    // Variabel Tambahan Soal Latihan
    private var maksNyawa = 6          // Poin 5
    private var skor = 0               // Poin 4
    private val tombolHurufMap = mutableMapOf<Char, Button>() // Poin 1

    // Referensi View
    private lateinit var tvNyawa: TextView
    private lateinit var tvSkor: TextView
    private lateinit var tvKata: TextView
    private lateinit var tvRiwayat: TextView
    private lateinit var tvHintGame: TextView
    private lateinit var gridKeyboard: GridLayout
    private lateinit var overlayHasil: LinearLayout
    private lateinit var overlayMulai: LinearLayout
    private lateinit var overlayKesulitan: LinearLayout
    private lateinit var tvHasil: TextView
    private lateinit var tvKataAkhir: TextView

    private lateinit var bagianTubuh: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi View
        tvNyawa = findViewById(R.id.tvNyawa)
        tvSkor = findViewById(R.id.tvSkor)
        tvKata = findViewById(R.id.tvKata)
        tvRiwayat = findViewById(R.id.tvRiwayat)
        tvHintGame = findViewById(R.id.tvHintGame)
        gridKeyboard = findViewById(R.id.gridKeyboard)
        overlayHasil = findViewById(R.id.overlayHasil)
        overlayMulai = findViewById(R.id.overlayMulai)
        overlayKesulitan = findViewById(R.id.overlayKesulitan)
        tvHasil = findViewById(R.id.tvHasil)
        tvKataAkhir = findViewById(R.id.tvKataAkhir)

        bagianTubuh = listOf(
            findViewById(R.id.imgKepala),
            findViewById(R.id.imgBadan),
            findViewById(R.id.imgTanganKiri),
            findViewById(R.id.imgTanganKanan),
            findViewById(R.id.imgKakiKiri),
            findViewById(R.id.imgKakiKanan)
        )

        buatKeyboard() // Generasi Tombol A-Z (Poin 1)

        // Handlers untuk Tombol Mulai & Kesulitan (Poin 5)
        findViewById<Button>(R.id.btnMulai).setOnClickListener {
            overlayMulai.visibility = View.GONE
            overlayKesulitan.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnMudah).setOnClickListener {
            maksNyawa = 8
            overlayKesulitan.visibility = View.GONE
            mulaiPermainanBaru()
        }

        findViewById<Button>(R.id.btnSedang).setOnClickListener {
            maksNyawa = 6
            overlayKesulitan.visibility = View.GONE
            mulaiPermainanBaru()
        }

        findViewById<Button>(R.id.btnSulit).setOnClickListener {
            maksNyawa = 4
            overlayKesulitan.visibility = View.GONE
            mulaiPermainanBaru()
        }

        findViewById<Button>(R.id.btnMainLagi).setOnClickListener {
            overlayKesulitan.visibility = View.VISIBLE
        }

        // Poin 6: Restore State saat Rotasi Layar
        if (savedInstanceState != null) {
            kataRahasia = savedInstanceState.getString("KATA_RAHASIA", "")
            hintKata = savedInstanceState.getString("HINT_KATA", "")
            skor = savedInstanceState.getInt("SKOR", 0)
            maksNyawa = savedInstanceState.getInt("MAKS_NYAWA", 6)

            val benarArr = savedInstanceState.getCharArray("HURUF_BENAR") ?: charArrayOf()
            val salahArr = savedInstanceState.getCharArray("HURUF_SALAH") ?: charArrayOf()

            hurufBenar.addAll(benarArr.toList())
            hurufSalah.addAll(salahArr.toList())

            overlayMulai.visibility = View.GONE
            overlayKesulitan.visibility = View.GONE

            gambarPapan()

            // Re-disable tombol huruf yang sudah pernah diklik
            (hurufBenar + hurufSalah).forEach { char ->
                tombolHurufMap[char]?.isEnabled = false
            }
        } else {
            overlayMulai.visibility = View.VISIBLE
        }
    }

    // Poin 1: Membuat Tombol A-Z secara Dinamis
    private fun buatKeyboard() {
        gridKeyboard.removeAllViews()
        tombolHurufMap.clear()

        for (ch in 'A'..'Z') {
            val btn = Button(this).apply {
                text = ch.toString()
                textSize = 12f
                        setPadding(0, 0, 0, 0)

                // Ukuran Tombol Kotak
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = params

                setOnClickListener {
                    isEnabled = false // Disable tombol saat ditebak (Poin 1)
                    prosesTebakan(ch)
                }
            }
            tombolHurufMap[ch] = btn
            gridKeyboard.addView(btn)
        }
    }

    private fun mulaiPermainanBaru() {
        val daftarKata = resources.getStringArray(R.array.daftar_kata)
        val daftarHint = resources.getStringArray(R.array.daftar_hint)
        val idx = daftarKata.indices.random()

        kataRahasia = daftarKata[idx]
        hintKata = daftarHint[idx] // Poin 3

        hurufBenar.clear()
        hurufSalah.clear()

        // Reset Status Tombol A-Z
        tombolHurufMap.values.forEach { it.isEnabled = true }

        overlayHasil.visibility = View.GONE
        gambarPapan()
    }

    // Poin 2: Animasi Gambar Gantungan Baru dengan Alpha
    private fun perbaruiGantungan(kesalahan: Int) {
        bagianTubuh.forEachIndexed { urutan, gambar ->
            if (urutan < kesalahan) {
                if (gambar.visibility != View.VISIBLE) {
                    gambar.alpha = 0f
                    gambar.visibility = View.VISIBLE
                    gambar.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }
            } else {
                gambar.visibility = View.INVISIBLE
            }
        }
    }

    private fun gambarPapan() {
        perbaruiGantungan(hurufSalah.size)

        tvKata.text = kataRahasia
            .map { if (hurufBenar.contains(it)) it else '_' }
            .joinToString(" ")

        tvHintGame.text = "Hint: $hintKata" // Display Hint Poin 3

        val sisaNyawa = (maksNyawa - hurufSalah.size).coerceAtLeast(0)
        tvNyawa.text = "Nyawa: $sisaNyawa"
        tvSkor.text = "Skor: $skor" // Poin 4

        tvRiwayat.text = if (hurufSalah.isEmpty()) {
            "Salah: -"
        } else {
            "Salah: ${hurufSalah.joinToString(" ")}"
        }
    }

    private fun prosesTebakan(huruf: Char) {
        if (kataRahasia.contains(huruf)) {
            hurufBenar.add(huruf)
        } else {
            hurufSalah.add(huruf)
        }

        gambarPapan()
        periksaAkhirPermainan()
    }

    private fun periksaAkhirPermainan() {
        val semuaTerbuka = kataRahasia.all { hurufBenar.contains(it) }

        when {
            semuaTerbuka -> {
                skor += 10 // Tambah +10 skor jika menang (Poin 4)
                tampilkanHasil(menang = true)
            }
            hurufSalah.size >= maksNyawa -> {
                // Kalah +0 skor (Poin 4)
                tampilkanHasil(menang = false)
            }
        }
    }

    private fun tampilkanHasil(menang: Boolean) {
        tvHasil.text = if (menang) getString(R.string.menang) else getString(R.string.kalah)
        tvHasil.setTextColor(if (menang) 0xFF3DDC84.toInt() else 0xFFF87171.toInt())
        tvKataAkhir.text = "Kata: $kataRahasia"

        tombolHurufMap.values.forEach { it.isEnabled = false }

        overlayHasil.alpha = 0f
        overlayHasil.visibility = View.VISIBLE
        overlayHasil.animate()
            .alpha(1f)
            .setDuration(350)
            .start()

        overlayHasil.requestFocus()
    }

    // Poin 6: Menyimpan State Permainan Saat Layar Diputar
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("KATA_RAHASIA", kataRahasia)
        outState.putString("HINT_KATA", hintKata)
        outState.putInt("SKOR", skor)
        outState.putInt("MAKS_NYAWA", maksNyawa)
        outState.putCharArray("HURUF_BENAR", hurufBenar.toCharArray())
        outState.putCharArray("HURUF_SALAH", hurufSalah.toCharArray())
    }
}