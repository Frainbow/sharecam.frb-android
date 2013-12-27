package tw.frb.sharecam;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ServerFragment extends Fragment {

    private static final String TAG = "ServerActivity";
    private ServerDialogFragment serverFragment;
    private ShareCam shareCam;
    private CommandRunnable cmdRunnable;
    private Thread cmdThread;
    private PowerManager.WakeLock wakeLock;

    final Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            TextView tvServerMessage = (TextView)getActivity().findViewById(R.id.tvServerMessage);
            boolean status = msg.getData().getBoolean("status");
            int port = msg.getData().getInt("client_port");

            if (tvServerMessage != null) {
                if (status)
                    tvServerMessage.setText(getResources().getString(R.string.server_msg_port) + port);
                else
                    tvServerMessage.setText(getResources().getString(R.string.server_msg_error));
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_server, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        wakeLock = ((MainActivity)getActivity()).wakeLock;

        serverFragment = new ServerDialogFragment();
        serverFragment.setCancelable(false);
        serverFragment.callback = new ServerDialogFragment.Callback() {

            @Override
            public void positive() {
                startServer();
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        wakeLock.acquire(150 * 1000);

        if (serverFragment.username.length() == 0) {
            serverFragment.show(getFragmentManager(), "server");
        } else {
            startServer();
        }
    }

    @Override
    public void onStop() {
        stopServer();

        if (wakeLock.isHeld())
            wakeLock.release();

        super.onStop();
    }

    public void startServer() {
        shareCam = new ShareCam(getActivity());
        cmdRunnable = new CommandRunnable(shareCam, serverFragment.username, serverFragment.password, handler);
        cmdThread = new Thread(cmdRunnable);
        cmdThread.start();
    }

    public void stopServer() {

        if (cmdRunnable != null)
            cmdRunnable.kill();

        try {
            if (cmdThread != null)
                cmdThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (shareCam != null)
            shareCam.release();
    }
}

