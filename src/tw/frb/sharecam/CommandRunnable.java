package tw.frb.sharecam;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class CommandRunnable implements Runnable {
    private static final String TAG = "CommandRunnable";
    private volatile boolean isRunning = true;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private StringBuffer stringBuffer;
    private StringBuffer headerBuffer;
    private StringBuffer bodyBuffer;
    private Matcher matcher;
    private static Pattern requestPattern = Pattern.compile("^(GET|POST) ([^ ]+) HTTP\\/1\\.[01]");
    private static Pattern lengthPattern = Pattern.compile("Content-Length: (\\d+)");
    private String username = "";
    private String password = "";
    private static String host = "sharecam.frb.tw";
    private static int port = 3000;
    private ShareCam shareCam;
    private Handler handler;
    private Message msg;
    private Bundle bundle;

    public CommandRunnable(ShareCam shareCam, String username, String password, Handler handler) {
        this.shareCam = shareCam;
        this.username = username;
        this.password = password;
        this.handler = handler;
        this.msg = handler.obtainMessage();
        this.bundle = new Bundle();
        this.stringBuffer = new StringBuffer();
        this.headerBuffer = new StringBuffer();
        this.bodyBuffer = new StringBuffer();
    }

    public void kill() {
        this.isRunning = false;

        try {
            if (inputStream != null)
                socket.shutdownInput();
            else
                socket.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 0);
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

            while (isRunning && (currByte = inputStream.read()) > -1) {
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

            JSONObject bodyObject = new JSONObject(bodyBuffer.toString());
            bundle.putBoolean("status", true);
            bundle.putInt("client_port", bodyObject.has("client_port") ? bodyObject.getInt("client_port") : -1);
            msg.setData(bundle);
            handler.sendMessage(msg);

            while (isRunning) {
                method = "";
                uri = null;
                currByte = -1;
                length = -1;
                headerBuffer.setLength(0);
                bodyBuffer.setLength(0);

                while (isRunning && (currByte = inputStream.read()) > -1) {
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

                if (!isRunning)
                    break;

                stringBuffer.setLength(0);

                if (method.compareTo("GET") == 0 && uri.getPath().compareTo("/client/snapshot") == 0) {
                    shareCam.jpeg = null;
                    shareCam.takePicture();

                    while (isRunning && shareCam.jpeg == null);
                    if (!isRunning)
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
            bundle.putBoolean("status", false);
            bundle.putInt("client_port", -1);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }
}
