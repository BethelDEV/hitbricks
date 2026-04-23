package funs.gamez.hit_bricks;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import funs.gamez.hit_bricks.Consts.Collision;
import funs.gamez.hit_bricks.Consts.Color;
import funs.gamez.hit_bricks.Consts.Config;
import funs.gamez.hit_bricks.Consts.Difficult;
import funs.gamez.hit_bricks.Consts.Hit;
import funs.gamez.hit_bricks.Consts.Lives;
import funs.gamez.hit_bricks.Consts.Score;
import funs.gamez.hit_bricks.Consts.ScoreMultiplier;
import funs.gamez.hit_bricks.graphics.Brick;
import funs.gamez.hit_bricks.graphics.Brick.Type;
import funs.gamez.hit_bricks.graphics.Explode;
import funs.gamez.hit_bricks.graphics.MotiveBrick;
import funs.gamez.hit_bricks.graphics.Racket;
import funs.gamez.hit_bricks.graphics.Rock;

public class GameController {

    // Consts
    private static final String TAG = "GameController";
    private static final int SCREEN_INITIAL_X = 0;
    private static final int SCREEN_INITIAL_Y = 0;

    //GameController objects
    private Racket mPaddle;
    private Rock mBall;
    private Brick[][] mBricks;
    private final SoundPool mSoundPool;
    private final HashMap<String, Integer> mSoundIds;
    private final Context mContext;
    private List<Explode> mExplosions;
    private List<MotiveBrick> mMobileBricks;
    private HashMap<Collision, Integer> mConsecutiveCollision;

    public GameController(Context context) {
        mContext = context;

        // Load sounds
        mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundIds = new HashMap<>(4);
        mSoundIds.put(Consts.K_SOUND_LOSE_LIFE, mSoundPool.load(mContext, R.raw.lost_life, 1));
        mSoundIds.put(Consts.K_SOUND_HIT_WALL, mSoundPool.load(mContext, R.raw.wall_hit, 1));
        mSoundIds.put(Consts.K_SOUND_HIT_RACKET, mSoundPool.load(mContext, R.raw.paddle_hit, 1));
        mSoundIds.put(Consts.K_SOUND_HIT_BRICK, mSoundPool.load(mContext, R.raw.brick_hit, 1));
        mSoundIds.put(Consts.K_SOUND_EXPLOSION, mSoundPool.load(mContext, R.raw.explosive_brick, 1));

        // Create level elements
        resetElements();
    }

    private void resetElements() {
        mExplosions = new ArrayList<>();
        mMobileBricks = new ArrayList<>();

        /* We don't have the screen measures on the first call of this function,
         * so set to a sane default. */
        State.setScreenMeasures(2.0f, 2.0f);

        // Initialize game state
        State.setGamePaused(true);
        State.setGameOver(false);
        State.setLives(Lives.RESTART_LEVEL);
        State.setScore(Score.RESTART_LEVEL);
        State.setScoreMultiplier(ScoreMultiplier.RESTART_LEVEL);
        mConsecutiveCollision = new HashMap<>();
        for (Collision type : Config.CONSECUTIVE_COLLISION_DETECTION) {
            mConsecutiveCollision.put(type, 0);
        }

        // 初始化
//	球拍子
        mPaddle = new Racket(Color.SIENNA, Config.PADDLE_INITIAL_POS_X, Config.PADDLE_INITIAL_POS_Y);
        Log.d(TAG, "Created paddle:" +
                " BottomY: " + mPaddle.getBottomY() +
                " TopY: " + mPaddle.getTopY() +
                " LeftX: " + mPaddle.getLeftX() +
                " RightX: " + mPaddle.getRightX()
        );

//	球
        mBall = new Rock(Config.BALL_INITIAL_PREVIOUS_POS_X, Config.BALL_INITIAL_PREVIOUS_POS_Y,
                Config.BALL_INITIAL_POS_X, Config.BALL_INITIAL_POS_Y, Difficult.BALL_SPEED[State.getDifficult()]);
        Log.d(TAG, "Created ball:" +
                " BottomY: " + mBall.getBottomY() +
                " TopY: " + mBall.getTopY() +
                " LeftX: " + mBall.getLeftX() +
                " RightX: " + mBall.getRightX()
        );

        createLevel(Config.NUMBER_OF_LINES_OF_BRICKS, Config.NUMBER_OF_COLUMNS_OF_BRICKS,
                Config.BRICKS_INITIAL_POS_X, Config.BRICKS_INITIAL_POS_Y);

        mExplosions = new ArrayList<>();
    }

