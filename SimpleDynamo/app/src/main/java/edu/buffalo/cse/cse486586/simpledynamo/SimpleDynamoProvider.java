package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	// This is the server port
	static final int SERVER_PORT = 10000;
	// Ports of all the 5 emulators
	static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
	static  final String[] AVD_ORDER = {"5562", "5556", "5554", "5558", "5560"};
	// hash value of the running emulator
	static BigInteger portHashCode;
	// port number of the running emulator
	static String currPort;
	// A key:value hash map for port:hash values of all 5 emulators
	static HashMap<String, BigInteger> portMap = new HashMap<String, BigInteger>();
	// Local emulator storage for key:value values
	static HashMap<String, Object[]> emulatorStorage = new HashMap<String, Object[]>();
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
			Log.d("FUNC", "Key= " + key + " Value= " + value);
			TreeSet<BigInteger> temp_ts = new TreeSet<BigInteger>(ts);
			BigInteger keyHash = null;

			keyHash = new BigInteger(genHash(key), 16);

			temp_ts.add(keyHash);
			Log.d("FUNC", "key-hash generated and added to temp_ts");
			int keyIndex = 0;

			ArrayList<BigInteger> temp_ts_list;
			temp_ts_list = new ArrayList<BigInteger>(temp_ts);
			for (int i = 0; i < temp_ts_list.size(); i++) {
				if (temp_ts_list.get(i).compareTo(keyHash) == 0) {
					keyIndex = i;
					break;
				}
			}

			// get other two hashes
			int otherKeyIndex1 = keyIndex + 2;
			int otherKeyIndex2 = keyIndex + 3;

			Log.d("FUNC", "Position of key-hash is= " + keyIndex);
			Log.d("FUNC", "key-hash= " + keyHash);
			BigInteger succHash;
			BigInteger succHash1;
			BigInteger succHash2;

			if (keyIndex > ts.size() - 1) {
				succHash = temp_ts_list.get(0);
			} else {
				succHash = temp_ts_list.get(keyIndex + 1);
			}

			succHash1 = temp_ts_list.get(otherKeyIndex1%(ts.size()+1));
			succHash2 = temp_ts_list.get(otherKeyIndex2%(ts.size()+1));


			String succPort = "";
			String succPort1 = "";
			String succPort2 = "";

