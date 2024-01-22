package aj.apps.droidkey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartServcOnBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent1) {
		Intent intent2=new Intent(); intent2.setAction("ServcBth");
		context.startService(intent2);
	}
	
}
