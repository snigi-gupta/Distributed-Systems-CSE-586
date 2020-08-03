package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
//        findViewById(R.id.button1).setOnClickListener(
//                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        try{

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4) ;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
//                TextView localTextView = (TextView) findViewById(R.id.textView1);
//                localTextView.append("\t" + msg); // This is one way to display a string.

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
//                return;
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Integer COUNT = 0;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            try {

                do {

                    Log.d("MSG", "10000" + " Waiting for client...");
                    ServerSocket serverSocket = sockets[0];
                    Socket soc = serverSocket.accept();
                    Log.d("MSG", "Successfully connected!!");

                    BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                    String receivedText = in.readLine();

//                    Thread.sleep(500);
//                    publishProgress(receivedText,COUNT.toString());
//                    COUNT ++;

                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
                    out.write("rcv");
                    out.flush();
                    publishProgress(receivedText,COUNT.toString());
                    COUNT ++;
                    soc.close();
//                    if (receivedText != null){
//                        Log.d("MSG", "10000" + " Received text:" + receivedText);
//                        publishProgress(receivedText,COUNT.toString());
//                        COUNT ++;
//                    }
//
//                    out.write("rcv");

                    soc.close();

                } while (true);
            }
            catch(Exception e){
                Log.d(TAG, e.getMessage());
            }
            return null;

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
//            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            String count = strings[1].trim();

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */
            ContentValues cv = new ContentValues();
            cv.put(KEY_FIELD, count);
            cv.put(VALUE_FIELD, strReceived);
            getContentResolver().insert(mUri,cv);
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            for (int i=0; i<5; i++) {
                try{
                    String remoteport = REMOTE_PORT[i];
                    Log.d("PORT", remoteport);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(remoteport));
                    Log.d("SOC", "CREATED");
                    String msgToSend = msgs[0];
                    Log.d("MSG", remoteport + "-Client started...");

                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    out.write(msgToSend);
                    out.flush();
                    Log.d("MSG", remoteport + "-sent text: " + msgToSend);

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    BufferedReader in =  new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String ack = in.readLine();
                    if (ack.equals("rcv")) {
                        socket.close();
                    }
//                    Thread.sleep(500);
//                    socket.close();
                }
                 catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                }
                catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    Log.e(TAG, "ClientTask socket IOException");
                }
//                catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }

            return null;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
