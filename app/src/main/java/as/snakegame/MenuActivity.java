package as.snakegame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends Activity {

    private Button mPlayButton;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mPlayButton = (Button) findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mIntent = new Intent(MenuActivity.this, SnakeActivity.class);
                MenuActivity.this.startActivity(mIntent);
            }
        });
    }
}
