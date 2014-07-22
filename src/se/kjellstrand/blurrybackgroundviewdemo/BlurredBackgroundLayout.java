
package se.kjellstrand.blurrybackgroundviewdemo;

import java.util.concurrent.atomic.AtomicBoolean;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * A layout that can blur and darker / zoom in its background and show a
 * details-view on-top of the blurred background. The blur animation is
 * triggered after runAnimations is called and a
 * ViewTreeObserver.OnPreDrawListener have had its preDraw method invoked. While
 * the views are animating onClick is disabled to avoid strange behaviors of the
 * views.
 */
public class BlurredBackgroundLayout extends RelativeLayout {

    // Time for animations to run when showing/hiding the details view.
    private static final long IN_OUT_ANIMATION_DURATION = 200;

    // Time for fade-out at the end of the hide animation.
    private static final long FADE_OUT_ANIMATION_DURATION = 100;

    // The blur radius used on the background at the end of animating in the
    // details view.
    private static final float BACKGROUND_MAX_BLUR_RADIUS = 9f;

    // Fraction that the background will move north/up while animating in.
    private static final float BACKGROUND_MAX_Y_TRANS_FRACTION = 0.10f;

    // Fraction that the background will scale down / move away from the user.
    private static final float BACKGROUND_MIN_SCALE = 0.95f;

    // Fraction that the background will fade towards black while animating in.
    private static final float BACKGROUND_DARKEN_BY_FRACTION = 0.6f;

    // Factor that we scale down the background before applying blur and other
    // effects, the lower this value is the faster the effects can be applied.
    private static final float BACKGROUND_SCALE_DOWN_FACTOR = 0.25f;

    // Fraction to fade the details view from and to.
    private static final float FOREGROUND_FADE_FRACTION = 0.1f;

    // AtomicBoolean that indicates if the details view is visible or not.
    private AtomicBoolean isShowingDetails;

    // The details view.
    private View detailsView = null;

    // The origins of the details view / the view clicked to show the details
    // view. Used to get start coordinates for the in animation.
    private View detailsViewOrigin;

    // The imageview that holds the blurred background.
    private ImageView backgroundImageView = null;

    // A temporary bitmap used to hold pixels in between image transformations.
    private Bitmap tmpBitmap;

    public BlurredBackgroundLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BlurredBackgroundLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurredBackgroundLayout(Context context) {
        super(context);
    }

    /**
     * Set the details view view.
     *
     * @param activity The activity showing the details view.
     * @param detailsView The details view.
     * @param detailsViewOrigin The origin view of the details view.
     */
    public void setInnerView(Activity activity, View detailsView, View detailsViewOrigin) {
        this.detailsView = detailsView;
        this.detailsViewOrigin = detailsViewOrigin;
        final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        root.addView(detailsView);
    }

    /**
     * Starts the in animation of the details view and the in animation of the
     * background.
     *
     * @param activity The activity showing the details view.
     * @param isShowingDetails AtomicBoolean that indicates if the details view
     *            is visible or not.
     */
    public void runAnimations(final Activity activity, AtomicBoolean isShowingDetails) {

        this.setVisibility(View.VISIBLE);

        this.isShowingDetails = isShowingDetails;

        // activity.getActionBar().hide();

        backgroundImageView = new ImageView(activity);

        // Retrieve the apps content view, will be used to attach the blurred
        // background and the details view to.
        final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);

        // Render the visible views to a bitmap to be used for blurring the
        // background while animating.
        Config config = Bitmap.Config.ARGB_8888;
        final Bitmap inputBitmap = Bitmap.createBitmap((int) (root.getWidth() * BACKGROUND_SCALE_DOWN_FACTOR),
                (int) (root.getHeight() * BACKGROUND_SCALE_DOWN_FACTOR), config);
        Canvas canvas = new Canvas(inputBitmap);
        Matrix matrix = new Matrix();
        matrix.setScale(BACKGROUND_SCALE_DOWN_FACTOR, BACKGROUND_SCALE_DOWN_FACTOR);
        canvas.drawColor(0xff000000);
        canvas.setMatrix(matrix);
        draw(canvas);

        root.addView(backgroundImageView);

        detailsView.bringToFront();

        final Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        final int[] viewCoords = new int[2];

        final Rect detailsViewStartPos = new Rect();

        final Rect detailsViewEndPos = new Rect();

