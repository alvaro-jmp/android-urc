package aj.apps.droidkey;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import aj.apps.droidkey.R;

public class ActvDroidKey extends Activity implements OnClickListener, OnKeyListener, OnItemClickListener, OnItemLongClickListener {

	//######################################
	//####    OBJETOS, VAR Y CONST     #####
	//######################################	
	
	//Objetos Bluetooths
	private BluetoothSocket bthSk=null; private BluetoothDevice bthDev=null; private BluetoothAdapter bthAdp=null;
	
	//Otros Objetos
	private int byteRecibido; private static boolean conexion; private static boolean dtcDroidKey;
	private AsyncRutAprn asyncRutAprn;
	private AdminBDSQLite gestorSQLite; private SQLiteDatabase bdSQLite; ArrayAdapter<String> arrAdpGestorListCtrls;
	private Cursor cursor; private TmpTiempoFueraAprnd tmpTiempoFueraAprnd;
	private Timer tmp; private boolean ctrlCnsgdo; private int posLsCtrlLv;	
	
	//Controles
	private Button btnAprndDroidKeyMain; private AlertDialog alrtDlg;
	private Dialog dlg; private AnimationDrawable animDraw00; private ImageView imgDialogAprndzj;
	private EditText etDlgAprndRlzdo; private Button btnAceptDlgAprndRlzdo; private Button btnCancelDlgAprndRlzdo; 
	private Button btnOkDlgAprndErr; private ListView lvListCtrlsIniDroidKey; private ListView lvOpcnsRpCtrl;
	private TextView tvStatIniDroidKey; static Context context; private ImageButton imgBtnVozIniDroidKey;
	
	//Constantes
	static final int bthOn=2, bthApareado=3, rcnzDeVoz=4;	
	// dROIDkey="00:06:66:46:AD:00" | bthPC="10:85:F8:90:BA:64"
	static final String macDroidKey="00:06:66:46:AD:00";
	
	//Atributos
	static boolean modoAprndzj; private ArrayList[] lsArrAux; private String[] stAux;	  
	private int scnciaAprn, scnciaErr; 
	private int intCntFl, intPosTcod, intTrst, intMtrasPort, intAux[]; 
	private String stTindc="", stTcod="", stNombreCtrl="", stCntFl="", stTrst="", stMtrasPort="";
	static final String stSeed="?ksh329732 '!$Â·!'3|@#4120i2 ''d6548/q/ 8/*qw0 87qw-Âº!!";
	private ArrayList<Integer> arrLstInt;
	private boolean nEnRlzAprndzj; private int cntBytesRcb;
	
	//######################################
	//########      ACTIVIDADES    #########
	//######################################	
	
