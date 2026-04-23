package funs.gamez.hit_bricks.graphics;

import android.util.Log;

import funs.gamez.hit_bricks.Consts;
import funs.gamez.hit_bricks.GameController.State;

// 运动的砖块
public class MotiveBrick extends Brick {

    private final int mFramesToWait;    // 砖块再次移动 需要等待的帧数。
    private int mToWait;        // 倒数计数（以 mFramesToWait 开头），说明移动砖块何时可以移动 （mToWait == 0）
    private float mSpeedX;        // 砖块的移动速度，移动砖块仅在 X 轴上移动。
    private boolean mCollided;    // 砖块是否碰撞，砖撞到墙上或另一块砖。

    // 砖块的索引，用于在 GameController 中索引 砖块的全局向量 （mBricks）
    private final int mIndexI;
    private final int mIndexJ;

    public MotiveBrick(float posX, float posY, Type type, int wait, int i, int j, float speedX) {
        this(Consts.Color.GOLD, posX, posY, type, wait, i, j, speedX);
    }

    public MotiveBrick(float[] colors, float posX, float posY, Type type, int wait, int i, int j, float speedX) {
        super(colors, posX, posY, type);

        mCollided = false;
        mIndexI = i;
        mIndexJ = j;

        // 砖块移动需要等待的帧数，基本上，两种方法来设置积木的速度：不移动球的帧数和移动的增量。
        mToWait = mFramesToWait = wait;
        mSpeedX = speedX;
    }

    public int getIndexI() {
        return mIndexI;
    }

    public int getIndexJ() {
        return mIndexJ;
    }

    public void move() {
        if (mToWait == 0) {
            if (mCollided) mSpeedX *= 2; // 很快从碰撞区域出来
            Log.v(TAG, "Moving MotiveBrick[" + mIndexI + "][" + mIndexJ + "], speed: " + mSpeedX + ", posX: " + mPosX);
            mPosX += mSpeedX;
            if (mCollided) {
                mSpeedX /= 2; // 恢复正常值
                mCollided = false;
            }
            mToWait = mFramesToWait;
        }
        mToWait--;
    }

    /*
     * 反转砖块的方向。
     */
    public void invertDirection() {
//      只在球准备好移动时反转方向。
        if (mToWait == 0) {
            Log.v(TAG, "Inverted MotiveBrick[" + mIndexI + "][" + mIndexJ + "]");
            mSpeedX *= -1;
        }
    }

    /*
     * 检测砖块是否碰撞
     */
    public boolean detectCollisionWithBrick(Brick other) {
        if (mToWait > 0) return false;

        if (this.getTopY() >= other.getBottomY() && this.getBottomY() <= other.getTopY() &&    // 与恰当的砖块相撞
                this.getRightX() >= other.getLeftX() && this.getLeftX() <= other.getRightX()) {
            if (this.getLeftX() < other.getLeftX()) {    // 与恰当的砖块相撞
                if (mSpeedX < 0)
                    mSpeedX *= -1;            // 砖块在与右侧砖块碰撞时应向右移动
                Log.v(TAG, "Collided in the right brick, MotiveBrick[" + mIndexI + "][" + mIndexJ + "]");
            } else {                                    //collided with the left brick
                if (mSpeedX > 0)
                    mSpeedX *= -1;            // 砖块在与左侧砖块碰撞时应向左移动
                Log.v(TAG, "Collided in the left brick, MotiveBrick[" + mIndexI + "][" + mIndexJ + "]");
            }
            mCollided = true;
            return true;
        } else return false;
    }

    /*
     * 检测到砖块和墙壁之间是否碰撞。
     */
    public boolean detectCollisionWithWall() {
        if (mToWait > 0) return false;

        if ((this.getRightX() >= State.getScreenHigherX())            // 与右墙相撞
                || (this.getLeftX() <= State.getScreenLowerX())) {    // 与左墙相撞
            if (this.getRightX() >= State.getScreenHigherX()) {        // 与右墙相撞
                if (mSpeedX < 0)
                    mSpeedX *= -1;                        // 砖块在与右墙碰撞时应向右移动
                Log.v(TAG, "Collided in the right wall, MotiveBrick[" + mIndexI + "][" + mIndexJ + "]");
            } else {                                                //collided with the left wall
                if (mSpeedX > 0)
                    mSpeedX *= -1;                        // 砖块在与左墙碰撞时应向左移动
                Log.v(TAG, "Collided in the left wall, MotiveBrick[" + mIndexI + "][" + mIndexJ + "]");
            }
            mCollided = true;
            return true;
        } else return false;
    }

    public boolean equal(int i, int j) {
        return (mIndexI == i) && (mIndexJ == j);
    }


}
