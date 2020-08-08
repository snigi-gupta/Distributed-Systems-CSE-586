package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static Integer FILECOUNTER = 0;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    HashMap<String, String[]> hashMap = new HashMap<String, String[]>();

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");


    //synchronized functions for HashMap
    public synchronized void write_to_hash(String receivedText, String avdproposal){
        hashMap.put(receivedText, new String[]{avdproposal, "0"});
    }

    public synchronized void update_hash(String receivedText, String maxproposal){
        hashMap.put(receivedText, new String[]{maxproposal, "1"});

        // print hashmap
        String res = "";
        for(String k : hashMap.keySet()){
            res = res + k + ":" + hashMap.get(k)[0] + "," + hashMap.get(k)[1] + "\n";
        }
        Log.d("RESULT", res);


    }

    public synchronized List<Integer> write_to_array(){

        List<Integer> pa = new ArrayList<Integer>();
        for (String[] val : hashMap.values()) {
            pa.add(Integer.valueOf(val[0]));
        }
        System.out.println("BEFORE SORTING: " + pa);
        Collections.sort(pa);
        System.out.println("AFTER SORTING: " + pa);
        return pa;
    }

    public synchronized Object[] get_valid_ports(List<Integer> pa){
        HashMap<String, String> vk = new HashMap<String, String>();
        ArrayList<Integer> sortedValidPorts = new ArrayList<Integer>();
        ArrayList<String> toRemoveKeys = new ArrayList<String>();
        boolean flag = false;

        for (Integer port : pa) {
            for (String k : hashMap.keySet()) {
                if (hashMap.get(k)[0].equals(String.valueOf(port))){
                    if (hashMap.get(k)[1].equals("1")) {
                        vk.put(k,hashMap.get(k)[0]);
                        sortedValidPorts.add(Integer.valueOf(hashMap.get(k)[0]));
                        toRemoveKeys.add(k);
//                      hashMap.remove(k);
                    }
                    else {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag){
                break;
            }
        }
        Collections.sort(sortedValidPorts);
        return new Object[]{vk, sortedValidPorts, toRemoveKeys};

    }

    public synchronized void remove_from_hash(ArrayList<String> rk){
        for(int i=0; i<rk.size(); i++){
            Log.d("REMOVE","Removed MSG-" + rk.get(i) + " from hashMap" + "  PORT-" + hashMap.get(rk.get(i))[0]);

            hashMap.remove(rk.get(i));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portString = tel.getLine1Number().substring(tel.getLine1Number().length()-4);
        final String currPort = String.valueOf((Integer.parseInt(portString)*2));


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
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        try{

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Object[] serverPort = new Object[] {serverSocket, currPort};
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4) ;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.

                new ClientTask().execute(msg);
            }
        });

    }

    private class ServerTask extends AsyncTask<Object, String, Void> {
        Integer COUNT = 1;
        Integer varProp = 0;

        @Override
        protected Void doInBackground(final Object... sockets) {
            ServerSocket serverSocket = (ServerSocket) sockets[0];
            String receivedPort = "";
            String receivedText = "";
            do {
                try {
                    Log.d("SRV", "10000" + " Waiting for client...");
                    Socket socket = serverSocket.accept();
                    String currPort = (String) sockets[1];
                    Log.d("SRV", "Successfully connected!!");

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    String message = (String) in.readObject();
                    receivedText = message.split("#")[0];
                    receivedPort = message.split("#")[1];
//                    String receivedText = (String) in.readObject();
                    Log.d("SRV", "receivedText= " + receivedText);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    COUNT = Math.max(COUNT, varProp) + 1;
                    String avdproposal = COUNT + currPort;
                    out.writeObject(avdproposal);
                    out.flush();
//                    write_to_hash(receivedText,avdproposal);
                    Log.d("SRV", "AVD-" + currPort + "  AVD Proposal-" + avdproposal + "  for MSG-" + receivedText);

                    ObjectInputStream in2 = new ObjectInputStream(socket.getInputStream());
                    String maxproposal = (String) in2.readObject();
//                    update_hash(receivedText, maxproposal);
                    Log.d("SRV", "AVD-" + currPort + "  RECEIVED Proposal-" + maxproposal + "  for MSG-" + receivedText);

//                    List<Integer> portArray = write_to_array();

//                    Object[] obj = get_valid_ports(portArray);
//                    HashMap<String, String> validKeys = (HashMap<String, String>) obj[0];
//                    ArrayList<Integer> sortedValidPorts = (ArrayList<Integer>) obj[1];
//                    ArrayList<String> toRemoveKeys = (ArrayList<String>) obj[2];

//                    remove_from_hash(toRemoveKeys);

//                    for(int j=0; j<sortedValidPorts.size(); j++){
//                        for(String key : validKeys.keySet()){
//                            if (validKeys.get(key).equals(String.valueOf(sortedValidPorts.get(j)))){
//                                Log.d("SRV", "AVD-" + currPort + "  FINAL Proposal-" + validKeys.get(key) + "  for MSG-" + key);
//                                String theProposal = validKeys.get(key);
//                                varProp =  Integer.valueOf(theProposal.substring(0,theProposal.length()-5));
//                                publishProgress(key, theProposal);
//                                Log.d("WRITE_SUCCESS", "Written MSG-" + key + "for PORT-" + validKeys.get(key));
//                            }
//                        }
//                    }
                    publishProgress(receivedText, maxproposal);

                }
//                catch (EOFException e){
//                    Log.d("SRV-EXCEPTION", e.getMessage());
//                    e.printStackTrace();
//                }
//                catch (IOException e) {
//                    Log.d("SRV-EXCEPTION", e.getMessage());
//                    e.printStackTrace();
//                }
//                catch (ClassNotFoundException e) {
//                    Log.d("SRV-EXCEPTION", e.getMessage());
//                    e.printStackTrace();
//                }
                catch (Exception e){
                    Log.d("SRV-EXCEPTION", "got an exception  " + receivedPort + receivedText);
                    e.printStackTrace();
                }

            }while (true);

        }


        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            String maxProposal = strings[1].trim();
            remoteTextView.append(maxProposal + "**" + strReceived + "\t\n");

            ContentValues cv = new ContentValues();
            cv.put(KEY_FIELD, FILECOUNTER);
            cv.put(VALUE_FIELD, strReceived);
            Log.d("PU", "File counter:" + FILECOUNTER);
            FILECOUNTER ++;
            getContentResolver().insert(mUri,cv);
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            List<Integer> proposal = new ArrayList<Integer>();
            Integer allCounterConfirmation = 0;
            ArrayList<Socket> socketArray = new ArrayList<Socket>();

            try{
                String remoteport = "11108";
                Socket soc0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                socketArray.add(soc0);
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out0 = new ObjectOutputStream(soc0.getOutputStream());
                out0.writeObject(msgToSend+"#"+remoteport);
                out0.flush();
                Log.d("CLI", "CLIENT MSG-" + msgToSend + "  TO AVD-" + remoteport);


                ObjectInputStream in0 = new ObjectInputStream(soc0.getInputStream());
                String prop = (String) in0.readObject();
                Log.d("CLI", "Received proposal from AVD-" + remoteport + "  proposal-" + prop + "  for MSG-" + msgToSend);
                proposal.add(Integer.parseInt(prop));
                allCounterConfirmation ++;
                System.out.println("PROPOSAL ARRAY" + proposal);
                Log.d("LENGTH", String.valueOf(proposal.size()));

            }
//            catch (UnknownHostException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (EOFException e){
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (ClassNotFoundException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                allCounterConfirmation++;
                e.printStackTrace();
            }



            try{
                String remoteport = "11112";
                Socket soc1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                socketArray.add(soc1);
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out1 = new ObjectOutputStream(soc1.getOutputStream());
                out1.writeObject(msgToSend+"#"+remoteport);
                out1.flush();
                Log.d("CLI", "CLIENT MSG-" + msgToSend + "  TO AVD-" + remoteport);


                ObjectInputStream in1 = new ObjectInputStream(soc1.getInputStream());
                String prop = (String) in1.readObject();
                Log.d("CLI", "Received proposal from AVD-" + remoteport + "  proposal-" + prop + "  for MSG-" + msgToSend);
                proposal.add(Integer.parseInt(prop));
                allCounterConfirmation ++;
                System.out.println("PROPOSAL ARRAY" + proposal);
                Log.d("LENGTH", String.valueOf(proposal.size()));

            }
//            catch (UnknownHostException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (EOFException e){
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (ClassNotFoundException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                allCounterConfirmation++;
                e.printStackTrace();
            }


            try{
                String remoteport = "11116";
                Socket soc2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                socketArray.add(soc2);
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out2 = new ObjectOutputStream(soc2.getOutputStream());
                out2.writeObject(msgToSend+"#"+remoteport);
                out2.flush();
                Log.d("CLI", "CLIENT MSG-" + msgToSend + "  TO AVD-" + remoteport);


                ObjectInputStream in2 = new ObjectInputStream(soc2.getInputStream());
                String prop = (String) in2.readObject();
                Log.d("CLI", "Received proposal from AVD-" + remoteport + "  proposal-" + prop + "  for MSG-" + msgToSend);
                proposal.add(Integer.parseInt(prop));
                allCounterConfirmation ++;
                System.out.println("PROPOSAL ARRAY" + proposal);
                Log.d("LENGTH", String.valueOf(proposal.size()));

            }
//            catch (UnknownHostException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (EOFException e){
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (ClassNotFoundException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                allCounterConfirmation++;
                e.printStackTrace();
            }


            try{
                String remoteport = "11120";
                Socket soc3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                socketArray.add(soc3);
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out3 = new ObjectOutputStream(soc3.getOutputStream());
                out3.writeObject(msgToSend+"#"+remoteport);
                out3.flush();
                Log.d("CLI", "CLIENT MSG-" + msgToSend + "  TO AVD-" + remoteport);


                ObjectInputStream in3 = new ObjectInputStream(soc3.getInputStream());
                String prop = (String) in3.readObject();
                Log.d("CLI", "Received proposal from AVD-" + remoteport + "  proposal-" + prop + "  for MSG-" + msgToSend);
                proposal.add(Integer.parseInt(prop));
                allCounterConfirmation ++;
                System.out.println("PROPOSAL ARRAY" + proposal);
                Log.d("LENGTH", String.valueOf(proposal.size()));

            }
//            catch (UnknownHostException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (EOFException e){
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (ClassNotFoundException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                allCounterConfirmation++;
                e.printStackTrace();
            }


            try{
                String remoteport = "11124";
                Socket soc4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                socketArray.add(soc4);
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out4 = new ObjectOutputStream(soc4.getOutputStream());
                out4.writeObject(msgToSend+"#"+remoteport);
                out4.flush();
                Log.d("CLI", "CLIENT MSG-" + msgToSend + "  TO AVD-" + remoteport);


                ObjectInputStream in4 = new ObjectInputStream(soc4.getInputStream());
                String prop = (String) in4.readObject();
                Log.d("CLI", "Received proposal from AVD-" + remoteport + "  proposal-" + prop + "  for MSG-" + msgToSend);
                proposal.add(Integer.parseInt(prop));
                allCounterConfirmation ++;
                System.out.println("PROPOSAL ARRAY" + proposal);
                Log.d("LENGTH", String.valueOf(proposal.size()));

            }
//            catch (UnknownHostException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (EOFException e){
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
//            catch (ClassNotFoundException e) {
//                Log.d("EXCEPTION", e.getMessage());
//                e.printStackTrace();
//            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                allCounterConfirmation++;
                e.printStackTrace();
            }


            if (allCounterConfirmation == 5){
                String maxproposal = String.valueOf(Collections.max(proposal));
                ObjectOutputStream out;

                for(int i=0; i<allCounterConfirmation; i++){
                    try {
                        out = new ObjectOutputStream(socketArray.get(i).getOutputStream());
                        out.writeObject(maxproposal);
                        out.flush();
                        Log.d("CLI", "MAX Proposal sent TO AVD-" + socketArray.get(i) + "  MAX Proposal-" + maxproposal + "  for MSG-" + msgToSend);

                    }
                    catch (Exception e) {
                        Log.d("EXCEPTION","Go exception while sending MAX PROPOSAL");
                        e.printStackTrace();
                    }
                }

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
