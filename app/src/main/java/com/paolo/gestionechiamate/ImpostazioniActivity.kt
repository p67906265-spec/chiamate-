package com.paolo.gestionechiamate

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.RadioGroup
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

        val switchNascosti = findViewById<Switch>(R.id.switchNascosti)
        val switchStranieri = findViewById<Switch>(R.id.switchStranieri)
        val switchNonInRubrica = findViewById<Switch>(R.id.switchNonInRubrica)

        switchNascosti.isChecked = Impostazioni.isBloccoNumeriNascosti(this)
        switchStranieri.isChecked = Impostazioni.isBloccoNumeriStranieri(this)
        switchNonInRubrica.isChecked = Impostazioni.isBloccoNonInRubrica(this)

        switchNascosti.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNumeriNascosti(this, checked)
        }
        switchStranieri.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNumeriStranieri(this, checked)
        }
        switchNonInRubrica.setOnCheckedChangeListener { _, checked ->
            Impostazioni.setBloccoNonInRubrica(this, checked)
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
}
