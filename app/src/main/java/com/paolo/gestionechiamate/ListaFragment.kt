package com.paolo.gestionechiamate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
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
    private var indiceAlfabeticoPagina: LinearLayout? = null
    private var adapterChiamate: ChiamataAdapter? = null
    private var gruppiChiamateSelezionati: List<VoceChiamata> = emptyList()
    private var azioneDopoPermessoScrittura: (() -> Unit)? = null

    private val richiediScritturaRegistro = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concesso ->
        val azione = azioneDopoPermessoScrittura
        azioneDopoPermessoScrittura = null
        if (concesso) azione?.invoke()
        else Toast.makeText(
            requireContext(),
            "Per eliminare le chiamate, imposta Gestione Chiamate come app Telefono predefinita",
            Toast.LENGTH_LONG
        ).show()
    }

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
        val indiceAlfabetico = view.findViewById<LinearLayout>(R.id.indiceAlfabetico)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        if (tipo == TIPO_CHIAMATE) {
            barraScorciatoie.visibility = View.VISIBLE
            val spazioScorciatoie = (96 * resources.displayMetrics.density).toInt()
            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                spazioScorciatoie
            )
            recycler.clipToPadding = false
            view.findViewById<TextView>(R.id.btnAnnullaSelezione).setOnClickListener {
                adapterChiamate?.annullaSelezione()
            }
            view.findViewById<TextView>(R.id.btnEliminaSelezione).setOnClickListener {
                confermaEliminaSelezionate()
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
            indiceAlfabetico.visibility = View.VISIBLE
            indiceAlfabeticoPagina = indiceAlfabetico
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
        adapterChiamate = null
        gruppiChiamateSelezionati = emptyList()
        recyclerPagina = null
        testoVuotoPagina = null
        indiceAlfabeticoPagina = null
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
                    mostraContatti(recycler, contattiCompleti)
                } else {
                    recycler.adapter = when (tipoPagina) {
                        TIPO_CHIAMATE -> ChiamataAdapter(dati as List<VoceChiamata>) { gruppi ->
                            aggiornaBarraSelezione(gruppi)
                        }.also { adapterChiamate = it }
                        else -> SmsAdapter(dati as List<Sms>) { ricaricaSms(recycler, txtVuoto) }
                    }
                }
            }
        }
    }

    private fun aggiornaBarraSelezione(gruppi: List<VoceChiamata>) {
        gruppiChiamateSelezionati = gruppi
        val root = view ?: return
        val barra = root.findViewById<View>(R.id.barraSelezioneChiamate)
        barra.visibility = if (gruppi.isEmpty()) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.barraScorciatoie).visibility =
            if (gruppi.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.txtConteggioSelezione).text =
            if (gruppi.size == 1) "1 gruppo selezionato"
            else "${gruppi.size} gruppi selezionati"
    }

    private fun confermaEliminaSelezionate() {
        val gruppi = gruppiChiamateSelezionati
        if (gruppi.isEmpty()) return
        val ids = gruppi.flatMap { it.ids }.toSet()
        mostraConfermaEliminazione(
            if (gruppi.size == 1) "Eliminare il gruppo selezionato?" else "Eliminare i gruppi selezionati?",
            if (gruppi.size == 1) "Saranno eliminate tutte le chiamate consecutive contenute in questo gruppo."
            else "Saranno eliminate tutte le chiamate contenute nei ${gruppi.size} gruppi selezionati.",
            "Elimina"
        ) { eseguiConPermessoScrittura { eliminaChiamate(ids) } }
    }

    fun confermaEliminaTutteRicevute() {
        if (tipoPagina != TIPO_CHIAMATE || !isAdded) return
        mostraConfermaEliminazione(
            "Eliminare tutte le chiamate ricevute?",
            "Saranno eliminate le chiamate ricevute, perse, rifiutate e bloccate. Le chiamate effettuate rimarranno.",
            "Elimina tutte"
        ) { eseguiConPermessoScrittura { eliminaTutteRicevute() } }
    }

    private fun mostraConfermaEliminazione(
        titolo: String, messaggio: String, testoAzione: String, conferma: () -> Unit
    ) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_conferma_eliminazione)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtTitoloConferma).text = titolo
        dialog.findViewById<TextView>(R.id.txtMessaggioConferma).text = messaggio
        dialog.findViewById<TextView>(R.id.btnConfermaElimina).apply {
            text = testoAzione
            setOnClickListener { dialog.dismiss(); conferma() }
        }
        dialog.findViewById<TextView>(R.id.btnConfermaAnnulla).setOnClickListener { dialog.dismiss() }
        ColoriTesto.applica(dialog.findViewById(R.id.pannelloConferma))
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun eseguiConPermessoScrittura(azione: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            azione()
        } else {
            azioneDopoPermessoScrittura = azione
            richiediScritturaRegistro.launch(Manifest.permission.WRITE_CALL_LOG)
        }
    }

    private fun eliminaChiamate(ids: Set<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val eliminati = withContext(Dispatchers.IO) {
                runCatching {
                    val segnaposto = ids.joinToString(",") { "?" }
                    requireContext().contentResolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        "${CallLog.Calls._ID} IN ($segnaposto)",
                        ids.map { it.toString() }.toTypedArray()
                    )
                }.getOrDefault(0)
            }
            adapterChiamate?.annullaSelezione()
            Toast.makeText(requireContext(), "$eliminati chiamate eliminate", Toast.LENGTH_SHORT).show()
            caricaPagina()
        }
    }

    private fun eliminaTutteRicevute() {
        viewLifecycleOwner.lifecycleScope.launch {
            val eliminati = withContext(Dispatchers.IO) {
                runCatching {
                    val tipi = intArrayOf(
                        CallLog.Calls.INCOMING_TYPE,
                        CallLog.Calls.MISSED_TYPE,
                        CallLog.Calls.REJECTED_TYPE,
                        CallLog.Calls.BLOCKED_TYPE
                    )
                    requireContext().contentResolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        "${CallLog.Calls.TYPE} IN (?,?,?,?)",
                        tipi.map { it.toString() }.toTypedArray()
                    )
                }.getOrDefault(0)
            }
            Toast.makeText(requireContext(), "$eliminati chiamate eliminate", Toast.LENGTH_SHORT).show()
            caricaPagina()
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
        mostraContatti(recycler, filtrati)
    }

    private fun mostraContatti(recycler: RecyclerView, contatti: List<Contatto>) {
        recycler.adapter = ContattoAdapter(contatti)
        aggiornaIndiceAlfabetico(recycler, contatti)
    }

    private fun aggiornaIndiceAlfabetico(recycler: RecyclerView, contatti: List<Contatto>) {
        val indice = indiceAlfabeticoPagina ?: return
        indice.removeAllViews()
        val posizioni = mutableMapOf<Char, Int>()
        contatti.forEachIndexed { posizione, contatto ->
            val lettera = inizialeIndice(contatto.nome)
            if (lettera != null && lettera !in posizioni) posizioni[lettera] = posizione
        }

        ('A'..'Z').forEach { lettera ->
            val disponibile = posizioni.containsKey(lettera)
            val voce = TextView(requireContext()).apply {
                text = lettera.toString()
                gravity = android.view.Gravity.CENTER
                textSize = 11f
                val coloreAttivo = Impostazioni.getColoreTesto(requireContext())
                    ?: ContextCompat.getColor(requireContext(), R.color.primary)
                setTextColor(if (disponibile) coloreAttivo else
                    ContextCompat.getColor(requireContext(), R.color.text_secondary))
                alpha = if (disponibile) 1f else 0.28f
                isEnabled = disponibile
                contentDescription = if (disponibile) "Vai alla lettera $lettera" else "Nessun contatto con $lettera"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                if (disponibile) {
                    setOnClickListener {
                        val layoutManager = recycler.layoutManager as? LinearLayoutManager
                        layoutManager?.scrollToPositionWithOffset(posizioni.getValue(lettera), 0)
                    }
                }
            }
            indice.addView(voce)
        }
    }

    private fun inizialeIndice(nome: String): Char? {
        val senzaAccenti = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return senzaAccenti.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }
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

        DialpadKeys.build(requireContext(), grid, keySizeDp = 60, usaColoreTema = true) { cifra ->
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
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
            ),
            null, null,
            CallLog.Calls.DATE + " DESC"
        )
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        cursor?.use {
            val idxId = it.getColumnIndex(CallLog.Calls._ID)
            val idxNome = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val idxNum = it.getColumnIndex(CallLog.Calls.NUMBER)
            val idxData = it.getColumnIndex(CallLog.Calls.DATE)
            val idxTipo = it.getColumnIndex(CallLog.Calls.TYPE)
            var count = 0
            while (it.moveToNext() && count < 200) {
                val id = if (idxId >= 0) it.getLong(idxId) else 0L
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

                val nuova = VoceChiamata(
                    id, listOf(id), nome, numeroGrezzo, formato.format(Date(data)), tipoChiamata
                )
                val precedente = lista.lastOrNull()
                if (precedente != null &&
                    normalizzaNumero(precedente.numero) == normalizzaNumero(numeroGrezzo)
                ) {
                    lista[lista.lastIndex] = precedente.copy(ids = precedente.ids + id)
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
    val id: Long,
    val ids: List<Long>,
    val nome: String,
    val numero: String,
    val dataFormattata: String,
    val tipo: Int
)
data class Sms(val mittente: String, val numero: String, val corpo: String, val dataFormattata: String)