    private void createLevel(final int blocksX, final int blocksY, final float initialX, final float initialY) {
        mBricks = new Brick[blocksX][blocksY];

        // 砖块的初始位置
        float newPosX = initialX;
        float newPosY = initialY;

        for (int i = 0; i < blocksX; i++) {
            int sign = 1;
            for (int j = 0; j < blocksY; j++) {
                sign *= -1; // 连续的砖块开始向不同的方向移动
                double prob = Math.random(); // 创建具有随机概率的特殊砖块
                if (prob <= (Difficult.MOBILE_BRICK_PROB[State.getDifficult()] +
                        Difficult.EX_BRICK_PROB[State.getDifficult()] +
                        Difficult.GREY_BRICK_PROB[State.getDifficult()])) {
                    if (prob <= Difficult.MOBILE_BRICK_PROB[State.getDifficult()]) {
                        MotiveBrick brick = new MotiveBrick(
                                newPosX, newPosY, Type.MOBILE, Config.MOBILE_BRICK_SKIP_FRAMES,
                                i, j, sign * Difficult.MOBILE_BRICK_SPEED[State.getDifficult()]);
                        mBricks[i][j] = brick;
                        mMobileBricks.add(brick);
                    } else if ((prob - Difficult.MOBILE_BRICK_PROB[State.getDifficult()]) <=
                            Difficult.EX_BRICK_PROB[State.getDifficult()]) {
                        mBricks[i][j] = new Brick(Color.RED, newPosX, newPosY, Type.EXPLOSIVE);
                    } else {
                        mBricks[i][j] = new Brick(Color.SLATE_BLUE, newPosX, newPosY, Type.HARD);
                    }
                } else {
                    mBricks[i][j] = new Brick(Color.WHITE, newPosX, newPosY, Type.NORMAL);
                }
                // 同一条线上下一块砖的位置应位于最后一块砖的右侧
                newPosX += mBricks[i][j].getSizeX() + Config.SPACE_BETWEEN_BRICKS;
            }
            // 完成填充一行砖块，重置到初始 X 位置以填充下一行
            newPosX = initialX;
            // 与 X 位置相似，将下一行砖块放在最后一行砖块的底部
            newPosY += mBricks[i][0].getSizeY() + Config.SPACE_BETWEEN_BRICKS;
        }

    }

    public void drawElements(GL10 gl) {
        // 绘制球和球拍
        mPaddle.draw(gl);
        mBall.draw(gl);

        // 绘制所有砖块
        for (Brick[] bricks : mBricks) {
            for (Brick brick : bricks) {
                if (brick != null) {
                    brick.draw(gl);
                }
            }
        }

        // 绘制爆炸（碎砖块）
        for (Explode explosion : mExplosions) {
            explosion.draw(gl);
        }
    }

    private void updateBrickExplosion() {
        for (Explode explosion : mExplosions) {
            if (explosion.isAlive()) {
                explosion.update();
            }
        }
    }

    public void updatePaddlePosX(float x) {
        // 通过触摸更新球拍位置
        mPaddle.setPosX(x);
    }

    /* 球拍
     *
     * x2 - x1			reflected angle
     * --------  = 	------------------------
     * width/2  	ANGLE_OF_REFLECTION_BOUND */
    private float calcReflectedAngle(float x2, float x1) {
        return Consts.ANGLE_OF_REFLECTION_BOUND * (x2 - x1) / (mPaddle.getWidth() / 2);
    }

