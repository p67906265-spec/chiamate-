package com.paolo.gestionechiamate

import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaFragment : Fragment() {

    companion object {
        const val TIPO_RUBRICA = 0
        const val TIPO_CHIAMATE = 1
        const val TIPO_SMS = 2
        private const val ARG_TIPO = "tipo"

        fun nuova(tipo: Int): ListaFragment {
            val f = ListaFragment()
            f.arguments = Bundle().apply { putInt(ARG_TIPO, tipo) }
            return f
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_lista, container, false)
        val tipo = arguments?.getInt(ARG_TIPO) ?: TIPO_RUBRICA
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerView)
        val txtVuoto = view.findViewById<TextView>(R.id.txtVuoto)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        if (!Permessi.tuttiConcessi(requireContext())) {
            txtVuoto.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            return view
        }

        scope.launch {
            val dati = withContext(Dispatchers.IO) {
                when (tipo) {
                    TIPO_RUBRICA -> caricaContatti()
                    TIPO_CHIAMATE -> caricaChiamate()
                    else -> caricaSms()
                }
            }
            if (dati.isEmpty()) {
                txtVuoto.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                recycler.adapter = when (tipo) {
                    TIPO_RUBRICA -> ContattoAdapter(dati as List<Contatto>)
                    TIPO_CHIAMATE -> ChiamataAdapter(dati as List<VoceChiamata>)
                    else -> SmsAdapter(dati as List<Sms>)
                }
            }
        }

        return view
    }

    // ---------- Rubrica ----------
    private fun caricaContatti(): List<Contatto> {
        val lista = mutableListOf<Contatto>()
        val cursor: Cursor? = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val idxNome = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val idxNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val nome = if (idxNome >= 0) it.getString(idxNome) else null
                val numero = if (idxNum >= 0) it.getString(idxNum) else null
                if (!numero.isNullOrBlank()) lista.add(Contatto(nome ?: numero, numero))
            }
        }
        // Un numero può comparire più volte perché lo stesso contatto è sincronizzato
        // da più fonti (memoria telefono + account Google) con formattazioni diverse
        // (spazi, trattini, prefisso internazionale). Normalizzo prima di deduplicare.
        return lista.distinctBy { normalizzaNumero(it.numero) }
    }

    private fun normalizzaNumero(numero: String): String {
        var soloCifre = numero.filter { it.isDigit() }
        if (soloCifre.length > 10) soloCifre = soloCifre.takeLast(10)
        return soloCifre
    }

    // ---------- Chiamate ----------
    private fun caricaChiamate(): List<VoceChiamata> {
        val lista = mutableListOf<VoceChiamata>()
        val cursor: Cursor? = requireContext().contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
            null, null,
            CallLog.Calls.DATE + " DESC"
        )
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        cursor?.use {
            val idxNome = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val idxNum = it.getColumnIndex(CallLog.Calls.NUMBER)
            val idxData = it.getColumnIndex(CallLog.Calls.DATE)
            var count = 0
            while (it.moveToNext() && count < 200) {
                val nome = if (idxNome >= 0) it.getString(idxNome) else null
                val numero = if (idxNum >= 0) it.getString(idxNum) else ""
                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                lista.add(VoceChiamata(nome ?: numero, numero, formato.format(Date(data))))
                count++
            }
        }
        return lista
    }

    // ---------- SMS ----------
    private fun caricaSms(): List<Sms> {
        val lista = mutableListOf<Sms>()
        val cursor: Cursor? = requireContext().contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null,
            Telephony.Sms.DATE + " DESC"
        )
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        cursor?.use {
            val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
            val idxData = it.getColumnIndex(Telephony.Sms.DATE)
            var count = 0
            while (it.moveToNext() && count < 200) {
                val mittente = if (idxAddr >= 0) it.getString(idxAddr) else "?"
                val corpo = if (idxBody >= 0) it.getString(idxBody) else ""
                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                lista.add(Sms(mittente ?: "?", corpo ?: "", formato.format(Date(data))))
                count++
            }
        }
        return lista
    }
}

data class Contatto(val nome: String, val numero: String)
data class VoceChiamata(val nome: String, val numero: String, val dataFormattata: String)
data class Sms(val mittente: String, val corpo: String, val dataFormattata: String)