	public void onCreate(Bundle SavedInstanceState) {
		super.onCreate(SavedInstanceState); Log.i("ajDroid","ActvDroidKey::onCreate");
		// Iniciar Servicio
		startService(new Intent(this,ServcBth.class));
		ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA=true;
		Log.i("ajDroid","ServcBth.ActivityiniDroidKeyActvo : "+ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA+" en onCreate()");
		//Referencias
 		this.setContentView(R.layout.actv_droidkey);
		this.btnAprndDroidKeyMain=(Button)findViewById(R.id.btnAprndDroidKeyMain);
		this.imgBtnVozIniDroidKey=(ImageButton)findViewById(R.id.imgBtnMicDroidKeyMain);
		this.lvListCtrlsIniDroidKey=(ListView)findViewById(R.id.lvDroidKeyMainListCtrls);
		this.tvStatIniDroidKey=(TextView)findViewById(R.id.tvDroidKeyMainStatus);
		//Listener
		this.btnAprndDroidKeyMain.setOnClickListener(this); this.lvListCtrlsIniDroidKey.setOnItemClickListener(this);
		this.lvListCtrlsIniDroidKey.setOnItemLongClickListener(this); this.imgBtnVozIniDroidKey.setOnClickListener(this);
		//Dialogs
		this.dlg=new Dialog(this); this.lsArrAux=newArray(ArrayList.class,5);		
		//Bth
		bthAdp=BluetoothAdapter.getDefaultAdapter();
		//BD
		this.gestorSQLite=new AdminBDSQLite(this,"bdSqliteDroidKey00",null,1);
		//Context
		context=getBaseContext();
		//Desregistro LocalBroadcastReceiver
		this.unRegBrdRcv();
		//Registro LocalBroadcastReceiver
		this.regBrdRcv();
		//Actualizo la lista de controles
		this.actlzListCtrls();
		//Reconocimiento de voz
		PackageManager pm = getPackageManager();
        List<ResolveInfo> activities=pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),0);
        //Recogiendo valor pasado por un intent
        Bundle bundle=this.getIntent().getExtras(); boolean iniAprnd=false;
        if (bundle!=null) iniAprnd=bundle.getBoolean("aprendizaje");
        if (iniAprnd) {
        	Log.i("ajDroid","Si se detecto del bundle inicio para aprender ctrl");
        	aprndCtrl();
        }
        else Log.i("ajDroid","No se ha detectado del bundle inicio para aprender ctrl");		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater=getMenuInflater(); menuInflater.inflate(R.layout.menu_actv_droidkey,menu);		
		return true;
	}

	@Override
	protected void onDestroy() {
		ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA=false;
		Log.i("ajDroid","ServcBth.ActivityiniDroidKeyActvo : "+ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA+" en onDestroy()");
		super.onDestroy(); Log.w("ajDroid","ActvDroidKey::onDestroy");
	}

	@Override
	protected void onResume() {super.onResume(); Log.i("ajDroid","ActvDroidKey::onResume");}	
	
	@Override
	protected void onPause() {Log.i("ajDroid","ActvDroidKey::onPause"); super.onPause();}
	
	@Override
	protected void onStop() {			
		ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA=false;
		Log.i("ajDroid","ServcBth.ActivityiniDroidKeyActvo : "+ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA+" en onStop()");
		// desregistro y finalizo la aplicación para que no se inicie de nuevo cuando llamo al ActvCmdVoz
		this.unRegBrdRcv();	
		super.onStop(); Log.i("ajDroid","ActvDroidKey::onStop");
		finish();
	}
	
	@Override
	protected void onRestart() {Log.i("ajDroid","ActvDroidKey::onRestart"); super.onRestart();}
	
	@Override
	protected void onStart() {
		ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA=true;
		Log.i("ajDroid","ServcBth.ActivityiniDroidKeyActvo : "+ServcBth.ACTV_DROID_KEY_ESTA_ACTIVA+" en onStart()");
		if (ServcBth.context!=null && ServcBth.conexion) {
			tvStatIniDroidKey.setText(Html.fromHtml("Status: <b>Conectado</b>"));
		}
		else if (ServcBth.context!=null && !ServcBth.conexion) {
			tvStatIniDroidKey.setText(Html.fromHtml("Status: <font color='red'><u>Desconectado</u></font>"));
		}
		super.onStart(); Log.i("ajDroid","ActvDroidKey::onStart");
	}

	//######################################
	//########      LISTENER       #########
	//######################################	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menuDroidKeyMainReConn:
				LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(new Intent(getString(R.string.soporteBthServc)));
				return true;
			case R.id.menuDroidKeyCmbClv:
				if (ServcBth.conexion) {
					final Dialog dlgCmbClv=new Dialog(this); dlgCmbClv.setContentView(R.layout.dlg_cmb_clv_bth);
					dlgCmbClv.setTitle("Cambiar clave dROIDkey"); 
					Button btnAceptDlgCmbClv, btnCancelDlgCmbClv; final EditText etDlgCmbClv;				
					btnAceptDlgCmbClv=(Button) dlgCmbClv.findViewById(R.id.btnAceptDlgCmbClvDroidKey);
					btnCancelDlgCmbClv=(Button) dlgCmbClv.findViewById(R.id.btnCancelDlgCmbClvDroidKey);
					etDlgCmbClv=(EditText)dlgCmbClv.findViewById(R.id.etDlgCmbClvDroidKey);
					btnAceptDlgCmbClv.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if ((!etDlgCmbClv.getText().toString().equals(""))&&(etDlgCmbClv.getText().toString().length()>=4)) {
								String stAux=etDlgCmbClv.getText().toString();
								Log.i("ajDroid","contraseña clave droidkey no es vacia y posee 4 caracteres y es "+stAux);
								Intent intent=new Intent(getString(R.string.cmbCntrsnaBthServc)); intent.putExtra("string",stAux);
								LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
								dlgCmbClv.dismiss();
							}
							else if (etDlgCmbClv.getText().toString().equals("")) {
								Toast toast1=Toast.makeText(getBaseContext(), "Contraseña vacía", Toast.LENGTH_SHORT);
								toast1.show();
							}
							else if (etDlgCmbClv.getText().toString().length()<4) {
								Toast toast1=Toast.makeText(getBaseContext(), "Contraseña menor a 4 caracteres", Toast.LENGTH_SHORT);
								toast1.show();
							}						
						}
					});
					btnCancelDlgCmbClv.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) { dlgCmbClv.dismiss(); }
					});
					
					dlgCmbClv.show();
				}
				else {
					Toast toast1=Toast.makeText(getBaseContext(), "dROIDkey no conectado", Toast.LENGTH_SHORT);
					toast1.show();
				}
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
		
	@Override
	public void onClick(View v) {
		if (v.getId()==this.btnAprndDroidKeyMain.getId()) {
			aprndCtrl();
		}
		if (v.getId()==this.imgBtnVozIniDroidKey.getId()) {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Esperando comando de voz...");
			startActivityForResult(intent, rcnzDeVoz);			
		}
	}

	@Override
	public void onBackPressed() {
		this.unRegBrdRcv();	finish();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (ServcBth.context!=null && ServcBth.conexion && (parent.getId()==this.lvListCtrlsIniDroidKey.getId())) {
			String stNombreCtrl=lvListCtrlsIniDroidKey.getItemAtPosition(position).toString().trim();
			new AsyncRutReproCtrl(stNombreCtrl).execute();
		}
		else {
			Toast toast1=Toast.makeText(getBaseContext(), "dROIDkey no conectado", Toast.LENGTH_SHORT);
			toast1.show();			
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
			long id) {	
		posLsCtrlLv=position; stAux=new String[2]; 
		stNombreCtrl=stAux[0]=this.lvListCtrlsIniDroidKey.getItemAtPosition(position).toString();		
		//Encripto el nombre para usarlo en la opcion cambiar nombre
		try {
//			stAux[1]=SimpleCrypto.encrypt(stSeed, String.valueOf(posLsCtrlLv));
			stNombreCtrl=SimpleCrypto.encrypt(stSeed, stNombreCtrl);
		} catch (Exception ex) {
			Log.e("ajDroid","Error al encryptar nombre del control en onItemLongClick");
		}
		final Dialog dlgOpcnsLvRpCtrl=new Dialog(this);	final Dialog dlgCmbrNmbrCtrl=new Dialog(this);
		//Dialog opciones reproducir control
		dlgOpcnsLvRpCtrl.setContentView(R.layout.dlg_opcns_lv_rp_ctrls);
		dlgOpcnsLvRpCtrl.setTitle("Control : "+stAux[0]);
		lvOpcnsRpCtrl=(ListView)dlgOpcnsLvRpCtrl.findViewById(R.id.lvOpcnsRpCtrl);
		String[] opcnsLv=getResources().getStringArray(R.array.opcnsLvRpCtrl);
		ArrayAdapter<String> arrAdpLv=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,opcnsLv);
		lvOpcnsRpCtrl.setAdapter(arrAdpLv);	
		lvOpcnsRpCtrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent1, View view1,
					int position1, long id1) {
				if (parent1.getId()==lvOpcnsRpCtrl.getId()) {
					if (lvOpcnsRpCtrl.getItemAtPosition(position1).toString().equals("Cambiar nombre")) {
						dlgOpcnsLvRpCtrl.dismiss();
						dlgCmbrNmbrCtrl.requestWindowFeature(Window.FEATURE_NO_TITLE);
						dlgCmbrNmbrCtrl.setContentView(R.layout.dlg_cmbr_nmbr_ctrl);
						final Button btnAceptDlgCmbrNmbrCtrl=(Button)dlgCmbrNmbrCtrl.findViewById(R.id.btnAceptDlgCmbrNmbrCtrl);
						final Button btnCancelDlgCmbrNmbrCtrl=(Button)dlgCmbrNmbrCtrl.findViewById(R.id.btnCancelDlgCmbrNmbrCtrl);
						final EditText etDlgCmbrNmbrCtrl=(EditText)dlgCmbrNmbrCtrl.findViewById(R.id.etDlgCmbrNmbrCtrl);
						btnAceptDlgCmbrNmbrCtrl.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								if (v.getId()==btnAceptDlgCmbrNmbrCtrl.getId()) {
									stAux[0]=etDlgCmbrNmbrCtrl.getText().toString().toLowerCase().trim();
									if (!stAux[0].equals("")) {
										Cursor cursor=null; String[] stCursor=new String[]{};								
										bdSQLite=gestorSQLite.getReadableDatabase();
										//Encripto el nuevo nombre										
										try {
											stAux[0]=SimpleCrypto.encrypt(stSeed, stAux[0]);
											Log.i("ajDroid","Se encripto el nuevo nombre del control");
										} catch(Exception ex) {
											Log.e("ajDroid","Error al encriptar el nuevo del control");
										}
										try {
											cursor=bdSQLite.rawQuery("SELECT * FROM y00 WHERE z00='"+stAux[0]+"';",stCursor);
										} catch(Exception ex) {
											Log.e("ajDroid","Error en la consulta para verificar si el nombre del control esta repetido");
										}
										// Nombre control repetido
										if (cursor.moveToFirst() && cursor!=null) {
											aDlgInfo("Este nombre se encuentra en uso en la lista, por favor escriba otro nombre", "Nombre de control repetido", android.R.drawable.ic_dialog_alert);
											bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada");
										}
										// Nombre de control no repetido
										else {
											bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada"); 
											//Guardo el nuevo nombre en la bd sqlite										
											bdSQLite=gestorSQLite.getWritableDatabase();
											Log.i("ajDroid","Se abriÃ³ la bd sqlite para escribir en la misma");
											try {											
												bdSQLite.execSQL("UPDATE y00 SET z00='"+stAux[0]+"' WHERE z00='"+stNombreCtrl+"';");
												Log.i("ajDroid","Se actualizo el nuevo nombre del control en la bd sqlite");
											} catch(SQLException ex) {
												Log.e("ajDroid","Error al actualizar el nombre de un ctrl en bd sqlite");
											}	
											bdSQLite.close(); Log.i("ajDroid","Se ha cerrado la bd sqlite"); dlgCmbrNmbrCtrl.dismiss();	
											actlzListCtrls();
										}
									}
									else {
										Toast toast1=Toast.makeText(getBaseContext(), "El campo del nombre del control esta vacÃ­o, intente de nuevo", Toast.LENGTH_SHORT);
										toast1.show();
									}
								}				
								
							}
						});
						btnCancelDlgCmbrNmbrCtrl.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								if (v.getId()==btnCancelDlgCmbrNmbrCtrl.getId()) {
									dlgCmbrNmbrCtrl.dismiss();
									Log.i("ajDroid","Se cancelo el dialog cambiar nombre de control");
								}
							}
						});
						dlgCmbrNmbrCtrl.show();
					}
					if (lvOpcnsRpCtrl.getItemAtPosition(position1).toString().equals("Eliminar")) {
						dlgOpcnsLvRpCtrl.dismiss(); bdSQLite=gestorSQLite.getWritableDatabase();
						try {
							bdSQLite.execSQL("DELETE FROM y00 WHERE z00='"+stNombreCtrl+"';");
							Log.i("ajDroid","Se eliminÃ³ el control de la bd sqlite");
						} catch(SQLException ex) {
							Log.e("ajDroid","Error al eliminar el control de la bd sqlite");
						}
						bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada"); 
						actlzListCtrls();						
					}
				}
			}
			
		});		
		dlgOpcnsLvRpCtrl.show();	
		return true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {return false;}
		
	//######################################
	//######## ON ACTIVITY RESULT  #########
	//######################################	

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==bthOn && resultCode==RESULT_CANCELED) {
			Log.i("ajDroid","Bluetooth apagado desde onActivityResult");
			alrtDlg=new AlertDialog.Builder(this).create(); alrtDlg.setTitle("Bluetooth Desactivado");
			alrtDlg.setMessage("Se requiere que el Bluetooth este activado para el funcionamiento de la aplicaciÃ³n de todos modos si selecciona Salir se saldra de la aplicaciÃ³n, mientras que activar le preguntarÃ¡ de nuevo si desea activar el Bluetooth, que le recomendamos que lo haga.");
			alrtDlg.setButton(AlertDialog.BUTTON_POSITIVE,"Salir",new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialgo, int which) {
						fin();
					}
			});
			alrtDlg.setButton(AlertDialog.BUTTON_NEGATIVE,"Activar Bluetooth",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialgo, int which) {
					soporteBth();
				}
			});
			alrtDlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
		        @Override
		        public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
		            if (keyCode == KeyEvent.KEYCODE_BACK && 
		                event.getAction() == KeyEvent.ACTION_UP && 
		                !event.isCanceled()) {
		                alrtDlg.dismiss(); fin();
		                return true;
		            }
		            return false;
		        }
		    });
			alrtDlg.show();
		}	
		if (requestCode==bthOn && resultCode==RESULT_OK || requestCode==bthApareado) {			
			Log.i("ajDroid","Bluetooth encendido o apareado desde onActivityResult");
			soporteBth();
		}
		if (requestCode==rcnzDeVoz && resultCode==RESULT_OK) {
			ArrayList<String> arrListStr=new ArrayList<String>();
			arrListStr=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			boolean cmdDeVozRcnz=false, ctrlCnsgdo=false, dtcAprnd=false; int aux=0; String[] stAux=new String[4];
			stAux[0]=stAux[1]=stAux[2]=stAux[3]="";			
			// Busco todas las oraciones reconocidas por el reconocimento de voz
			for (int i=0; i<arrListStr.size(); i++) {
				// Almaceno la primera oraciÃ³n del reconocimiento de voz
				stAux[0]=((String) arrListStr.get(i)).toString().toLowerCase().trim();
				// Busco el index del primer espacio de la primera palabra
				aux=((String) arrListStr.get(i)).toString().indexOf(' ',0);
				// Si el index es diferente de -1 toma la primera palabra y el resto
				if (aux!=-1) {					
					stAux[1]=stAux[0].substring(0,aux); // Tomo la primera palabra
					stAux[2]=stAux[0].substring(aux+1,stAux[0].length()); // Tomo el resto de la oraciÃ³n despues de la primera palabra
				}
				// Busco en esta condiciÃ³n si la primera palabra es aprender en espaÃ±ol, ingles, protuguÃ©s, italiano, frances y alemÃ¡n 
				if ((stAux[0].equals("aprender")||stAux[0].equals("learn")||stAux[0].equals("imparare")
						||stAux[0].equals("imparare")||stAux[0].equals("apprendre")||stAux[0].equals("lernen")) 
						&&(!cmdDeVozRcnz)) {
					cmdDeVozRcnz=true;
					Log.i("ajDroid","Se reconociÃ³ Aprender : "+stAux[0]);					
					new AsyncTask<Void,Void,Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							aprndCtrl(); return null;
						}
					}.execute();
					break;
				}
				// Busco en esta condiciÃ³n si la primera palabra es abrir en espaÃ±ol, ingles, portuguÃ©s, italiano, francÃ©s y alemÃ¡n
				// y el resto de la oraciÃ³n me indica la puerta que se desea abrir
				if ((stAux[1].equals("abrir")||stAux[1].equals("open")||stAux[1].equals("aprire")||stAux[1].equals("aperto")||
						stAux[1].equals("ouvert")||stAux[0].equals("offen"))&&(aux!=-1)&&(!cmdDeVozRcnz)) {
					cmdDeVozRcnz=true; stAux[3]=stAux[2];
					dtcAprnd=true; Cursor cursor=null; String[] stCursor=new String[]{};
					// Encriptar nombre de la puerta o acceso que se desea abrir para luego buscarla en la bd sqlite
					try {
						stAux[2]=SimpleCrypto.encrypt(stSeed, stAux[2]);
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
						this.lvListCtrlsIniDroidKey.setEnabled(false); 
						Toast toast1=Toast.makeText(getBaseContext(),"Ok, abriendo la puerta \""+stAux[3]+"\"",Toast.LENGTH_SHORT);
						toast1.show();
						new AsyncRutReproCtrl(stAux[3]).execute();
						break;
					}	
					else lvListCtrlsIniDroidKey.setEnabled(true);
				}
			}
			if (!cmdDeVozRcnz) {
				Log.i("ajDroid","No se reconociÃ³ el comando de voz");
				Toast toast1=Toast.makeText(getBaseContext(), "Comando de voz no reconocido", Toast.LENGTH_SHORT);
				toast1.show();
			}
			if(dtcAprnd && !ctrlCnsgdo) {
				Log.i("ajDroid","No se reconociÃ³ el comando de voz");
				Toast toast1=Toast.makeText(getBaseContext(), "Nombre de control no reconocido", Toast.LENGTH_SHORT);
				toast1.show();
			}
		}
	}
	
	//######################################
	//######    BROADCAST RECEIVER    ######
	//######################################		
	
	// BROADCAST RECEIVER ACTIVITY 
	private final BroadcastReceiver brdRcvDroidKey=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			if (action!=null) {
				if (action.equals(getString(R.string.bthNullActv))) {
					aDlgInfo("Su dispositivo no posee Bluetooth, se cerrarÃ¡ la aplicaciÃ³n. P"
							+ "resione OK para cerrar la aplicaciÃ³n", "Soporte Bluetooh no encontrado", 
							android.R.drawable.ic_dialog_alert,2); // --> 2 indica que se va salir de la aplicaciÃ³n por que no hay soporte bluetooth
				}
				if (action.equals(getString(R.string.bthApagActv))) {
					Log.w("ajDroid","Bth apagado, msj recibido en activity");
					Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(intent1, bthOn);
				}
				if (action.equals(getString(R.string.bthNoDtcActv))) {
					Log.i("ajDroid","Bth no detectado, msj recibido en activity");					
					aDlgInfo("Recuerde seleccionar dROIDkey, si no aparece presione el botÃ³n explorar hasta que aparezca. La contraseÃ±a es 1234 y RECUERDE PRESIONAR EL BOTON ATRAS CUANDO HAYA APAREADO !!!!!" , "dROIDkey no detectado", android.R.drawable.ic_dialog_alert,
						3); //-> el 3 indica que se va proceder a cambiar al menu de configuraciÃ³n del bth para aparear con el bth.				
				}				
				if (action.equals(getString(R.string.bthConectandoActv))) {
					Log.i("ajDroid","Droidkey conectando, msj recibido en activity");	
					tvStatIniDroidKey.setText(Html.fromHtml("Status: <i>Conectando</i>"));
				}
				if (action.equals(getString(R.string.bthConnActv))) {
					Log.i("ajDroid","Droidkey conectado, msj recibido en activity");	
					tvStatIniDroidKey.setText(Html.fromHtml("Status: <b>Conectado</b>"));
				}
				if (action.equals(getString(R.string.bthDescon1Actv))) {
					Log.w("ajDroid","Droidkey desconectado, msj recibido en activity");
					//aDlgInfo("Se desconecto el dROIDkey, para volver a conectar seleccione la opciÃ³n conectar provista en el menu","DesconexiÃ³n",android.R.drawable.ic_dialog_alert);
					tvStatIniDroidKey.setText(Html.fromHtml("Status: <font color='red'><u>Desconectado</u></font>"));
				}
				if (action.equals(getString(R.string.enListViewCtrls))) {
					lvListCtrlsIniDroidKey.setEnabled(true);
					Log.i("ajDroid","Se habilitó el ListView rpCtrls");
				}
				if (action.equals(getString(R.string.iniAprndzj))) {
					new AsyncTask<Void,Void,Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							aprndCtrl(); return null;
						}}.execute();
				}
				if (action.equals(getString(R.string.salirActvDroidKey))) {
					Log.i("ajDroid","brdRcvIniDroidKey::salirActvDroidkey");
					finish();
				}		
				if (action.equals(getString(R.string.corrCmbClvDroidKey))) {
					Toast.makeText(getBaseContext(),"Clave del dROIDkey cambiada con éxito", Toast.LENGTH_LONG).show();				
				}
				if (action.equals(getString(R.string.errCmbClvDroidKey))) {
					aDlgInfo("Error al cambiar la clave del dROIDkey. Por favor intentar de nuevo","Error al cambiar clave",android.R.drawable.ic_dialog_alert);
				}
				if (action.equals(getString(R.string.byteRcvActv))) {
					if (modoAprndzj) {
						rgnrTmpTiempoFueraAprnd(); ++cntBytesRcb;
						if (cntBytesRcb==2&&!nEnRlzAprndzj) {
							if (intent.getIntExtra("int",0)==122 ){
								modoAprndzj=false; dtnTmpTiempFueraAprnd(); if (animDraw00!=null) animDraw00.stop();
								dlg.dismiss(); aDlgInfo("Por favor conectar cable Mini USB para poder aprender el control.", 
										"Cable Mini USB no conectado",android.R.drawable.ic_dialog_alert);
							}
							nEnRlzAprndzj=true;
						}
//						lsArrAux[0].add(intent.getIntExtra("int",0));
						arrLstInt.add(intent.getIntExtra("int",0));
						if (scnciaAprn==intent.getIntExtra("int",0)) {
							if (scnciaAprn==5) {
								Log.i("ajDroid","Aprendizaje realizado");
								dtnTmpTiempFueraAprnd(); modoAprndzj=false; 
								Log.i("ajDroid","modoAprndzj:"+modoAprndzj);
								dlg.setContentView(R.layout.dlg_aprnd_rlzdo); dlg.setTitle(getString(R.string.stTittleDlgAprndRlzdo));
								btnAceptDlgAprndRlzdo=(Button)dlg.findViewById(R.id.btnAcepDlgAprndRlzdo);
								btnCancelDlgAprndRlzdo=(Button)dlg.findViewById(R.id.btnCancelDlgAprndRlzdo);
								etDlgAprndRlzdo=(EditText)dlg.findViewById(R.id.etDlgAprndRlzdo);
								btnAceptDlgAprndRlzdo.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {
										if (etDlgAprndRlzdo.getText().toString().trim().equals("")) {//											
											Toast.makeText(getBaseContext(),"Nombre de control vacio, intente de nuevo", Toast.LENGTH_LONG).show();
										}
										else {
											Cursor cursor=null; String[] stCursor=new String[]{};								
											bdSQLite=gestorSQLite.getReadableDatabase();											
											stNombreCtrl=etDlgAprndRlzdo.getText().toString().toLowerCase().trim();
											try {
												stNombreCtrl=SimpleCrypto.encrypt(stSeed, stNombreCtrl);
											} catch(Exception ex) {
												Log.e("ajDroid","Error al encriptar el nombre del control");
											}											
											try {
												cursor=bdSQLite.rawQuery("SELECT * FROM y00 WHERE z00='"+stNombreCtrl+"';",stCursor);
											} catch(Exception ex) {
												Log.e("ajDroid","Error en la consulta  si el nombre del control esta repetido");
											}
											// Nombre control repetido
											if (cursor.moveToFirst() && cursor!=null) {
												aDlgInfo("Este nombre se encuentra en uso en la lista, por favor escriba otro nombre", "Nombre de control repetido", android.R.drawable.ic_dialog_alert);
												bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada");
											}
											// Nombre de control no repetido
											else {
												bdSQLite.close(); Log.i("ajDroid","bd sqlite cerrada");
												dlg.dismiss(); arrglDataAprnd();
											}
										}										
									}
								});
								btnCancelDlgAprndRlzdo.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {
										dlg.dismiss();									
									}
								});
								dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
				                    @Override
				                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				                        if(keyCode == KeyEvent.KEYCODE_BACK){		                                
				                        	dlg.dismiss(); Log.i("ajDroid","Dialog cancelado por el boton fÃ­sico back"); return true;
				                        }
				                        return false;		                        
				                    }
				                });
								dlg.show();								
							}
							else ++scnciaAprn;
						}
						else scnciaAprn=1;
						if (intent.getIntExtra("int",0)==0xFF) {
							++scnciaErr;
							if (scnciaErr==6) {
								Log.e("ajDroid","Error en el aprendizaje");
								dtnTmpTiempFueraAprnd(); modoAprndzj=false;
								Log.i("ajDroid","modoAprndzj:"+modoAprndzj);
								dlg.setContentView(R.layout.dlg_aprnd_err); dlg.setTitle(getString(R.string.stTittleDlgAprndErr));
								btnOkDlgAprndErr=(Button)dlg.findViewById(R.id.btnOkDlgAprndErr);
								btnOkDlgAprndErr.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {dlg.dismiss();}
								});
								dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
				                    @Override
				                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				                        if(keyCode == KeyEvent.KEYCODE_BACK){		                                
				                        	dlg.dismiss(); Log.i("ajDroid","Dialog cancelado por el boton back"); return true;
				                        }
				                        return false;		                        
				                    }
				                });
								dlg.show();							
							}
						}
						else scnciaErr=0;
					}
				}				
			}
		}			
		
	};
	
	// BROADCAST RECEIVER ACTIVITY BLUETOOTH 
	private final BroadcastReceiver brdRcvDroidKeyBth=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			// Evaluo el estado del bluetooth si esta apagado o no en este activity por que, es aqui donde le voy a indicar al
			// usuario que active el bluetooth
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				if (ServcBth.bthAdp!=null) {
					if (ServcBth.bthAdp.getState()==BluetoothAdapter.STATE_ON) {
						Log.i("ajDroid","bth encendido"); /*bthEncnd=false;*/
					}
					else if (ServcBth.bthAdp.getState()==BluetoothAdapter.STATE_OFF) {
						Log.i("ajDroid","bth apagado"); /*bthEncnd=true;*/
						if (ActvDroidKey.context!=null) {
							Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(intent1, bthOn);						
						}					
					}	
				}
				else Log.i("ajDroid","bthAdp es diferente de nulo");
			}
		}
	};
	
	
	//######################################
	//########       MÉTODOS       #########
	//######################################
	
	//·Â·Â·Â·Â·Â·Â·Â·Â··Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â··Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·
	// Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â· 			NO BORRRRARRRRRRR 			Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·	
	//Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·
			@SuppressWarnings("unchecked")
			<E> E[] newArray(Class<?> classE, int length)
			{
				return (E[])java.lang.reflect.Array.newInstance(classE, length);
			}
	//Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â
	//Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â·Â
	
	private void arrglDataAprnd() {		
		String stCntFl="", stTrst="", stMtrasPort="", stAux[];
//		stCntFl=String.valueOf(((Integer)this.lsArrAux[0].get(1)).intValue());
		stCntFl=String.valueOf(((Integer)this.arrLstInt.get(1)).intValue());
//		stTrst=String.valueOf(65535-((((Integer)this.lsArrAux[0].get(2)).intValue()<<8)|(((Integer)this.lsArrAux[0].get(3)).intValue())));
		stTrst=String.valueOf(65535-((((Integer)this.arrLstInt.get(2)).intValue()<<8)|(((Integer)this.arrLstInt.get(3)).intValue())));
//		stMtrasPort=String.valueOf(((Integer)this.lsArrAux[0].get(4)<<8)|((Integer)this.lsArrAux[0].get(5)));
		stMtrasPort=String.valueOf(((Integer)this.arrLstInt.get(4)<<8)|((Integer)this.arrLstInt.get(5)));
		intAux=new int[2];
		for (int i=6;;i++) {
//			if (this.lsArrAux[0].get(i).equals(this.scnciaAprn)) {
			if (this.arrLstInt.get(i).equals(this.scnciaAprn)) {
				if (this.scnciaAprn==4) {
					this.stTindc=this.stTindc.substring(0,this.stTindc.length()-15); // Se resta menos 10 por las comas	
					if ((stTindc.length()-1)!=stTindc.lastIndexOf(',')) {
						stTindc+=",";
					} // Se agrega la ultima coma por si no la tiene
//					this.lsArrAux[0].set(0,i+1); break;
					this.arrLstInt.set(0,i+1); break;
				}
				else ++scnciaAprn;
			}
			else scnciaAprn=1;
			if ((i%2==0)) {			
//				intAux[0]=65535-((((Integer)this.lsArrAux[0].get(i-1)).intValue()<<8)|(((Integer)this.lsArrAux[0].get(i)).intValue()));
				intAux[0]=65535-((((Integer)this.arrLstInt.get(i-1)).intValue()<<8)|(((Integer)this.arrLstInt.get(i)).intValue()));
				this.stTindc+=String.valueOf(intAux[0]>>8)+","+String.valueOf(intAux[0]&0x00FF)+",";
			}			
		}		
		
//		for (int i=(((Integer)this.lsArrAux[0].get(0)).intValue());;i++) {
		for (int i=(((Integer)this.arrLstInt.get(0)).intValue());;i++) {
//			this.stTcod+=String.valueOf(this.lsArrAux[0].get(i))+",";
			this.stTcod+=String.valueOf(this.arrLstInt.get(i))+",";
//			if (this.lsArrAux[0].get(i).equals(this.scnciaAprn)) {
			if (this.arrLstInt.get(i).equals(this.scnciaAprn)) {
				if (this.scnciaAprn==5) {
					this.stTcod=this.stTcod.substring(0,this.stTcod.length()-12); // Se resta 12 por las comas
					break;
				}
				else ++this.scnciaAprn;
			}
			else this.scnciaAprn=1;			
		}
		//Encriptar
		try {
			stCntFl="'"+SimpleCrypto.encrypt(stSeed,stCntFl)+"'";
			stTrst="'"+SimpleCrypto.encrypt(stSeed, stTrst)+"'";
			stMtrasPort="'"+SimpleCrypto.encrypt(stSeed, stMtrasPort)+"'";
			stTindc="'"+SimpleCrypto.encrypt(stSeed,stTindc)+"'";
			stTcod="'"+SimpleCrypto.encrypt(stSeed, stTcod)+"'";
		} catch(Exception ex) {
			Log.e("ajDroid","Error al encriptar los bytes recibido del aprendizaje");
		}
		stNombreCtrl="'"+stNombreCtrl+"'";
		this.bdSQLite=this.gestorSQLite.getWritableDatabase();
		Log.i("ajDroid","Se abriÃ³ la bd sqlite para escribir en la misma");
		try {
			this.bdSQLite.execSQL("insert into y00 (z00, z01, z02, z03, z04, z05)"+
				"values ("+stNombreCtrl+","+stCntFl+","+stTrst+","+stTindc+","+stTcod+","+stMtrasPort+");");
			Log.i("ajDroid","Se escribiÃ³ una tabla en la bd sqlite");
		} catch(SQLException ex) {
			Log.e("ajDroid","Error al crear una fila en bd sqlite");
		}
		this.bdSQLite.close(); Log.i("ajDroid","Se cerrÃ³ la bd sqlite"); this.actlzListCtrls();
	}
	private void regBrdRcv() {		
		//Reg Broadcast Receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthNullActv)));	
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthApagActv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthNoDtcActv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthConectandoActv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthConnActv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.bthDescon1Actv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.byteRcvActv)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.enListViewCtrls)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.iniAprndzj)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.salirActvDroidKey)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.corrCmbClvDroidKey)));
		LocalBroadcastManager.getInstance(this).registerReceiver(brdRcvDroidKey, new IntentFilter(getString(R.string.errCmbClvDroidKey)));
		IntentFilter intentFilter=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED); this.registerReceiver(this.brdRcvDroidKeyBth, intentFilter);
		Log.i("ajDroid","Se registro LocalBroadcastReceiver para Activity");
	}
	private void unRegBrdRcv() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvDroidKey);
		Log.i("ajDroid","Se desregistro ActvDroidKey::LocalBroadcastManager.getInstance(this).unregisterReceiver(brdRcvDroidKey);");
		try {
			unregisterReceiver(brdRcvDroidKeyBth);
			Log.i("ajDroid","Se desregistro ActvDroidKey::unregisterReceiver(brdRcvDroidKeyBth)");
		} catch (Exception ex){Log.e("ajDroid","Error a unregisterReceiver(brdRcvDroidKeyBth) en ActvDroidKey");}		
	}

	private void actlzListCtrls() {
		this.lvListCtrlsIniDroidKey.setAdapter(null);
		Cursor cursor=null; String[] stCursor=new String[]{}; String stAux1[]=new String[2]; 
		this.arrAdpGestorListCtrls=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		
		// Leo los nombres de los ctrls de la bd sqlite y genero la id de c/u y trasnfiero los nombres a la listview
		this.bdSQLite=this.gestorSQLite.getReadableDatabase(); 
		Log.i("ajDroid","Abriendo la bd sqlite para leer y escribir las id de los controles");
		try {
			cursor=bdSQLite.rawQuery("SELECT * FROM y00;",stCursor);
			Log.i("ajDroid","Se selecionÃ³ la tabla ctrls bd sqlite");
		} catch (SQLException ex){Log.e("ajDroid","Error al seleccionar la tabla ctrls de la bd sqlite");}
		if (cursor.moveToFirst() && cursor!=null) {
			do {
				try {
					stAux1[0]=SimpleCrypto.decrypt(stSeed, cursor.getString(0));	//Desencripto nombre					
				} catch(Exception ex) {
					Log.e("ajDroid","Error al desencriptar nombre y encriptar id de la lista de los controles");
				}
				//Agrego el nombre del control en la array adapter
				this.arrAdpGestorListCtrls.add(stAux1[0]);				
			} while(cursor.moveToNext());
			this.lvListCtrlsIniDroidKey.setAdapter(this.arrAdpGestorListCtrls);
		}		
		this.bdSQLite.close(); Log.i("ajDroid","Se cerró la bd sqlite");
	}

	private void aDlgInfo(String txt,String txtTitulo, int icon) {
		new AlertDialog.Builder(this)
	    .setTitle(txtTitulo)
	    .setMessage(txt)
	    .setCancelable(true)
	    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	        	dialog.cancel();
	        }
	     })
	    .setIcon(icon)
	     .show();
	}
	private void aDlgInfo(String txt,String txtTitulo, int icon, final int indicador) {		
		new AlertDialog.Builder(this)
	    .setTitle(txtTitulo)
	    .setMessage(txt)
	    .setCancelable(true)
	    
	    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        	dialog.cancel();
	        	switch (indicador) {
	        		case 1: break;	// No quitar breakpoint
	        		case 2:	fin(); break;
	        		case 3:
	        			Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
						startActivityForResult(settingsIntent, bthApareado);
						break;
	        	}
	        }
	     })
	    .setIcon(icon)
	     .show();
		
	}	
	
	private void rgnrTmpTiempoFueraAprnd() {
		if (tmp!=null) tmp.cancel(); tmp=new Timer(); tmpTiempoFueraAprnd=new TmpTiempoFueraAprnd();
		tmp.schedule(tmpTiempoFueraAprnd,16000);		
	}
	
	private void dtnTmpTiempFueraAprnd() {
		tmp.cancel(); if (animDraw00!=null) animDraw00.stop(); dlg.dismiss();		
	}
	
	private void aprndCtrl() {
		if (ServcBth.context!=null && ServcBth.conexion) {
//			this.lsArrAux=newArray(ArrayList.class,5);
			rgnrTmpTiempoFueraAprnd();
			this.arrLstInt=new ArrayList<Integer>();
			this.asyncRutAprn=new AsyncRutAprn(); this.asyncRutAprn.execute();				
		}
		else {
			Toast toast1=Toast.makeText(getBaseContext(), "dROIDkey no conectado", Toast.LENGTH_SHORT);
			toast1.show();
		}
	}
	
	private void soporteBth() {	
		LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(new Intent(getString(R.string.soporteBthServc)));
	}
	private void desconnBth() {
		LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(new Intent(getString(R.string.desconBthServc)));			
	}
	private void fin() {this.finish();}
	

	//######################################
	//########      SUB-CLASES      ########
	//######################################		
	
	private class AsyncRutAprn extends AsyncTask<Void,Void,Void>{
		@Override
		protected Void doInBackground(Void... params) {			
			modoAprndzj=true; lsArrAux[0]=new ArrayList<Integer>(); stTcod=stTindc="";	nEnRlzAprndzj=false; cntBytesRcb=0;
			Log.i("ajDroid","modoAprndzj:"+modoAprndzj);
			Intent intent=new Intent(getString(R.string.escrbBthServc)); intent.putExtra("int",(int)'a');
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
			try {Thread.sleep(2);} catch(InterruptedException ex){;}
			LocalBroadcastManager.getInstance(ServcBth.context).sendBroadcast(intent);
			publishProgress(); return null;
		}

		@Override
		protected void onProgressUpdate(Void... pUpdt) {
			dlg.setContentView(R.layout.dlg_aprnd);	dlg.setTitle(R.string.stTittleDlgAprnd);
			imgDialogAprndzj=(ImageView)dlg.findViewById(R.id.dlgAprndAnim00);
			animDraw00=(AnimationDrawable)imgDialogAprndzj.getDrawable(); 
			dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
	            @Override
	            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
	                if(keyCode == KeyEvent.KEYCODE_BACK){
	                	dtnTmpTiempFueraAprnd();
	                	modoAprndzj=false;
	                	Log.i("ajDroid","modoAprndzj:"+modoAprndzj);	                    	
	                	btnAprndDroidKeyMain.setEnabled(true);
	                	Log.i("ajDroid","Dialog cancelado por el boton fÃ­sico back"); return true;
	                }
	                return false;		                        
	            }
	        });
			dlg.show(); 
			View view=new View(getApplicationContext()); 
			view.post(new Runnable() {
				@Override
				public void run() {animDraw00.start();}
			});
		}
	}
	
	private class TmpTiempoFueraAprnd extends TimerTask {

		@Override
		public void run() {
			modoAprndzj=false;	if (animDraw00!=null) animDraw00.stop(); dlg.dismiss(); 
			Log.i("ajDroid","modoAprndzj:"+modoAprndzj);
			Log.i("ajDroid","Tiempo Fuera: Se acabo el tiempo para aprender");			
		}
		
	}
		
}
