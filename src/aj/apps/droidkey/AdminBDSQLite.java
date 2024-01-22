package aj.apps.droidkey;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AdminBDSQLite extends SQLiteOpenHelper {

	public AdminBDSQLite(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i("getString(R.string.ajdroid","Se procede a creer la tabla ctrls");
		this.creaDeTblCtrl(db);		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("getString(R.string.ajdroid","Se procede a actualizar las tablas");
		this.creaDeTblCtrl(db); this.elmnTblCtrl(db);		
	}
	
	public void creaDeTblCtrl(SQLiteDatabase db)  {
		try {
			db.execSQL("create table if not exists y00(z00 text, z01 text, z02 text, z03 text, z04 text, z05 text, z06 text);");
			//db.execSQL("create table if not exists y01(z00 text, z01 text);");
			Log.i("getString(R.string.ajdroid","Se creó la tabla ctrls y usuario");
		} catch (SQLException ex) {Log.e("getString(R.string.ajdroid","Se produjo un error al crear la tabla");}
	}
	
	public void elmnTblCtrl(SQLiteDatabase db) {
		try {
			db.execSQL("drop table if exists y00;"); 
			//db.execSQL("drop table if exists y01;");
			Log.e("getString(R.string.ajdroid","Se eliminaron las tablas");
		} catch (SQLException ex) {Log.e("getString(R.string.ajdroid","Error al borrar la tabla ctrls y/o ctrlsErr");}
	}
}
