
package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.util.AttributeSet;

import java.util.Random;

public class StarGameView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final int starCount = 7;
    private final float[] starX = new float[starCount];
    private final float[] starY = new float[starCount];
    private final float[] starSpeed = new float[starCount];
    private final float[] starSize = new float[starCount];

    private float basketX, basketY, basketWidth, basketHeight;
    private int score = 0;
    private long startTime, previousFrameTime;

    private boolean running = false;
    private boolean initialized = false;
    private boolean gameOver = false;

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            updateGame();
            invalidate();
            handler.postDelayed(this, 16);
        }
    };


    public StarGameView(Context context) {
        super(context);
        init();
    }

    public StarGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StarGameView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
    }


    private void initializeGame() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        basketWidth = getWidth() * 0.22f;
        basketHeight = 20;
        basketY = getHeight() * 0.84f;
        basketX = getWidth() / 2f;

        for (int i = 0; i < starCount; i++) {
            resetStar(i, true);
        }

        score = 0;
        gameOver = false;
        initialized = true;

        startTime = SystemClock.elapsedRealtime();
        previousFrameTime = startTime;
    }

    private void resetStar(int i, boolean randomY) {
        starX[i] = 20 + random.nextFloat()
                * Math.max(1, getWidth() - 40);

        starY[i] = randomY
                ? random.nextFloat() * getHeight() * 0.65f
                : -30 - random.nextFloat() * 200;

        starSize[i] = 12 + random.nextFloat() * 12;
        starSpeed[i] = 180 + random.nextFloat() * 220;
    }

    public void resumeGame() {
        if (running) return;

        running = true;

        if (!initialized) {
            initializeGame();
        } else if (!gameOver) {
            previousFrameTime = SystemClock.elapsedRealtime();
        }

        handler.removeCallbacks(gameLoop);
        handler.post(gameLoop);
    }

    public void pauseGame() {
        running = false;
        handler.removeCallbacks(gameLoop);
    }

    @Override
    protected void onSizeChanged(
            int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        initializeGame();
    }

    private void updateGame() {
        if (!initialized || gameOver) return;

        long now = SystemClock.elapsedRealtime();

        float dt = Math.min(
                (now - previousFrameTime) / 1000f, 0.05f);
        previousFrameTime = now;

        if (now - startTime >= 30_000L) {
            gameOver = true;
            return;
        }

        for (int i = 0; i < starCount; i++) {
            starY[i] += starSpeed[i] * dt;

            boolean caught =
                    starX[i] >= basketX - basketWidth / 2f
                            && starX[i] <= basketX + basketWidth / 2f
                            && starY[i] + starSize[i] >= basketY
                            && starY[i] - starSize[i]
                            <= basketY + basketHeight;

            if (caught) {
                score++;
                resetStar(i, false);
            } else if (starY[i] - starSize[i] > getHeight()) {
                resetStar(i, false);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(13, 19, 51));

        if (!initialized) return;

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("SCORE: " + score, 20, 35, paint);

        int remaining = (int) Math.max(0,
                (30_000L - (SystemClock.elapsedRealtime()
                        - startTime) + 999) / 1000);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("TIME: " + remaining,
                getWidth() - 20, 35, paint);

        for (int i = 0; i < starCount; i++) {
            drawStar(canvas, starX[i], starY[i], starSize[i]);
        }

        paint.setColor(Color.rgb(55, 210, 220));
        RectF basket = new RectF(
                basketX - basketWidth / 2f,
                basketY,
                basketX + basketWidth / 2f,
                basketY + basketHeight);

        canvas.drawRoundRect(basket, 10, 10, paint);

        if (gameOver) drawGameOver(canvas);
    }

    private void drawStar(Canvas canvas, float x, float y, float r) {
        Path path = new Path();

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * i / 5 - Math.PI / 2;
            float radius = i % 2 == 0 ? r : r * 0.45f;

            float px = x + (float) Math.cos(angle) * radius;
            float py = y + (float) Math.sin(angle) * radius;

            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }

        path.close();
        paint.setColor(Color.rgb(255, 211, 70));
        canvas.drawPath(path, paint);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawColor(0xCC080D25);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(34);
        paint.setColor(Color.rgb(255, 215, 75));
        canvas.drawText("GAME OVER!", cx, cy - 35, paint);

        paint.setTextSize(24);
        paint.setColor(Color.WHITE);
        canvas.drawText("Score: " + score, cx, cy + 5, paint);

        paint.setTextSize(20);
        paint.setColor(Color.rgb(55, 210, 220));
        canvas.drawText("TAP TO RESTART", cx, cy + 55, paint);
        paint.setFakeBoldText(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {

            if (gameOver) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    initializeGame();
                    invalidate();
                }
                return true;
            }

            basketX = Math.max(basketWidth / 2f,
                    Math.min(getWidth() - basketWidth / 2f,
                            event.getX()));

            invalidate();
            return true;
        }

        return event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL;
    }
}
