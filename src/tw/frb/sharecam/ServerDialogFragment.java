package tw.frb.sharecam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

public class ServerDialogFragment extends DialogFragment {

    interface Callback {
        void positive();
    }

    Callback callback;
    String username = "";
    String password = "";
    private EditText etUsername;
    private EditText etPassowrd;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder
        .setTitle(getResources().getString(R.string.server_dialog_title))
        .setView(inflater.inflate(R.layout.dialog_management, null))
        .setPositiveButton(getResources().getString(R.string.server_dialog_button), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // TODO Auto-generated method stub
                etUsername = (EditText)getDialog().findViewById(R.id.etUsername);
                etPassowrd = (EditText)getDialog().findViewById(R.id.etPassword);

                username = etUsername.getText().toString();
                password = etPassowrd.getText().toString();

                if (callback != null)
                    callback.positive();
            }

        });

        return builder.create();
    }
}