    // 检测连续碰撞
    private Collision detectConsecutiveCollision(Collision collisionType) {
        // 检测球和某物之间的各种碰撞
        for (Map.Entry<Collision, Integer> entry : mConsecutiveCollision.entrySet()) {

            Collision currentType = entry.getKey();
            int currentValue = entry.getValue();

            if (currentValue > 100) {
                Log.e(TAG, "Detected infinite consecutive collision of type " + currentType.name() +
                        ", restarting round.");
                mBall = new Rock(Config.BALL_INITIAL_PREVIOUS_POS_X, Config.BALL_INITIAL_PREVIOUS_POS_Y,
                        Config.BALL_INITIAL_POS_X, Config.BALL_INITIAL_POS_Y,
                        Difficult.BALL_SPEED[State.getDifficult()]);
                entry.setValue(0);
                State.setGamePaused(true);
                return Collision.NOT_AVAILABLE;
            } else if (currentType == collisionType && currentValue > 0) {
                // 当前碰撞值高于 0，当前碰撞类型与检测碰撞相同。
//这意味着在两个连续的帧上发生了两次相同类型的碰撞。
//因此，请跳过此碰撞，否则我们可以进入无效状态（例如无限碰撞）。
                Log.d(TAG, "Detected consecutive collision of type " + currentType.name() + ", skipping.");
                entry.setValue(++currentValue);
                return Collision.NOT_AVAILABLE;
            } else if (currentType == collisionType) {
                // 要检测我们是否处于连续（可能是无限）碰撞状态，请增加当前碰撞类型的值。
                entry.setValue(++currentValue);
            } else if (currentValue > 0) {
                //当前冲突值大于 0，但当前冲突类型与检测冲突类型不同。
                // 在这种情况下，检测到的碰撞与旧碰撞不同，因此我们可以安全地降低此类碰撞的当前值。
                entry.setValue(--currentValue);
            }
        }
        return collisionType;

    }

