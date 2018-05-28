package com.monitoreomayorista.omw;

public class Abonado{
	private String rc;
	private String num;
	private String clave;
	private String user;
	private String port;
	
	Abonado(String rc, String num, String clave, String user, String port){
		this.rc = rc;
		this.num = num;
		this.clave = clave;
		this.user = user;
		this.port = port;
	}
	
	public String getRc(){
		return rc;
	}
	
	public String getNum(){
		return num;
	}
	
	public String getClave(){
		return clave;
	}
	
	public String getUser(){
		return user;
	}
	
	public String getPort(){
		return port;
	}
}
