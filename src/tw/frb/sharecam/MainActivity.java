package tw.frb.sharecam;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ServerDialogFragment serverFragment;
    private ShareCam shareCam;
    private CommandRunnable cmdRunnable;
    private Thread cmdThread;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    final Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            TextView tvServerMessage = (TextView)findViewById(R.id.tvServerMessage);
            boolean status = msg.getData().getBoolean("status");
            int port = msg.getData().getInt("client_port");

            if (status)
                tvServerMessage.setText(getResources().getString(R.string.server_msg_port) + port);
            else
                tvServerMessage.setText(getResources().getString(R.string.server_msg_error));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "sharecam");

        this.serverFragment = new ServerDialogFragment();
        this.serverFragment.setCancelable(false);
        this.serverFragment.callback = new ServerDialogFragment.Callback() {

            @Override
            public void positive() {
                startServer();
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        this.wakeLock.acquire(150 * 1000);

        if (this.serverFragment.username.length() == 0) {
            this.serverFragment.show(getFragmentManager(), "server");
        } else {
            startServer();
        }
    }

    @Override
    public void onStop() {
        stopServer();

        if (this.wakeLock.isHeld())
            this.wakeLock.release();

        super.onStop();
    }

    public void startServer() {
        this.shareCam = new ShareCam(this);
        this.cmdRunnable = new CommandRunnable(shareCam, serverFragment.username, serverFragment.password, handler);
        this.cmdThread = new Thread(cmdRunnable);
        this.cmdThread.start();
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
