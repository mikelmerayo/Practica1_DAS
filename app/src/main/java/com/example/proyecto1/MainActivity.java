package com.example.proyecto1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private static String usu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //this.deleteDatabase("miBD");


        ActivityManager am= (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.P) {
            if(am.isBackgroundRestricted()==true){
                Toast.makeText(getApplicationContext(), "Habilite el modo background de la aplicación si desea recibir mensajes FCM", Toast.LENGTH_LONG).show();
            }
        }

        //Al pulsar el boton login
        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Cogemos los valores de todos los EditText tanto usuario como contraseña
                EditText usuario2 = (EditText) findViewById(R.id.usuario2);
                String usuario = usuario2.getText().toString();

                EditText contraseña2 = (EditText) findViewById(R.id.contraseña2);
                String contraseña = contraseña2.getText().toString();

                miBD gestorDB = new miBD(getApplicationContext(), "miBD", null, 1);

                //Comprobamos si ese usuario esta registrado en la base de datos
                boolean validado = gestorDB.comprobarUsuario(usuario, contraseña); //Esto mira en la BD local
                final String[] resul = {""};

                Data datos = new Data.Builder()
                        .putString("usuario", usuario)
                        .putString("password", contraseña)
                        .build();
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(comprobarUsuarioDBWebService.class).setInputData(datos).build();
                WorkManager.getInstance(MainActivity.this).getWorkInfoByIdLiveData(otwr.getId()).observe(MainActivity.this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            resul[0] = workInfo.getOutputData().getString("resultado");
                            Log.i("resul", "" + resul[0]);
                            this.logearUsuario();
                        }
                    }

                    private void logearUsuario() {
                        if (validado && resul[0].equals("existe")) { //Si lo esta
                            String idioma = gestorDB.consultarIdioma(usuario);//Consultamos su idioma preferente, por defecto, ingles
                            //Asignamos el idioma castellano o ingles.
                            if(idioma.equals("Castellano")){
                                Locale nuevaloc = new Locale("es");
                                Locale.setDefault(nuevaloc);
                                Configuration config = new Configuration();
                                config.locale = nuevaloc;
                                getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
                            }else if(idioma.equals("Ingles")){
                                Locale nuevaloc = new Locale("en");
                                Locale.setDefault(nuevaloc);
                                Configuration config = new Configuration();
                                config.locale = nuevaloc;
                                getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
                            }

                            //Escribimos en el fichero de logs (/data/data/com.example.proyecto1/files/logs.txt) que se ha logeado un usuario y la fecha de login
                            try {
                                OutputStreamWriter fichero = new OutputStreamWriter(openFileOutput("logs.txt", Context.MODE_APPEND));

                                //Se obtiene la fecha y la hora actual
                                Date date = new Date();
                                DateFormat hourdateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

                                fichero.write("Se ha logeado el usuario "+ usuario+ ". Hora y fecha: " + hourdateFormat.format(date) + '\n');
                                fichero.close();
                            } catch (IOException e){
                                e.printStackTrace();
                            }

                            //Accedemos al menu principal
                            Intent i = new Intent(MainActivity.this, MenuPrincipal.class);
                            setUsuario(usuario);
                            startActivity(i);
                            finish();

                        } else { //En caso de que no este registrado ese usuario y contraseña, mensaje de error
                            Toast.makeText(getApplicationContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                WorkManager.getInstance(MainActivity.this).enqueue(otwr);
            }
        });

        //Al pulsar el boton registro llamamos a la actividad registrarse
        Button registro = (Button) findViewById(R.id.registro);
        registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Registrarse.class);
                startActivity(i);

            }
        });


    }


    public static void setUsuario(String pusuario) { //Se usa para mantener el usuario de login como un atributo estatico de la clase

        usu = pusuario;
    }

    public static String getUsuario() { //Para acceder desde cualquier clase al usuario login

        return usu;
    }





    @Override
    protected void onSaveInstanceState(Bundle outState){ //Metodo para guardar los valores en caso de girar, pausar... la app
        super.onSaveInstanceState(outState);


        EditText usuario2 = (EditText) findViewById(R.id.usuario2);
        String usuario = usuario2.getText().toString();
        outState.putString("Usuario", usuario);

        EditText contraseña2 = (EditText) findViewById(R.id.contraseña2);
        String contraseña = contraseña2.getText().toString();
        outState.putString("Contraseña", contraseña);


    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){ //Metodo para restablecer los valores guardados
        super.onRestoreInstanceState(savedInstanceState);

        String usuario = savedInstanceState.getString("Usuario");
        EditText usuario2 = (EditText) findViewById(R.id.usuario2);
        usuario2.setText(String.valueOf(usuario));

        String contraseña = savedInstanceState.getString("Contraseña");
        EditText contraseña2 = (EditText) findViewById(R.id.contraseña2);
        contraseña2.setText(String.valueOf(contraseña));
    }


}

