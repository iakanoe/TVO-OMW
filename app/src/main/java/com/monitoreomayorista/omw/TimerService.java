package com.monitoreomayorista.omw;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class TimerService extends Service{
	private static final int NotID = 48812;
	private IBinder binder;
	private CountDownTimer timer;
	private Notification.Builder not;
	private int numMsg = 0;
	private String time = "5:00";
	private TimerListener listener;
	private Abonado abonado;
	private String ack;
	
	public void setListener(TimerListener listener){
		this.listener = listener;
	}
	
	public String getTime(){
		return time;
	}
	
	public void start(@NonNull TimerListener listener, Abonado abonado){
		this.abonado = abonado;
		this.listener = listener;
		sendNot();
		timer.start();
	}
	
	@Override
	public void onCreate(){
		binder = new Binder();
		super.onCreate();
		createNot();
		createTimer();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent){
		return binder;
	}
	
	public void stopTimer(){
		timer.cancel();
	}
	
	private void createNot(){
		Intent i = new Intent(getBaseContext(), Main.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		not = new Notification.Builder(getApplicationContext())
			.setAutoCancel(false)
			.setContentTitle("En camino")
			.setContentText("5:00")
			.setContentIntent(pendingIntent)
			.setOngoing(true)
			.setNumber(++numMsg)
			.setSmallIcon(R.drawable.ic_black);
	}
	
	private void createTimer(){
		//timer = new CountDownTimer(300000, 500){
		timer = new CountDownTimer(10000, 500){
			@Override
			public void onTick(long millis){
				time = String.valueOf((int) (millis / 60000)) + ":" + String.format("%02d", (int) ((millis % 60000) / 1000));
				not.setContentText(time).setNumber(++numMsg);
				sendNot();
				listener.onTick(millis);
			}
			
			@Override
			public void onFinish(){
				sendNotVencido();
				enviarEvento();
				listener.onFinish();
			}
		};
	}
	
	private void enviarEvento(){
		if(getSystemService(VIBRATOR_SERVICE) != null)
			((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(200);
		
		generateACK();
		UDPTask udpTask = new UDPTask("ram.dyndns.ws", abonado.getPort());
		udpTask.setACK(getACK());
		udpTask.setOnTaskCompletedListener(new UDPTask.OnTaskCompletedListener(){
			@Override
			public void onTaskCompleted(boolean result){
				stopSelf();
			}
		});
		udpTask.sendUDP(makeMsg());
	}
	
	String makeMsg(){
		return
			"$B," +
				String.format("%04d", Integer.valueOf(abonado.getNum())) +
				"," +
				getACK() +
				"," +
				(new SimpleDateFormat("HH:mm")).format(Calendar.getInstance().getTime()) +
				",01," +
				String.format("%04d", Integer.valueOf(abonado.getNum())) +
				"18198700" +
				String.format("%03d", Integer.valueOf(abonado.getUser())) +
				",8,0,0," +
				abonado.getClave() +
				",10,4_4.3,$E";
	}
	
	private void generateACK(){
		ack = String.format("%02d", (new Random()).nextInt(100));
	}
	
	private String getACK(){
		return ack;
	}
	
	private void sendNot(){
		startForeground(NotID, not.build());
	}
	
	private void sendNotVencido(){
		not.setNumber(++numMsg)
			.setContentIntent(null)
			.setContentTitle("En camino vencido")
			.setContentText("Enviando señal de vencido a la central.")
			.setOngoing(false)
			.setSubText(null);
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NotID, not.build());
	}
	
	public interface TimerListener{
		void onTick(long l);
		
		void onFinish();
	}
	
	class Binder extends android.os.Binder{
		TimerService getService(){
			return TimerService.this;
		}
	}
}
