package com.monitoreomayorista.superapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nullwire.trace.ExceptionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends AppCompatActivity {
    SharedPreferences tinyDB;
	String rcAbonado;
	String numAbonado;
    String claveAbonado;
	String userAbonado;
	String portAbonado;
	String rcName;
	String smsNum = "";
	String ack = "";
	int minutos;
	int segundos;
    Timer timer;
    Handler handler = new Handler();
    Handler h2 = new Handler();
    Vibrator vibrator;
    EventRunnable runMedica = new EventRunnable(Evento.MEDICA);
    EventRunnable runFuego = new EventRunnable(Evento.FUEGO);
    EventRunnable runPanico = new EventRunnable(Evento.PANICO);
    EventRunnable runTVO = new EventRunnable(Evento.TVO);
	//EventRunnable runTest = new EventRunnable(Evento.TEST);
	Map<String, Integer> rc2img = new HashMap<>();
	SendSMS sendSMS;
	boolean badRC = false;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(!checkConnection()) return;
		ExceptionHandler.register(this, "http://ayaxseg.000webhostapp.com/exc.php");
		setContentView(R.layout.activity_main);
		tinyDB = this.getPreferences(Context.MODE_PRIVATE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		initializeUi();
		refrescar();
		createMap();
	}
	
	void createMap(){
		rc2img.put("A5", R.drawable.logo_ays);
	}

    void initializeUi(){
        (findViewById(R.id.btnAmbulancia)).setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(runMedica, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(runMedica);
                }
	            return v.performClick();
            }
        });
        (findViewById(R.id.btnBomberos)).setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(runFuego, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(runFuego);
                }
	            return v.performClick();
            }
        });
        (findViewById(R.id.btnPanico)).setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(runPanico, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(runPanico);
                }
	            return v.performClick();
            }
        });
        (findViewById(R.id.btnTVO)).setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(runTVO, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(runTVO);
                }
	            return v.performClick();
            }
        });
        /*(findViewById(R.id.btnTest)).setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(runTest, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(runTest);
                }
                return false;
            }
        });*/
	    (findViewById(R.id.loginBtn)).setOnTouchListener(new View.OnTouchListener(){
		    @Override
		    public boolean onTouch(View v, MotionEvent event){
			    if(event.getAction() == MotionEvent.ACTION_UP) crearLoginDialog();
			    return v.performClick();
		    }
	    });
    }
	
	void getThings(){
		@SuppressLint("InflateParams") final View v = getLayoutInflater().inflate(R.layout.loading_dialog, null);
		final AlertDialog loadingDialog = new AlertDialog.Builder(this)
			.setView(v)
			.setCancelable(false)
			.create();
		loadingDialog.show();
		new DataGetter(rcAbonado, new DataGetter.OnDataGotListener(){
			@Override
			public void gotData(JSONObject result) throws JSONException{
				loadingDialog.dismiss();
				justGotData(result);
			}
		}).execute();
	}
	
	void justGotData(JSONObject result) throws JSONException{
		if(result != null){
			if(result.length() > 0){
				rcName = result.getString("nombre");
				portAbonado = result.getString("port");
				smsNum = result.getString("smsnum");
				tinyDB.edit()
					.putString("rcname", rcName)
					.putString("port", portAbonado)
					.putString("smsnum", smsNum)
					.apply();
				testearDatos();
			}
		} else {
			badRC = true;
			crearLoginDialog();
		}
	}
	
	boolean checkConnection(){
		ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		if(networkInfo != null) return true;
		new AlertDialog.Builder(this)
			.setMessage("No hay conexión a Internet.")
			.setCancelable(false)
			.setNegativeButton("Cerrar", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialogInterface, int i){
					finishAndRemoveTask();
				}
			}).create().show();
		return false;
	}

	void crearLoginDialog(){
		@SuppressLint("InflateParams") final View v = getLayoutInflater().inflate(R.layout.login_dialog, null);
		if(!(numAbonado.equals("") || claveAbonado.equals("") || userAbonado.equals("") || rcAbonado.equals(""))){
			((TextInputEditText) v.findViewById(R.id.userInput)).setText(numAbonado);
			((TextInputEditText) v.findViewById(R.id.passInput)).setText(claveAbonado);
			((TextInputEditText) v.findViewById(R.id.zoneInput)).setText(userAbonado);
			((TextInputEditText) v.findViewById(R.id.rcInput)).setText(rcAbonado);
			if(badRC){
				((TextInputEditText) v.findViewById(R.id.rcInput)).setError("El RC no existe");
			}
		}
		AlertDialog.Builder b = new AlertDialog.Builder(this)
			.setView(v)
			.setPositiveButton("Iniciar sesion", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					String a = ((TextInputEditText) v.findViewById(R.id.userInput)).getText().toString();
					String b = ((TextInputEditText) v.findViewById(R.id.passInput)).getText().toString();
					String c = ((TextInputEditText) v.findViewById(R.id.zoneInput)).getText().toString();
					String d = ((TextInputEditText) v.findViewById(R.id.rcInput)).getText().toString();
					dialog.cancel();
					procesarUserPass(a, b, c, d);
				}
			})
			.setCancelable(false);
		
		if(!badRC){
			b.setNegativeButton("Cancelar", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					dialog.cancel();
				}
			});
			b.setCancelable(true);
		}
		
		b.show();
		badRC = false;
	}
	
	void procesarUserPass(String user, String pass, String zone, String rc){
		tinyDB.edit()
			.putString("num", user)
			.putString("key", pass)
			.putString("user", zone)
			.putString("rc", rc)
			.apply();
		refrescar();
	}
	
	@SuppressLint("SetTextI18n")
	void refrescar(){
		String xxx = "No conectado";
		numAbonado = tinyDB.getString("num", "");
        claveAbonado = tinyDB.getString("key", "");
		userAbonado = tinyDB.getString("user", "");
		rcAbonado = tinyDB.getString("rc", "");
		if(numAbonado.equals("") || claveAbonado.equals("") || userAbonado.equals("") || rcAbonado.equals(""))
			crearLoginDialog();
		else {
			getThings();
			xxx = "Conectado con cuenta " + rcAbonado + "-" + numAbonado + "/" + userAbonado;
			setImage();
		}
		((TextView) findViewById(R.id.statusTxt)).setText(xxx);
	}
	
	void setImage(){
		if(!rc2img.containsKey(rcAbonado))
			((ImageView) findViewById(R.id.imgRC)).setImageDrawable(null);
		else
			((ImageView) findViewById(R.id.imgRC)).setImageResource(rc2img.get(rcAbonado));
	}
	
	@SuppressLint({"SetTextI18n", "DefaultLocale"})
	void timer(){
		if(segundos == 0 && minutos == 0){
			timer.cancel();
			timer.schedule(new TimerTask() {
				@Override public void run() { runOnUiThread(new Runnable() {
					@Override public void run() {
						(findViewById(R.id.btnTVO)).setClickable(false);
						(findViewById(R.id.bwx4)).setVisibility(View.VISIBLE);
						(findViewById(R.id.tvo1txt)).setVisibility(View.GONE);
						(findViewById(R.id.tvo2txt)).setVisibility(View.GONE);
					}
				});}}, 1000);
		} else if(segundos == 0){
			segundos = 59;
			minutos--;
		} else segundos--;
		((TextView) findViewById(R.id.tvo2txt)).setText(String.valueOf(minutos) + ':' + String.format("%02d", segundos));
	}
	
	@SuppressLint({"SetTextI18n", "DefaultLocale"})
	void callback(Evento evt, boolean result){
		vibrator.vibrate(200);
		if (!result) {
	        Snackbar.make(findViewById(R.id.coord), "La señal no se pudo enviar", Snackbar.LENGTH_SHORT).show();
	        return;
		}
        h2.removeCallbacks(sendSMS);
        if (evt == Evento.TEST) {
            Snackbar.make(findViewById(R.id.coord), "La señal de prueba ha sido recibida", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (evt == Evento.TVO) {
	        (findViewById(R.id.btnTVO)).setEnabled(false);
	        (findViewById(R.id.bwx4)).setVisibility(View.GONE);
            (findViewById(R.id.tvo1txt)).setVisibility(View.VISIBLE);
            (findViewById(R.id.tvo2txt)).setVisibility(View.VISIBLE);
            minutos = 5;
            segundos = 0;
            timer = new Timer();
            timer.schedule(new TimerTask() {
	            @Override
	            public void run(){
		            runOnUiThread(new Runnable(){
			            @Override
			            public void run(){
				            timer();
			            }
		            });
	            }
            }, 1000, 1000);
            ((TextView) findViewById(R.id.tvo2txt)).setText(String.valueOf(minutos) + ':' + String.format("%02d", segundos));
        }
        Snackbar.make(findViewById(R.id.coord), "Señal enviada", Snackbar.LENGTH_SHORT).show();
    }
	
	@SuppressLint("DefaultLocale")
	void generateACK(){
		ack = String.format("%02d", (new Random()).nextInt(100));
	}
	
	String getACK(){
		return ack;
	}
	
	@SuppressLint({"SimpleDateFormat", "DefaultLocale"})
	String makeMsg(Evento evt){
		Date d = Calendar.getInstance().getTime();
		return
			"$B," +
				String.format("%04d", Integer.valueOf(numAbonado)) +
				"," +
				getACK() +
				"," +
				(new SimpleDateFormat("HH:mm")).format(d) +
				",01," +
				String.format("%04d", Integer.valueOf(numAbonado)) +
				"181" +
				String.format("%03d", evt.code) +
				"00" +
				String.format("%03d", Integer.valueOf(userAbonado)) +
				",8,0,0," +
				claveAbonado +
				",10,4_4.3,$E";
	}
	
	void evento(final Evento evt) {
		vibrator.vibrate(200);
		if(numAbonado.equals("")){
			Snackbar.make(findViewById(R.id.coord), "No está conectado", Snackbar.LENGTH_SHORT).show();
			return;
		}
		generateACK();
		UDPTask udpTask = new UDPTask("ram.dyndns.ws", portAbonado);
		udpTask.setOnTaskCompletedListener(new UDPTask.OnTaskCompletedListener() {
	        @Override
	        public void onTaskCompleted(boolean result){
		        callback(evt, result);
	        }});
		udpTask.setACK(getACK());
		String a = makeMsg(evt);
		udpTask.sendUDP(a);
		if(smsNum == null) return;
		sendSMS = new SendSMS(evt);
        h2.postDelayed(sendSMS, 5000);
    }
	
	void testearDatos(){
		generateACK();
		UDPTask udpTask = new UDPTask("ram.dyndns.ws", portAbonado);
		udpTask.setOnTaskCompletedListener(new UDPTask.OnTaskCompletedListener(){
			@Override
			public void onTaskCompleted(boolean result){
				if(result) return;
				createDialog();
			}
		});
		udpTask.setACK(getACK());
		udpTask.sendUDP(makeMsg(Evento.TEST));
	}
	
	void createDialog(){
		new AlertDialog.Builder(this)
			.setMessage("El número de abonado es incorrecto.")
			.setCancelable(false)
			.setNegativeButton("Reintentar", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialogInterface, int i){
					crearLoginDialog();
					dialogInterface.dismiss();
				}
			}).create().show();
	}
	
	enum Evento{
		MEDICA(100),
		FUEGO(110),
		PANICO(120),
		TVO(886),
		TEST(603);
		int code;
		
		Evento(int code){
			this.code = code;
		}
	}
	
	class EventRunnable implements Runnable{
		private Evento evt;
		
		EventRunnable(Evento e){
			evt = e;
		}
		
		public void run(){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					evento(evt);
				}
			});
		}
	}
	
	class SendSMS implements Runnable{
		private Evento evt;
		SendSMS(Evento e){
			evt = e;
		}
		public void run(){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					SmsManager.getDefault().sendTextMessage(smsNum, null, makeMsg(evt), null, null);
				}
			});
		}
	}
}
