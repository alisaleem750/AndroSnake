package as.snakegame;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

public class SnakeActivity extends Activity {

    Intent mIntent;

    // Declare an instance of SnakeView
    SnakeView snakeView;
    // We will initialize it in onCreate
    // once we have more details about the Player's device

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //find out the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Load the resolution into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a new View based on the SnakeView class
        snakeView = new SnakeView(this, size);

        // Make snakeView the default view of the Activity
        setContentView(snakeView);
    }

    // Start the thread in snakeView when this Activity
    // is shown to the player
    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    // Make sure the thread in snakeView is stopped
    // If this Activity is about to be closed
    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    public void goToMenu(){
        mIntent = new Intent(SnakeActivity.this, MenuActivity.class);
        SnakeActivity.this.startActivity(mIntent);
    }
}