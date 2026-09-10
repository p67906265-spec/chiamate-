package com.paolo.gestionechiamate

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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

    private var contattiCompleti: List<Contatto> = emptyList()
    private var tipoPagina: Int = TIPO_RUBRICA
    private var recyclerPagina: RecyclerView? = null
    private var testoVuotoPagina: TextView? = null

    private val creaContatto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { caricaPagina() }

    private val sceltaContattoSms = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { risultato ->
        if (risultato.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = risultato.data?.data
            if (uri != null) {
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idxNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val numero = if (idxNum >= 0) it.getString(idxNum) else null
                        if (!numero.isNullOrBlank()) {
                            apriConversazione(numero)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_lista, container, false)
        val tipo = arguments?.getInt(ARG_TIPO) ?: TIPO_RUBRICA
        tipoPagina = tipo
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerView)
        val txtVuoto = view.findViewById<TextView>(R.id.txtVuoto)
        recyclerPagina = recycler
        testoVuotoPagina = txtVuoto
        val barraScorciatoie = view.findViewById<View>(R.id.barraScorciatoie)
        val barraRicerca = view.findViewById<View>(R.id.barraRicerca)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        if (tipo == TIPO_CHIAMATE) {
            barraScorciatoie.visibility = View.VISIBLE
            view.findViewById<View>(R.id.btnScorciatoiaRubrica).setOnClickListener {
                vaiAllaPagina(2)
            }
            view.findViewById<View>(R.id.btnScorciatoiaPreferiti).setOnClickListener {
                vaiAllaPagina(3)
            }
            view.findViewById<View>(R.id.btnScorciatoiaTastierino).setOnClickListener {
                mostraDialogTastierino()
            }
        }

        if (tipo == TIPO_RUBRICA) {
            barraRicerca.visibility = View.VISIBLE
            view.findViewById<ImageButton>(R.id.btnNuovoContatto).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    creaContatto.launch(
                        Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                    )
                }
            }
            val editCerca = view.findViewById<EditText>(R.id.editCerca)
            editCerca.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filtraContatti(recycler, s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        if (tipo == TIPO_SMS) {
            val btnNuovoMessaggio = view.findViewById<ImageButton>(R.id.btnNuovoMessaggio)
            btnNuovoMessaggio.visibility = View.VISIBLE
            btnNuovoMessaggio.setOnClickListener {
                mostraSceltaNuovoMessaggio()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        caricaPagina()
    }

    override fun onDestroyView() {
        recyclerPagina = null
        testoVuotoPagina = null
        super.onDestroyView()
    }

    private fun caricaPagina() {
        val recycler = recyclerPagina ?: return
        val txtVuoto = testoVuotoPagina ?: return
        if (!Permessi.tuttiConcessi(requireContext())) {
            txtVuoto.text = getString(R.string.permessi_necessari)
            txtVuoto.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val dati = withContext(Dispatchers.IO) {
                when (tipoPagina) {
                    TIPO_RUBRICA -> caricaContatti()
                    TIPO_CHIAMATE -> caricaChiamate()
                    else -> caricaSms()
                }
            }
            if (dati.isEmpty()) {
                txtVuoto.text = when (tipoPagina) {
                    TIPO_RUBRICA -> "Nessun contatto"
                    TIPO_CHIAMATE -> "Nessuna chiamata"
                    else -> "Nessun messaggio"
                }
                txtVuoto.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                txtVuoto.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                if (tipoPagina == TIPO_RUBRICA) {
                    @Suppress("UNCHECKED_CAST")
                    contattiCompleti = dati as List<Contatto>
                }
                recycler.adapter = when (tipoPagina) {
                    TIPO_RUBRICA -> ContattoAdapter(dati as List<Contatto>)
                    TIPO_CHIAMATE -> ChiamataAdapter(dati as List<VoceChiamata>)
                    else -> SmsAdapter(dati as List<Sms>) { ricaricaSms(recycler, txtVuoto) }
                }
            }
        }
    }

    private fun filtraContatti(recycler: RecyclerView, testo: String) {
        val filtrati = if (testo.isBlank()) {
            contattiCompleti
        } else {
            contattiCompleti.filter {
                it.nome.contains(testo, ignoreCase = true) ||
                    it.numeri.any { numero -> numero.numero.contains(testo) }
            }
        }
        recycler.adapter = ContattoAdapter(filtrati)
    }

    private fun vaiAllaPagina(posizione: Int) {
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).currentItem = posizione
    }

    private fun mostraSceltaNuovoMessaggio() {
        val opzioni = arrayOf("Scegli da rubrica", "Nuovo numero")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Nuovo messaggio")
            .setItems(opzioni) { _, which ->
                if (which == 0) {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    sceltaContattoSms.launch(intent)
                } else {
                    apriConversazione(null)
                }
            }
            .show()
    }

    private fun apriConversazione(numero: String?) {
        val intent = Intent(requireContext(), ComponiSmsActivity::class.java)
        if (!numero.isNullOrBlank()) {
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, numero)
        }
        startActivity(intent)
    }

    private fun ricaricaSms(recycler: RecyclerView, txtVuoto: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val dati = withContext(Dispatchers.IO) { caricaSms() }
            if (dati.isEmpty()) {
                txtVuoto.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                txtVuoto.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                recycler.adapter = SmsAdapter(dati) { ricaricaSms(recycler, txtVuoto) }
            }
        }
    }

    private fun mostraDialogTastierino() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_tastierino)
        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val numero = StringBuilder()
        val txtNumero = dialog.findViewById<TextView>(R.id.txtNumeroDialog)
        val btnBackspace = dialog.findViewById<android.widget.ImageButton>(R.id.btnBackspaceDialog)
        val grid = dialog.findViewById<android.widget.GridLayout>(R.id.gridTastiDialog)
        val btnChiama = dialog.findViewById<android.widget.ImageButton>(R.id.btnChiamaDialog)

        fun aggiornaDisplay() {
            txtNumero.text = numero.toString()
            btnBackspace.visibility = if (numero.isNotEmpty()) View.VISIBLE else View.INVISIBLE
        }

        DialpadKeys.build(requireContext(), grid, keySizeDp = 60, stileScuro = true) { cifra ->
            numero.append(cifra)
            aggiornaDisplay()
        }

        btnBackspace.setOnClickListener {
            if (numero.isNotEmpty()) {
                numero.deleteCharAt(numero.length - 1)
                aggiornaDisplay()
            }
        }
        btnBackspace.setOnLongClickListener {
            numero.clear()
            aggiornaDisplay()
            true
        }

        btnChiama.setOnClickListener {
            if (numero.isNotEmpty()) {
                ChiamaHelper.chiama(requireContext(), numero.toString())
                dialog.dismiss()
            }
        }

        dialog.findViewById<View>(R.id.sfondoDialog).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ---------- Rubrica ----------
    private fun caricaContatti(): List<Contatto> {
        val contatti = linkedMapOf<Long, Contatto>()
        val cursor: Cursor? = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val idxId = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val idxLookup = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val idxNome = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val idxNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val idxTipo = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val idxEtichetta = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
            while (it.moveToNext()) {
                val id = if (idxId >= 0) it.getLong(idxId) else 0L
                val lookup = if (idxLookup >= 0) it.getString(idxLookup) else null
                val nome = if (idxNome >= 0) it.getString(idxNome) else null
                val numero = if (idxNum >= 0) it.getString(idxNum) else null
                val tipo = if (idxTipo >= 0) it.getInt(idxTipo) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                val etichettaPersonalizzata = if (idxEtichetta >= 0) it.getString(idxEtichetta) else null
                if (!numero.isNullOrBlank()) {
                    val etichetta = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        resources, tipo, etichettaPersonalizzata
                    ).toString()
                    val esistente = contatti[id]
                    val voceNumero = NumeroContatto(numero, etichetta)
                    if (esistente == null) {
                        contatti[id] = Contatto(id, lookup, nome ?: numero, listOf(voceNumero))
                    } else if (esistente.numeri.none { n -> normalizzaNumero(n.numero) == normalizzaNumero(numero) }) {
                        contatti[id] = esistente.copy(numeri = esistente.numeri + voceNumero)
                    }
                }
            }
        }
        return contatti.values.toList()
    }

    private fun normalizzaNumero(numero: String): String {
        var soloCifre = numero.filter { it.isDigit() }
        if (soloCifre.length > 10) soloCifre = soloCifre.takeLast(10)
        return soloCifre
    }

    // ---------- Chiamate ----------
    private fun caricaChiamate(): List<VoceChiamata> {
        val mappaRubrica = costruisciMappaRubrica()
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
            val idxTipo = it.getColumnIndex(CallLog.Calls.TYPE)
            var count = 0
            while (it.moveToNext() && count < 200) {
                val nomeCache = if (idxNome >= 0) it.getString(idxNome) else null
                val numeroGrezzo = if (idxNum >= 0) it.getString(idxNum) else ""
                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                val tipoChiamata = if (idxTipo >= 0) it.getInt(idxTipo) else CallLog.Calls.INCOMING_TYPE

                val numeroVisualizzato = when (numeroGrezzo) {
                    "-1" -> "Numero sconosciuto"
                    "-2" -> "Numero privato"
                    "-3" -> "Cabina telefonica"
                    "" -> "Numero sconosciuto"
                    else -> numeroGrezzo
                }

                val nome = mappaRubrica[normalizzaNumero(numeroGrezzo)]
                    ?: nomeCache?.takeIf { it.isNotBlank() }
                    ?: numeroVisualizzato

                val nuova = VoceChiamata(nome, numeroGrezzo, formato.format(Date(data)), tipoChiamata)
                val precedente = lista.lastOrNull()
                if (precedente != null && normalizzaNumero(precedente.numero) == normalizzaNumero(numeroGrezzo)) {
                    lista[lista.lastIndex] = precedente.copy(conteggio = precedente.conteggio + 1)
                } else {
                    lista.add(nuova)
                }
                count++
            }
        }
        return lista
    }

    private fun costruisciMappaRubrica(): Map<String, String> {
        val mappa = mutableMapOf<String, String>()
        val cursor: Cursor? = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        cursor?.use {
            val idxNome = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val idxNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val nome = if (idxNome >= 0) it.getString(idxNome) else null
                val numero = if (idxNum >= 0) it.getString(idxNum) else null
                if (nome != null && numero != null) {
                    mappa[normalizzaNumero(numero)] = nome
                }
            }
        }
        return mappa
    }

    // ---------- SMS ----------
    private fun caricaSms(): List<Sms> {
        val mappaRubrica = costruisciMappaRubrica()
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
                val numero = if (idxAddr >= 0) it.getString(idxAddr) else "?"
                val corpo = if (idxBody >= 0) it.getString(idxBody) else ""
                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                val nomeVisualizzato = mappaRubrica[normalizzaNumero(numero ?: "")] ?: numero ?: "?"
                lista.add(Sms(nomeVisualizzato, numero ?: "?", corpo ?: "", formato.format(Date(data))))
                count++
            }
        }
        return lista
    }
}

data class NumeroContatto(val numero: String, val etichetta: String)
data class Contatto(
    val id: Long,
    val lookupKey: String?,
    val nome: String,
    val numeri: List<NumeroContatto>
) {
    val numero: String get() = numeri.firstOrNull()?.numero.orEmpty()
}
data class VoceChiamata(
    val nome: String,
    val numero: String,
    val dataFormattata: String,
    val tipo: Int,
    val conteggio: Int = 1
)
data class Sms(val mittente: String, val numero: String, val corpo: String, val dataFormattata: String)