//			System.out.println(portMap);

			for (Map.Entry<String, BigInteger> entry : portMap.entrySet()) {
				if (entry.getValue().equals(succHash)) {
					succPort = entry.getKey();
//				    System.out.println(succPort);
				}
				if (entry.getValue().equals(succHash1)) {
					succPort1 = entry.getKey();
//				    System.out.println(succPort1);
				}
				if (entry.getValue().equals(succHash2)) {
					succPort2 = entry.getKey();
//				    System.out.println(succPort2);
				}
			}

			obj = new Object[]{succPort,succPort1,succPort2};

		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return obj;

	}

	public int calculateMod(int num){
		if (num>=0){
			return num%ts.size();
		}
		else{
			int m = num+ts.size();
			return m;
		}
	}

	public Object[] getRecoveryPorts(BigInteger keyHash){
		Object[] obj = new Object[]{};

		try {

			TreeSet<BigInteger> temp_ts = new TreeSet<BigInteger>(ts);

			temp_ts.add(keyHash);
			Log.d("FUNC", "hash generated and added to temp_ts");
			int keyIndex = 0;

			ArrayList<BigInteger> temp_ts_list;
			temp_ts_list = new ArrayList<BigInteger>(temp_ts);
			for (int i = 0; i < temp_ts_list.size(); i++) {
				if (temp_ts_list.get(i).compareTo(keyHash) == 0) {
					keyIndex = i;
				}
			}

			// get other two hashes
			int otherKeyIndex1 = keyIndex - 1;
			int otherKeyIndex2 = keyIndex - 2;


			Log.d("FUNC", "Position of port " + " is= " + keyIndex);
//			Log.d("FUNC", "hash= " + keyHash);
			BigInteger succHash1;
			BigInteger predHash1;
			BigInteger predHash2;

			succHash1 = temp_ts_list.get(calculateMod(keyIndex+1));
			predHash1 = temp_ts_list.get(calculateMod(otherKeyIndex1));
			predHash2 = temp_ts_list.get(calculateMod(otherKeyIndex2));


			String succPort1 = "";
			String predPort1 = "";
			String predPort2 = "";

//			System.out.println(portMap);

			for (Map.Entry<String, BigInteger> entry : portMap.entrySet()) {
				if (entry.getValue().equals(succHash1)) {
					succPort1 = entry.getKey();
//				    System.out.println(succPort);
				}
				if (entry.getValue().equals(predHash1)) {
					predPort1 = entry.getKey();
//				    System.out.println(succPort1);
				}
				if (entry.getValue().equals(predHash2)) {
					predPort2 = entry.getKey();
//				    System.out.println(succPort2);
				}
			}

			obj = new Object[]{succPort1,predPort1,predPort2};

		}
		catch (Exception e) {
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
			Object[] recoveryPorts = getSuccHashAndPort(selection, null);
			Object[] result = emulatorStorage.remove(selection);
			if(result==null){
				for(int i=0; i<recoveryPorts.length; i++){
					try{
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt((String)recoveryPorts[i]));
						Log.d("DELETE", "Socket created");
						ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
						out.writeObject(new Object[]{"DEL", selection});
						out.flush();
						Log.d("DELETE", "Request sent to " + recoveryPorts[i]);
					}
					catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		}
		catch (Exception e){
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
//			BigInteger succHash = (BigInteger)ob[1];

			String succPort1 = (String)ob[1];
//			BigInteger succHash1 = (BigInteger)ob[3];

			String succPort2 = (String)ob[2];
//			BigInteger succHash2 = (BigInteger)ob[5];

//			Log.d("INSERT", "key-hash will be sent to AVD= " + succPort);

			Date timestamp = new Date();
			timestamp = new Timestamp(timestamp.getTime());

			try {

				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(succPort));
//				Log.d("INSERT", "Socket Created");
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(new Object[]{"ORG", key, value, timestamp});
				out.flush();
				Log.d("INSERT", "Sent " + key + "- " + value + "-Tag:ORG " + "to " + succPort);
			}
			catch (Exception e){
				e.printStackTrace();
			}

			try{
				Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(succPort1));
//				Log.d("INSERT", "Socket Created");
				ObjectOutputStream out1 = new ObjectOutputStream(socket1.getOutputStream());
				out1.writeObject(new Object[]{"REP1", key, value, timestamp});
				out1.flush();
				Log.d("INSERT", "Sent " + key + "- " + value + "-Tag:REP1 " + "to " + succPort1);

			}
			catch (Exception e){
				e.printStackTrace();
			}

			try {
				Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(succPort2));
				Log.d("INSERT", "Socket Created");
				ObjectOutputStream out2 = new ObjectOutputStream(socket2.getOutputStream());
				out2.writeObject(new Object[]{"REP2", key, value, timestamp});
				out2.flush();
				Log.d("INSERT", "Sent " + key + "- " + value + "-Tag:REP2 " + "to " + succPort2);
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
		catch (Exception e){
			e.printStackTrace();
			Log.d("INSERT-EXP", e.getMessage());
		}

		return uri;
//		return null;
	}


	/**
	 * This function runs when emualtor is run
	 * It gets the port and emulator number for the running port and performs default initializations.
	 * @return
	 */
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub

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

			for(int i=0; i<AVD_ORDER.length;i++){
				BigInteger hash = new BigInteger(genHash(AVD_ORDER[i]), 16);
				portMap.put(String.valueOf(Integer.valueOf(AVD_ORDER[i])*2), hash);
				ts.add(hash);
			}

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
//				Log.d("REMOTE PORT ", remoteport);
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
//				Log.d("SEND ALL", "Socket Created");

				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(new Object[]{"SRV", ts, portMap});
				out.flush();
