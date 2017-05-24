package com.mmalk.RunningManager;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Class responsible for retrieving data from external server
 * it works asynchronously, because it performs networking operations
 * DialogManagerInterface provides the ability of passing the data to UI thread
 */

public class ResponseServer extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "ResponseServer";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private String response;
    private boolean listen;
    private DialogManagerInterface dialogManagerInterface;

    public ResponseServer(Activity activity) {

        this.dialogManagerInterface = (DialogManagerInterface) activity;
    }

    /**
     * prepare ServerSocket, before executing background task
     */
    @Override
    protected void onPreExecute() {

        super.onPreExecute();
        Log.i(TAG, "onPreExecute");
        response = "";

        listen = true;
        try {
            serverSocket = new ServerSocket(8080);
            serverSocket.setSoTimeout(10000);
        } catch (IOException e) {
            Log.e(TAG, "Could not listen on port: 8080");
            response = "Cannot listen for response on port: 8080";
            listen = false;
        }
    }

    /**
     * called after using new ResponseServer.execute()
     * opens clientSocket and waits for a connection from the external server
     * modifies "response" accordingly
     */
    @Override
    protected Void doInBackground(Void... voids) {

        Log.i(TAG, "Server working in background");

        if (listen) {

            try {
                clientSocket = serverSocket.accept();
                Log.i(TAG, "Connection from: " + clientSocket.getInetAddress());
                inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                bufferedReader = new BufferedReader(inputStreamReader);
                String responseInJson = bufferedReader.readLine();
                boolean responseBool = new Gson().fromJson(responseInJson, Boolean.class);
                //responseBool says whether given Result was already saved on the server or not

                Log.i(TAG, "Response from server: " + responseBool);
                if (responseBool) {
                    response = "This result has already been saved!";
                } else {
                    response = "Result was saved successfully!";
                }

                inputStreamReader.close();
                clientSocket.close();
                serverSocket.close();
                Log.i(TAG, "server exiting from background");
                return null;
            }
            catch (SocketTimeoutException e){
                Log.i(TAG, "Waiting for server connection timed out!");

                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
        Log.i(TAG, "server exiting from background");
        return null;
    }

    /**
     * method called after the background task is finished
     * it sends the server's response to the UI thread via DialogManagerInterface
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.i(TAG, "onPostExecute");
        if(!response.equals("")) {
            dialogManagerInterface.updateDialog(response);
        }
    }


}
