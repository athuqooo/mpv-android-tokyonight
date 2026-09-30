package `is`.xyz.mpv

import `is`.xyz.filepicker.AbstractFilePickerFragment
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Predicate
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.RecyclerView
import `is`.xyz.filepicker.DocumentPickerFragment
import `is`.xyz.filepicker.FilePickerFragment
import `is`.xyz.mpv.preferences.PreferenceActivity
import java.io.File
import java.io.FileFilter

class FilePickerActivity : AppCompatActivity(), AbstractFilePickerFragment.OnFilePickedListener {
    private var fragment: MPVFilePickerFragment? = null
    private var fragment2: MPVDocumentPickerFragment? = null
    private var homeMode = false
    private var oledThemeApplied = false

    private var lastSeenInsets: WindowInsets? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        oledThemeApplied = Utils.isOledTheme(this)
        Utils.applyOledTheme(this, R.style.FilePickerTheme_Oled)
        super.onCreate(null)
        Log.v(TAG, "FilePickerActivity: created")
        homeMode = intent.getIntExtra("skip", -1) == -1

        setContentView(R.layout.activity_filepicker)
        supportActionBar?.title = getString(R.string.mpv_activity)
        supportActionBar?.hide()

        onBackPressedDispatcher.addCallback(this) {
            onBackPressedImpl()
        }

        // The basic issue we have here is this: https://stackoverflow.com/questions/31190612/
        // Some part of the view hierarchy swallows the insets during fragment transitions
        // and it's impossible to invoke this calculation a second time (requestApplyInsets doesn't help).
        // For that reason I wrote this creative workaround, it works surprisingly well.
        findViewById<View>(R.id.fragment_container_view).setOnApplyWindowInsetsListener { _, insets ->
            lastSeenInsets = WindowInsets(insets)
            insets
        }

