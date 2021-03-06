package com.zuowei.circleimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;

/**
 * 项目名称：CircleImageView
 * 类描述：
 * 创建人：zuowei
 * 创建时间：16-11-14 上午10:35
 * 修改人：zuowei
 * 修改时间：16-11-14 上午10:35
 * 修改备注：
 */
public class CircleImageView extends ImageView {
    float radius;
    Path  circlePath;
    Paint paint;
    Paint layerPaint;
    RectF layerRect;
    PorterDuffXfermode porterDuffXfermode;
    boolean drawableSettled;
    volatile boolean initDone;


    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        init();
    }

    private void init() {
        circlePath = new Path();
        paint = new Paint();
        layerPaint = new Paint();
        layerRect = new RectF();
        porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                removeOnLayoutChangeListener(this);
                drawableSettled = syncDrawable();
                initDone = true;
            }
        });
    }

    private boolean syncDrawable() {

        Drawable drawable = getDrawable();
        if (drawable != null) {
            layerRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
            Rect bounds = drawable.getBounds();
            int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
            int vheight = getHeight() - getPaddingTop() - getPaddingBottom();
            float[] scaleParams = getScaleParams();
            radius = min(vwidth / 2f, vheight / 2f, bounds.centerX() * scaleParams[0], bounds.centerY() * scaleParams[1]);
            if (getScaleType() == ScaleType.MATRIX) {
                setImageMatrix(getNewMatrix(bounds.left, bounds.top, bounds.right, bounds.bottom));//support matrix
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        drawableSettled = syncDrawable();
    }

    private void syncDrawableIf(Drawable drawable) {
        if (initDone || !verifyDrawable(drawable)) {
            drawableSettled = syncDrawable();
        }
    }

    private float[] getScaleParams() {
        Matrix imageMatrix = getImageMatrix();
        float[] martixValues = new float[9];
        imageMatrix.getValues(martixValues);
        return new float[]{martixValues[0],martixValues[4]};
    }

    @Override
    public void setBackground(Drawable background) {
        setImageDrawable(background);
    }

    @Override
    public void setBackgroundColor(int color) {
        setBackground(new ColorDrawable(color));
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        setBackground(background);
    }


    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (!isLayoutRequested()) {
            syncDrawableIf(drawable);
        }
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        syncDrawableIf(null);
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        syncDrawableIf(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawableSettled) {
            canvas.saveLayer(layerRect, layerPaint, Canvas.CLIP_SAVE_FLAG);//clear background
                circlePath.addCircle(layerRect.centerX(), layerRect.centerY(), radius, Path.Direction.CCW);
                canvas.clipPath(circlePath);
                circlePath.reset();
                circlePath.addCircle(layerRect.centerX(), layerRect.centerY(), radius - 1f, Path.Direction.CCW);//Antialiasing
                canvas.save(Canvas.ALL_SAVE_FLAG);
                    paint.setXfermode(porterDuffXfermode);
                    paint.setAntiAlias(true);
                    super.onDraw(canvas);
                    canvas.drawPath(circlePath, paint);
                canvas.restore();
            canvas.restore();
        }else {
            super.onDraw(canvas);
        }
    }

    private Matrix getNewMatrix(int left,int top,int right,int bottom) {
        float scale;
        float dx = 0;
        float dy = 0;
        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();
        int width = right - left;
        int height = bottom - top;

        Matrix matrix = new Matrix();
        matrix.set(null);

        if (width * vheight > vwidth * height) {
            scale = vheight / (float) height;
            dx = (vwidth - width * scale) * 0.5f;
        } else {
            scale = vwidth / (float) height;
            dy = (vheight - height * scale) * 0.5f;
        }

        matrix.setScale(scale, scale);
        matrix.postTranslate((int) (dx + 0.5f) + left, (int) (dy + 0.5f) + top);

        return matrix;
    }

    private float min_r(float... values){
        if (values.length == 1){
            return values[0];
        }
        float [] newValues = Arrays.copyOf(values,values.length - 1);
        newValues[newValues.length - 1] = Math.min(values[values.length - 1],values[values.length - 2]);
        return min_r(newValues);
    }

    private float min(float... values){
        if (values.length <= 0){
            throw new IndexOutOfBoundsException("values is empty");
        }

        float min = values[0];
        for (float value : values){
            min = Math.min(value,min);
        }

        return min;
    }
}
