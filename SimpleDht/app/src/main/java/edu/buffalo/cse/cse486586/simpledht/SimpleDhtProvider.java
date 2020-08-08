package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import static android.content.Context.SEARCH_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    // This is the server port
    static final int SERVER_PORT = 10000;
    // Ports of all the 5 emulators
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    // hash value of the running emulator
    static BigInteger portHashCode;
    // port number of the running emulator
    static String currPort;
    // A key:value hash map for port:hash values of all 5 emulators
    static HashMap<String, BigInteger> portMap = new HashMap<String, BigInteger>();
    // Local emulator storage for key:value values
    static HashMap<String, String> emulatorStorage = new HashMap<String, String>();
    // successor and predecessor hash value
    BigInteger sport;
    BigInteger pport;

    // Tree set with sorted hash values of all 5 emulators
    TreeSet<BigInteger> ts = new TreeSet<BigInteger>();

    /**
     * Function to get node that will store the message based on the key's hash value.
     *
     * @param key
     * @param value
     * @return Object[] with successor port and hash value
     */
    public Object[] getSuccHashAndPort(String key, String value){
        Object[] obj = new Object[]{};

        try {
            if(value==null){
                value = "no val";
            }
            Log.d("INSERT", "Key= " + key + " Value= " + value);
            TreeSet<BigInteger> temp_ts = new TreeSet<BigInteger>(ts);
            //            temp_ts = ts;
            BigInteger keyHash = null;

            keyHash = new BigInteger(genHash(key), 16);

            temp_ts.add(keyHash);
            Log.d("INSERT", "key-hash generated and added to temp_ts");
            int keyIndex = 0;

            ArrayList<BigInteger> temp_ts_list;
            temp_ts_list = new ArrayList<BigInteger>(temp_ts);
            for (int i = 0; i < temp_ts_list.size(); i++) {
                if (temp_ts_list.get(i).compareTo(keyHash) == 0) {
                    keyIndex = i;
                }
            }

            Log.d("INSERT", "Position of key-hash is= " + keyIndex);
            Log.d("INSERT", "key-hash= " + keyHash);
            BigInteger succHash;
            if (keyIndex > ts.size() - 1) {
                succHash = temp_ts_list.get(0);
            } else {
                succHash = temp_ts_list.get(keyIndex + 1);
            }


            String succPort = "";

            System.out.println(portMap);

            for (Map.Entry<String, BigInteger> entry : portMap.entrySet()) {
                if (entry.getValue().equals(succHash)) {
                    succPort = entry.getKey();
                    //                    System.out.println(succPort);
                }
            }
            obj = new Object[]{succPort,succHash};

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return obj;

    }

    /**
     * Function to delete a key:value pain from emulator storage
     * If key not in local emulator, delete from the emulator storage for that particular port
     *
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return None
     */

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        try{
            Log.d("DELETE", "Inside Delete");
            Object[] ob = getSuccHashAndPort(selection, null);
            String remoteport = (String)ob[0];
            if(remoteport.equals(currPort)){
                emulatorStorage.remove(selection);
            }
            else{
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                Log.d("DELETE", "Socket created");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(new Object[]{"DELETE", selection});
                out.flush();
                Log.d("DELETE", "Request sent to " + remoteport);
            }


        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Function to insert key:value into emulator storage
     *
     * @param uri
     * @param values
     * @return uri
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        try {
            Log.d("INSERT", "Inside Insert");
            String key = values.get("key").toString();
            String value = values.get("value").toString();

            Object[] ob = getSuccHashAndPort(key, value);
            String succPort = (String)ob[0];
            BigInteger succHash = (BigInteger)ob[1];
            Log.d("INSERT", "key-hash will be sent to AVD= " + succPort + " Hash= " + succHash);

            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(succPort));
            Log.d("INSERT", "Socket Created");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new Object[]{"INSERT", key, value});
            out.flush();
            Log.d("INSERT", "Sent to " + succPort);

//            String filePath = getContext().getCacheDir().toString() + "/" + values.get("key").toString() + ".txt";
//            Log.d("I-PRNT",filePath);
//            FileWriter fw = new FileWriter(new File(filePath));
//            fw.write(values.get("value").toString());
//            fw.flush();
//            fw.close();
        }
        catch (Exception e){
            e.printStackTrace();
            Log.d("INSERT-EXP", e.getMessage());
        }

        return uri;
