
package se.kjellstrand.blurrybackgroundviewdemo;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.GridLayout;
import android.widget.ImageView;

public class DemoActivity extends Activity {

    private AtomicBoolean isShowingDetails = new AtomicBoolean(false);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = View.inflate(this, R.layout.activity_demo, null);

        GridLayout gl = (GridLayout) view.findViewById(R.id.grid_layout);

        int[] images = new int[] {
                R.drawable._1, R.drawable._2, R.drawable._14, R.drawable._15, R.drawable._16, R.drawable._17, R.drawable._3,
                R.drawable._4, R.drawable._5, R.drawable._13, R.drawable._14, R.drawable._15, R.drawable._16, R.drawable._6,
                R.drawable._7, R.drawable._8, R.drawable._12, R.drawable._17, R.drawable._18, R.drawable._9, R.drawable._10,
                R.drawable._18, R.drawable._19, R.drawable._1, R.drawable._6, R.drawable._7, R.drawable._10, R.drawable._11,
                R.drawable._19, R.drawable._1, R.drawable._2, R.drawable._3, R.drawable._4, R.drawable._5, R.drawable._10,
                R.drawable._11, R.drawable._12, R.drawable._8, R.drawable._9, R.drawable._5, R.drawable._6, R.drawable._7,
                R.drawable._8, R.drawable._9, R.drawable._13, R.drawable._14, R.drawable._15, R.drawable._16, R.drawable._2,
                R.drawable._3, R.drawable._4, R.drawable._17, R.drawable._11, R.drawable._12, R.drawable._13, R.drawable._18,
                R.drawable._19
        };
        for (final int id : images) {
            View imageView = new ImageView(this);
            imageView.setBackgroundResource(id);
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    BlurredBackgroundLayout bluredBackgroundLayout = (BlurredBackgroundLayout) findViewById(R.id.launch_parent);
                    View detailsView = getLayoutInflater().inflate(R.layout.details_view, bluredBackgroundLayout, false);
                    ((ImageView) detailsView.findViewById(R.id.image_view)).setBackgroundResource(id);
                    bluredBackgroundLayout.setInnerView(DemoActivity.this, detailsView, view);
                    bluredBackgroundLayout.runAnimations(DemoActivity.this, isShowingDetails);
                }
            });
            gl.addView(imageView);
        }

        setContentView(view);

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
