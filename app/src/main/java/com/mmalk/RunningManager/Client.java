package com.mmalk.RunningManager;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Class used to send data to the server asynchronously
 * It communicates with the UI thread via DialogManagerInterface, giving information about connection
 */
public class Client extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "Client";
    private String serverAddress;
    private int serverPort;
    private Result result;
    private String response;
    private DialogManagerInterface dialogManagerInterface;

    /**
     * Constructor used to initialize fields
     * result - Result object to be sent to server
     * response - contains information about connection with the server
     * dialogManagerInterface - interface used to communicate with UI thread
     */
    public Client(String address, int port, Result result, Activity activity) {

        this.serverAddress = address;
        this.serverPort = port;
        this.result = result;
        this.dialogManagerInterface = (DialogManagerInterface) activity;
        Log.i(TAG, "Client created with result: " + serverAddress + "/" + serverPort);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        response = "";
    }

    /**
     * method that is being executed after calling new Client.execute()
     * it tries to connect with the external server via sockets
     * data sent is in JSON format
     * if an exception is thrown, "response" is modified accordingly
     */
    @Override
    protected Void doInBackground(Void... params) {

        Log.i(TAG, "Background task started");
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, serverPort), 6000);
            Log.i(TAG, "socket created: " + serverAddress + "/" + serverPort);
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            Log.i(TAG, "Gson created, trying to write: " + result);

            try {
                JSONObject json = new JSONObject();
                Log.i(TAG, "Json created, trying to write: "
                        + result.getDistance()
                        + result.getTime()
                        + result.getDate()
                        + result.getComment());
                json.put("distance", result.getDistance());
                json.put("time", result.getTime());
                json.put("date", result.getDate());
                json.put("comment", result.getComment());

                printWriter.write(json.toString());
                printWriter.flush();
                printWriter.close();

                Log.i(TAG, "Json.toString(): " + json.toString());
                response = "Result was sent to the server!";

            }
            catch (JSONException e){
                e.printStackTrace();
                response = "JSONException encountered!";
            }
            finally {
                printWriter.close();
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "Server cannot be found!";
        } catch (ConnectException e){
            response = "Host you are trying to connect is unreachable!";
        }
        catch (SocketTimeoutException e){
            response = "Connection timed out!";
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                Log.e(TAG, "Socket null");
            }
        }
        Log.i(TAG, "Client exiting");
        return null;
    }

    /**
     * method executed after background task is finished
     * it modifies the UI via interface with "response"
     */

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(!response.equals("")) {
            dialogManagerInterface.updateDialog(response);
        }
        Log.i(TAG, "asyncTask executed");
    }
}
