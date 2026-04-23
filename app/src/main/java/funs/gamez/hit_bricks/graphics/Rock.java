package funs.gamez.hit_bricks.graphics;

import android.util.Log;

import funs.gamez.hit_bricks.Consts;
import funs.gamez.hit_bricks.Consts.BallDirection;
import funs.gamez.hit_bricks.Consts.Hit;

// 小石头、小球，用来打砖块的
public class Rock extends Rectangle {
    private static final String TAG = "Rock";
    private static final float SCALE = 0.1f;
    private static final float[] VERTICES = {
            -0.25f, -0.25f, // bottom left
            -0.25f, 0.25f, // top left
            0.25f, -0.25f, // bottom right
            0.25f, 0.25f, // top right
    };

    private float mPrevPosX;            //上一个球的X坐标
    private float mPrevPosY;            // 上一个球的Y坐标
    // 用于轨迹方程
    private float mSlope;                // 球的轨迹斜率
    private boolean mUndefinedSlope;    // 如果球沿 Y 轴移动，则斜率未定义
    private final float mTrajectoryIncrement;    // 球的轨迹增量，其中一个轴上轨迹的增量

    public Rock(float previousPosX, float previousPosY, float posX, float posY, float trajetoryInc) {
        this(Consts.Color.SIENNA, previousPosX, previousPosY, posX, posY, trajetoryInc);
    }

    public Rock(float[] colors, float previousPosX, float previousPosY, float posX, float posY, float trajetoryInc) {
        super(VERTICES, SCALE, colors, posX, posY);

        mPrevPosX = previousPosX;
        mPrevPosY = previousPosY;

        if (mPosX == mPrevPosX) {
            mUndefinedSlope = true;
        } else {
            mUndefinedSlope = false;
            mSlope = (mPosY - mPrevPosY) / (mPosX - mPrevPosX);
        }

        mTrajectoryIncrement = trajetoryInc;
    }

    /*
     * y2 - y1
     * -------- = mSlope => y2 = y1 + mSlope * (x2 - x1)
     * x2 - x1
     */
    private float getY2InEquation(float x1, float y1, float x2) {
        return y1 + mSlope * (x2 - x1);
    }

    /*
     * y2 - y1
     * -------- = mSlope => x2 = (y2 - y1)/mSlope + x1
     * x2 - x1
     */
    private float getX2InEquation(float x1, float y1, float y2) {
        return (y2 - y1) / mSlope + x1;
    }

    /*
     * 获取球轨迹的入射角（相对于 Y 轴）。
     */
    public float getAngle() {
        return (float) (Consts.RIGHT_ANGLE - Math.abs(Math.toDegrees(Math.atan(mSlope))));
    }

    // 获取球的轨迹方向，计算球的移动方向
    private BallDirection getDirection() {
        if ((mPosX > mPrevPosX) && (mPosY > mPrevPosY))
            return BallDirection.RIGHT_UPWARD;
        else if ((mPosX > mPrevPosX) && (mPosY < mPrevPosY))
            return BallDirection.RIGHT_DOWNWARD;
        else if ((mPosX < mPrevPosX) && (mPosY > mPrevPosY))
            return BallDirection.LEFT_UPWARD;
        else if ((mPosX < mPrevPosX) && (mPosY < mPrevPosY))
            return BallDirection.LEFT_DOWNWARD;
        else if ((mPosX == mPrevPosX) && (mPosY > mPrevPosY))
            return BallDirection.UPWARD;
        else if ((mPosX == mPrevPosX) && (mPosY < mPrevPosY))
            return BallDirection.DOWNWARD;
        return BallDirection.UNKNOWN_DIRECTION;
    }

