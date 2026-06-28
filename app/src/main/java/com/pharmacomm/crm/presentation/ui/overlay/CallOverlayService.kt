package com.pharmacomm.crm.presentation.ui.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.pharmacomm.crm.R
import com.pharmacomm.crm.domain.model.*
import com.pharmacomm.crm.utils.AppContainer
import kotlinx.coroutines.*

class CallOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phone") ?: return START_NOT_STICKY
        val callerName = intent.getStringExtra("name") ?: "رقم غير معروف"

        showOverlay(phoneNumber, callerName)
        return START_NOT_STICKY
    }

    private fun showOverlay(phone: String, callerName: String) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_call_dialog, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 120

        windowManager?.addView(overlayView, params)

        // Bind views
        val tvTitle = overlayView?.findViewById<TextView>(R.id.tvTitle)
        val tvPhone = overlayView?.findViewById<TextView>(R.id.tvPhone)
        val etName = overlayView?.findViewById<EditText>(R.id.etName)
        val etSpecialty = overlayView?.findViewById<EditText>(R.id.etSpecialty)
        val etRegion = overlayView?.findViewById<EditText>(R.id.etRegion)
        val spinnerClass = overlayView?.findViewById<Spinner>(R.id.spinnerClass)
        val btnSave = overlayView?.findViewById<Button>(R.id.btnSave)
        val btnDismiss = overlayView?.findViewById<Button>(R.id.btnDismiss)

        tvTitle?.text = "مكالمة فائتة جديدة"
        tvPhone?.text = phone
        etName?.setText(callerName)

        // Class Spinner
        val classes = arrayOf("Class A", "Class B", "Class C")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classes)
        spinnerClass?.adapter = adapter
        spinnerClass?.setSelection(1)

        btnSave?.setOnClickListener {
            val clientName = etName?.text?.toString()?.ifBlank { callerName } ?: callerName
            val spec = etSpecialty?.text?.toString()
            val reg = etRegion?.text?.toString()
            val classStr = spinnerClass?.selectedItem?.toString() ?: "Class B"

            serviceScope.launch {
                // Create new client
                val newClient = Client(
                    name = clientName,
                    clientType = ClientType.DOCTOR,
                    importanceClass = ImportanceClass.valueOf(classStr.uppercase().replace(" ", "_")),
                    specialty = spec?.ifBlank { null },
                    region = reg?.ifBlank { null }
                )
                val clientId = AppContainer.clientRepository.insertClient(newClient)

                // Add phone
                AppContainer.clientRepository.insertPhoneNumber(
                    PhoneNumber(clientId = clientId, number = phone, isPrimary = true)
                )

                // Auto save lookups
                if (!spec.isNullOrBlank()) AppContainer.lookupRepository.insertSpecialty(spec)
                if (!reg.isNullOrBlank()) AppContainer.lookupRepository.insertRegion(reg)

                Toast.makeText(this@CallOverlayService, "تم حفظ العميل في الـ CRM", Toast.LENGTH_SHORT).show()
            }
            dismissOverlay()
        }

        btnDismiss?.setOnClickListener {
            dismissOverlay()
        }
    }

    private fun dismissOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
        }
        overlayView = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager?.removeView(it)
        }
        serviceScope.cancel()
    }
}