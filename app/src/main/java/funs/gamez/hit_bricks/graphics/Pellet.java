package funs.gamez.hit_bricks.graphics;

import funs.gamez.hit_bricks.GameController.State;

/**
 * 颗粒 粒子
 */
public class Pellet extends Rectangle {
	
	private static final float SCALE = 0.03f;
	private static final float[] VERTICES = {
		-0.25f, -0.25f, // bottom left
		-0.25f,  0.25f, // top left
		0.25f, -0.25f, // bottom right
		0.25f,  0.25f, // top right
	};
	
	private static final int STATE_ALIVE = 0;	// 粒子是活的
	private static final int STATE_DEAD = 1;		// 粒子"死了"
	
	private static final int DEFAULT_LIFETIME 	= 30;	// 粒子的生命
	private static final float MAX_SPEED			= ((VERTICES[3] - VERTICES[1]) * SCALE) * 3; // 每次更新
	
	private int mState;			// 粒子是活的还是死的
	private double mXv, mYv;	// 垂直和水平速度
	private int mAge;			// 粒子的年龄
	private final int mLifetime;		// 粒子在达到此值时死亡

	public Pellet(float[] colors, float posX, float posY) {
		super(VERTICES, SCALE, colors, posX, posY);
		mState = Pellet.STATE_ALIVE;
		mLifetime = DEFAULT_LIFETIME;
		mAge = 0;
		/*
		 * 随机 水平和垂直速度。
		 */
		mXv = (rndDbl(0, MAX_SPEED * 2) - MAX_SPEED);
		mYv = (rndDbl(0, MAX_SPEED * 2) - MAX_SPEED);
		/*
		 * 粒子速度不能太快
		 * 通过勾股定理, x^2 + y^2 = d^2.
		 */
		if (mXv * mXv + mYv * mYv > MAX_SPEED * MAX_SPEED) {
			mXv *= 0.7;
			mYv *= 0.7;
		}
	}
	
	// helper methods -------------------------
	public boolean isAlive() {
		return mState == STATE_ALIVE;
	}

	private static double rndDbl(double min, double max) {
		return min + (max - min) * Math.random();
	}
	
	private void move() {
		if (mState != STATE_DEAD) {
			mPosX += mXv;
			mPosY += mYv;
			mAge++;	// 增加颗粒的年龄
			
			if (mAge >= mLifetime) { // 达到生命终点
				mState = STATE_DEAD;
			}
		}
	}
	
	public void update() {		
		// 碰撞
		if (isAlive()) {
			if ((mPosX - (getWidth()/2) <= State.getScreenLowerX()) || (mPosX >= State.getScreenHigherX() - (getWidth()/2))) {
				mXv *= -1;
			}

			if ((mPosY - (getHeight()/2) <= State.getScreenHigherY()) || (mPosY >= State.getScreenLowerY() - (getHeight()/2))) {
				mYv *= -1;
			}
		}
		move();
	}

}