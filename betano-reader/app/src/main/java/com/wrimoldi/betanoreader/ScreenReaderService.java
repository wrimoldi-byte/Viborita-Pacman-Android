package com.wrimoldi.betanoreader;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.view.*;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.*;

public class ScreenReaderService extends Service {
    private static final String CHANNEL = "screen_reader";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Double> reference = new LinkedHashMap<>();
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private WindowManager windowManager;
    private TextView overlay;
    private TextRecognizer recognizer;
    private boolean processing;

    @Override public void onCreate() {
        super.onCreate();
        reference.put("Blackjack",99.00);
        reference.put("Baccarat",98.94);
        reference.put("Ruleta",97.30);
        reference.put("Aviator",97.00);
        reference.put("Big Bass Bonanza",96.71);
        reference.put("Sweet Bonanza",96.51);
        reference.put("The Dog House",96.51);
        reference.put("Gates of Olympus",96.50);
        reference.put("Sugar Rush",96.50);
        reference.put("Starlight Princess",96.50);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        createChannel();
        showOverlay();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Analizador flotante activo")
                .setContentText("Leyendo nombres visibles en pantalla")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        if (projection == null && intent != null) {
            int code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("resultData");
            if (code == Activity.RESULT_OK && data != null) startProjection(code, data);
        }
        return START_NOT_STICKY;
    }

    private void startProjection(int code, Intent data) {
        MediaProjectionManager manager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(code, data);
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, handler);

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int density = dm.densityDpi;
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        display = projection.createVirtualDisplay("VisibleGameReader", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, handler);
        reader.setOnImageAvailableListener(r -> readLatest(), handler);
    }

    private void readLatest() {
        if (processing || reader == null) return;
        Image image = reader.acquireLatestImage();
        if (image == null) return;
        processing = true;
        Bitmap bitmap = null;
        try {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();
            Bitmap padded = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
            padded.recycle();
        } catch (Exception e) {
            processing = false;
        } finally {
            image.close();
        }
        if (bitmap == null) return;
        Bitmap finalBitmap = bitmap;
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(text -> updateRanking(text.getText()))
                .addOnCompleteListener(task -> {
                    finalBitmap.recycle();
                    handler.postDelayed(() -> processing = false, 1200);
                });
    }

    private void updateRanking(String all) {
        String lower = all.toLowerCase(Locale.ROOT);
        ArrayList<Game> found = new ArrayList<>();
        for (Map.Entry<String, Double> e : reference.entrySet()) {
            if (lower.contains(e.getKey().toLowerCase(Locale.ROOT))) found.add(new Game(e.getKey(), e.getValue()));
        }
        found.sort((a,b) -> Double.compare(b.rtp, a.rtp));
        StringBuilder s = new StringBuilder();
        if (found.isEmpty()) {
            s.append("Buscando juegos visibles…");
        } else {
            s.append("Mayor RTP visible\n");
            for (int i=0; i<Math.min(3, found.size()); i++) {
                Game g = found.get(i);
                s.append(i+1).append(". ").append(g.name).append(" — ")
                        .append(String.format(Locale.US,"%.2f",g.rtp)).append("%\n");
            }
            s.append("RTP de referencia; no predice premios");
        }
        overlay.setText(s.toString());
    }

    private void showOverlay() {
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        overlay = new TextView(this);
        overlay.setText("Buscando juegos visibles…");
        overlay.setTextColor(0xFFFFFFFF);
        overlay.setTextSize(14);
        overlay.setPadding(20,14,20,14);
        overlay.setBackgroundColor(0xDD07111F);

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 16;
        p.y = 120;
        windowManager.addView(overlay, p);
    }

    private void createChannel() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Lectura visible", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (reader != null) reader.close();
        if (display != null) display.release();
        if (projection != null) projection.stop();
        if (recognizer != null) recognizer.close();
        if (overlay != null && windowManager != null) windowManager.removeView(overlay);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    static class Game { final String name; final double rtp; Game(String n,double r){name=n;rtp=r;} }
}
