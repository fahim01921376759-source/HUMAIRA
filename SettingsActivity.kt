package com.humaira.app

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.humaira.app.databinding.ActivitySettingsBinding
import kotlin.math.roundToInt

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var storage: Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        storage = Storage(this)

        b.apiKey.setText(storage.apiKey)
        when (storage.model) {
            Storage.MODEL_SMART -> b.radioSmart.isChecked = true
            Storage.MODEL_BEST -> b.radioBest.isChecked = true
            else -> b.radioFast.isChecked = true
        }
        b.switchSpeak.isChecked = storage.speakReplies
        b.seekSpeed.max = 10
        b.seekSpeed.progress = ((storage.speechRate - 0.5f) * 10f).roundToInt().coerceIn(0, 10)
        updateSpeedLabel()
        b.instructions.setText(storage.instructions)

        b.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedLabel()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        b.btnBack.setOnClickListener { finish() }
        b.btnSave.setOnClickListener {
            save()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
        b.btnResetInstructions.setOnClickListener {
            b.instructions.setText(Storage.DEFAULT_INSTRUCTIONS)
        }
        b.btnDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete all conversations?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    storage.deleteAll()
                    storage.currentId = ""
                    Toast.makeText(this, "All conversations deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        save() // changes are kept even if you just press Back
    }

    private fun updateSpeedLabel() {
        val rate = 0.5f + b.seekSpeed.progress / 10f
        b.speedLabel.text = "Speaking speed: ${"%.1f".format(rate)}x"
    }

    private fun save() {
        storage.apiKey = b.apiKey.text.toString()
        storage.model = when (b.modelGroup.checkedRadioButtonId) {
            R.id.radioSmart -> Storage.MODEL_SMART
            R.id.radioBest -> Storage.MODEL_BEST
            else -> Storage.MODEL_FAST
        }
        storage.speakReplies = b.switchSpeak.isChecked
        storage.speechRate = 0.5f + b.seekSpeed.progress / 10f
        storage.instructions =
            b.instructions.text.toString().ifBlank { Storage.DEFAULT_INSTRUCTIONS }
    }
}
