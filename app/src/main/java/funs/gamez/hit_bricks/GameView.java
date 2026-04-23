package funs.gamez.hit_bricks;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import funs.gamez.hit_bricks.Consts.Config;
import funs.gamez.hit_bricks.GameController.State;

public class GameView extends GLSurfaceView {

    private static final String TAG = "GameView";

    private long mPrevFrameTime;
    private long mCurrentTime;
    private long mElapsedTime;
    private long mLag;

    private final Renderer mRenderer;

    private int mScreenWidth;
    private int mScreenHeight;

    private final float[] mUnprojectViewMatrix = new float[16];
    private final float[] mUnprojectProjMatrix = new float[16];

    private class Renderer implements GLSurfaceView.Renderer {

        private final GameController mGame;

        Renderer(Context context) {
            mGame = new GameController(context);
            mPrevFrameTime = System.nanoTime();
        }

        /* onDrawFrame():每帧都通过该方法进行绘制。
        绘制时通常先调用 glClear　函数来清空 framebuffer，然后在调用 OpenGL ES 的起它的接口进行绘制。
         */
        @SuppressWarnings("unused")
        @Override
        public void onDrawFrame(GL10 gl) {
            mCurrentTime = System.nanoTime();
            mElapsedTime = (mCurrentTime - mPrevFrameTime) / Consts.NANOS_PER_SECONDS;
            if (!State.getGamePaused()) mLag += mElapsedTime;
            // 限制帧渲染速度
            mPrevFrameTime = mCurrentTime + limitFps(Config.FPS_LIMIT);

            if (0 != mElapsedTime)
                Log.v(TAG, "FPS: " + Consts.MS_PER_SECONDS / mElapsedTime);

            // 一个简单的游戏循环实现
            int frame_counter = 0;
            while (mLag >= Config.MS_PER_UPDATE) {

                // 如果游戏结束或暂停，则停止更新状态并冻结最后一帧，以便用户可以看到发生了什么。
                if (!State.getGamePaused() && !State.getGameOver()) {
                    mGame.updateState();
                }
                mLag -= Config.MS_PER_UPDATE;

                // 如果设备速度太慢，无法保持良好的帧速率，则跳过游戏处理，以便游戏在该设备上运行速度较慢。
                if (frame_counter >= Config.FRAME_SKIP) {
                    break;
                }
                frame_counter++;
            }

            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            mGame.drawElements(gl);
        }

        /* onSurfaceChanged():当 surface 的尺寸发生改变时该方法被调用。
            在这里设置 viewport。
         */
        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mScreenWidth = width;
            mScreenHeight = height;

            float ratio = (float) width / height;
            State.setScreenMeasures((2.0f * Config.SCREEN_RATIO) - Config.WALL, 2.0f - Config.WALL);

            // 定义固定游戏屏幕比例(独立于屏幕分辨率的比例)
            if (ratio >= Config.SCREEN_RATIO) {
                int newWidth = Math.round(height * Config.SCREEN_RATIO);
                gl.glViewport((width - newWidth) / 2, 0, newWidth, height);
            } else {
                int newHeight = Math.round(width / Config.SCREEN_RATIO);
                gl.glViewport(0, (height - newHeight) / 2, width, newHeight);
            }
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(-Config.SCREEN_RATIO, Config.SCREEN_RATIO, -1.0f, 1.0f, -1.0f, 1.0f);

            Matrix.orthoM(mUnprojectProjMatrix, 0, -Config.SCREEN_RATIO, Config.SCREEN_RATIO, -1.0f, 1.0f, -1.0f, 1.0f);
            Matrix.setIdentityM(mUnprojectViewMatrix, 0);
        }

        /* onSurfaceCreated():该方法在渲染开始前调用，OpenGL ES 的绘制上下文被重建时也会被调用。
        当 activity 暂停时绘制上下文会丢失，当 activity 继续时，绘制上下文会被重建。
        另外，创建长期存在的 OpenGL 资源(如texture)往往也在这里进行。
         */
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glDisable(GL10.GL_DITHER);
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

            //      绘制背景色
            float[] bgColor = Consts.Color.L_GREEN_RGB;
            gl.glClearColor(bgColor[0], bgColor[1], bgColor[2], 1f);
            //      gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glDisable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glDisable(GL10.GL_DEPTH_TEST);
        }

        void updatePaddlePosition(final float x) {
            // 在用户取消暂停游戏（即再次点击屏幕）之前，不要允许用户更新球拍位置
            if (!State.getGamePaused() && !State.getGameOver()) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mGame.updatePaddlePosX(x);
                    }
                });
            }
        }

        /**
         * 用于调试目的。我们可以用这个代码人为地限制游戏的FPS。
         *
         * @param maxFps 游戏应运行的最大 FPS
         * @return 自执行限制 FPS 以来经过的时间（需要正确更新上一次）
         */
        private long limitFps(int maxFps) {
            if (maxFps == 0) return 0;

            long framesPerSec = Consts.MS_PER_SECONDS / maxFps;
            if (mElapsedTime < framesPerSec) {
                long sleepTime = framesPerSec - mElapsedTime;
                Log.v(TAG, "deltaTime: " + mElapsedTime + "ms, frame limit set to: "
                        + framesPerSec + "ms, waiting " + sleepTime + "ms");
                try {
                    Thread.sleep(sleepTime);
                    return sleepTime;
                } catch (InterruptedException e) {
                    return 0;
                }
            }
            return 0;
        }
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mRenderer = new Renderer(context);
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                final float screenX = e.getX();
                final float screenY = mScreenHeight - e.getY();

                final int[] viewport = {
                        0, 0, mScreenWidth, mScreenHeight
                };

                float[] resultWorldPos = {
                        0.0f, 0.0f, 0.0f, 0.0f
                };

                GLU.gluUnProject(screenX, screenY, 0, mUnprojectViewMatrix, 0, mUnprojectProjMatrix, 0,
                        viewport, 0, resultWorldPos, 0);
                resultWorldPos[0] /= resultWorldPos[3];
                resultWorldPos[1] /= resultWorldPos[3];
                resultWorldPos[2] /= resultWorldPos[3];
                resultWorldPos[3] = 1.0f;

                // 更新球拍位置
                mRenderer.updatePaddlePosition(resultWorldPos[0]);
                break;

            case MotionEvent.ACTION_DOWN:
                // 仅当用户单击屏幕时才开始游戏
                State.setGamePaused(false);
                break;
        }
        return true;
    }
}