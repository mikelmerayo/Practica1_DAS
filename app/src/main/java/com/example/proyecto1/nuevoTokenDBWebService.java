package com.example.proyecto1;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.HttpsURLConnection;

public class nuevoTokenDBWebService extends Worker {
    public nuevoTokenDBWebService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String token= getInputData().getString("token");
        HttpsURLConnection urlConnection= GeneradorConexionesSeguras.getInstance().crearConexionSegura(getApplicationContext(),"https://134.209.235.115/mmerayo002/WEB/nuevoToken.php");
        JSONObject parametrosJSON= new JSONObject();
        try {
            parametrosJSON.put("token", token);
            Log.i("token: ", " " + token);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            PrintWriter out= new PrintWriter(urlConnection.getOutputStream());
            out.print(parametrosJSON.toString());
            out.close();
            Log.i("Conexion: ", "Enviada");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        /*
        Uri.Builder builder= new Uri.Builder().appendQueryParameter("usuario", usuario);
        String parametros= builder.build().getEncodedQuery();
        urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
        */

        int statusCode= 0;
        try {
            statusCode = urlConnection.getResponseCode();
            Log.i("status: "," " +statusCode);
            if(statusCode== 200) {
                Log.i("resultado: ", "token añadido" );
                return Result.success();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Result.success() Result.failure() Result.retry()
        return Result.failure();
    }
}
