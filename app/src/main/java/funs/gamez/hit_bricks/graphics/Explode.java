package funs.gamez.hit_bricks.graphics;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import funs.gamez.hit_bricks.Consts.Color;

/**
 * 爆炸效果
 * {@link Pellet}
 * {@link Brick}
 */
public class Explode {

    private static final String TAG = "Explode";

    private static final int STATE_ALIVE = 0;
    private static final int STATE_DEAD = 1;

    private final Pellet[] mPellets;            // 爆炸中的颗粒
    private int mState;                        // 爆炸状态

    public Explode(int particleNr, float x, float y) {
        Log.d(TAG, "Explode created at X=" + x + ", Y=" + y);
        mState = STATE_ALIVE;
        mPellets = new Pellet[particleNr];

        for (int i = 0; i < mPellets.length; i++) {
            Pellet p = new Pellet(Color.RED, x, y);
            mPellets[i] = p;
        }
    }

    // helper methods -------------------------
    public boolean isAlive() {
        return mState == STATE_ALIVE;
    }

    public void update() {
        if (mState != STATE_DEAD) {
            boolean isDead = true;
            for (Pellet p : mPellets) {
                if (p.isAlive()) {
                    p.update();
                    isDead = false;
                }
            }
            if (isDead)
                mState = STATE_DEAD;
        }
    }

    public void draw(GL10 gl) {
        for (Pellet p : mPellets) {
            if (p.isAlive()) {
                p.draw(gl);
            }
        }
    }
}