    public void updateState() {
        float reflectedAngle, angleOfBallSlope;

        // 碰撞
        Collision collisionType = detectConsecutiveCollision(detectCollision());

        switch (collisionType) {
            case WALL_RIGHT_LEFT_SIDE:
                // 碰墙
                Log.d(TAG, "Detected collision between ball and left/right wall");
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_HIT_WALL), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                mBall.turnToPerpendicularDirection(Hit.RIGHT_LEFT);
                Log.d(TAG, "Next slope: " + mBall.getSlope());
                break;
            case WALL_TOP_BOTTOM_SIDE:
                Log.d(TAG, "Detected collision between ball and top/bottom wall");
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_HIT_WALL), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                mBall.turnToPerpendicularDirection(Hit.TOP_BOTTOM);
                Log.d(TAG, "Next slope: " + mBall.getSlope());
                break;
            case BRICK_BALL:
                // 当击中砖块时，增加得分并播放音效
                State.setScore(Score.BRICK_HIT);
                Log.i(TAG, "Score multiplier: " + State.getScoreMultiplier() + " Score: " + State.getScore());
                State.setScoreMultiplier(ScoreMultiplier.BRICK_HIT); // Update multiplier for the next brick hit
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_HIT_BRICK), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                mBall.turnToPerpendicularDirection(Hit.TOP_BOTTOM);
                break;
            case EX_BRICK_BALL:
                // 爆炸砖有不同的音效和配乐，但其余的都是一样的
                State.setScore(Score.EX_BRICK_HIT);
                Log.i(TAG, "Score multiplier: " + State.getScoreMultiplier() + " Score: " + State.getScore());
                State.setScoreMultiplier(ScoreMultiplier.BRICK_HIT);
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_EXPLOSION), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                mBall.turnToPerpendicularDirection(Hit.TOP_BOTTOM);
                break;
            case PADDLE_BALL:
                Log.d(TAG, "Detected collision between ball and paddle on position X=" + mPaddle.getPosX());
                State.setScoreMultiplier(ScoreMultiplier.PADDLE_HIT);
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_HIT_RACKET), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                // 计算反射角，斜率的角度（球轨迹的）是反射角的补充。
                if (mPaddle.getPosX() >= mBall.getPosX()) {    // 球击中了右半部分的球拍。
                    reflectedAngle = calcReflectedAngle(mBall.getPosX(), mPaddle.getPosX());
                    angleOfBallSlope = (Consts.RIGHT_ANGLE - reflectedAngle);
                } else {                                    // 球击中了左半部分的球拍。
                    reflectedAngle = calcReflectedAngle(mPaddle.getPosX(), mBall.getPosX());
                    // 球向左移动。
                    angleOfBallSlope = -1 * (Consts.RIGHT_ANGLE - reflectedAngle);
                }
                mBall.turnByAngle(angleOfBallSlope);
                break;
            case LIFE_LOST:
                State.setLives(Lives.LOST_LIFE);
                mSoundPool.play(mSoundIds.get(Consts.K_SOUND_LOSE_LIFE), State.getVolume(), State.getVolume(), 1, 0, 1.0f);
                // 如果游戏没有结束
                if (!State.getGameOver()) {
                    Log.i(TAG, "User lost a live, new live count: " + State.getLives());
                    mBall = new Rock(Config.BALL_INITIAL_PREVIOUS_POS_X, Config.BALL_INITIAL_PREVIOUS_POS_Y,
                            Config.BALL_INITIAL_POS_X, Config.BALL_INITIAL_POS_Y,
                            Difficult.BALL_SPEED[State.getDifficult()]);
                    State.setScoreMultiplier(ScoreMultiplier.LOST_LIFE);
                    State.setGamePaused(true);
                } else {
                    Log.i(TAG, "No more lives, GameController Over");
                }
                break;
            case NOT_AVAILABLE:
                // Nothing to do here
                break;
            default:
                Log.e(TAG, "Invalid collision");
                break;
        }

        updateBrickExplosion();

        moveMobileBricks();

        mBall.move();
    }

    private void moveMobileBricks() {
        for (int a = 0; a < mMobileBricks.size(); a++) {
            mMobileBricks.get(a).move();
        }
    }

    private void brickExploded(int i, int j) {
        // 删除周围的砖块
        for (int a = Math.max(i - 1, 0); a < Math.min(i + 2, mBricks.length); a++) {
            for (int b = Math.max(j - 1, 0); b < Math.min(j + 2, mBricks[i].length); b++) {
                if (mBricks[a][b] != null) {
                    if (mBricks[a][b].getLives() == 0) {
                        mBricks[a][b] = null; // 删除砖块
                        State.setScore(Score.BRICK_HIT); // 并添加得分
                    } else {
                        decrementBrickLife(a, b);
                    }
                }
            }
        }
    }

    // 减少砖块的生命值
    private void decrementBrickLife(int i, int j) {
        mBricks[i][j].decrementLives();
        if (mBricks[i][j].getType() == Type.HARD) {
            mBricks[i][j].setColor(Color.WHITE);
        }
    }

    // 检测移动砖块的碰撞
    private void detectCollisionOfMobileBricks() {
        for (int a = 0; a < mMobileBricks.size(); a++) {
            boolean collided = false;
            MotiveBrick mBrick = mMobileBricks.get(a);

            int i = mBrick.getIndexI();
            int j = mBrick.getIndexJ();

            for (int x = 0; x < Config.NUMBER_OF_COLUMNS_OF_BRICKS; x++) {
                if (x != j) {
                    Brick brick = mBricks[i][x];
                    if ((brick != null) && (mBrick.detectCollisionWithBrick(brick))) {
                        Log.v(TAG, "Going to call invert, brick: [" + i + "][" + j + "]");
                        mBrick.invertDirection();
                        collided = true;
                        break;
                    }
                }
            }

            if (!collided && mBrick.detectCollisionWithWall()) {
                mBrick.invertDirection();
            }
        }
    }

    private Collision detectCollision() {

        detectCollisionOfMobileBricks();

        // 检测球与墙壁之间的碰撞
        if (mBall.getBottomY() <= State.getScreenLowerY()                //球与底壁相撞
                && !Difficult.INVINCIBILITY[State.getDifficult()]) {
            return Collision.LIFE_LOST; //  失去一条生命
        } else if ((mBall.getTopY() >= State.getScreenHigherY())    // 撞在顶墙上
                || (mBall.getBottomY() <= State.getScreenLowerY()    // 在底壁上相撞...
                && Difficult.INVINCIBILITY[State.getDifficult()]))    //...开启无限模式
        {
            return Collision.WALL_TOP_BOTTOM_SIDE;
        } else if ((mBall.getRightX() >= State.getScreenHigherX())    // 撞在右边的墙上
                || (mBall.getLeftX() <= State.getScreenLowerX()))    // 撞在左边的墙上
        {
            return Collision.WALL_RIGHT_LEFT_SIDE;
        }

        // 检测球与球拍之间的碰撞
        if (mBall.getTopY() >= mPaddle.getBottomY() && mBall.getBottomY() <= mPaddle.getTopY() &&
                mBall.getRightX() >= mPaddle.getLeftX() && mBall.getLeftX() <= mPaddle.getRightX()) {
            return Collision.PADDLE_BALL;
        }

        // 游戏结束
        boolean gameFinish = true;

        for (int i = 0; i < mBricks.length; i++) {
            /* 这应该会稍微优化碰撞处理：一旦我们已经检查了线 i 中一块砖的 Y 位置，就没有必要检查同一行中其他砖块的 Y 位置。
checkedLine 标志为我们执行此操作。这种优化之所以有效，是因为 && 是短路算子 。 */
            boolean checkedLine = false;
            for (int j = 0; j < mBricks[i].length; j++) {
                // 检查砖块是否尚未销毁
                if (mBricks[i][j] != null) {
                    // 如果还有砖块，游戏还没有结束
                    gameFinish = false;

                    // 检查砖块与球的碰撞，检查球是否与砖块处于相同的 Y 位置
                    if (checkedLine
                            || (mBall.getTopY() >= mBricks[i][j].getBottomY()
                            && mBall.getBottomY() <= mBricks[i][j].getTopY())) {
                        checkedLine = true;
                        // 检查碰撞是否真的发生
                        if (mBall.getRightX() >= mBricks[i][j].getLeftX()
                                && mBall.getLeftX() <= mBricks[i][j].getRightX()) {
                            Log.d(TAG, "Detected collision between ball and brick[" + i + "][" + j + "]");
                            // 更新砖块状态
                            if (mBricks[i][j].getLives() == 0) {
                                if (mBricks[i][j].getType() == Type.EXPLOSIVE) {
                                    mExplosions.add(new Explode(Brick.BRICK_EXPLOSION_SIZE,
                                            mBricks[i][j].getPosX(), mBricks[i][j].getPosY()));
                                    // 砖块爆炸
                                    brickExploded(i, j);
                                    return Collision.EX_BRICK_BALL;
                                } else if (mBricks[i][j].getType() == Type.MOBILE) {
                                    deleteMobileBrick(i, j);
                                }
                                mBricks[i][j] = null; // 删除砖块
                            } else {
                                decrementBrickLife(i, j);
                            }
                            return Collision.BRICK_BALL;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        // 如果没有砖块了，游戏就结束了
        State.setGameOver(gameFinish);

        return Collision.NOT_AVAILABLE;
    }

    private void deleteMobileBrick(int i, int j) {
        for (int a = 0; a < mMobileBricks.size(); a++) {
            if (mMobileBricks.get(a).equal(i, j)) {
                mMobileBricks.remove(a);
                return;
            }
        }
    }

    /**
     * 游戏状态，例如实际游戏得分、生命值以及游戏是否结束。
     * <p>
     * 这个类应是静态的，因为我们需要在外部访问这些信息。
     */
    public static class State {
        private static long sScore;
        private static int sScoreMultiplier;
        private static int sLives;
        private static boolean sGameOver;
        private static float sScreenHigherY;
        private static float sScreenLowerY;
        private static float sScreenHigherX;
        private static float sScreenLowerX;
        private static boolean sGamePaused;
        private static int sDifficult;
        private static float sVolume;

        static void setScore(Score event) {
            switch (event) {
                case BRICK_HIT:
                    sScore += Difficult.HIT_SCORE[sDifficult] * getScoreMultiplier();
                    break;
                case RESTART_LEVEL:
                    sScore = 0;
                    break;
                case EX_BRICK_HIT:
                    sScore += Difficult.HIT_SCORE[sDifficult] * 2 * getScoreMultiplier();
                    break;
            }
        }

        static void setScoreMultiplier(ScoreMultiplier event) {
            switch (event) {
                case RESTART_LEVEL:
                case LOST_LIFE:
                    sScoreMultiplier = 1;
                    break;
                case BRICK_HIT:
                    if (sScoreMultiplier < Difficult.MAX_SCORE_MULTIPLIER[sDifficult]) {
                        sScoreMultiplier *= 2;
                    }
                    break;
                case PADDLE_HIT:
                    if (sScoreMultiplier > 1) {
                        sScoreMultiplier /= 2;
                    }
                    break;
            }
        }

        static void setLives(Lives event) {
            switch (event) {
                case RESTART_LEVEL:
                    sGameOver = false;
                    sLives = Difficult.LIFE_STOCK[sDifficult];
                    break;
                case LOST_LIFE:
                    if (sLives > 0) {
                        sLives--;
                    } else {
                        sGameOver = true;
                    }
                    break;
            }
        }

        public static void setDifficult(int difficult) {
            if (difficult < 0) {
                Log.e(TAG, "Invalid difficult preference: " + difficult);
                // 设置为默认难度
                sDifficult = 0;
            } else {
                sDifficult = difficult;
            }
        }

        static void setGameOver(boolean gameIsOver) {
            // 如果游戏结束了
            if (gameIsOver) {
                sScore += sLives * Difficult.LIFE_SCORE_BONUS[sDifficult];
            }
            sGameOver = gameIsOver;
        }

        public static void setGamePaused(boolean gamePaused) {
            sGamePaused = gamePaused;
        }

        static void setVolume(float volume) {
            if (volume >= 0.0f || volume <= 1.0f) {
                sVolume = volume;
            } else {
                Log.e(TAG, "Invalid sound effect volume: " + volume);
                sVolume = 0.0f;
            }
        }

        public static void enableSoundEffects(boolean enable) {
            if (enable) {
                setVolume(1.0f);
            } else {
                setVolume(0.0f);
            }
        }

        public static boolean getGameOver() {
            return sGameOver;
        }

        public static boolean getGamePaused() {
            return sGamePaused;
        }

        public static long getScore() {
            return sScore;
        }

        public static int getScoreMultiplier() {
            return sScoreMultiplier;
        }

        public static int getLives() {
            return sLives;
        }

        public static float getScreenLowerX() {
            return sScreenLowerX;
        }

        public static float getScreenHigherX() {
            return sScreenHigherX;
        }

        public static float getScreenLowerY() {
            return sScreenLowerY;
        }

        public static float getScreenHigherY() {
            return sScreenHigherY;
        }

        public static void setScreenMeasures(float screenWidth, float screenHeight) {
            // 计算新的屏幕 度量值。我们需要为球划出一堵墙。
            sScreenLowerX = SCREEN_INITIAL_X - screenWidth / 2;
            sScreenHigherX = SCREEN_INITIAL_X + screenWidth / 2;
            sScreenLowerY = SCREEN_INITIAL_Y - screenHeight / 2;
            sScreenHigherY = SCREEN_INITIAL_Y + screenHeight / 2;
        }

        static int getDifficult() {
            return sDifficult;
        }

        public static float getVolume() {
            return sVolume;
        }
    }
}