        // Wait for all the views to be measured so that we can start the
        // details view animations from the correct location and move the view
        // to the correct destination.
        ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                BlurredBackgroundLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);

                // Measure the origin of the details view.
                detailsViewOrigin.getLocationOnScreen(viewCoords);
                detailsViewStartPos.left = viewCoords[0];
                detailsViewStartPos.top = (int) (viewCoords[1] - root.getY());
                detailsViewStartPos.right = detailsViewStartPos.left + detailsViewOrigin.getWidth();
                detailsViewStartPos.bottom = (int) (detailsViewStartPos.top + detailsViewOrigin.getHeight() - root.getY());

                // Center the details view.
                detailsView.setX(root.getWidth() / 2 - detailsView.getWidth() / 2);
                detailsView.setY(root.getHeight() / 2 - detailsView.getHeight() / 2 - root.getY());

                // Measure the destination of the details view.
                detailsView.getLocationOnScreen(viewCoords);
                detailsViewEndPos.left = viewCoords[0];
                detailsViewEndPos.top = viewCoords[1];
                detailsViewEndPos.right = detailsViewEndPos.left + detailsView.getWidth();
                detailsViewEndPos.bottom = detailsViewEndPos.top + detailsView.getHeight();


                // Start the animation that brings in the details-view and
                // fades/blurs the background.
                animateIn(activity, backgroundImageView, root, inputBitmap, outputBitmap, detailsViewStartPos, detailsViewEndPos);

                return true;
            }
        });
    }

    /**
     * A method for closing the details view, used from the activity's
     * onBackPressed method to simulate normal back behavior.
     */
    public void forceCloseDetailsView() {
        if (detailsView != null) {
            detailsView.performClick();
        }
    }

    private void animateIn(final Activity activity, final ImageView bgImageView, final ViewGroup root, final Bitmap inputBitmap,
            final Bitmap outputBitmap, final Rect innerViewStartPos, final Rect innerViewEndPos) {

        // Disable all click listeners while animating.
        setClickListener(root, null);

        // Initialize the temporary bitmap if its null or different in size from
        // the input bitmap.
        if (tmpBitmap == null || tmpBitmap.getHeight() != inputBitmap.getHeight() || tmpBitmap.getWidth() != inputBitmap.getWidth()) {
            tmpBitmap = Bitmap.createBitmap(inputBitmap);
        }

        final Canvas canvas = new Canvas(tmpBitmap);
        final Matrix matrix = new Matrix();

        ValueAnimator blurAnim = ValueAnimator.ofFloat(1, 1);
        AnimatorUpdateListener blurUpdateListener = new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                float scale = ((1 - BACKGROUND_MIN_SCALE) * (1 - va.getAnimatedFraction()) + BACKGROUND_MIN_SCALE);
                matrix.setScale(scale, scale, tmpBitmap.getWidth() / 2, tmpBitmap.getHeight() / 2);
                canvas.drawColor(0xff000000);
                canvas.setMatrix(matrix);
                canvas.drawBitmap(inputBitmap, matrix, null);

                RenderscriptHelper.init(activity, tmpBitmap);
                RenderscriptHelper.run(outputBitmap, tmpBitmap, //
                        BACKGROUND_MAX_BLUR_RADIUS * va.getAnimatedFraction());
                bgImageView.setImageBitmap(outputBitmap);
            }
        };
        blurAnim.addUpdateListener(blurUpdateListener);

        AnimatorListener animatorListener = new AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator arg0) {
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                OnClickListener clickLisstener = new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        animateOut(activity, backgroundImageView, root, inputBitmap, outputBitmap, innerViewStartPos,
                                innerViewEndPos);
                    }
                };
                // Enable click listeners as the in animation is finished.
                setClickListener(root, clickLisstener);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
            }

            @Override
            public void onAnimationStart(Animator arg0) {
            }
        };
        blurAnim.addListener(animatorListener);

        detailsView.setPivotX(0);
        detailsView.setPivotY(0);

        AnimatorSet animSet = new AnimatorSet();

        float yTranslate = -getHeight() * BACKGROUND_MAX_Y_TRANS_FRACTION;
        float scale = (float) innerViewStartPos.width() / innerViewEndPos.width();
        animSet.setDuration(IN_OUT_ANIMATION_DURATION);
        // Syncronize the animations
        animSet.playTogether(
                getBgAnimatorSet(blurAnim, bgImageView, 0, yTranslate, 1f, BACKGROUND_DARKEN_BY_FRACTION),//
                getFgAnimatorSet(FOREGROUND_FADE_FRACTION, 1f, scale, 1f, //
                        innerViewStartPos.left, innerViewEndPos.left,//
                        innerViewStartPos.top, innerViewEndPos.top));

        // Play the animations.
        animSet.start();
    }

    private void animateOut(final Activity activity, final ImageView bgImageView, final ViewGroup root, final Bitmap inputBitmap,
            final Bitmap outputBitmap, Rect innerViewStartPos, Rect innerViewEndPos) {

        // Prevent multiple clicks causing multiple animateOut calls resulting
        // in broken animations
        setClickListener(root, null);

        final Canvas canvas = new Canvas(tmpBitmap);
        final Matrix matrix = new Matrix();

        ValueAnimator blurAnim = ValueAnimator.ofFloat(1, 1);
        AnimatorUpdateListener updateListener = new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                float scale = ((1 - BACKGROUND_MIN_SCALE) * (va.getAnimatedFraction()) + BACKGROUND_MIN_SCALE);
                matrix.setScale(scale, scale, tmpBitmap.getWidth() / 2, tmpBitmap.getHeight() / 2);
                canvas.drawColor(0xff000000);
                canvas.setMatrix(matrix);
                canvas.drawBitmap(inputBitmap, matrix, null);

                RenderscriptHelper.init(activity, tmpBitmap);
                RenderscriptHelper.run(outputBitmap, tmpBitmap, //
                        BACKGROUND_MAX_BLUR_RADIUS * (1 - va.getAnimatedFraction()));
                bgImageView.setImageBitmap(outputBitmap);
            }
        };
        blurAnim.addUpdateListener(updateListener);

        AnimatorSet fgAndBgAnimSet = new AnimatorSet();
        float yTranslate = -getHeight() * BACKGROUND_MAX_Y_TRANS_FRACTION;
        float scale = (float) innerViewStartPos.width() / innerViewEndPos.width();
        fgAndBgAnimSet.setDuration(IN_OUT_ANIMATION_DURATION);
        fgAndBgAnimSet.playTogether(
                getBgAnimatorSet(blurAnim, bgImageView, yTranslate, 0, BACKGROUND_DARKEN_BY_FRACTION, 1f), //
                getFgAnimatorSet(1f, FOREGROUND_FADE_FRACTION, 1f, scale, innerViewEndPos.left, innerViewStartPos.left,
                        innerViewEndPos.top, innerViewStartPos.top));

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(fgAndBgAnimSet, getFadeOutAnimatorSet(bgImageView));
        animSet.addListener(getFinishAnimatorListener(activity, bgImageView, root));
        animSet.start();
    }

    private AnimatorSet getFadeOutAnimatorSet(final ImageView bgImageView) {
        AnimatorSet fadeOutAnim = new AnimatorSet();
        fadeOutAnim.setDuration(FADE_OUT_ANIMATION_DURATION);
        fadeOutAnim.setStartDelay(IN_OUT_ANIMATION_DURATION - FADE_OUT_ANIMATION_DURATION);
        fadeOutAnim.playTogether(ObjectAnimator.ofObject(bgImageView, "alpha", new FloatEvaluator(), 1f, 0f));
        return fadeOutAnim;
    }

    private AnimatorSet getFgAnimatorSet(float alphaStart, float alphaEnd, float scaleStart, float scaleEnd, float xStart,
            float xEnd, float yStart, float yEnd) {
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(ObjectAnimator.ofObject(detailsView, "alpha", new FloatEvaluator(), alphaStart, alphaEnd),//
                ObjectAnimator.ofObject(detailsView, "x", new FloatEvaluator(), xStart, xEnd),//
                ObjectAnimator.ofObject(detailsView, "y", new FloatEvaluator(), yStart, yEnd),//
                ObjectAnimator.ofObject(detailsView, "scaleX", new FloatEvaluator(), scaleStart, scaleEnd),//
                ObjectAnimator.ofObject(detailsView, "scaleY", new FloatEvaluator(), scaleStart, scaleEnd));
        return animSet;
    }

    private AnimatorSet getBgAnimatorSet(Animator blurAnim, final ImageView bgImageView, float startTransY, float endTransY,
            float startDarken, float endDarken) {

        ValueAnimator darkenAnim = ValueAnimator.ofFloat(startDarken, endDarken);
        AnimatorUpdateListener darkenUpdateListener = new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                float fraction = (Float) va.getAnimatedValue();
                byte b = (byte) (fraction * 255);
                int color = b + (b << 8) + (b << 16) + (0xff << 24);
                bgImageView.setColorFilter(color, Mode.MULTIPLY);
            }
        };
        darkenAnim.addUpdateListener(darkenUpdateListener);

        // Get the offset from the top of the screen to the blurSourceView.
        // int[] location = new int[2];
        // getLocationOnScreen(location);
        // final float backgroundInitialY = 0;//location[1];

        AnimatorSet animSet = new AnimatorSet();
        // if (backgroundInitialY != 0) {
        // animSet.playTogether(blurAnim, darkenAnim, //
        // ObjectAnimator.ofObject(bgImageView, "translationY", new
        // FloatEvaluator(),
        // backgroundInitialY + startTransY, backgroundInitialY + endTransY));
        // } else {
        animSet.playTogether(blurAnim, darkenAnim);
        // }
        return animSet;
    }

    private AnimatorListener getFinishAnimatorListener(final Activity activity, final ImageView backgroundImageView,
            final ViewGroup root) {
        return new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator arg0) {
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                root.removeView(backgroundImageView);
                root.removeView(detailsView);
                backgroundImageView.setAlpha(1f);
                // activity.getActionBar().show();
                isShowingDetails.set(false);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
            }
        };
    }

    private void setClickListener(final ViewGroup root, final View.OnClickListener clickListener) {
        detailsView.setOnClickListener(clickListener);
        root.setOnClickListener(clickListener);
        backgroundImageView.setOnClickListener(clickListener);
    }

}
