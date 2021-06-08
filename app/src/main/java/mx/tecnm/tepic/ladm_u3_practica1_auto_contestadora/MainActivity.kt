package mx.tecnm.tepic.ladm_u3_practica1_auto_contestadora

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.CallLog
import android.telephony.SmsManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var baseRemota = FirebaseFirestore.getInstance()
    var listaTelefonos = ArrayList<String>()
    val siLecturaLlamadas = 1
    val siEnviarMensajes = 2
    var listaMensajesMandados = ArrayList<String>()

    var mensajeAgradable = "Hola! Estoy no disponible hasta las 12:00, pero te regreso la llamada"
    var mensajeDesagradable = "Por favor ya no intentes llamarme!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Permiso leer historial de llamadas
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_CALL_LOG), siLecturaLlamadas)
        }

        //Permiso enviar mensaje de texto
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.SEND_SMS),siEnviarMensajes)
        }

        btnMensajes.setOnClickListener {
            actualizarMensajes()
        }


        btnAgregar.setOnClickListener {
            insertarContactos()
        }

        var timer = object : CountDownTimer(20000,5000){
            override fun onTick(millisUntilFinished: Long) {
                cargarListaLlamadas()
                alerta("Buscando llamadas perdidas")
            }

            override fun onFinish() {
                enviarSMS()
                start()
            }
        }.start()

    }

    private fun actualizarMensajes() {
        mensajeAgradable = txtMensajeAgradable.text.toString()
        mensajeDesagradable = txtMensajeDesagradable.text.toString()

        mensaje("MENSAJES ACTUALIZADOS")

        //Limpiar campos mensajes
        txtMensajeAgradable.setText("")
        txtMensajeDesagradable.setText("")
    }

    private fun insertarContactos() {
        var tipo = ""

        if (rbtnAgradables.isChecked){
            tipo = rbtnAgradables.text.toString()
        }else {
            tipo = rbtnDesagradables.text.toString()
        }

        var datosInsertar = hashMapOf(
            "nombre" to txtNombre.text.toString(),
            "telefono" to txtTelefono.text.toString(),
            "tipo" to tipo
        )

        baseRemota.collection("contactos")
            .add(datosInsertar)
            .addOnSuccessListener {
                mensaje("EXITO! SE INSERTO CORRECTAMENTE")

                //Limpiar campos contacto
                txtNombre.setText("")
                txtTelefono.setText("")
            }
            .addOnFailureListener {
                mensaje("ERROR! no se pudo insertar")
            }
    }

    private fun enviarSMS() {
        var tipo = ""
        if (listaTelefonos.isNotEmpty()){
            var telefono = ""
            listaTelefonos.forEach {
                baseRemota.collection("contactos").addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        mensaje(error.message!!)
                        return@addSnapshotListener
                    }
                    for (document in querySnapshot!!) {
                        tipo = "${document.getString("tipo")}"
                        telefono = document.getString("telefono").toString()

                        if (listaMensajesMandados.contains(telefono)){

                        }else{
                            //Comparar tipos
                            if (tipo.equals("AGRADABLES")) {
                                //COMPARAR TELEFONOS DE LA NUBE CON LOS DE LAS LLAMADAS PERDIDAS
                                if (it.equals(document.getString("telefono"))) {
                                    SmsManager.getDefault().sendTextMessage(telefono,null, mensajeAgradable,null,null)
                                    listaMensajesMandados.add(telefono)
                                }
                            } else {
                                if (it.equals(document.getString("telefono"))) {
                                    SmsManager.getDefault().sendTextMessage(telefono,null, mensajeDesagradable,null,null)
                                    listaMensajesMandados.add(telefono)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cargarListaLlamadas() {
        var llamadas = ArrayList<String>()
        val seleccion = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE
        var cursor = contentResolver.query(
            Uri.parse("content://call_log/calls"),
            null, seleccion, null, null
        )
        listaTelefonos.clear()
        var registro = ""
        while (cursor!!.moveToNext()){
            var nombre = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME))
            var telefono = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                telefono = telefono.replace(" ".toRegex(), "")
            var fecha = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE))

            val seconds: Long = fecha.toLong()
            val formatter = SimpleDateFormat("DD-MM-YY HH:mm")
            val dateString: String = formatter.format(Date(seconds))

            registro = "NOMBRE: ${nombre} \nNUMERO: ${telefono} \nFECHA: ${dateString}"
            llamadas.add(registro)
            listaTelefonos.add(telefono)
        }
        listallamadas.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, llamadas
        )
        cursor.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == siLecturaLlamadas){ cargarListaLlamadas() }
    }

    private fun mensaje(s: String) {
        AlertDialog.Builder(this).setTitle("ATENCION")
            .setMessage(s)
            .setPositiveButton("OK"){ d,i-> }
            .show()
    }

    private fun alerta(s: String) {
        Toast.makeText(this,s, Toast.LENGTH_LONG).show()
    }


}