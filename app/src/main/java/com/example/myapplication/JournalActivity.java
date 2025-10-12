package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JournalActivity extends AppCompatActivity {

    private EditText etNote;
    private ListView listViewNotes;
    private ArrayList<JournalNote> noteList;
    private ArrayList<String> noteIds;
    private JournalAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private CollectionReference notesRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        etNote = findViewById(R.id.etNote);
        listViewNotes = findViewById(R.id.listViewNotes);

        noteList = new ArrayList<>();
        noteIds = new ArrayList<>();

        adapter = new JournalAdapter(this, noteList);
        listViewNotes.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notesRef = db.collection("users").document(userId).collection("notes");

        loadNotes();

        etNote.setOnClickListener(v -> showNoteDialog(null, null));

        listViewNotes.setOnItemClickListener((parent, view, position, id) -> {
            String noteId = noteIds.get(position);
            JournalNote selectedNote = noteList.get(position);
            showNoteDialog(noteId, selectedNote);
        });
    }

    private void loadNotes() {
        notesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            noteList.clear();
            noteIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String id = doc.getId();
                String title = doc.getString("title");
                String text = doc.getString("text");

                com.google.firebase.Timestamp ts = doc.getTimestamp("date");
                String date = (ts != null)
                        ? new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(ts.toDate())
                        : "";

                noteList.add(new JournalNote(id, title, text, date));
                noteIds.add(id);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showNoteDialog(String noteId, JournalNote oldNote) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottomsheet_note);

        EditText etTitleInput = dialog.findViewById(R.id.etDialogTitleInput);
        EditText etNoteInput = dialog.findViewById(R.id.etDialogNote);
        TextView dialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnDelete = dialog.findViewById(R.id.btnDelete);

        if (noteId != null && oldNote != null) {
            if (etTitleInput != null) etTitleInput.setText(oldNote.getTitle());
            if (etNoteInput != null) etNoteInput.setText(oldNote.getText());
            if (dialogTitle != null) dialogTitle.setText("Edit Note");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String titleText = etTitleInput.getText().toString().trim();
                String noteText = etNoteInput.getText().toString().trim();

                if (titleText.isEmpty() || noteText.isEmpty()) {
                    showCustomToast("Title and Note cannot be empty");
                    return;
                }

                if (noteId == null) {
                    Map<String, Object> note = new HashMap<>();
                    note.put("title", titleText);
                    note.put("text", noteText);
                    note.put("date", com.google.firebase.Timestamp.now());

                    notesRef.add(note).addOnSuccessListener(ref -> {
                        loadNotes();
                        Log.d("JournalActivity", "Note added!");
                        dialog.dismiss();
                    });
                } else {
                    DocumentReference docRef = notesRef.document(noteId);
                    docRef.update("title", titleText, "text", noteText).addOnSuccessListener(aVoid -> {
                        loadNotes();
                        Log.d("JournalActivity", "Note updated!");
                        dialog.dismiss();
                    });
                }
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (noteId != null) {
                    notesRef.document(noteId).delete().addOnSuccessListener(aVoid -> {
                        loadNotes();
                        Log.d("JournalActivity", "Note deleted!");
                        dialog.dismiss();
                    });
                }
            });
        }

        dialog.show();
    }

    private static class JournalNote {
        private String id;
        private String title;
        private String text;
        private String date;

        public JournalNote(String id, String title, String text, String date) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.date = date;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getText() { return text; }
        public String getDate() { return date; }
    }

    private class JournalAdapter extends android.widget.ArrayAdapter<JournalNote> {
        public JournalAdapter(@Nullable android.content.Context context, ArrayList<JournalNote> notes) {
            super(context, 0, notes);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.note_item, parent, false);
            }

            JournalNote note = getItem(position);

            TextView tvTitle = convertView.findViewById(R.id.tvNote);
            TextView tvDate = convertView.findViewById(R.id.tvDate);

            if (note != null) {
                tvTitle.setText(note.getTitle());
                tvDate.setText(note.getDate());
            }

            return convertView;
        }
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
