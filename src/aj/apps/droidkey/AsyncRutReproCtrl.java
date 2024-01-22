package aj.apps.droidkey;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class AsyncRutReproCtrl extends AsyncTask<Void,Void,Void> {
	private String nmbrCtrl; private AdminBDSQLite gestorSQLite; private SQLiteDatabase bdSQLite;
	
	public AsyncRutReproCtrl(String nmbrCtrl) {
		this.nmbrCtrl=nmbrCtrl;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Cursor cursor=null; String[] stCursor=new String[]{}; Boolean ctrlCnsgdo=false;
		String stCntFl="", stTrst="", stMtrasPort="", stTindc="", stTcod="";		
		//Encripto el nombre del control para luego buscarlo
		try {
			nmbrCtrl=SimpleCrypto.encrypt(ActvDroidKey.stSeed,nmbrCtrl); 
		} catch (Exception ex) {
			Log.e("ajDroid","Error al encryptar la posición del control en onItemClick");
		}
		//BD
		gestorSQLite=new AdminBDSQLite(ActvDroidKey.context,"bdSqliteDroidKey00",null,1);
		bdSQLite=gestorSQLite.getReadableDatabase(); 
		Log.i("ajDroid","Se va leer en la bd sqlite para reproducir ctrls desde listview");			
		Log.i("ajDroid","Se va realizar la consulta : select * from y00 where z00='"+nmbrCtrl+"'; bd sqlite");
		try {
			cursor=bdSQLite.rawQuery("SELECT * FROM y00 WHERE z00='"+nmbrCtrl+"';",stCursor);
			Log.i("ajDroid","Consulta realizada bd sqlite");
		} catch (SQLException ex){
			Log.e("ajDroid","Error al realizar la consulta bd sqlite select * from y00 where z00='"+nmbrCtrl+";");
		}
		if (cursor!=null && cursor.moveToFirst()) {				
			stCntFl=cursor.getString(1); stTrst=cursor.getString(2); stTindc=cursor.getString(3); stTcod=cursor.getString(4); 
			stMtrasPort=cursor.getString(5); ctrlCnsgdo=true;				
		}
		bdSQLite.close(); Log.i("ajDroid","Se cerró la bd sqlite");	
		if (ctrlCnsgdo) {
			try {
				stCntFl=SimpleCrypto.decrypt(ActvDroidKey.stSeed, stCntFl);
				stTrst=SimpleCrypto.decrypt(ActvDroidKey.stSeed, stTrst);
				stTindc=SimpleCrypto.decrypt(ActvDroidKey.stSeed, stTindc);
				stTcod=SimpleCrypto.decrypt(ActvDroidKey.stSeed,stTcod);
				stMtrasPort=SimpleCrypto.decrypt(ActvDroidKey.stSeed,stMtrasPort);
			} catch (Exception ex) {
				Log.e("ajDroid","Error al desencriptar los valores de la fila selecionada de la bd sqlite");
			}
			ArrayList<Integer> arrListInt1=new ArrayList<Integer>();
			int intAux[]=new int[2]; 
			while(true) {
				if (isCancelled()) break;
				intAux[1]=stTindc.indexOf(',',intAux[0]+1);			
				if (intAux[0]==0) {
					intAux[1]=stTindc.indexOf(',',intAux[0]);
					arrListInt1.add(Integer.parseInt(stTindc.substring(intAux[0],(intAux[1]))));
				}
				else if ((intAux[1]!=-1) && (intAux[0]>0)) {
					arrListInt1.add(Integer.parseInt(stTindc.substring(intAux[0]+1,(intAux[1]))));
				}
				intAux[0]=intAux[1];
				if (intAux[1]==-1) {break;}
			}	
			intAux=new int[2];
			ArrayList<Integer> arrListInt2=new ArrayList<Integer>();
			while(true) {
				if (isCancelled()) break;
				intAux[1]=stTcod.indexOf(',',intAux[0]+1);			
				if (intAux[0]==0) { 
					intAux[1]=stTcod.indexOf(',',intAux[0]);
					arrListInt2.add(Integer.parseInt(stTcod.substring(intAux[0],(intAux[1]))));
				}
				else if ((intAux[1]!=-1) && (intAux[0]>0)) {
					arrListInt2.add(Integer.parseInt(stTcod.substring(intAux[0]+1,(intAux[1]))));
				}
				intAux[0]=intAux[1];
				if (intAux[1]==-1) {
					break;
				}
			}
			Float ftAux=(float)((((((Integer.parseInt(stMtrasPort)*64)/(103.036437247*Math.pow(10,-6)))/1000000)/16)-16)*4096);
	        DecimalFormatSymbols decSym=new DecimalFormatSymbols(); DecimalFormat decForm=new DecimalFormat("#",decSym);				
			int intMtrasPort=(int)Integer.parseInt(decForm.format(ftAux).toString()); intMtrasPort=intMtrasPort>>8;						
			//'r'
			Intent intent=new Intent("escrbBthServc"); 
			intent.putExtra("int",(int)'r'); 
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
			Log.i("ajDroid","Se envió el byte : " + "r");
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			//'r'
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); 
			Log.i("ajDroid","Se envió el byte : " + "r"); 
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			// _FIN_TINDC
			intAux[0]=7+arrListInt1.size();	intent.putExtra("int",intAux[0]);
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); Log.i("ajDroid","Se envió el byte : "+intAux[0]); 
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			 //_FIN_TRAMA
			intAux[0]=intAux[0]+arrListInt2.size(); intent.putExtra("int",intAux[0]);
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); Log.i("ajDroid","Se envió el byte : "+intAux[0]); 
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			//cntFl
			intent.putExtra("int",Integer.parseInt(stCntFl));
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); Log.i("ajDroid","Se envió el byte : " + Integer.parseInt(stCntFl)); 
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			 // tRst H
			intent.putExtra("int",(Integer.parseInt(stTrst)>>8));
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); Log.i("ajDroid","Se envió el byte : " + (Integer.parseInt(stTrst)>>8));
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			// tRst L				
			intent.putExtra("int",(Integer.parseInt(stTrst)&0xFF));
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent); Log.i("ajDroid","Se envió el byte : " + (Integer.parseInt(stTrst)&0xFF));
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			//mtrasPort = _FREC
			intent.putExtra("int",intMtrasPort);
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent) ;Log.i("ajDroid","Se envió el byte : " + intMtrasPort); 
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			// tIndc
			for (int i=0;i<arrListInt1.size();i++) {
				if (isCancelled()) break;
				intent.putExtra("int", ((Integer)arrListInt1.get(i)).intValue());
				LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
				try {Thread.sleep(2);}catch(InterruptedException ex){;}  //tIndc
			}			
			try {Thread.sleep(2);}catch(InterruptedException ex){;}
			// tCod
			for (int i=0;i<arrListInt2.size();i++) {
				if (isCancelled()) break;
				intent.putExtra("int",((Integer)arrListInt2.get(i)).intValue());
				LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
				try {Thread.sleep(2);}catch(InterruptedException ex){;}  //tCod
			}			
			try {Thread.sleep(2);}catch(InterruptedException ex){;} publishProgress();
			LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent("enListViewCtrls"));			
		}
		return null;
	}

}
