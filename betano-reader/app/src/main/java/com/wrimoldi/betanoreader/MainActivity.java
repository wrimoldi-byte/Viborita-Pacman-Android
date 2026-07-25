package com.wrimoldi.betanoreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 7;
    private TextView result;
    private final Map<String, Double> reference = new HashMap<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        reference.put("Sweet Bonanza",96.51); reference.put("Gates of Olympus",96.50);
        reference.put("Big Bass Bonanza",96.71); reference.put("Sugar Rush",96.50);
        reference.put("The Dog House",96.51); reference.put("Starlight Princess",96.50);
        reference.put("Blackjack",99.00); reference.put("Baccarat",98.94);
        reference.put("Ruleta",97.30); reference.put("Aviator",97.00);

        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32,32,32,32); box.setBackgroundColor(Color.rgb(7,17,31));
        box.addView(text("Analizador de captura Betano",26,Color.WHITE));
        box.addView(text("Elegí una captura donde se vean los juegos o su información. La app leerá únicamente esa imagen y ordenará lo detectado por RTP visible o de referencia.",16,Color.LTGRAY));
        Button pick=button("Elegir captura");
        pick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); startActivityForResult(i,PICK_IMAGE); });
        result=text("Todavía no analizaste ninguna captura.",17,Color.WHITE);
        box.addView(pick); box.addView(result); scroll.addView(box); setContentView(scroll);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_IMAGE||resultCode!=RESULT_OK||data==null)return;
        Uri uri=data.getData(); if(uri==null)return;
        try{
            InputImage image=InputImage.fromFilePath(this,uri);
            result.setText("Leyendo captura…");
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener(t->analyze(t.getText()))
                    .addOnFailureListener(e->result.setText("No se pudo leer la captura: "+e.getMessage()));
        }catch(Exception e){ result.setText("No se pudo abrir la imagen: "+e.getMessage()); }
    }

    private void analyze(String all){
        String lower=all.toLowerCase(Locale.ROOT);
        ArrayList<Game> found=new ArrayList<>();
        Pattern p=Pattern.compile("(9[0-9](?:[.,][0-9]{1,2})?)\\s*%");
        Matcher m=p.matcher(all); Double visible=null;
        if(m.find()) visible=Double.parseDouble(m.group(1).replace(',','.'));
        for(Map.Entry<String,Double> e:reference.entrySet()){
            if(lower.contains(e.getKey().toLowerCase(Locale.ROOT))) found.add(new Game(e.getKey(),visible!=null?visible:e.getValue(),visible!=null));
        }
        found.sort(Comparator.comparingDouble((Game g)->g.rtp).reversed());
        if(found.isEmpty()){
            result.setText("No reconocí juegos conocidos. Probá con una captura más clara donde se vea el nombre del juego o el RTP.\n\nTexto detectado:\n"+(all.length()>700?all.substring(0,700)+"…":all));
            return;
        }
        StringBuilder s=new StringBuilder("Ranking de la captura\n\n"); int n=1;
        for(Game g:found) s.append(n++).append(". ").append(g.name).append(" — RTP ")
                .append(String.format(Locale.US,"%.2f",g.rtp)).append("%")
                .append(g.visible?" (visible en la captura)":" (referencia; verificar dentro del juego)").append("\n");
        s.append("\nEsto no predice cuál juego pagará próximamente."); result.setText(s.toString());
    }

    private TextView text(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,16,0,16);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(17);b.setAllCaps(false);return b;}
    static class Game{final String name;final double rtp;final boolean visible;Game(String n,double r,boolean v){name=n;rtp=r;visible=v;}}
}
