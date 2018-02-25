package com.dev.pavelharetskiy.notes_kotlin.activities

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.content.SharedPreferences
import android.net.Uri
import com.dev.pavelharetskiy.notes_kotlin.R
import com.dev.pavelharetskiy.notes_kotlin.fragments.NotesFragment
import java.io.File
import android.content.Intent
import android.provider.Settings
import com.dev.pavelharetskiy.notes_kotlin.models.Note
import android.support.v4.app.FragmentTransaction
import android.provider.MediaStore
import android.widget.Toast
import android.os.Environment
import com.dev.pavelharetskiy.notes_kotlin.dialogs.CreateDialog
import com.dev.pavelharetskiy.notes_kotlin.orm.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val requestCodePhotoMake = 4554
    private val requestCodeFotoPick = 4124

    private var fragment: NotesFragment? = null
    private var isFavOnScreen = false
    private var uri: Uri? = null
    private var idToChangePhoto = -1
    private lateinit var directory: File
    private lateinit var spref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spref = getPreferences(MODE_PRIVATE)

        if (spref.contains("idToChange")) {
            idToChangePhoto = spref.getInt("idToChange", -1)
        }
        if (spref.contains("uri")) {
            uri = Uri.parse(spref.getString("uri", ""))
        }
        setListNotes()
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            CreateDialog().show(supportFragmentManager, null)
        }

        val toggle = ActionBarDrawerToggle(
                this, drawerlayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerlayout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawerlayout.isDrawerOpen(GravityCompat.START)) {
            drawerlayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startSetttingsActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startSetttingsActivity() {
        val openSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName))
        startActivity(openSettingsIntent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_notes -> setListAllNotesAndDoTransaction(false)
            R.id.nav_favorite_notes -> setListAllNotesAndDoTransaction(true)
            else -> isFavOnScreen = false
        }

        drawerlayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setListAllNotesAndDoTransaction(isListOfFavoriteNotes: Boolean) {
        fragment = NotesFragment()
        lateinit var notesList: List<Note>
        if (isListOfFavoriteNotes) {
            notesList = getListOfFavoriteNotes()
            isFavOnScreen = true
        } else {
            notesList = getListOfAllNotes()
            isFavOnScreen = false
        }
        fragment?.setNoteList(notesList)
        doTransaction(fragment)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isFavorite", isFavOnScreen)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            isFavOnScreen = savedInstanceState.getBoolean("isFavorite", false)
        }
    }

    override fun onResume() {
        super.onResume()
        fragment = NotesFragment()
        fragment?.setNoteList(if (isFavOnScreen) getListOfFavoriteNotes() else getListOfAllNotes())
        doTransaction(fragment)
    }

    override fun onStop() {
        super.onStop()
        val ed = spref.edit()
        if (idToChangePhoto != -1) {
            ed.putInt("idToChange", idToChangePhoto)
        }
        if (uri != null) {
            ed.putString("uri", uri?.path)
        }
        ed.apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {//возврат результата от камеры
        if (requestCode == requestCodePhotoMake) {//результат с камеры
            if (resultCode == Activity.RESULT_OK) {
                if (resultIntent == null) {
                    //добавлеие
                    try {
                        val note = getNoteById(idToChangePhoto)
                        val uriStr: String = if (uri.toString()[0] == '/') {
                            "file://" + uri.toString()
                        } else {
                            uri.toString()
                        }

                        note?.uri = uriStr
                        updateNote(note)
                        setListNotes()
                    } catch (ex: Exception) {
                        //
                    }

                }
            }
        } else if (requestCode == requestCodeFotoPick) {//возврат результата от выбора фото
            if (resultCode == Activity.RESULT_OK) {
                val note = getNoteById(idToChangePhoto)
                if (resultIntent != null) {
                    val imageUri = resultIntent.data
                    if (imageUri != null) {
                        note?.uri = imageUri.toString()
                        updateNote(note)
                        setListNotes()
                    }
                }
            }
        }
    }

    //===================================================//
    //Metods
    //===================================================//

    private fun doTransaction(fragment: NotesFragment?) {
        try {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.frameForFragments, fragment)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            ft.commit()
        } catch (ex: Exception) {
            //Error
        }

    }

    private fun setListNotes() {
        try {
            var notes: List<Note>? = null
            if (isFavOnScreen) {
                notes = getListOfFavoriteNotes()
            } else if (!isFavOnScreen) {
                notes = getListOfAllNotes()
            }
            if (notes != null && fragment != null) {
                fragment?.setNoteList(notes)
            }
        } catch (ex: Exception) {
            //
        }
    }

    fun startActivityDetail(id: Int) {
        val uriPath = getNoteById(id)?.uri
        if (uriPath != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(uriPath), "image/*")
            startActivity(intent)
        } else {
            Toast.makeText(this, "There is no photo..", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCameraActivity(id: Int) {
        generateFileUri()
        idToChangePhoto = id
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)//запускаем камеру с помощью интента
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, requestCodePhotoMake)
    }

    fun delPhotoNote(id: Int) {
        val note = getNoteById(id)
        note?.uri = null
        updateNote(note)
        setListNotes()
    }

    fun startPickPhotoActivity(id: Int) {
        idToChangePhoto = id
        val photoAddIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        photoAddIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

        startActivityForResult(photoAddIntent, requestCodeFotoPick)
    }

    private fun generateFileUri() {//генерируем путь к фото
        createDirectory()
        val file = File(directory.path + "/" + "photo_" + System.currentTimeMillis() + ".jpg")
        uri = Uri.fromFile(file)
    }

    private fun createDirectory() {//создаем папку для фото
        directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "NotesAppKotlin")
        if (!directory.exists()) directory.mkdirs()
    }

    fun updateScreen() {
        if (fragment != null) {
            try {
                if (!isFavOnScreen) {
                    fragment?.updateNoteList(getListOfAllNotes())
                } else if (isFavOnScreen) {
                    fragment?.updateNoteList(getListOfFavoriteNotes())
                }
            } catch (ex: Exception) {
                Toast.makeText(this, "error..", Toast.LENGTH_SHORT).show()
            }
        }
    }
}