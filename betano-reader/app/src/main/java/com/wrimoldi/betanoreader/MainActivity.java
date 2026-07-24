package com.wrimoldi.betanoreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);
        box.setBackgroundColor(Color.rgb(7,17,31));

        TextView title = text("Analizador visible de juegos", 26, Color.WHITE);
        TextView info = text("Lee solamente nombres y porcentajes que estén visibles en pantalla. No predice premios, no toca Betano y no realiza apuestas.", 16, Color.LTGRAY);
        Button enable = button("1. Activar lectura de pantalla");
        Button refresh = button("2. Ver último análisis");
        status = text("Todavía no hay lecturas.", 17, Color.WHITE);

        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        refresh.setOnClickListener(v -> loadResult());

        box.addView(title); box.addView(info); box.addView(enable); box.addView(refresh); box.addView(status);
        scroll.addView(box); setContentView(scroll);
    }

    @Override protected void onResume() { super.onResume(); loadResult(); }

    private void loadResult() {
        String result = getSharedPreferences("scan", MODE_PRIVATE).getString("result",
                "Activá el servicio, abrí Betano y navegá por el casino. La app analizará cada pantalla visible y guardará el último ranking.");
        status.setText(result);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(0,16,0,16); return t;
    }

    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(17); b.setAllCaps(false); return b;
    }
}
