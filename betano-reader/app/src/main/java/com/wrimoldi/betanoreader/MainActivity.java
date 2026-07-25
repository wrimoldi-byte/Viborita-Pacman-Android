package com.wrimoldi.betanoreader;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 20;
    private static final int CAPTURE_REQUEST = 21;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32,32,32,32);
        box.setBackgroundColor(Color.rgb(7,17,31));

        box.addView(text("Analizador flotante de juegos",26,Color.WHITE));
        box.addView(text("Lee automáticamente los nombres visibles en pantalla y muestra una lista flotante ordenada por RTP de referencia. No controla Betano ni predice resultados.",16,Color.LTGRAY));

        Button start = button("Iniciar lectura flotante");
        start.setOnClickListener(v -> begin());
        Button stop = button("Detener lectura");
        stop.setOnClickListener(v -> stopService(new Intent(this, ScreenReaderService.class)));
        box.addView(start);
        box.addView(stop);
        box.addView(text("Android te pedirá dos permisos: mostrar la burbuja sobre otras apps y compartir la pantalla. La lectura queda visible mediante una notificación mientras está activa.",15,Color.LTGRAY));
        scroll.addView(box);
        setContentView(scroll);
    }

    private void begin() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, OVERLAY_REQUEST);
            return;
        }
        MediaProjectionManager m = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(), CAPTURE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) begin();
            return;
        }
        if (requestCode == CAPTURE_REQUEST && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, ScreenReaderService.class);
            service.putExtra("resultCode", resultCode);
            service.putExtra("resultData", data);
            startForegroundService(service);
            moveTaskToBack(true);
        }
    }

    private TextView text(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,16,0,16);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(17);b.setAllCaps(false);return b;}
}