    /*
     * 球反射的轨迹方向为与 Y 轴平行的方向。
     */
    public void turnToPerpendicularDirection(Hit hitedSide) {
        // 球沿 Y 轴移动。
        if (mUndefinedSlope) {
            float tempY = mPrevPosY;
            mPrevPosY = mPosY;
            mPosY = tempY;
            return;
        }

        mSlope = -1 * (mSlope);
        float tempX = mPosX;
        float tempY = mPosY;
        switch (hitedSide) {
            case RIGHT_LEFT:
                mPosY = getY2InEquation(mPosX, mPosY, mPrevPosX);
                mPosX = mPrevPosX;
                break;
            case TOP_BOTTOM:
                mPosX = getX2InEquation(mPosX, mPosY, mPrevPosY);
                mPosY = mPrevPosY;
                break;
        }

        mPrevPosX = tempX;
        mPrevPosY = tempY;
    }

    /* 将球的轨迹更改为基于新坡度的新轨迹。
     * 新斜率基于作为参数传递的角度。
     */
    public void turnByAngle(float angle) {
        if (angle == Consts.RIGHT_ANGLE) {
            mUndefinedSlope = true;
            mPrevPosX = mPosX;
            turnToPerpendicularDirection(Hit.TOP_BOTTOM);
            return;
        } else {
            mUndefinedSlope = false;
        }
        mSlope = (float) Math.tan(Math.toRadians(angle));
        float tempX = mPosX;
        float tempY = mPosY;
        mPosX = getX2InEquation(mPosX, mPosY, mPrevPosY);
        mPosY = mPrevPosY;
        mPrevPosX = tempX;
        mPrevPosY = tempY;

    }

    public float getSlope() {
        return mSlope;
    }

    public String toString() {
        return TAG + " form, PosX: " + getPosX() +
                ", PrevPosX: " + mPrevPosX + ", PosY: " + getPosY() + ", PrevPosY: " + mPrevPosY;
    }

    public void move() {

        BallDirection dir = getDirection();

        switch (dir) {
            case RIGHT_UPWARD:
            case RIGHT_DOWNWARD:
                mPrevPosX = mPosX;
                mPrevPosY = mPosY;
                if (Math.abs(mSlope) <= 1) {    // 球在 X 轴上的移动速度快于在 Y 轴上的移动速度
                    float x2 = mPosX + mTrajectoryIncrement;
                    mPosY = getY2InEquation(mPosX, mPosY, x2);
                    mPosX = x2;
                } else {                        // 球在 Y 轴上的移动速度快于在 X 轴上的移动速度

                    float y2;
                    if (dir == BallDirection.RIGHT_UPWARD) y2 = mPosY + mTrajectoryIncrement;
                    else y2 = mPosY - mTrajectoryIncrement;

                    mPosX = getX2InEquation(mPosX, mPosY, y2);
                    mPosY = y2;
                }
                break;
            case LEFT_UPWARD:
            case LEFT_DOWNWARD:
                mPrevPosX = mPosX;
                mPrevPosY = mPosY;
                if (Math.abs(mSlope) <= 1) {    // 球在 X 轴上的移动速度快于在 Y 轴上的移动速度
                    float x2 = mPosX - mTrajectoryIncrement;
                    mPosY = getY2InEquation(mPosX, mPosY, x2);
                    mPosX = x2;
                } else {                        // 球在 Y 轴上的移动速度快于在 X 轴上的移动速度
                    float y2;
                    if (dir == BallDirection.LEFT_UPWARD) y2 = mPosY + mTrajectoryIncrement;
                    else y2 = mPosY - mTrajectoryIncrement;

                    mPosX = getX2InEquation(mPosX, mPosY, y2);
                    mPosY = y2;
                }
                break;

            // 球沿 Y 轴移动
            case UPWARD:
                mPrevPosY = mPosY;
                mPosY = mPosY + mTrajectoryIncrement;
                break;
            case DOWNWARD:
                mPrevPosY = mPosY;
                mPosY = mPosY - mTrajectoryIncrement;
                break;
        }

        Log.v(TAG, "Rock position: X=" + mPosX + ", Y=" + mPosY);
    }

}
