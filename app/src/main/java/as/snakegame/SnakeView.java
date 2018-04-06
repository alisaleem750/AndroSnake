package as.snakegame;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;

public class SnakeView extends SurfaceView implements Runnable {

    Intent mIntent;

    // All the code will run separately to the UI
    private Thread mThread = null;

    // This variable determines when the game is playing
    // It is declared as volatile because
    // it can be accessed from inside and outside the thread
    private volatile boolean mPlaying;

    // This is what we draw on
    private Canvas mCanvas;

    // This is required by the Canvas class to do the drawing
    private SurfaceHolder mHolder;

    // This lets us control colors etc
    private Paint mMPaintSnake;
    private Paint mMPaintSnakeHead;
    private Paint mMPaintMouse;

    // This will be a reference to the Activity
    private Context mContext;

    // Sound
    private SoundPool mSoundPool;
    private int mGetMouseSound = -1;
    private int mDeadSound = -1;

    // For tracking movement mDirection
    public enum Direction {UP, RIGHT, DOWN, LEFT}

    // Start by heading to the right
    private Direction mDirection = Direction.RIGHT;

    // What is the screen resolution
    private int mScreenWidth;
    private int mScreenHeight;

    // Control pausing between updates
    private long mNextFrameTime;

    // Update the game 10 times per second
    private final long FPS = 10;

    // There are 1000 milliseconds in a second
    private final long MILLIS_IN_A_SECOND = 1000;
    // We will draw the frame much more often

    // The current mScore
    private int mScore;

    // The location in the grid of all the segments
    private int[] mSnakeXs;
    private int[] mSnakeYs;

    // How long is the snake at the moment
    private int mSnakeLength;

    // Where is the mouse
    private int mMouseX;
    private int mMouseY;

    // The size in pixels of a snake segment
    private int mBlockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh; // determined dynamically

    public SnakeView(Context context, Point size) {
        super(context);

        mContext = context;

        mScreenWidth = size.x;
        mScreenHeight = size.y;

        //Determine the size of each block/place on the game board
        mBlockSize = mScreenWidth / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = mScreenHeight / mBlockSize;

        // Set the sound up
        loadSound();

        // Initialize the drawing objects
        mHolder = getHolder();
        mMPaintSnake = new Paint();
        mMPaintSnakeHead = new Paint();
        mMPaintMouse = new Paint();

        // If you score 200 you are rewarded with a crash achievement!
        mSnakeXs = new int[200];
        mSnakeYs = new int[200];

        // Start the game
        startGame();
    }

    @Override
    public void run() {
        // The check for mPlaying prevents a crash at the start
        // You could also extend the code to provide a pause feature
        while (mPlaying) {

            // Update 10 times a second
            if(checkForUpdate()) {
                updateGame();
                drawGame();
            }

        }
    }

    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void startGame() {
        // Start with just a head, in the middle of the screen
        mSnakeLength = 1;
        mSnakeXs[0] = NUM_BLOCKS_WIDE / 2;
        mSnakeYs[0] = mNumBlocksHigh / 2;

        // And a mouse to eat
        spawnMouse();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update is triggered immediately
        mNextFrameTime = System.currentTimeMillis();
    }