        when (intent.getIntExtra("skip", -1)) {
            URL_DIALOG -> {
                showUrlDialog()
                return
            }
            FILE_PICKER -> {
                initFilePicker()
                return
            }
            DOC_PICKER -> {
                val root = Uri.parse(intent.getStringExtra("root")!!)
                initDocPicker(root)
                return
            }
        }
        initFilePicker()
    }

    override fun onResume() {
        super.onResume()
        if (oledThemeApplied != Utils.isOledTheme(this))
            recreate()
    }

    private fun doUiTweaks() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Part 2 of the workaround: apply the insets to the recycler so it can
        // take them into account.
        val recycler: RecyclerView = findViewById(android.R.id.list)
        lastSeenInsets?.let { recycler.onApplyWindowInsets(lastSeenInsets) }
    }

    private fun getFilterState(): Boolean {
        with (PreferenceManager.getDefaultSharedPreferences(this)) {
            // naming is a legacy leftover
            return getBoolean("MainActivity_filter_state", true)
        }
    }

    private fun saveFilterState(enabled: Boolean) {
        with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
            this.putBoolean("MainActivity_filter_state", enabled)
            apply()
        }
    }

    private fun inflateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.menu_filepicker, menu)
        // document picker does not have a concept of storages
        if (fragment == null)
            menu.findItem(R.id.action_external_storage).isVisible = false
        menu.findItem(R.id.action_request_access).isVisible =
            fragment != null && !FilePickerFragment.hasPermission(this, File("/"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType != Configuration.UI_MODE_TYPE_TELEVISION) {
            inflateOptionsMenu(menu)
        } else {
            // add a dummy menu item so the menu icon shows up, even though you can't use it on TV.
            // it is instead opened via dpad keys
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "...")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_external_storage -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    val path = Environment.getExternalStorageDirectory()
                    fragment!!.goToDir(path) // attempt to do something useful
                    return true
                }

                val vols = Utils.getStorageVolumes(this)

                with (AlertDialog.Builder(this)) {
                    setItems(vols.map { it.description }.toTypedArray()) { dialog, item ->
                        val vol = vols[item]
                        with (fragment!!) {
                            root = vol.path
                            goToDir(vol.path)
                        }
                        dialog.dismiss()
                    }
                    show()
                }
                return true
            }
            R.id.action_file_filter -> {
                var old = false
                fragment?.apply {
                    old = filterPredicate != null
                    filterPredicate = if (!old) MEDIA_FILE_FILTER else null
                }
                fragment2?.apply {
                    old = filterPredicate != null
                    filterPredicate = if (!old) MEDIA_DOC_FILTER else null
                }
                with (Toast.makeText(this, "", Toast.LENGTH_SHORT)) {
                    setText(if (!old) R.string.notice_show_media_files else R.string.notice_show_all_files)
                    show()
                }
                saveFilterState(!old)
                return true
            }
            R.id.action_request_access -> {
                fragment?.goToDir(File("/"))
                return true
            }
            R.id.action_open_url -> {
                showUrlDialog()
                return true
            }
            R.id.action_settings_home -> {
                startActivity(Intent(this, PreferenceActivity::class.java))
                return true
            }
            else -> return false
        }
    }

    private fun initFilePicker() {
        // Create fragment first
        if (fragment == null) {
            fragment = MPVFilePickerFragment()
            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, fragment!!, null)
                runOnCommit { doUiTweaks() }
                commit()
            }
        }
        supportActionBar?.show()

        if (!FilePickerFragment.hasPermission(this, File("/"))) {
            Log.v(TAG, "FilePickerActivity: waiting for file picker permission")
            return
        }

        Log.v(TAG, "FilePickerActivity: showing file picker")
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (getFilterState())
            fragment!!.filterPredicate = MEDIA_FILE_FILTER

        var defaultPathStr = intent.getStringExtra("default_path")
        if (defaultPathStr.isNullOrEmpty()) {
            // TODO: rework or remove this setting
            defaultPathStr = sharedPrefs.getString("default_file_manager_path",
                Environment.getExternalStorageDirectory().path)
        }
        val defaultPath = File(defaultPathStr!!)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // check that the preferred path is inside a storage volume
            val vols = Utils.getStorageVolumes(this)
            var vol = vols.find { defaultPath.startsWith(it.path) }
            if (vol == null) {
                // looks like it wasn't
                Log.w(TAG, "default path set to \"$defaultPath\" but no such storage volume")
                vol = vols.firstOrNull()
                if (vol == null) {
                    Log.e(TAG, "can't find any volumes at all!")
                } else {
                    with(fragment!!) {
                        root = vol.path
                        goToDir(vol.path)
                    }
                }
            } else {
                with (fragment!!) {
                    root = vol.path
                    goToDir(defaultPath)
                }
            }
        } else {
            // Old device: go to preferred path but don't restrict root
            fragment!!.goToDir(defaultPath)
        }
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        // If up is pressed at the header element display the usual options menu as a popup menu
        // to make it usable on Android TV.
        var openMenu = false
        if (ev.action == KeyEvent.ACTION_DOWN && ev.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            val recycler: RecyclerView = findViewById(android.R.id.list)
            val holder = try {
                window.currentFocus?.let { recycler.getChildViewHolder(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
            openMenu = (holder is AbstractFilePickerFragment<*>.HeaderViewHolder)
        }
        if (openMenu) {
            PopupMenu(this, findViewById(R.id.context_anchor)).apply {
                setOnMenuItemClickListener {
                    this@FilePickerActivity.onOptionsItemSelected(it)
                }
                this@FilePickerActivity.inflateOptionsMenu(menu)
                show()
            }
            return true
        }

        return super.dispatchKeyEvent(ev)
    }

    private fun initDocPicker(root: Uri) {
        Log.v(TAG, "FilePickerActivity: showing document picker at \"$root\"")
        assert(fragment2 == null)
        fragment2 = MPVDocumentPickerFragment(root)
        supportActionBar?.show()

        val defaultPathStr = intent.getStringExtra("default_path")
        if (!defaultPathStr.isNullOrEmpty()) {
            fragment2!!.apply {
                goToDir(pathFromString(defaultPathStr))
            }
        }

        if (getFilterState())
            fragment2!!.filterPredicate = MEDIA_DOC_FILTER

        with (supportFragmentManager.beginTransaction()) {
            setReorderingAllowed(true)
            add(R.id.fragment_container_view, fragment2!!, null)
            runOnCommit { doUiTweaks() }
            commit()
        }
    }

    private fun showUrlDialog() {
        Log.v(TAG, "FilePickerActivity: showing url dialog")
        val helper = Utils.OpenUrlDialog(this)
        with (helper) {
            builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                if (homeMode)
                    playFile(helper.text)
                else
                    finishWithResult(RESULT_OK, helper.text)
            }
            builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            if (!homeMode)
                builder.setOnCancelListener { finishWithResult(RESULT_CANCELED) }
            create().show()
        }
    }

    private fun onBackPressedImpl() {
        fragment?.apply {
            if (!isBackTop) {
                goUp()
                return
            }
        }
        fragment2?.apply {
            if (!isBackTop) {
                goUp()
                return
            }
        }
        finishWithResult(RESULT_CANCELED)
    }

    private fun finishWithResult(code: Int, path: String? = null) {
        val result = Intent()
        fragment?.apply {
            result.putExtra("last_path", pathToString(currentDir))
        }
        fragment2?.apply {
            result.putExtra("last_path", pathToString(currentDir))
        }
        if (path != null) {
            result.putExtra("path", path)
            Log.v(TAG, "FilePickerActivity: picked \"$path\"")
        } else {
            Log.v(TAG, "FilePickerActivity: nothing picked")
        }
        setResult(code, result)
        finish()
    }

    // Listener methods

    override fun onFilePicked(file: File) {
        if (homeMode)
            playFile(file.absolutePath)
        else
            finishWithResult(RESULT_OK, file.absolutePath)
    }

    override fun onDirPicked(dir: File) {
        if (homeMode)
            fragment?.goToDir(dir)
        else
            finishWithResult(RESULT_OK, dir.absolutePath)
    }

    override fun onDocumentPicked(uri: Uri, isDir: Boolean) {
        assert(fragment2 != null)
        if (!isDir) {
            val path = fragment2!!.pathToString(uri)
            if (homeMode)
                playFile(path)
            else
                finishWithResult(RESULT_OK, path)
        }
    }

    private fun playFile(path: String) {
        val playerIntent = if (path.startsWith("content://")) {
            Intent(Intent.ACTION_VIEW, Uri.parse(path)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            Intent().putExtra("filepath", path)
        }
        playerIntent.setClass(this, MPVActivity::class.java)
        startActivity(playerIntent)
        if (!homeMode)
            finish()
    }

    override fun onPermissionGranted() {
        assert(fragment != null)
        // re-init file picker with correct paths
        initFilePicker()
    }

    override fun onCancelled() {
        if (!homeMode)
            finishWithResult(RESULT_CANCELED)
    }

    companion object {
        private const val TAG = "mpv"

        private val MEDIA_FILE_FILTER = FileFilter { file ->
            if (file.isDirectory) {
                val contents: Array<String> = file.list() ?: arrayOf()
                // filter hidden files due to stuff like ".thumbnails"
                contents.filterNot { it.startsWith('.') }.any()
            } else {
                Utils.MEDIA_EXTENSIONS.contains(file.extension.lowercase())
            }
        }

        private val MEDIA_DOC_FILTER = Predicate<DocumentPickerFragment.Document> { doc ->
            if (doc.isDirectory) {
                true
            } else {
                val ext = doc.displayName.substringAfterLast('.', "")
                Utils.MEDIA_EXTENSIONS.contains(ext.lowercase())
            }
        }

        // values for "skip" in intent
        const val URL_DIALOG = 0
        const val FILE_PICKER = 1
        const val DOC_PICKER = 2
    }
}
