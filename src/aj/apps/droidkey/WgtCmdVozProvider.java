package aj.apps.droidkey;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import aj.apps.droidkey.R;

public class WgtCmdVozProvider extends AppWidgetProvider  {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i("ajDroid","WgtCmdVozProvider::onUpdate");
		for (int i=0;i<appWidgetIds.length;i++) {			
			RemoteViews remoteViews=new RemoteViews(context.getPackageName(),R.layout.wgt_cmd_voz);	
			Intent intent=new Intent(context,WgtCmdVozProvider.class); intent.setAction("Clicked");
			PendingIntent pendingIntent=PendingIntent.getBroadcast(context,appWidgetIds[i],intent,0);
			remoteViews.setOnClickPendingIntent(R.id.widgetCmdVoz1,pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
		}		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
		
	@Override
	public void onEnabled(Context context) {
		Log.i("ajDroid","WgtCmdVozProvider::onEnabled()");
		super.onEnabled(context);
	}
	
	public void onReceive(Context context,Intent intent) {
		Log.i("ajDroid","WgtCmdVozProvider::onReceive");
		if (intent.getAction().equals("Clicked")) {
			Log.i("ajDroid","Se selecciono el widget comando de voz");
			Intent intent1=new Intent(context,ActvCmdVoz.class);
			intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent1);
		}
		super.onReceive(context, intent);
	}

}