//				Log.d("SEND ALL", "Sent to " + remoteport);

			} catch (Exception e) {
                e.printStackTrace();
//				Log.d("SEND ALL EXP", "Port FAILED ");
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
			uriBuilder.authority("edu.buffalo.cse.cse486586.simpledynamo.provider");
			uriBuilder.scheme("content");
			Uri mUri = uriBuilder.build();

			String label = "";


			do {
				try {
//					Log.d("SERVER", "10000" + " Waiting for client...");
					Socket socket = serverSocket.accept();

//					Log.d("SERVER", "Successfully connected!!");

					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					Object[] message = (Object[]) in.readObject();


					label = (String) message[0];
					Log.d("SERVER", "Label= " + label);

					if ("SRV".equals(label)){

						ts = (TreeSet) message[1];
						portMap = (HashMap<String, BigInteger>) message[2];
						Log.d("SERVER", "Received Tree Set and Updated Port Map");
						sortPorts();

						Log.d("SERVER", "Calling sortports for NEW SUCC and PRED");
//						System.out.println(ts);
					}
					else if("ORG".equals(label)){
						Log.d("SERVER", "Received Original-Insert");
						String k = (String)message[1];
						String v = (String)message[2];
						Date timestamp = (Date)message[3];
						emulatorStorage.put(k, new Object[]{v,"ORG", timestamp});
					}
					else if("REP1".equals(label) || "REP2".equals(label)){
						Log.d("SERVER", "Received Replicated-Insert");
						String k = (String)message[1];
						String v = (String)message[2];
						Date timestamp = (Date)message[3];
						emulatorStorage.put(k,new Object[]{v,label, timestamp});
					}
					else if("GLOBAL".equals(label) || "RECOVERY".equals(label)){
						ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
						out.writeObject(emulatorStorage);
						out.flush();
						Log.d("SERVER", "Sending emulator storage");
					}
					else if("OTHER".equals(label)){
						String k = (String)message[1];
						Object[] values = emulatorStorage.get(k);
						String v = (String)values[0];
						ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//						out.writeObject(v);
						out.writeObject(values);
						out.flush();
						Log.d("SERVER", "For Key= " + k + " Value found was= " + v + " with label= " + values[1]);

					}
					else if("DEL".equals(label)){
						String k = (String)message[1];
						emulatorStorage.remove(k);
						Log.d("SERVER", "For Key= " + k + "Value deleted from emulatorStorage");
					}

				}
				catch (Exception e){

					e.printStackTrace();
				}

			}while (true);

		}
	}

	public HashMap<String, Object[]>  getFilteredRecoveryData(HashMap<String, Object[]> data, int version){
		HashMap<String, Object[]> filteredData = new HashMap<String, Object[]>();
		for(Map.Entry<String, Object[]> entry : data.entrySet()){
			Object[] values = entry.getValue();
			String v = (String)values[0];
			String l = (String)values[1];
			if (version==0 && l.equals("REP1")){
				filteredData.put(entry.getKey(), new Object[]{v,"ORG", values[2]});
			}
			else if(version==1 && l.equals("ORG")){
				filteredData.put(entry.getKey(), new Object[]{v,"REP1", values[2]});
			}
			else if(version==2 && l.equals("ORG")){
				filteredData.put(entry.getKey(), new Object[]{v,"REP2", values[2]});
			}
		}
		return filteredData;
	}

	public void updateEmulatorStorage(HashMap<String, Object[]> filteredData){
		for(Map.Entry<String, Object[]> entry : filteredData.entrySet()){
			emulatorStorage.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Client-side code
	 */
	public class ClientTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... voids) {

			try{
				Object[] recoveryPorts = getRecoveryPorts(portHashCode);
				Log.d("CLIENT", "Recovery Ports= " + recoveryPorts[0] + "  " + recoveryPorts[1] + "  " + recoveryPorts[2]);
				for(int i=0; i<recoveryPorts.length; i++){
					try {

						Socket soc1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt((String) recoveryPorts[i]));
						ObjectOutputStream out1 = new ObjectOutputStream(soc1.getOutputStream());
						out1.writeObject(new Object[]{"RECOVERY"});
						out1.flush();

						ObjectInputStream in1 = new ObjectInputStream((soc1.getInputStream()));
						HashMap<String, Object[]> recoveryData = (HashMap<String, Object[]>) in1.readObject();
						recoveryData = getFilteredRecoveryData(recoveryData, i);
						updateEmulatorStorage(recoveryData);
					}
					catch (Exception e){
						e.printStackTrace();
					}
				}
			}
			catch (Exception e){
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
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		try{
			MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
			Log.d("QUERY", "selection found:" + selection);
			if (selection.equals("@")){
				for(Map.Entry<String, Object[]> en: emulatorStorage.entrySet()){
					String k = en.getKey();
					Object[] values = en.getValue();
					mc.addRow(new Object[]{k,values[0]});
					Log.d("QUERY", "key= " + k + "value= " + values[0] + "Tag: " + values[1]);
				}
				Log.d("QUERY", "Local Dumped");
			}
			else if(selection.equals("*")){
				HashMap<String, Object[]> mostUpdatedMap = new HashMap<String, Object[]>();
				for (int i=0; i<5; i++) {
					try {

						String remoteport = REMOTE_PORT[i];
//						Log.d("QUERY", remoteport);
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
//						Log.d("QUERY", "Socket created");

						ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
						out.writeObject(new Object[]{"GLOBAL"});
						out.flush();
						Log.d("QUERY", "Request sent to " + remoteport);

						ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
						HashMap<String, Object[]> message = (HashMap<String, Object[]>) in.readObject();
						for(Map.Entry<String, Object[]> en : message.entrySet()){
							String k = en.getKey();
							Object[] values = en.getValue();
							String v = (String)values[0];
							String l = (String)values[1];
							Date time = (Date)values[2];

							if(mostUpdatedMap.containsKey(k)){
								Object[] temp_result = mostUpdatedMap.get(k);
								Date temp_time = (Date)temp_result[2];
								if(temp_time.compareTo(time)<0){
									mostUpdatedMap.put(k,en.getValue());
								}
							}
							else{
								mostUpdatedMap.put(k, en.getValue());
							}
						}
					}
					catch (Exception e) {
                		e.printStackTrace();
						Log.d("QUERY", "SEND ALL EXP, Port FAILED ");
					}
					for(Map.Entry<String,Object[]> entry : mostUpdatedMap.entrySet()){
						mc.addRow(new Object[]{entry.getKey(),entry.getValue()[0]});
					}
				}
				Log.d("QUERY", "Global Dumped");

			}
			else{
//				Log.d("QUERY", "Inside ELSE!");
				Object[] values = emulatorStorage.get(selection);
				if (values == null || !values[1].equals("ORG")){
					String val = null;
					Object[] recoveryPorts = getSuccHashAndPort(selection,val);
					Log.d("QUERY", "Recovery Ports= " + recoveryPorts[0] + "  " + recoveryPorts[1] + "  " + recoveryPorts[2]);

					HashMap<String, Object[]> mostRecent = new HashMap<String, Object[]>();
					for(int i=0; i<recoveryPorts.length; i++){
						try{
							Socket soc1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt((String)recoveryPorts[i]));
							ObjectOutputStream out1 = new ObjectOutputStream(soc1.getOutputStream());
							out1.writeObject(new Object[]{"OTHER", selection});
							out1.flush();

							ObjectInputStream in1 = new ObjectInputStream((soc1.getInputStream()));
							Object[] updatedData = (Object[]) in1.readObject();
							if(mostRecent.containsKey(selection)){
								Object[] temp_result = mostRecent.get(selection);
								Date temp_time = (Date)temp_result[2];
								if(temp_time.compareTo((Date)updatedData[2])<0){
									mostRecent.put(selection,updatedData);
								}
							}
							else {
								mostRecent.put(selection, updatedData);
							}
						}
						catch (Exception e){
							e.printStackTrace();
						}
					}

					mc.addRow(new Object[]{selection,mostRecent.get(selection)[0]});
				}
				else{
					mc.addRow(new Object[]{selection,values[0]});
					Log.d("QUERY", "key= " + selection + "value= " + values[0] + "Tag: " + values[1]);
				}

			}
			return mc;

		}
		catch (Exception e){
			e.printStackTrace();
		}

		Log.v("query", selection);

		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
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
