package tw.frb.sharecam;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static Boolean cmdThreadRun;
    private static Thread cmdThread;
    private ShareCam shareCam;
    private static ServerDialogFragment serverFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cmdThreadRun = false;

        serverFragment = new ServerDialogFragment();
        serverFragment.setCancelable(false);
        serverFragment.show(getFragmentManager(), "server");
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
        super.onStop();

        cmdThreadRun = false;
        shareCam.release();
    }

    public void startServer() {
        shareCam = new ShareCam(getApplicationContext(), (FrameLayout)findViewById(R.id.camera_preview));

        if (!cmdThreadRun) {
            cmdThreadRun = true;
            cmdThread = new Thread(new Command());
            cmdThread.start();
        }
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
        Pattern requestPattern = Pattern.compile("^(GET|POST) ([^ ]+) HTTP\\/1\\.[01]");
        Pattern lengthPattern = Pattern.compile("Content-Length: (\\d+)");
        private String username = "";
        private String password = "";
        private String host = "192.168.0.101";
        private int port = 3000;

        public Command() {
            username = serverFragment.username;
            password = serverFragment.password;
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
                String method = "";
                URI uri;

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
                    method = "";
                    uri = null;
                    currByte = -1;
                    length = -1;
                    headerBuffer.setLength(0);
                    bodyBuffer.setLength(0);

                    while (cmdThreadRun && (currByte = inputStream.read()) > -1) {
                        if (length == -1) {
                            headerBuffer.append((char)currByte);
                            if (headerBuffer.length() >= 4 && headerBuffer.indexOf("\r\n\r\n") == headerBuffer.length() - 4) {
                                matcher = requestPattern.matcher(headerBuffer.toString());
                                if (matcher.find()) {
                                    method = matcher.group(1);
                                    uri = new URI(matcher.group(2));
                                } else {
                                    break;
                                }
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

                    if (!cmdThreadRun)
                        break;

                    stringBuffer.setLength(0);

                    if (method.compareTo("GET") == 0 && uri.getPath().compareTo("/snapshot") == 0) {
                        shareCam.jpeg = null;
                        shareCam.takePicture();

                        while (cmdThreadRun && shareCam.jpeg == null);
                        if (!cmdThreadRun)
                            break;

                        stringBuffer.append("HTTP/1.1 200 OK\r\n");
                        stringBuffer.append("Content-Type: image/jpeg\r\n");
                        stringBuffer.append("Content-Length: " + shareCam.jpeg.length + "\r\n");
                        stringBuffer.append("\r\n");

                        outputStream.write(stringBuffer.toString().getBytes());
                        outputStream.write(shareCam.jpeg);

                        continue;
                    }

                    stringBuffer.append("HTTP/1.1 400 BAD REQUEST\r\n");
                    stringBuffer.append("Content-Length: 0\r\n\r\n");

                    outputStream.write(stringBuffer.toString().getBytes());
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
