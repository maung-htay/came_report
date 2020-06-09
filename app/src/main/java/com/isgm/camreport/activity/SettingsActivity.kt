package com.isgm.camreport.activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.isgm.camreport.R
class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        // supportActionBar?.setDisplayHomeAsUpEnabled(true) // To remove unnecessary Back Icon
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return true
    }
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}