package tw.frb.sharecam;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static Boolean cmdThreadRun;
    private static Thread cmdThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cmdThreadRun = false;
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

        if (!cmdThreadRun) {
            cmdThreadRun = true;
            cmdThread = new Thread(new Command());
            cmdThread.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        cmdThreadRun = false;
    }

    class Command implements Runnable {

        private static final String TAG = "Command";
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private StringBuffer stringBuffer;
        private StringBuffer headerBuffer;
        private StringBuffer bodyBuffer;
        Matcher matcher;
        Pattern lengthPattern = Pattern.compile("Content-Length: (\\d+)");
        private String username = "";
        private String password = "";
        private String host = "192.168.0.101";
        private int port = 3000;

        public Command() {
            stringBuffer = new StringBuffer();
            headerBuffer = new StringBuffer();
            bodyBuffer = new StringBuffer();
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub

            try {
                socket = new Socket(host, port);
                socket.setKeepAlive(true);

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                String body = "username=" + username + "&password=" + password;

                stringBuffer.append("POST /device/connect HTTP/1.1\r\n");
                stringBuffer.append("Content-Length: " + body.length() + "\r\n");
                stringBuffer.append("\r\n");
                stringBuffer.append(body);

                outputStream.write(stringBuffer.toString().getBytes());
                outputStream.flush();

                int currByte = -1;
                int length = -1;

                while (cmdThreadRun && (currByte = inputStream.read()) > -1) {
                    if (length == -1) {
                        headerBuffer.append((char)currByte);
                        if (headerBuffer.length() >= 4 && headerBuffer.indexOf("\r\n\r\n") == headerBuffer.length() - 4) {
                            matcher = lengthPattern.matcher(headerBuffer.toString());
                            length = matcher.find() ? Integer.valueOf(matcher.group(1)) : 0;
                            if (length == 0)
                                break;
                        }
                    } else {
                        bodyBuffer.append((char)currByte);
                        if (bodyBuffer.length() >= length)
                            break;
                    }
                }

                while (cmdThreadRun) {

                }

                outputStream.close();
                inputStream.close();
                socket.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
