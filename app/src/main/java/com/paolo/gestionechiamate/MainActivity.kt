package com.paolo.gestionechiamate

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private val richiediPermessi = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { risultati ->
        // Se qualche permesso viene negato, le singole schermate mostrano comunque
        // un messaggio e permettono di riprovare.
        setupViewPager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (!Permessi.tuttiConcessi(this)) {
            richiediPermessi.launch(Permessi.mancanti(this))
        }

        setupViewPager()
    }

    override fun onResume() {
        super.onResume()
        // Aggiorna i preferiti/rubrica quando si torna alla schermata (es. dopo aver dato i permessi)
        (findViewById<ViewPager2>(R.id.viewPager).adapter as? PagerAdapter)?.notifyDataSetChanged()
    }

    private fun setupViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = PagerAdapter(this)
        viewPager.offscreenPageLimit = 4

        val titoli = listOf(
            getString(R.string.tab_preferiti),
            getString(R.string.tab_tastierino),
            getString(R.string.tab_rubrica),
            getString(R.string.tab_chiamate),
            getString(R.string.tab_sms)
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titoli[position]
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.azione_dialer_predefinito) {
            richiediRuoloDialer()
            return true
        }
        if (item.itemId == R.id.azione_permesso_overlay) {
            richiediPermessoOverlay()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun richiediPermessoOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(this)
        ) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun richiediRuoloDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                }
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

    private class PagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 5
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> PreferitiFragment()
            1 -> TastierinoFragment()
            2 -> ListaFragment.nuova(ListaFragment.TIPO_RUBRICA)
            3 -> ListaFragment.nuova(ListaFragment.TIPO_CHIAMATE)
            else -> ListaFragment.nuova(ListaFragment.TIPO_SMS)
        }
    }
}
