package tw.frb.sharecam;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private CommandRunnable cmdRunnable;
    private Thread cmdThread;
    private ServerDialogFragment serverFragment;

    static ShareCam shareCam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.serverFragment = new ServerDialogFragment();
        this.serverFragment.setCancelable(false);
        this.serverFragment.show(getFragmentManager(), "server");
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
    }

    @Override
    public void onStop() {
        stopServer();
        super.onStop();
    }

    public void startServer() {
        shareCam = new ShareCam(getApplicationContext(), (FrameLayout)findViewById(R.id.camera_preview));
        this.cmdRunnable = new CommandRunnable(serverFragment.username, serverFragment.password);
        this.cmdThread = new Thread(cmdRunnable);
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
