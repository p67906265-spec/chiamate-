package com.paolo.gestionechiamate

import android.app.role.RoleManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.EditText
import android.widget.Button
import android.widget.ScrollView
import android.widget.RadioGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton

class ImpostazioniActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        try {
            val versione = packageManager.getPackageInfo(packageName, 0).versionName
            findViewById<TextView>(R.id.txtVersione).text = "Gestione Chiamate — versione $versione"
        } catch (e: Exception) {
        }

        val gruppoTema = findViewById<RadioGroup>(R.id.gruppoTema)
        when (Impostazioni.getTema(this)) {
            Impostazioni.TEMA_CHIARO -> gruppoTema.check(R.id.radioChiaro)
            Impostazioni.TEMA_SCURO -> gruppoTema.check(R.id.radioScuro)
            else -> gruppoTema.check(R.id.radioSistema)
        }
        gruppoTema.setOnCheckedChangeListener { _, checkedId ->
            val tema = when (checkedId) {
                R.id.radioChiaro -> Impostazioni.TEMA_CHIARO
                R.id.radioScuro -> Impostazioni.TEMA_SCURO
                else -> Impostazioni.TEMA_SISTEMA
            }
            Impostazioni.setTema(this, tema)
        }

        aggiornaPulsanteColore()
        findViewById<MaterialButton>(R.id.btnColoreTesto).setOnClickListener {
            mostraSceltaColore()
        }

        val switchRichiamo = findViewById<Switch>(R.id.switchRichiamoAutomatico)
        switchRichiamo.isChecked = Impostazioni.isRichiamoAutomatico(this)
        switchRichiamo.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setRichiamoAutomatico(this, checked)
            if (!checked) AutoRichiamo.annulla(this)
        }
        findViewById<EditText>(R.id.editMessaggioRifiuto).apply {
            setText(Impostazioni.getMessaggioRifiuto(this@ImpostazioniActivity))
            setOnFocusChangeListener { _, haFocus ->
                if (!haFocus) Impostazioni.setMessaggioRifiuto(this@ImpostazioniActivity, text.toString())
            }
        }

        val switchNascosti = findViewById<Switch>(R.id.switchNascosti)
        val switchStranieri = findViewById<Switch>(R.id.switchStranieri)
        val switchNonInRubrica = findViewById<Switch>(R.id.switchNonInRubrica)
        val switchPrefissi = findViewById<Switch>(R.id.switchPrefissi)

        switchNascosti.isChecked = Impostazioni.isBloccoNumeriNascosti(this)
        switchStranieri.isChecked = Impostazioni.isBloccoNumeriStranieri(this)
        switchNonInRubrica.isChecked = Impostazioni.isBloccoNonInRubrica(this)
        switchPrefissi.isChecked = Impostazioni.isBloccoPrefissiAttivo(this)

        switchNascosti.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNumeriNascosti(this, checked)
        }
        switchStranieri.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNumeriStranieri(this, checked)
        }
        switchNonInRubrica.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNonInRubrica(this, checked)
        }
        switchPrefissi.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoPrefissiAttivo(this, checked)
        }
        aggiornaPulsantePrefissi()
        findViewById<MaterialButton>(R.id.btnGestisciPrefissi).setOnClickListener {
            mostraGestionePrefissi()
        }

        aggiornaStatoFiltro()
        findViewById<MaterialButton>(R.id.btnAttivaFiltro).setOnClickListener {
            richiediRuoloFiltroChiamate()
        }
    }

    override fun onResume() {
        super.onResume()
        aggiornaStatoFiltro()
    }

    override fun onPause() {
        findViewById<EditText>(R.id.editMessaggioRifiuto).let {
            Impostazioni.setMessaggioRifiuto(this, it.text.toString())
        }
        super.onPause()
    }

    private fun aggiornaStatoFiltro() {
        val txt = findViewById<TextView>(R.id.txtStatoFiltro)
        val btn = findViewById<MaterialButton>(R.id.btnAttivaFiltro)
        if (filtroAttivo()) {
            txt.text = "Il filtro chiamate è attivo: le chiamate bloccate verranno rifiutate automaticamente."
            btn.visibility = android.view.View.GONE
        } else {
            txt.text = "Il filtro non è ancora attivo. Tocca il pulsante per attivarlo: verrà chiesto di impostare " +
                "questa app come app per il controllo delle chiamate."
            btn.visibility = android.view.View.VISIBLE
        }
    }

    private fun filtroAttivo(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        }
        val telecomManager = getSystemService(TelecomManager::class.java)
        return telecomManager?.defaultDialerPackage == packageName
    }

    private fun richiediRuoloFiltroChiamate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager == null) {
                mostraErroreFiltro()
                return
            }
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                android.widget.Toast.makeText(
                    this,
                    "Il tuo telefono non consente ad app di terze parti di filtrare le chiamate. " +
                        "Prova a cercare \"controllo chiamate\" o \"app predefinite\" nelle impostazioni di sistema.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
            try {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            } catch (e: Exception) {
                mostraErroreFiltro()
            }
        } else {
            try {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                startActivity(intent)
            } catch (e: Exception) {
                mostraErroreFiltro()
            }
        }
    }

    private fun mostraErroreFiltro() {
        android.widget.Toast.makeText(
            this,
            "Non è stato possibile aprire le impostazioni per il filtro chiamate su questo telefono.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun aggiornaPulsantePrefissi() {
        val quanti = Impostazioni.getPrefissiBloccati(this).size
        findViewById<MaterialButton>(R.id.btnGestisciPrefissi).text = "Gestisci prefissi ($quanti)"
    }

    private fun mostraGestionePrefissi() {
        val contenitore = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        val spiegazione = TextView(this).apply {
            text = "Esempi: 02, 081, 899 oppure +39 0432. Il blocco comprende tutti i numeri che iniziano con il prefisso."
            setTextColor(androidx.core.content.ContextCompat.getColor(this@ImpostazioniActivity, R.color.text_secondary))
            textSize = 13f
            setPadding(0, 0, 0, dp(12))
        }
        val editPrefisso = EditText(this).apply {
            hint = "Prefisso"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
        }
        val editDescrizione = EditText(this).apply {
            hint = "Descrizione facoltativa (es. Milano)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
        }
        val btnAggiungi = Button(this).apply { text = "Aggiungi alla lista" }
        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        val scroll = ScrollView(this).apply {
            addView(lista)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(300)
            )
        }
        contenitore.addView(spiegazione)
        contenitore.addView(editPrefisso)
        contenitore.addView(editDescrizione)
        contenitore.addView(btnAggiungi)
        contenitore.addView(scroll)

        fun ricostruisciLista() {
            lista.removeAllViews()
            val prefissi = Impostazioni.getPrefissiBloccati(this)
            if (prefissi.isEmpty()) {
                lista.addView(TextView(this).apply {
                    text = "Nessun prefisso bloccato"
                    gravity = android.view.Gravity.CENTER
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@ImpostazioniActivity, R.color.text_secondary))
                    setPadding(8, dp(20), 8, dp(20))
                })
                return
            }
            prefissi.forEach { voce ->
                val riga = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, dp(5), 0, dp(5))
                }
                riga.addView(TextView(this).apply {
                    text = if (voce.descrizione.isBlank()) voce.prefisso
                    else "${voce.prefisso}  •  ${voce.descrizione}"
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@ImpostazioniActivity, R.color.text_primary))
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                riga.addView(Button(this).apply {
                    text = "Elimina"
                    setOnClickListener {
                        Impostazioni.eliminaPrefissoBloccato(this@ImpostazioniActivity, voce.prefisso)
                        ricostruisciLista()
                        aggiornaPulsantePrefissi()
                    }
                })
                lista.addView(riga)
            }
        }

        btnAggiungi.setOnClickListener {
            if (Impostazioni.aggiungiPrefissoBloccato(
                    this,
                    editPrefisso.text.toString(),
                    editDescrizione.text.toString()
                )
            ) {
                editPrefisso.text.clear()
                editDescrizione.text.clear()
                editPrefisso.error = null
                ricostruisciLista()
                aggiornaPulsantePrefissi()
            } else {
                editPrefisso.error = "Inserisci almeno 2 cifre"
            }
        }
        ricostruisciLista()
        AlertDialog.Builder(this)
            .setTitle("Prefissi bloccati")
            .setView(contenitore)
            .setNegativeButton("Chiudi", null)
            .create()
            .show()
    }

    private fun aggiornaPulsanteColore() {
        val colore = Impostazioni.getColoreTesto(this)
        findViewById<MaterialButton>(R.id.btnColoreTesto).text = if (colore == null) {
            "Automatico (tema dell'app)"
        } else String.format("Colore scelto  #%06X", 0xFFFFFF and colore)
    }

    private fun mostraSceltaColore() {
        val iniziale = Impostazioni.getColoreTesto(this) ?: Color.rgb(21, 101, 192)
        val contenitore = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        val anteprima = TextView(this).apply {
            text = "Anteprima del testo"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setTextColor(iniziale)
            setPadding(8, dp(18), 8, dp(18))
        }
        contenitore.addView(anteprima)

        val barre = mutableListOf<SeekBar>()
        fun aggiungiBarra(titolo: String, valore: Int) {
            contenitore.addView(TextView(this).apply {
                text = titolo
                setTextColor(androidx.core.content.ContextCompat.getColor(this@ImpostazioniActivity, R.color.text_primary))
            })
            contenitore.addView(SeekBar(this).apply {
                max = 255
                progress = valore
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (barre.size == 3) {
                            anteprima.setTextColor(Color.rgb(barre[0].progress, barre[1].progress, barre[2].progress))
                        }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
                barre += this
            })
        }
        aggiungiBarra("Rosso", Color.red(iniziale))
        aggiungiBarra("Verde", Color.green(iniziale))
        aggiungiBarra("Blu", Color.blue(iniziale))

        android.app.AlertDialog.Builder(this)
            .setTitle("Colore del testo")
            .setView(contenitore)
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Automatico") { _, _ ->
                Impostazioni.setColoreTesto(this, null)
                recreate()
            }
            .setPositiveButton("Applica") { _, _ ->
                Impostazioni.setColoreTesto(
                    this, Color.rgb(barre[0].progress, barre[1].progress, barre[2].progress)
                )
                aggiornaPulsanteColore()
                ColoriTesto.applica(window.decorView)
            }
            .show()
    }

    private fun dp(valore: Int) = (valore * resources.displayMetrics.density).toInt()
}
