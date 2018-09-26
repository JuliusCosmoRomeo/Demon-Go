package com.github.demongo.Map;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

interface NameDialogChosenListener {
    public void nameChosen(String name);
}

class NameDialog {
    NameDialog(Context context, final NameDialogChosenListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Sehr gut, du hast den Dämonen gefangen! Wie möchtest du ihn nennen?");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.nameChosen(input.getText().toString());
            }
        });

        builder.show();
    }
}