    public void loadSound() {
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            // Create objects of the 2 required classes
            // Use m_Context because this is a reference to the Activity
            AssetManager assetManager = mContext.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the two sounds in memory
            descriptor = assetManager.openFd("get_mouse_sound.ogg");
            mGetMouseSound = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("death_sound.ogg");
            mDeadSound = mSoundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }
    }

    public void spawnMouse() {
        Random random = new Random();
        mMouseX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        mMouseY = random.nextInt(mNumBlocksHigh - 1) + 1;
    }

    private void eatMouse(){
        //  Got one! Squeak!!
        // Increase the size of the snake
        mSnakeLength++;
        //replace the mouse
        spawnMouse();
        //add to the mScore
        mScore = mScore + 1;
        mSoundPool.play(mGetMouseSound, 1, 1, 0, 0, 1);
    }

    private void moveSnake(){
        // Move the body
        for (int i = mSnakeLength; i > 0; i--) {
            // Start at the back and move it
            // to the position of the segment in front of it
            mSnakeXs[i] = mSnakeXs[i - 1];
            mSnakeYs[i] = mSnakeYs[i - 1];

            // Exclude the head because
            // the head has nothing in front of it
        }

        // Move the head in the appropriate mDirection
        switch (mDirection) {
            case UP:
                mSnakeYs[0]++;
                break;

            case RIGHT:
                mSnakeXs[0]++;
                break;

            case DOWN:
                mSnakeYs[0]--;
                break;

            case LEFT:
                mSnakeXs[0]--;
                break;
        }
    }

    private boolean detectDeath(){
        // Has the snake died?
        boolean dead = false;

        // Hit a wall?
        if (mSnakeXs[0] == -1) dead = true;
        if (mSnakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
        if (mSnakeYs[0] == -1) dead = true;
        if (mSnakeYs[0] == mNumBlocksHigh) dead = true;

        // Eaten itself?
        for (int i = mSnakeLength - 1; i > 0; i--) {
            if ((i > 4) && (mSnakeXs[0] == mSnakeXs[i]) && (mSnakeYs[0] == mSnakeYs[i])) {
                dead = true;
            }
        }

        return dead;
    }

    public void updateGame() {
        // Did the head of the snake touch the mouse?
        if (mSnakeXs[0] == mMouseX && mSnakeYs[0] == mMouseY) {
            eatMouse();
        }

        moveSnake();

        if (detectDeath()) {
            //start again
            mSoundPool.play(mDeadSound, 1, 1, 0, 0, 1);

            endGame();
        }
    }

    private void endGame() {
        mIntent = new Intent(mContext,MenuActivity.class);
        mContext.startActivity(mIntent);
    }

    public void drawGame() {
        // Prepare to draw
        if (mHolder.getSurface().isValid()) {
            mCanvas = mHolder.lockCanvas();

            // Clear the screen with my favorite color
            mCanvas.drawColor(Color.parseColor("#FF6666"));

            // Set the color of the paint to draw the snake and mouse with
            mMPaintSnake.setColor(Color.argb(255, 255, 255, 255));

            // Set the color of the paint to draw the head of the snake with
            mMPaintSnakeHead.setColor(Color.argb(255, 204, 0, 0));

            // Set the color of the paint to draw the mouse with
            mMPaintMouse.setColor(Color.argb(255, 255, 255, 51));

            // Choose how big the score will be
            mMPaintSnake.setTextSize(30);
            mMPaintSnakeHead.setTextSize(30);
            mMPaintMouse.setTextSize(30);
            mCanvas.drawText("Score:" + mScore, 10, 30, mMPaintSnake);

            //Draw the snake
            for (int i = 0; i < mSnakeLength; i++) {
                if (i<1){
                    mCanvas.drawRect(mSnakeXs[i] * mBlockSize,
                            (mSnakeYs[i] * mBlockSize),
                            (mSnakeXs[i] * mBlockSize) + mBlockSize,
                            (mSnakeYs[i] * mBlockSize) + mBlockSize,
                            mMPaintSnakeHead);
                } else {
                    mCanvas.drawRect(mSnakeXs[i] * mBlockSize,
                            (mSnakeYs[i] * mBlockSize),
                            (mSnakeXs[i] * mBlockSize) + mBlockSize,
                            (mSnakeYs[i] * mBlockSize) + mBlockSize,
                            mMPaintSnake);
                }
            }

            //draw the mouse
            mCanvas.drawRect(mMouseX * mBlockSize,
                    (mMouseY * mBlockSize),
                    (mMouseX * mBlockSize) + mBlockSize,
                    (mMouseY * mBlockSize) + mBlockSize,
                    mMPaintMouse);

            // Draw the whole frame
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    public boolean checkForUpdate() {

        // Are we due to update the frame
        if(mNextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime =System.currentTimeMillis() + MILLIS_IN_A_SECOND / FPS;

            // Return true so that the update and draw
            // functions are executed
            return true;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if(mDirection == Direction.RIGHT || mDirection == Direction.LEFT){
                    if(motionEvent.getY() >= mScreenHeight / 2){
                        mDirection = Direction.UP;
                        break;
                    } else {
                        mDirection = Direction.DOWN;
                        break;
                    }
                }
                if(mDirection == Direction.DOWN || mDirection == Direction.UP){
                    if(motionEvent.getX() >= mScreenWidth / 2) {
                        mDirection = Direction.RIGHT;
                        break;
                    } else {
                        mDirection = Direction.LEFT;
                        break;
                    }
                }
        }
        return true;
    }
}
