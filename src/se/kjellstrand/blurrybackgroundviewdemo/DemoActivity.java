
package se.kjellstrand.blurrybackgroundviewdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class DemoActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = View.inflate(this, R.layout.activity_demo, null);
        setContentView(view);

    }
}
