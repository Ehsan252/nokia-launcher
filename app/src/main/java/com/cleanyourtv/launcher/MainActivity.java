package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private FrameLayout rootLayout;
    private BackgroundView backgroundView;
    private ItachiEyesView itachiEyesView;
    private OdometerClockView clockView;
    private AppDockView appDockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        backgroundView = new BackgroundView(this);
        rootLayout.addView(backgroundView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        itachiEyesView = new ItachiEyesView(this);
        FrameLayout.LayoutParams eyeParams = new FrameLayout.LayoutParams(600, 200);
        eyeParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        eyeParams.topMargin = 80;
        rootLayout.addView(itachiEyesView, eyeParams);

        clockView = new OdometerClockView(this);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(400, 150);
        clockParams.gravity = Gravity.TOP | Gravity.RIGHT;
        clockParams.topMargin = 50;
        clockParams.rightMargin = 50;
        rootLayout.addView(clockView, clockParams);

        appDockView = new AppDockView(this, itachiEyesView);
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400);
        dockParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        dockParams.bottomMargin = 150;
        rootLayout.addView(appDockView, dockParams);

        setContentView(rootLayout);
        registerReceiver(wakeReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private final BroadcastReceiver wakeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                itachiEyesView.triggerSharinganSpin();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        itachiEyesView.triggerSharinganSpin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wakeReceiver);
        backgroundView.stopRotation();
    }

    class BackgroundView extends View {
        private Bitmap currentWallpaper = null;
        private final Paint dimPaint = new Paint();
        private final Handler rotationHandler = new Handler(Looper.getMainLooper());
        private int fileIndex = 0;
        private final String wallpaperDir = "/sdcard/Download/heroes/";

        public BackgroundView(Context context) {
            super(context);
            dimPaint.setColorFilter(new PorterDuffColorFilter(0xCC000000, PorterDuff.Mode.SRC_ATOP));
            loadNextWallpaper();
            rotationHandler.postDelayed(rotationRunnable, 5 * 60 * 1000);
        }

        private final Runnable rotationRunnable = new Runnable() {
            @Override
            public void run() {
                loadNextWallpaper();
                rotationHandler.postDelayed(this, 5 * 60 * 1000);
            }
        };

        private void loadNextWallpaper() {
            File dir = new File(wallpaperDir);
            if (!dir.exists() || !dir.isDirectory()) return;
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) return;

            if (fileIndex >= files.length) fileIndex = 0;
            File targetFile = files[fileIndex++];

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(targetFile.getAbsolutePath(), options);
            options.inSampleSize = 2;
            options.inJustDecodeBounds = false;

            Bitmap newBmp = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), options);
            if (currentWallpaper != null) currentWallpaper.recycle();
            currentWallpaper = newBmp;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (currentWallpaper != null) {
                canvas.drawBitmap(currentWallpaper, 0, 0, null);
                canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
            }
        }

        public void stopRotation() {
            rotationHandler.removeCallbacks(rotationRunnable);
        }
    }    class OdometerClockView extends View {
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int lastSecond = -1;
        private float rollProgress = 1.0f;
        private final Handler timeHandler = new Handler(Looper.getMainLooper());

        public OdometerClockView(Context context) {
            super(context);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(48f);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            
            timeHandler.post(new Runnable() {
                @Override
                public void run() {
                    Calendar c = Calendar.getInstance();
                    int sec = c.get(Calendar.SECOND);
                    if (sec != lastSecond) {
                        lastSecond = sec;
                        animateRoll();
                    }
                    timeHandler.postDelayed(this, 100);
                }
            });
        }

        private void animateRoll() {
            rollProgress = 0.0f;
            android.view.animation.Animation anim = new android.view.animation.Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, android.view.animation.Transformation t) {
                    rollProgress = interpolatedTime;
                    invalidate();
                }
            };
            anim.setDuration(250);
            startAnimation(anim);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Calendar c = Calendar.getInstance();
            String timeStr = String.format("%02d:%02d:", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            canvas.drawText(timeStr, 50, 80, textPaint);

            int curSec = c.get(Calendar.SECOND);
            int prevSec = (curSec == 0) ? 59 : curSec - 1;
            float yOffset = rollProgress * 60; 
            
            canvas.save();
            canvas.clipRect(220, 30, 300, 100);
            canvas.drawText(String.format("%02d", prevSec), 220, 80 + yOffset, textPaint);
            canvas.drawText(String.format("%02d", curSec), 220, 20 + yOffset, textPaint);
            canvas.restore();
        }
    }

    class ItachiEyesView extends View {
        private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float targetFocusX = 300; 
        private float currentPupilX = 300;
        private boolean isBlinking = false;
        private float spinDegree = 0f;
        private final Handler blinkHandler = new Handler(Looper.getMainLooper());

        public ItachiEyesView(Context context) {
            super(context);
            eyePaint.setColor(Color.RED);
            scheduleBlink();
        }

        public void updateFocusPosition(float x) {
            this.targetFocusX = x;
            currentPupilX += (targetFocusX - currentPupilX) * 0.15f;
            invalidate();
        }

        public void triggerSharinganSpin() {
            android.view.animation.Animation anim = new android.view.animation.Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, android.view.animation.Transformation t) {
                    spinDegree = interpolatedTime * 720f;
                    invalidate();
                }
            };
            anim.setDuration(800);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            startAnimation(anim);
        }

        private void scheduleBlink() {
            blinkHandler.postDelayed(() -> {
                isBlinking = true;
                invalidate();
                blinkHandler.postDelayed(() -> {
                    isBlinking = false;
                    invalidate();
                    scheduleBlink();
                }, 150);
            }, 2000 + (long) (Math.random() * 4000));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (isBlinking) {
                eyePaint.setColor(Color.DKGRAY); eyePaint.setStrokeWidth(5);
                canvas.drawLine(50, h/2, w/2 - 40, h/2, eyePaint);
                canvas.drawLine(w/2 + 40, h/2, w - 50, h/2, eyePaint);
                return;
            }
            eyePaint.setColor(Color.WHITE);
            canvas.drawOval(50, h/2 - 30, w/2 - 40, h/2 + 30, eyePaint);
            canvas.drawOval(w/2 + 40, h/2 - 30, w - 50, h/2 + 30, eyePaint);

            eyePaint.setColor(Color.RED);
            float pupilOffset = ((currentPupilX / getRootView().getWidth()) * 60) - 30;

            canvas.save(); canvas.translate(w/4 + pupilOffset, h/2); canvas.rotate(spinDegree);
            canvas.drawCircle(0, 0, 20, eyePaint); eyePaint.setColor(Color.BLACK); canvas.drawCircle(0, 0, 6, eyePaint); canvas.restore();

            eyePaint.setColor(Color.RED);
            canvas.save(); canvas.translate((3 * w)/4 + pupilOffset, h/2); canvas.rotate(spinDegree);
            canvas.drawCircle(0, 0, 20, eyePaint); eyePaint.setColor(Color.BLACK); canvas.drawCircle(0, 0, 6, eyePaint); canvas.restore();
        }
    }

    class AppDockView extends ViewGroup {
        private final List<View> appItems = new ArrayList<>();
        private float spotlightX = 0f;
        private final Paint spotlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ItachiEyesView eyesTracker;

        public AppDockView(Context context, ItachiEyesView eyes) {
            super(context);
            this.eyesTracker = eyes;
            setWillNotDraw(false);
            addAppTile("Prime Video", "com.amazon.amazonvideo.livingroom");
            addAppTile("Salin TV", "com.salintv.app");
            appItems.get(0).requestFocus();
        }

        private void addAppTile(String label, String pkg) {
            AppItemView itemView = new AppItemView(getContext(), label, pkg);
            itemView.setFocusable(true); itemView.setClickable(true);
            
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateSpotlight(v.getLeft() + (v.getWidth() / 2f));
                    v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(200).start();
                    ((AppItemView) v).showText(true);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    ((AppItemView) v).showText(false);
                }
            });

            itemView.setOnClickListener(v -> {
                v.animate().scaleX(8.0f).scaleY(8.0f).alpha(0.0f).setDuration(300).withEndAction(() -> {
                    Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) getContext().startActivity(launchIntent);
                    v.setScaleX(1.0f); v.setScaleY(1.0f); v.setAlpha(1.0f);
                }).start();
            });

            appItems.add(itemView); addView(itemView);
        }

        private void animateSpotlight(float targetX) {
            android.view.animation.Animation anim = new android.view.animation.Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, android.view.animation.Transformation t) {
                    spotlightX += (targetX - spotlightX) * interpolatedTime;
                    eyesTracker.updateFocusPosition(spotlightX + getLeft());
                    invalidate();
                }
            };
            anim.setDuration(150); startAnimation(anim);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            RadialGradient gradient = new RadialGradient(spotlightX, getHeight() / 2f, 180f, 
                    new int[]{0x44FFFFFF, 0x00FFFFFF}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
            spotlightPaint.setShader(gradient);
            canvas.drawCircle(spotlightX, getHeight() / 2f, 180f, spotlightPaint);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int startX = (getWidth() - (getChildCount() * 160 + (getChildCount() - 1) * 100)) / 2;
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(startX + i * 260, 80, startX + i * 260 + 160, 240);
            }
        }
    }

    class AppItemView extends View {
        private final String label;
        private Bitmap logoBitmap = null;
        private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean isFocused = false;

        public AppItemView(Context context, String label, String pkg) {
            super(context);
            this.label = label;
            try {
                android.graphics.drawable.Drawable icon = context.getPackageManager().getApplicationIcon(pkg);
                if (icon != null) {
                    logoBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(logoBitmap);
                    icon.setBounds(0, 0, 120, 120); icon.draw(c);
                    logoPaint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                }
            } catch (Exception ignored) {}
            textPaint.setColor(Color.WHITE); textPaint.setTextSize(24f); textPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void showText(boolean show) { this.isFocused = show; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            if (logoBitmap != null) canvas.drawBitmap(logoBitmap, (getWidth() - 120) / 2f, 10, logoPaint);
            if (isFocused) canvas.drawText(label, getWidth() / 2f, getHeight() - 5, textPaint);