//        return null;
    }

    /**
     * This function runs when emualtor is run
     * It gets the port and emulator number for the running port and performs default initializations.
     * @return
     */
    @Override
    public boolean onCreate() {

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portString = tel.getLine1Number().substring(tel.getLine1Number().length()-4);
        currPort = String.valueOf((Integer.parseInt(portString)*2));

        Log.d("INIT", "PORT: " + currPort + " PORT STRING: " + portString);

        try{

            portHashCode = new BigInteger(genHash(portString),16);
            sport = portHashCode;
            pport = portHashCode;
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Object[] serverPort = new Object[] {serverSocket, currPort};
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverPort);

            portMap.put(currPort,portHashCode);
            ts.add(portHashCode);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * Function to sort all hash values of all the ports
     * @return
     */
    public Void sortPorts(){

        ArrayList<BigInteger> tempList;
        tempList = new ArrayList<BigInteger>(ts);
        for(int i=0; i<tempList.size(); i++){
            if(tempList.get(i).compareTo(portHashCode) == 0){
                if(i==0){
                    pport = tempList.get(tempList.size()-1);
                }
                else{
                    pport = tempList.get(i-1);
                }
                if(i==tempList.size()-1){
                    sport = tempList.get(0);
                }
                else{
                    sport = tempList.get(i+1);
                }

            }
        }
        return null;
    }

    /**
     * Function to create sockets on client side and send update tree set and port map to all
     * emulators on the server end
     *
     */
    public void sendToAll(){


        for (int i=0; i<5; i++) {
            try {
                String remoteport = REMOTE_PORT[i];
                Log.d("REMOTE PORT ", remoteport);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                Log.d("SEND ALL", "Socket Created");

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(new Object[]{"SRV", ts, portMap});
                out.flush();
                Log.d("SEND ALL", "Sent to " + remoteport);

            } catch (Exception e) {
//                e.printStackTrace();
                Log.d("SEND ALL EXP", "Port FAILED ");
            }
        }

    }

    /**
     * Server-side code
     */

    private class ServerTask extends AsyncTask<Object, String, Void> {

        @Override
        protected Void doInBackground(final Object... sockets) {
            ServerSocket serverSocket = (ServerSocket) sockets[0];

            ContentValues cv = new ContentValues();
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
            uriBuilder.scheme("content");
            Uri mUri = uriBuilder.build();

            String label = "";


            do {
                try {
                    Log.d("SRV", "10000" + " Waiting for client...");
                    Socket socket = serverSocket.accept();

                    Log.d("SRV", "Successfully connected!!");

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Object[] message = (Object[]) in.readObject();


                    label = (String) message[0];
                    Log.d("SRV", "Label= " + label);

                    if ("NEW".equals(label)){

                        Log.d("SRV", "Label= " + message[0] + " Hash= " + message[1] + " Port= " + message[2]);
                        portMap.put((String) message[2], (BigInteger) message[1]);
//                        tm.put((String) message[2], (String) message[1]);
                        ts.add((BigInteger) message[1]);
                        sortPorts();
                        System.out.println(ts);

                        sendToAll();
                    }
                    else if ("SRV".equals(label)){

                        ts = (TreeSet) message[1];
                        portMap = (HashMap<String, BigInteger>) message[2];
                        Log.d("CLI", "Received Tree Set and Updated Port Map");
                        sortPorts();

                        Log.d("SRV", "Calling sortports for NEW SUCC and PRED");
                        System.out.println(ts);
                    }
                    else if("INSERT".equals(label)){
                        Log.d("SRV", "Received Insert from ");
                        String k = (String)message[1];
                        String v = (String)message[2];
                        emulatorStorage.put(k,v);
                    }
                    else if("GLOBAL".equals(label)){
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(emulatorStorage);
                        out.flush();
                        Log.d("SRV", "Sending emulator storage");
                    }
                    else if("OTHER".equals(label)){
                        String k = (String)message[1];
                        String v = emulatorStorage.get(k);
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(v);
                        out.flush();
                        Log.d("SRV", "For Key= " + k + " Value found was= " + v);

                    }
                    else if("DELETE".equals(label)){
                        String k = (String)message[1];
                        emulatorStorage.remove(k);
                        Log.d("SRV", "For Key= " + k + "Value deleted from emulatorStorage");
                    }

                }
                catch (Exception e){

                    e.printStackTrace();
                }

            }while (true);

        }
    }

    /**
     * Client-side code
     */
    public class ClientTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            Object[] msgToSend = new Object[]{"NEW", portHashCode, currPort};

            try{
                String remoteport = "11108";
                Socket soc0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                Log.d("CLI", "AVD-" + remoteport + "  Client started...");

                ObjectOutputStream out0 = new ObjectOutputStream(soc0.getOutputStream());
                out0.writeObject(msgToSend);
                out0.flush();
                Log.d("CLI", "CLIENT Hash= " + msgToSend[1] + " Port= " + msgToSend[2] + " TO AVD= " + remoteport);

            }
            catch (Exception e){
                Log.d("EXCEPTION", "got an exception");
                e.printStackTrace();
            }
            return null;
        }
    }


    /**
     * Function to perform a query and get value for the required key from the emulator storage
     * If selection = @, return all key,value in local emulator storage
     * If selection = *, return all key,value in all emulator storage
     * If selection = key, return key,value from the emulator storage it is stored in
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub

        try{
            MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});

            if (selection.equals("@")){
                for(Map.Entry<String, String> en: emulatorStorage.entrySet()){
                    String k = en.getKey();
                    String v = en.getValue();
                    mc.addRow(new Object[]{k,v});
                }
                Log.d("QUERY", "Local Dumped");
            }
            else if(selection.equals("*")){
                for (int i=0; i<5; i++) {
                    try {
                        String remoteport = REMOTE_PORT[i];
                        Log.d("QUERY", remoteport);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                        Log.d("QUERY", "Socket created");

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(new Object[]{"GLOBAL"});
                        out.flush();
                        Log.d("QUERY", "Request sent to " + remoteport);

                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        HashMap<String, String> message = (HashMap<String, String>) in.readObject();
                        for(Map.Entry<String, String> en : message.entrySet()){
                            String k = en.getKey();
                            String v = en.getValue();
                            mc.addRow(new Object[]{k,v});
                        }

                    }
                    catch (Exception e) {
//                e.printStackTrace();
                        Log.d("SEND ALL EXP", "Port FAILED ");
                    }
                }
                Log.d("QUERY", "Global Dumped");

            }
            else{
                String val = emulatorStorage.get(selection);
                if (val == null){
                    Object[] ob = getSuccHashAndPort(selection,val);
                    String remoteport = (String) ob[0];

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
                    Log.d("QUERY", "Socket created");

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Object[]{"OTHER", selection});
                    out.flush();
                    Log.d("QUERY", "Request sent to " + remoteport + "to get value from OTHER AVD");

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    val = (String)in.readObject();
                }
                mc.addRow(new Object[]{selection,val});

            }
            return mc;

//            String filePath = getContext().getCacheDir().toString() + "/" + selection + ".txt";
//            Log.d("Q-PRNT", filePath);
//            File file = new  File (filePath);
//            if (file.exists()){
//                BufferedReader br = new BufferedReader(new FileReader(filePath));
//                String val = br.readLine();
//                Log.d("Q2-PRNT", val);
//                MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
//                mc.addRow(new Object[]{selection,val});
//                return mc;
//            }

        }
        catch (Exception e){
            Log.d("EXP", e.getMessage());
        }
        
        Log.v("query", selection);
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
