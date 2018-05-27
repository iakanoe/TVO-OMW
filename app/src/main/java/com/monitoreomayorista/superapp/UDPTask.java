package com.monitoreomayorista.superapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UDPTask extends AsyncTask<Void, Void, Boolean> {
    private String ip;
    private int port;
    private String msg;
	private String ack;
	private OnTaskCompletedListener onTaskCompletedListener;
	
	public UDPTask(String ip, String port){
		this.ip = ip;
		this.port = Integer.valueOf(port);
	}
	
	public void setACK(String ack){
		this.ack = ack;
	}

    public void setOnTaskCompletedListener(OnTaskCompletedListener onTaskCompletedListener){
        this.onTaskCompletedListener = onTaskCompletedListener;
    }

    public void sendUDP(String packet){
        this.msg = packet;
        execute();
    }

    @Override protected Boolean doInBackground(Void... params) {
	    try{
		    byte[] message = msg.getBytes();
		    DatagramSocket socket = new DatagramSocket();
		    InetAddress ipname = InetAddress.getByName(ip);
		    socket.connect(ipname, port);
		    socket.send(new DatagramPacket(message, message.length));
		    DatagramPacket reception = new DatagramPacket(new byte[29], 29);
		    socket.receive(reception);
		    socket.close();
		    String data = new String(reception.getData());
		    if(data.equals("")) return false;
		    String[] datos = data.split(",");
		    return datos[0].equals("$B") && datos[3].split("=")[1].equals(ack);
	    } catch (IOException e) {
            Log.println(Log.ASSERT, "IOException", e.toString());
            return false;
        }
    }

    @Override protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        onTaskCompletedListener.onTaskCompleted(result);
    }
	
	public interface OnTaskCompletedListener{
		void onTaskCompleted(boolean result);
	}
}