package aj.apps.droidkey;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import aj.apps.droidkey.R;

public class ActvCmdVoz extends Activity {
	private final int rcnzDeVoz=4;
	
	@Override
	public void onCreate(Bundle SavedInstanceState) {
		super.onCreate(SavedInstanceState); setContentView(R.layout.actv_cmd_voz);
		Log.i("ajDroid","ActvCmdVoz::onCreate()");
		//Matar ActvDroidkey
//		if (ActvDroidKey.context!=null) {
//			LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.salirActvDroidKey)));
//		}
		//Reconocimiento de voz
		PackageManager pm = getPackageManager();
        List<ResolveInfo> activities=pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),0);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Esperando comando de voz...");
		startActivityForResult(intent, this.rcnzDeVoz);		
	}
	
	@Override 
	public void onDestroy() {
		Log.i("ajDroid","ActvCmdVoz::onDestroy()");
		super.onDestroy();
	}	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==rcnzDeVoz && resultCode==RESULT_OK) {
			if (ServcBth.conexion) {
				ArrayList<String> arrListStr=new ArrayList<String>();
				arrListStr=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				AdminBDSQLite gestorSQLite; SQLiteDatabase bdSQLite;
				gestorSQLite=new AdminBDSQLite(ActvDroidKey.context,"bdSqliteDroidKey00",null,1);
				boolean cmdDeVozRcnz=false, ctrlCnsgdo=false, dtcAprnd=false; int aux=0; String[] stAux=new String[4];
				stAux[0]=stAux[1]=stAux[2]=stAux[3]="";			
				// Busco todas las oraciones reconocidas por el reconocimento de voz
				for (int i=0; i<arrListStr.size(); i++) {
					// Almaceno la primera oración del reconocimiento de voz
					stAux[0]=((String) arrListStr.get(i)).toString().toLowerCase().trim();
					// Busco el index del primer espacio de la primera palabra
					aux=((String) arrListStr.get(i)).toString().indexOf(' ',0);
					// Si el index es diferente de -1 toma la primera palabra y el resto
					if (aux!=-1) {					
						stAux[1]=stAux[0].substring(0,aux); // Tomo la primera palabra
						stAux[2]=stAux[0].substring(aux+1,stAux[0].length()); // Tomo el resto de la oración despues de la primera palabra
					}
					// Busco en esta condición si la primera palabra es aprender en español, ingles, protugués, italiano, frances y alemán 
					if ((stAux[0].equals("aprender")||stAux[0].equals("learn")||stAux[0].equals("imparare")
							||stAux[0].equals("imparare")||stAux[0].equals("apprendre")||stAux[0].equals("lernen")) 
							&&(!cmdDeVozRcnz)) {
						cmdDeVozRcnz=true;
						Log.i("ajDroid","Se reconoció Aprender : "+stAux[0]);					
						new AsyncTask<Void,Void,Void>() {
							@Override
							protected Void doInBackground(Void... params) {		
								Intent intent=new Intent(getBaseContext(),ActvDroidKey.class); intent.putExtra("aprendizaje",true);
								PendingIntent pendingIntent=PendingIntent.getActivity(getBaseContext(),0,intent,0);
								try {								
									pendingIntent.send();
								} catch (CanceledException e) {
									e.printStackTrace();
								}
//								startActivity(pendingIntent);
//								LocalBroadcastManager.getInstance(ActvDroidKey.context).sendBroadcast(new Intent(getString(R.string.iniAprndzj)));
								return null;
							}
						}.execute();
						break;
					}
					// Busco en esta condición si la primera palabra es abrir en español, ingles, portugués, italiano, francés y alemán
					// y el resto de la oración me indica la puerta que se desea abrir
					if ((stAux[1].equals("abrir")||stAux[1].equals("open")||stAux[1].equals("aprire")||stAux[1].equals("aperto")||
							stAux[1].equals("ouvert")||stAux[0].equals("offen"))&&(aux!=-1)&&(!cmdDeVozRcnz)) {
						cmdDeVozRcnz=true; stAux[3]=stAux[2];
						dtcAprnd=true; Cursor cursor=null; String[] stCursor=new String[]{};
						// Encriptar nombre de la puerta o acceso que se desea abrir para luego buscarla en la bd sqlite
						try {
							stAux[2]=SimpleCrypto.encrypt(ActvDroidKey.stSeed, stAux[2]);
						} catch(Exception ex) {
							Log.e("ajDroid","Error al encriptar el nombre del acesso, recibido desde el comando de voz");
						}
						bdSQLite=gestorSQLite.getReadableDatabase(); 
						try {
							cursor=bdSQLite.rawQuery("SELECT * FROM y00 WHERE z00='"+stAux[2]+"';",stCursor);
						} catch(Exception ex) {
							Log.e("ajDroid","Error al consultar en bd sqlite si el acceso que se ha escuchado desde el comando de voz existe");
						}
						if (cursor!=null && cursor.moveToFirst()) {ctrlCnsgdo=true;}
						bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada");
						if (ctrlCnsgdo) {
							Toast toast1=Toast.makeText(getBaseContext(),"Ok, abriendo la puerta \""+stAux[3]+"\"",Toast.LENGTH_SHORT);
							toast1.show();
							new AsyncRutReproCtrl(stAux[3]).execute();
							break;
						}
					}
				}
				if (!cmdDeVozRcnz) {
					Log.i("ajDroid","No se reconoció el comando de voz");
					Toast toast1=Toast.makeText(getBaseContext(), "Comando de voz no reconocido", Toast.LENGTH_SHORT);
					toast1.show();
				}
				if(dtcAprnd && !ctrlCnsgdo) {
					Log.i("ajDroid","No se reconoció el comando de voz");
					Toast toast1=Toast.makeText(getBaseContext(), "Nombre de control no reconocido", Toast.LENGTH_SHORT);
					toast1.show();
				}
			}
			else {
				Toast toast1=Toast.makeText(getBaseContext(), "dROIDkey no conectado", Toast.LENGTH_SHORT);
				toast1.show();
			}
		}
		if (requestCode==rcnzDeVoz && resultCode==RESULT_CANCELED) {
			Log.i("ajDroid","Se cancelo el reconocimiento de Voz en ActvCmdVoz");
		}
		finish();
	}
}
