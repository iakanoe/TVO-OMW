package com.monitoreomayorista.omw;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

class DataGetter extends AsyncTask<Void, Void, JSONObject>{
	private OnDataGotListener onDataGotListener;
	private String rc;
	
	DataGetter(String rc, OnDataGotListener onDataGotListener){
		this.rc = rc;
		this.onDataGotListener = onDataGotListener;
	}
	
	@Override
	protected JSONObject doInBackground(Void... params){
		try{
			URL url = new URL("http://ayaxseg.000webhostapp.com/getNum.php?rc=".concat(rc));
			InputStream in = url.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			StringBuilder sb = new StringBuilder();
			while((line = reader.readLine()) != null) sb.append(line);
			in.close();
			String r = sb.toString();
			JSONArray arr = new JSONArray(r);
			return arr.getJSONObject(0);
		} catch(Exception e) {
			e.printStackTrace();
        }
		return null;
	}
	
	@Override
	protected void onPostExecute(JSONObject result){
		super.onPostExecute(result);
		try{
			onDataGotListener.gotData(result);
		} catch(JSONException e) {
			e.printStackTrace();
		}
	}
	
	public interface OnDataGotListener{
		void gotData(JSONObject result) throws JSONException;
	}
}
