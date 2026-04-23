package funs.gamez.pages;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import funs.gamez.hit_bricks.GameView;
import funs.gamez.hit_bricks.R;
import funs.gamez.hit_bricks.Consts;
import funs.gamez.hit_bricks.Consts.Config;
import funs.gamez.hit_bricks.GameController.State;

public class GameActivity extends Activity {

    private GameView mGameView;
    private Handler mHandler;
    private TextView mScoreTextView;
    private TextView mLivesTextView;
    private TextView mHighScoreTextView;
    private TextView mReadyTextView;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mSharedPrefsEditor;
    private long mHighScore;
    private boolean mNewHighScore;
    private boolean mFinish;
    private SoundPool mSoundPool;
    private HashMap<String, Integer> mSoundIds;
    private View mDecorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initState();
        setContentView(R.layout.activity_game);

        mHandler = new Handler();
        mNewHighScore = false;
        mFinish = false;
        mDecorView = getWindow().getDecorView();

        mGameView = findViewById(R.id.opengl);
        mHighScore = mSharedPrefs.getLong(Consts.KEY_HIGH_SCORE, 0);

        /*  TextView 展示游戏的得分等信息 */
        mScoreTextView = findViewById(R.id.score);
        mScoreTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        mLivesTextView = findViewById(R.id.lives);
        mLivesTextView.setTextColor(Color.RED);
        mHighScoreTextView = findViewById(R.id.highScore);
        mHighScoreTextView.setTextColor(Color.WHITE);
        mReadyTextView = findViewById(R.id.ready);
        mReadyTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

        // 初始化 SoundPool
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundIds = new HashMap<>(1);
        mSoundIds.put(Consts.K_SOUND_VICTORY, mSoundPool.load(this, R.raw.victory_fanfare, 1));

        // 无法从 GL 线程更新 UI，因此设置了一个计时器，用来更新UI。
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!mFinish) updateUI();
            }
        }, 0, Config.MS_PER_UPDATE * 10);
    }

//    设定游戏的初始状态
    private void initState() {
        // 初始化 SharedPreferences
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mSharedPrefsEditor = mSharedPrefs.edit();
//        State.enableSoundEffects(true);
//        State.setDifficult(2);
        State.enableSoundEffects(mSharedPrefs.getBoolean(Consts.KEY_SOUND_EFFECT, true));
        State.setDifficult(mSharedPrefs.getInt(Consts.KEY_DIFFICULTY, 2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeGame();
    }

    private void resumeGame() {
        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存最高得分
        if (mNewHighScore) {
            mSharedPrefsEditor.putLong(Consts.KEY_HIGH_SCORE, mHighScore);
            mSharedPrefsEditor.commit();
        }
        pauseGame();
    }

    // 如果用户退出应用程序，则暂停游戏。
    private void pauseGame() {
        State.setGamePaused(true);
        mGameView.onPause();
    }

    /* 沉浸模式 */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (hasFocus) {
                mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                State.setGamePaused(true);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showGameOverDialog(long finalScore, boolean newHighScore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.game_over);
        // 如果玩家获得最新高分，则显示不同的消息
        if (newHighScore) {
            builder.setMessage(getString(R.string.new_high_score) + finalScore + "\n" +
                    getString(R.string.do_you_want_to_restart_the_game));
        } else {
            builder.setMessage(getString(R.string.final_score) + finalScore + "\n" +
                    getString(R.string.do_you_want_to_restart_the_game));
        }

        // 再玩一局
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartGame();
            }
        });

        // 关闭当前页面
        builder.setNegativeButton(R.string.no, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        if (!isFinishing()) builder.show().setCanceledOnTouchOutside(false);
    }

    private void restartGame() {
        recreate();
    }

    private void updateUI() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                // 更新提示信息
                if (State.getGamePaused()) {
                    mReadyTextView.setVisibility(View.VISIBLE);
                } else {
                    mReadyTextView.setVisibility(View.INVISIBLE);
                }

                mScoreTextView.setText(getString(R.string.score) + String.format("%8d", State.getScore()));

                //玩家获得最新高分
                if (State.getScore() > mHighScore) {
                    mHighScore = State.getScore();
                    mNewHighScore = true;
                    mHighScoreTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                }

                mHighScoreTextView.setText(getString(R.string.high_score) + String.format("%8d", mHighScore));
                updateLivesText();

                if (State.getGameOver()) {
                    // 显示用户分数并询问用户是否想再玩一局
                    showGameOverDialog(State.getScore(), mNewHighScore);
                    if (mNewHighScore) {
                        mSharedPrefsEditor.putLong(Consts.KEY_HIGH_SCORE, mHighScore);
                        mSharedPrefsEditor.commit();
                        mSoundPool.play(mSoundIds.get(Consts.K_SOUND_VICTORY), State.getVolume(), State.getVolume(), 0, 0, 1.0f);
                    }

                    // 标记游戏结束
                    mFinish = true;
                }
            }
        });
    }

    // 更新玩家的生命值
    private void updateLivesText() {
        final int lives = State.getLives();
        mLivesTextView.setText("");
        for (int i = 0; i < lives; i++) {
            mLivesTextView.append("♥");
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        showExitDialog();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.game_exit);
        builder.setMessage(R.string.do_you_want_to_exit_the_game);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mFinish = true;
                finish();
            }
        });
        builder.setNegativeButton(R.string.no, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                resumeGame();
            }
        });

        if (!isFinishing()) builder.show().setCanceledOnTouchOutside(false);

        pauseGame();
    }
}