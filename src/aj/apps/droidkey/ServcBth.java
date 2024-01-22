package aj.apps.droidkey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import aj.apps.droidkey.R;

public class ServcBth extends Service{
	
	//Rutinas de tarea
	private Timer timer1; //Lectura leer=new Lectura(); private AsyncTarea asyncTarea; 
	//Bluetooth
	static BluetoothSocket bthSk;	static BluetoothDevice bthDev; static BluetoothAdapter bthAdp;
	 private Lectura lectura;
	//Otros
	static Context context; 	private InputStream is; private OutputStream os;	
	static boolean conexion; static boolean droidKeyDtc; static boolean regBrdRcv;	
	static final UUID _UUID_SPP=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private AsyncConn asyncConn; private boolean esMedVbat;
	private int cntBytes; private int bytesMedVbat[], medVbat, statChrgBat;
	private boolean notfSonidoVibBat; private Timer tempNotfBatFull;
	private Timer tmpConnDeNuevo; static boolean ACTV_DROID_KEY_ESTA_ACTIVA;
	private boolean enSoporteBthOnStartCommand=false; private boolean enCmbCntrsnaBth=false;
	private Timer tmp; private int cnt=0; private String buffer1; private byte[] nuevaClave;

	//######################################
	//########      SERVICIO    ############
	//######################################	
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("ajDroid","ServcBth::onCreate");
		// Desregistro Broadcast Receiver
		unRegBrdRcv();
		Log.i("ajDroid","Se desregistro broadcast receiver brdRcvServcBth en el Service en onCreate");
		// Registro Broadcast Receiver
		regBrdRcv();
		Log.i("ajDroid","Se registro broadcast receiver brdRcvServcBth en el Service en onCreate");
		// Instancio context y Bluetooth Adapter
		context=getBaseContext(); 
		// Instancio bytes para la medición de batería
		this.bytesMedVbat=new int[2]; 
		
		if (lectura!=null) lectura.cancel(true);
		if (asyncConn!=null) asyncConn.cancel(true);
		try {bthSk.close();}
		catch(Exception ex) {
			Log.e("ajDroid","Error al cerrar el socket desde el service probablemente no esta instanciado");
		}
		enSoporteBthOnStartCommand=true;
		soporteBth();		
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Instancio el servicio como START_STICKY que quiere decir: Si el sistema operativo mata el servicio
		// se vuelve a ejecutar de nuevo
		// Esto no se ejecuta si el sistema operativo lo mato y lo vuelve a ejecutar. Esto ha sido verificado con
		// las pruebas y revisando el logCat
		Log.i("ajDroid","Servicio onStartCommand"); Log.i("ajDroid","Service como START_STICKY");
		if (!enSoporteBthOnStartCommand) soporteBth();		
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		// Esto se ejecutar cuando el servicio ha sido detenido. Si el sistema operativo lo mata nunca llega aquí				
		LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvServc); // <--- Revisar
		Log.i("ajDroid","Se desregistro broadcast para Servicio");
		try {bthSk.close();}
		catch (Exception ex) {
			Log.e("ajDroid","Error al cerrar el bthsk desde onDestroy service");
		}
		if (lectura!=null) lectura.cancel(true);
		if (asyncConn!=null) asyncConn.cancel(true);
		Log.i("ajDroid","ServcBth::onDestroy");
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {	
		return null;
	}

	//######################################
	//######    BROADCAST RECEIVER    ######
	//######################################		
	
	
	// BROADCAST RECEIVER ACTIVITY TO SERVICE
	private final BroadcastReceiver brdRcvServc=new BroadcastReceiver() {		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			// Recibo un Broadcast para ejecutar el método soporteBth
			if (action.equals(getString(R.string.soporteBthServc))) {
				soporteBth();
			}
			// Recibo un Broadcast para ejecutar el método desconexión
			if (action.equals(getString(R.string.desconBthServc))) {
				desconBth();
			}
			// Recibo un Broadcast, que se produjo una petición de desconexión en el Bluetooth 
			if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
				Log.i("ajDroid","Se escucho: ACTION_ACL_DISCONNECT_REQUESTED");
				conexion=false; desconBth();				
			}
			// Recibo un Broadcast, que se produjo desconexión en el Bluetooth
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				Log.i("ajDroid","Se escucho: BluetoothDevice.ACTION_ACL_DISCONNECTED");
				conexion=false; desconBth();
			}
			// Recibo un Broadcast para enviar un byte por el Bluetooth
			if (action.equals(getString(R.string.escrbBthServc))) {
				if (bthSk!=null) {					
					escribirByte(intent.getIntExtra("int", 0));
				}
			}
			// Cambiar contraseña del dROIDkey
			if (action.equals(getString(R.string.cmbCntrsnaBthServc))) {
				if (bthSk!=null) {		
					String stAux1=intent.getStringExtra("string"); byte byteAux[]=stAux1.getBytes(); 
					nuevaClave=new byte[stAux1.length()+4]; // Sumo 5 a la clave por esto -> SP, + CLAVE + CR 
					nuevaClave[0]='S'; nuevaClave[1]='P';nuevaClave[2]=',';nuevaClave[stAux1.length()+3]=0x0D;
					for (int i=0;i<stAux1.length();i++) {
						nuevaClave[i+3]=byteAux[i];
					}
					Log.i("ajDroid","Listo");
					escribirByte('p');enCmbCntrsnaBth=true;
				} 
			}
