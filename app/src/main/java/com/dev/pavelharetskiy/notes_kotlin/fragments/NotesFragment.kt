package com.dev.pavelharetskiy.notes_kotlin.fragments

import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import com.dev.pavelharetskiy.notes_kotlin.adapters.NotesAdapter
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.dev.pavelharetskiy.notes_kotlin.R
import com.dev.pavelharetskiy.notes_kotlin.models.Note
import kotlinx.android.synthetic.main.fragment_notes.view.*


class NotesFragment : Fragment() {

    private var notesAdapter: NotesAdapter? = null
    private var noteList: List<Note>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)

        notesAdapter = NotesAdapter(noteList)

        view.recyclerViewNotesFragment.layoutManager = LinearLayoutManager(activity)
        view.recyclerViewNotesFragment.adapter = notesAdapter

        return view
    }

    fun setNoteList(noteList: List<Note>?) {
        this.noteList = noteList
        if (noteList != null) {
            notesAdapter?.setNoteList(noteList)
            notesAdapter?.notifyDataSetChanged()
        }
    }
}
