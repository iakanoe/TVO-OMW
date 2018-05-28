package com.monitoreomayorista.omw;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
    Handler handler = new Handler();
    Handler h2 = new Handler();
    Vibrator vibrator;
    EventRunnable runMedica = new EventRunnable(Evento.MEDICA);
    EventRunnable runFuego = new EventRunnable(Evento.FUEGO);
    EventRunnable runPanico = new EventRunnable(Evento.PANICO);
	OMWRunnable runOMW = new OMWRunnable();
	UnbindRunnable runUnbind = new UnbindRunnable();
	Map<String, Integer> rc2img = new HashMap<>();
	SendSMS sendSMS;
	boolean badRC = false;
	TimerService.Binder binder;
	boolean sconnected = false;
	boolean saconnected = false;
	ServiceConnection sconn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName componentName, final IBinder iBinder){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					sconnected = true;
					servConnected(iBinder);
				}
			});
		}
		
		@Override
		public void onServiceDisconnected(ComponentName componentName){
			sconnected = false;
		}
	};
	ServiceConnection sAlreadyConn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName componentName, final IBinder iBinder){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					saconnected = true;
					servAlreadyConnected(iBinder);
				}
			});
		}
		
		@Override
		public void onServiceDisconnected(ComponentName componentName){
			saconnected = false;
		}
	};
	
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
		if(isTimerRunning())
			bindService(new Intent(getApplicationContext(), TimerService.class), sAlreadyConn, Context.BIND_AUTO_CREATE);
	}
	
	boolean isTimerRunning(){
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
			if(TimerService.class.getName().equals(service.service.getClassName())){
				return true;
			}
		}
		return false;
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
		(findViewById(R.id.btnOMW)).setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View view, MotionEvent event){
				switch(event.getAction()){
					case MotionEvent.ACTION_UP:
						runOMW.run();
				}
				return true;
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
	
	void onMyWay(){
		startService(new Intent(getApplicationContext(), TimerService.class));
		bindService(new Intent(getApplicationContext(), TimerService.class), sconn, Context.BIND_AUTO_CREATE);
	}
	
	void servConnected(IBinder b){
		vibrator.vibrate(200);
		(findViewById(R.id.bwx4)).setVisibility(View.GONE);
		(findViewById(R.id.omw1txt)).setVisibility(View.VISIBLE);
		(findViewById(R.id.omw2txt)).setVisibility(View.VISIBLE);
		(findViewById(R.id.btnOMW)).setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View view, MotionEvent event){
				switch(event.getAction()){
					case MotionEvent.ACTION_UP:
						runUnbind.run();
				}
				return true;
			}
		});
		binder = (TimerService.Binder) b;
		binder.getService().start(new TimerService.TimerListener(){
			@Override
			public void onTick(long l){
				((TextView) findViewById(R.id.omw2txt)).setText(binder.getService().getTime());
			}
			
			@Override
			public void onFinish(){
				unbindTimer();
			}
		}, new Abonado(rcAbonado, numAbonado, claveAbonado, userAbonado, portAbonado));
	}
	
	void unbindTimer(){
		binder.getService().stopTimer();
		binder.getService().stopSelf();
		if(sconnected) unbindService(sconn);
		if(saconnected) unbindService(sAlreadyConn);
		(findViewById(R.id.bwx4)).setVisibility(View.VISIBLE);
		(findViewById(R.id.omw1txt)).setVisibility(View.GONE);
		(findViewById(R.id.omw2txt)).setVisibility(View.GONE);
		initializeUi();
	}
	
	void servAlreadyConnected(IBinder b){
		vibrator.vibrate(200);
		(findViewById(R.id.bwx4)).setVisibility(View.GONE);
		(findViewById(R.id.omw1txt)).setVisibility(View.VISIBLE);
		(findViewById(R.id.omw2txt)).setVisibility(View.VISIBLE);
		(findViewById(R.id.btnOMW)).setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View view, MotionEvent event){
				switch(event.getAction()){
					case MotionEvent.ACTION_UP:
						runUnbind.run();
				}
				return true;
			}
		});
		binder = (TimerService.Binder) b;
		((TextView) findViewById(R.id.omw2txt)).setText(binder.getService().getTime());
		binder.getService().setListener(new TimerService.TimerListener(){
			@Override
			public void onTick(long l){
				((TextView) findViewById(R.id.omw2txt)).setText(binder.getService().getTime());
			}
			
			@Override
			public void onFinish(){
				unbindTimer();
			}
		});
	}
	
	enum Evento{
		MEDICA(100),
		FUEGO(110),
		PANICO(120),
		OMW(987),
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
	
	class OMWRunnable implements Runnable{
		OMWRunnable(){
		}
		
		public void run(){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					onMyWay();
				}
			});
		}
	}
	
	class UnbindRunnable implements Runnable{
		UnbindRunnable(){
		}
		
		public void run(){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					unbindTimer();
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