//			if (action.equals(getString(R.string.statusConServc))) {
//				if (conexion) {
//					Log.i("ajDroid","Bth Conectado");
//					LocalBroadcastManager.getInstance(IniDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthConnActv)));
//				}
//				else {
//					Log.i("ajDroid","Bth Desconectado");
//					LocalBroadcastManager.getInstance(IniDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthDescon1Actv)));
//					conexion=false; desconBth();
//				}
//			}
		}
	};
	
	// BROADCAST RECEIVER ACTIVITY TO SERVICE
	private final BroadcastReceiver brdRcvServcBth=new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
				Log.i("ajDroid","Se escucho: ACTION_ACL_DISCONNECT_REQUESTED");
				conexion=false; desconBth();				
			}
			// Recibo un Broadcast, que se produjo desconexión en el Bluetooth
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				Log.i("ajDroid","Se escucho: BluetoothDevice.ACTION_ACL_DISCONNECTED");
				conexion=false; desconBth();
			}	
		}
	};
	
	//######################################
	//########       MÉTODOS       #########
	//######################################	

	private void soporteBth() {	
		bthAdp=BluetoothAdapter.getDefaultAdapter();
		// Bluetooth adaptador es nulo
		if (bthAdp==null) {		
			// Notifico al usuario desde el activity que su dispositivo no tiene Bluetooth
			if (ActvDroidKey.context!=null) {
				LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthNullActv)));					
			}
			else Log.e("ajDroid","Activity muerta");
		}
		// Bluetooth adaptador no es nulo
		else {
			// Bluetooth apagado realizo esto
			if (!bthAdp.isEnabled() && ACTV_DROID_KEY_ESTA_ACTIVA) {
				LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthApagActv)));
			}
			// Bluetooth encendido realizo esto
			else if(bthAdp.isEnabled()) {
				Set<BluetoothDevice> bthPareado = bthAdp.getBondedDevices();
				if ( (bthPareado.size()>0) && bthAdp.isEnabled()) {
					for ( BluetoothDevice bthDevSelc : bthPareado ) {				
						if ( bthDevSelc.getName().equals("dROIDkey")) {
							droidKeyDtc=true; bthDev=bthDevSelc;
							Log.i("ajDroid","DroidKey detectado en el service");
							break;
						}
					}
				}
				// Se consiguió el bth "dROIDkey" y procedo a conectarme
				if (droidKeyDtc && !conexion && bthAdp.isEnabled()) {
					if (asyncConn!=null) asyncConn.cancel(true);
					asyncConn=new AsyncConn(); asyncConn.execute();
				}
				// No se consiguió el bth "dROIDkey" y procedo en la activity a buscarlo y aparearme con el desde el menu Bluetooth
				if (!droidKeyDtc && !conexion && bthAdp.isEnabled() && ACTV_DROID_KEY_ESTA_ACTIVA) {
					LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthNoDtcActv)));
				}
			}
		}
	}
	
	private void desconBth() {
		// Se produjo una desconexión o una mala conexión y los procedimiento que aqui se indican son para cerrar el
		// socket y las tareas asincronas. Se genera de nuevo una conexión con un retraso por si se produce otra 
		// desconexión y no estar constantemente conectando, ya que esto consume recursos en el celular
		cntBytes=0;		// No borrar previene error de medición del ADC
		int retraso=0;
		if (lectura!=null) lectura.cancel(true); if (asyncConn!=null) asyncConn.cancel(true);
		if (tmpConnDeNuevo!=null) tmpConnDeNuevo.cancel();
		try {bthSk.close();}
		catch(Exception ex) {
			Log.e("ajDroid","Error al cerra el socket desde el service probablemente no esta instanciado");
		}
		// Aqui indico a la activity que cambia el texto a desconectado para el TextView relacionado al Status
		LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthDescon1Actv)));
		conexion=false;
		if (ACTV_DROID_KEY_ESTA_ACTIVA) retraso=2000;	// Retraso de 2 segundos
		else retraso=30000;	// Retraso de 30 milisegundos
		if (bthAdp.isEnabled()) {
			Log.i("ajDroid","Se va producir una nueva conexión dentro de "+retraso+" ms");
			// Comienza el retraso			
			tmpConnDeNuevo=new Timer();
			tmpConnDeNuevo.schedule(new TimerTask() {
				// Termino el retraso procedo a realizar una conexión de nuevo
				@Override
				public void run() {
//					asyncConn=new AsyncConn(); asyncConn.execute();
					soporteBth();
				}
			}, retraso);
		}
	}

	// Para enviar enviar un byte por el Bluetooth
	private void escribirByte(int byteTx) {
		try { os.write((int)byteTx); Log.i("ajDroid","Se envio el byte : "+byteTx+" desde el service");	} 
		catch (Exception ex) {Log.e("ajDroid","Error al enviar el byte : "+byteTx+" desde el service");	}
	}	
	
	private void escribirBuffer(byte[] byteTx) {
		try { os.write(byteTx);}
		catch (Exception ex) {Log.e("ajDroid","Error al enviar el byte : "+byteTx+" desde el service");	}
		Log.i("ajDroid","Se envio el buffer byte : "+byteTx+" desde el service");
	}
	
	// Para no estar notificando a cada rato de que la bateria esta cargada completamente o un nivel de voltaje bajo,
	// genero un retraso de 30 min. entre cada notificación sonora y vibratoria, ya que de lo contrario esto
	// fastidia al usuario y consume recursos en el celular.
	private void tempSonidoVibBat() {
		Log.i("ajDroid","Se va iniciar una nueva temporización de 30 min. para habilitar notificación sonora y vibratoria de que la batería esta full cargada");
		tempNotfBatFull=new Timer();
		tempNotfBatFull.schedule(new TimerTask() {
			@Override
			public void run() {
				notfSonidoVibBat=false; Log.i("ajDroid","notBatFull : "+notfSonidoVibBat);
			}
		}, 1800000L ); // Retraso de 30 min.
	}
	
	private void unRegBrdRcv() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvServc);
		try{
			unregisterReceiver(brdRcvServc);
		} catch (Exception ex){Log.e("ajDroid","Error a unregisterReceiver(brdRcvServcBth) en ServcBth");}
	}
	
	private void regBrdRcv() {
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.connBthServc)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.desconBthServc)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.soporteBthServc)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.escrbBthServc)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.statusConServc)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvServc, new IntentFilter(getString(R.string.cmbCntrsnaBthServc)));
		IntentFilter intentFilter=new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED); this.registerReceiver(this.brdRcvServcBth, intentFilter);
		intentFilter=new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED); this.registerReceiver(this.brdRcvServcBth, intentFilter);
	}
	
	private void regnTmpFueraCmbCntrsna() {
		if (tmp!=null) tmp.cancel(); tmp=new Timer(); tmp.schedule(new TmpFueraEnCmbCntrsnaBth(), 1000);
	}
	
	private void dtnTmpFueraCmbCntrsna() {
		if (tmp!=null) tmp.cancel(); enCmbCntrsnaBth=false;
	}
	
	//######################################o
	//########      SUB-CLASES      ########
	//######################################	
	
	// Proceso en segundo plano para crear una conexión e instanciar un objecto InputStream para recibir datos por
	// el Bluetooth y un objeto OutputStream para enviar datos por el Bluetooth
	private class AsyncConn extends AsyncTask<Void,Void,Void>  {
		@Override
		protected Void doInBackground(Void... params) {			
			if (bthAdp.isEnabled()) {
				Log.i("ajDroid","Iniciando ASyncTarea desde service");
				BluetoothSocket bthSkTmp=null; InputStream tmpIs=null; OutputStream tmpOs=null; 
				// Indico a la activity que cambie el Status por "Conectando"
				LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthConectandoActv)));			
				try {
//					bthSkTmp=bthDev.createRfcommSocketToServiceRecord(_UUID_SPP); conexion=true;					
					Method m=bthDev.getClass().getMethod("createRfcommSocket",new Class[] {int.class});
					bthSkTmp=(BluetoothSocket)m.invoke(bthDev, 1); conexion=true;
				}
				catch (Exception ex) {
					conexion=false;
					Log.e("ajDroid","Error en el intento bthSkTmp=bthDev.createRfcommSocketToServiceRecord(_UUID_SPP);");
				}
				if (conexion && bthAdp.isEnabled()) {
					bthSk=bthSkTmp;
					Log.i("ajDroid","Instanciado bthSkTmp=bthDev.createRfcommSocketToServiceRecord(_UUID_SPP);");
					bthAdp.cancelDiscovery(); Log.i("ajDroid","Intento para crear la conexión desde el service");
					try {bthSk.connect(); conexion=true;}
					catch(Exception ex) {
						conexion=false;	Log.e("ajDroid","Error en el intento para crear la conexión desde el service");
					}				
				}			
				if (conexion && bthAdp.isEnabled()) {
					// Se logro la conexión
					Log.i("ajDroid","dROIDkey Conectado");
					try {tmpIs=bthSk.getInputStream();}
					catch (IOException e) {
						conexion=false;	Log.e("ajDroid","Error en el intento tmpIs=bthSk.getInputStream();");
					}
					if (conexion && bthAdp.isEnabled()) {
						Log.i("ajDroid","Instanciado tmpIs=bthSk.getInputStream();");
						try {tmpOs=bthSk.getOutputStream();} 
						catch (IOException e) {
							conexion=false; Log.e("ajDroid","Error en el intento tmpIs=bthSk.getOutputStream();");
						}
						if (conexion && bthAdp.isEnabled()) {
							//Se logro instancia el objecto InputStream y OutputStream
							Log.i("ajDroid","Instanciado tmpOs=bthSk.getOutputStream();");
							is=tmpIs; os=tmpOs; conexion=true; enSoporteBthOnStartCommand=false;
							LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthConnActv)));
							Log.i("ajDroid","dROIDkey conectado");
							lectura=new Lectura(); lectura.execute();
							// Activo tiempo fuera para cambiar la contraseña
							if (enCmbCntrsnaBth) {
								try{Thread.sleep(10);} catch(Exception ex){;} regnTmpFueraCmbCntrsna(); buffer1=new String(); cnt=0;								
								byte byteAux[]=new byte[]{'$','$','$'}; escribirBuffer(byteAux);
								Log.i("ajDroid","Se envió el buffer byte : "+buffer1+" al bth");
							}
						}						
					}									
				}				
			}			
			return null;
		}
				
		@Override
		protected void onCancelled() {
			Log.w("ajDroid","Se cancelo AsyncConn desde el service");
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Se produjo un error en el proceso de crear la conexión o instanciar el objecto InputStream
			// o OutputStream. Y se procede a desconectar y a volver a conectar con el método desconBth();
			if (!conexion) {desconBth();} enSoporteBthOnStartCommand=false;
		}
	}
	
	private class Lectura extends AsyncTask<Void, Void, Void> {
		
		private int byteRcv;
		
		@Override
		protected Void doInBackground (Void... params) {
			while(true && bthAdp.isEnabled()) {
				if (isCancelled()) {break;}
				try {
					this.byteRcv=(int)is.read();
				} 
				catch (IOException ex0){break;}
				Log.i("ajDroid","Byte Recibido desde el service : "+byteRcv+", char: "+Character.toString((char) byteRcv));				
				Intent intent=new Intent(getString(R.string.byteRcvActv)); intent.putExtra("int",byteRcv);
				if (ActvDroidKey.context!=null) {
					LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(intent);	
				}
				// Realizo esto si el modo de aprendizaje o programación de control este desactivado y enCmbCntrsnaBth=false
				if (!ActvDroidKey.modoAprndzj&&!enCmbCntrsnaBth) {
					// Medición de la batería y el status del chip cargador de la batería 
					if (cntBytes>=1 && cntBytes<4) {
						++cntBytes; bytesMedVbat[cntBytes-2]=byteRcv;
						if (cntBytes==3) {
							// Medición realizada y procedo a evaluar
							cntBytes=0;	
							medVbat=bytesMedVbat[1]; medVbat|=(bytesMedVbat[0]&0x03)<<8; statChrgBat=bytesMedVbat[0]>>7;
							medVbat+=5; // Compensación
							Log.i("ajDroid","medVbat+comp ADC: "+medVbat+", medVbat-comp ADC: "+(medVbat-4)+
									" vBat-comp: "+String.valueOf((((medVbat-4)*2.5)/1024)*2)+
									", vBat+comp: "+String.valueOf((((medVbat)*2.5)/1024)*2) +
									" y el STAT_CHRG_BAT: "+statChrgBat);							
							//Log.i("ajDroid","notfBatFull : "+notfSonidoVibBat);
							new AsyncNotfChargObatBaja().execute();
						}
					}
					else if (byteRcv==100) cntBytes=1;
					else cntBytes=0;
				}
				if (!ActvDroidKey.modoAprndzj&&enCmbCntrsnaBth&&ACTV_DROID_KEY_ESTA_ACTIVA) {
					regnTmpFueraCmbCntrsna();
					// Detecto los Carry Return
					if (byteRcv==0x0D) {
						if (cnt==0&&buffer1.equals("CMD")) {
							Log.i("ajDroid","Se reconoció CMD en el proceso para cambiar la contraseña");
							new AsyncTask<Void,Void,Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									++cnt; buffer1=""; escribirBuffer(nuevaClave);									
									return null;
								}
							}.execute();
						}
						if (cnt==1&&buffer1.equals("AOK")) {
							Log.i("ajDroid","Clave cambiada con éxito");
							enCmbCntrsnaBth=false;
							new AsyncTask<Void,Void,Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									buffer1=""; byte byteAux[]=new byte[]{'R',',','1',0x0D}; escribirBuffer(byteAux);
									LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.corrCmbClvDroidKey)));
									return null;
								}
							}.execute();
						}
						else if (cnt==1&&buffer1.equals("ERR")) {
							Log.i("ajDroid","Error al cambiar la clave");
							enCmbCntrsnaBth=false;
							new AsyncTask<Void,Void,Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									buffer1=""; byte byteAux[]=new byte[]{'R',',','1',0x0D}; escribirBuffer(byteAux);	
									LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.errCmbClvDroidKey)));
									return null;
								}
							}.execute();
						}
					}
					else buffer1+=Character.toString((char) byteRcv);
				}
			}
			return null;			
		}
		
		@Override
		protected void onProgressUpdate(Void... pUpdt) {
			new AsyncNotfChargObatBaja().execute();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Log.e("ajDroid","Error en is.read() desde el service::Lectura::onPostExecute, probar ejecutar de nuevo la conexión");
			LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.bthDescon1Actv)));
			conexion=false; enSoporteBthOnStartCommand=false;
		}
		
		@Override
		protected void onCancelled() {
			Log.w("ajDroid","Se cancelo ASync leer desde el service"); conexion=false;
		}
	}	
	
	private class AsyncNotfChargObatBaja extends AsyncTask<Void,Void,Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// Notificación de batería completamente cargada			
			if ((medVbat>851) && (statChrgBat==1)) {
				NotificationManager nm=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);			
				Notification notify=new Notification(android.R.drawable.stat_notify_more,"dROIDkey: Batería cargada",System.currentTimeMillis());
				String titulo="dROIDkey: Batería cargada"; String detalles="La batería esta 100% cargada, por favor desconectar cargador";
				Intent intent=new Intent(getApplicationContext(),ServcBth.class);
				PendingIntent pending=PendingIntent.getService(getApplicationContext(), 0, intent, 0);
				notify.setLatestEventInfo(getApplicationContext(), titulo, detalles, pending);				
				if (!notfSonidoVibBat) {
					notify.defaults|=Notification.DEFAULT_VIBRATE;
					notify.defaults|=Notification.DEFAULT_SOUND; 
					notfSonidoVibBat=true; tempSonidoVibBat(); 
				}
				nm.notify(0,notify);
			}
			// Notificación de batería con nivel de voltaje bajo. Cuando el nivel de voltaje de la batería es menor a 3.15V
			else if ((medVbat<=653) && (statChrgBat==1)) {
				NotificationManager nm=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);			
				Notification notify=new Notification(android.R.drawable.stat_notify_more,"dROIDkey: Batería baja",System.currentTimeMillis());
				String titulo="dROIDkey: Batería baja"; String detalles="Por favor conectar el cargador";
				Intent intent=new Intent(getApplicationContext(),ServcBth.class);
				PendingIntent pending=PendingIntent.getService(getApplicationContext(), 0, intent, 0);
				notify.setLatestEventInfo(getApplicationContext(), titulo, detalles, pending);				
				if (!notfSonidoVibBat) {
					notify.defaults|=Notification.DEFAULT_VIBRATE;
					notify.defaults|=Notification.DEFAULT_SOUND; 
					notfSonidoVibBat=true; tempSonidoVibBat(); 
				}
				nm.notify(0,notify);
			}
			return null;
		}
		
	}
	private class AsyncCallSoporteBth extends AsyncTask<Void,Void,Void>{

		@Override
		protected Void doInBackground(Void... params) {
			soporteBth(); return null;
		}
	}
	private class TmpFueraEnCmbCntrsnaBth extends TimerTask {
		@Override
		public void run() {enCmbCntrsnaBth=false;}
	}
	
}
