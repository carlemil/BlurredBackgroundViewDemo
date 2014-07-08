
package se.kjellstrand.blurrybackgroundviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class RenderscriptHelper {
    private static final String LOG_TAG = RenderscriptHelper.class.getCanonicalName();

    private static RenderScript rs = null;

    private static Allocation blurInputAllocation = null;

    private static Allocation blurOutputAllocation = null;

    private static ScriptIntrinsicBlur blurScript = null;

    private static int currentBlurBitmapHeight = -1;

    private static int currentBlurBitmapWidth = -1;

    public static void init(Context context, Bitmap inputBitmap) {
        if (inputBitmap.getHeight() != currentBlurBitmapHeight
                || inputBitmap.getWidth() != currentBlurBitmapWidth) {
            Log.d(LOG_TAG,
                    "Initializing renderscript, should not be seen often in the log since its an expensive operation.");
            if (rs != null) {
                rs.destroy();
                blurInputAllocation.destroy();
                blurOutputAllocation.destroy();
            }
            rs = RenderScript.create(context);
            blurInputAllocation = Allocation.createFromBitmap(rs, inputBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            blurOutputAllocation = Allocation.createTyped(rs, blurInputAllocation.getType());
            blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

            currentBlurBitmapHeight = inputBitmap.getHeight();
            currentBlurBitmapWidth = inputBitmap.getWidth();
        }
    }

    public static void run(Bitmap outputBitmap, Bitmap inputBitmap, float blurStrength) {
        blurInputAllocation.copyFrom(inputBitmap);
        blurScript.setRadius((blurStrength == 0) ? 0.0001f : blurStrength);
        blurScript.setInput(blurInputAllocation);
        blurScript.forEach(blurOutputAllocation);
        blurOutputAllocation.copyTo(outputBitmap);
    }
}
