
package se.kjellstrand.blurrybackgroundviewdemo;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class DemoActivity extends Activity {

    private AtomicBoolean isShowingDetails = new AtomicBoolean(false);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = View.inflate(this, R.layout.activity_demo, null);
        setContentView(view);

        findViewById(R.id.imageView1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                BlurredBackgroundLayout bluredBackgroundLayout =
                        (BlurredBackgroundLayout) findViewById(R.id.launch_parent);
                View detailsView = getLayoutInflater().inflate(R.layout.details_view,
                        bluredBackgroundLayout, false);
                bluredBackgroundLayout.setInnerView(DemoActivity.this, detailsView, view);
                bluredBackgroundLayout.runAnimations(DemoActivity.this, isShowingDetails);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isShowingDetails.get()) {
            BlurredBackgroundLayout v = (BlurredBackgroundLayout) findViewById(R.id.launch_parent);
            v.forceCloseDetailsView();
        } else {
            super.onBackPressed();
        }
    }
}
