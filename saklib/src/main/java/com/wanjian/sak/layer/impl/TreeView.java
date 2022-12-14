package com.wanjian.sak.layer.impl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wanjian.sak.R;
import com.wanjian.sak.converter.ISizeConverter;
import com.wanjian.sak.layer.Layer;
import com.wanjian.sak.layer.impl_tmp.InfoLayer;
import com.wanjian.sak.utils.ScreenUtils;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


/**
 * Created by wanjian on 2016/10/24.
 */

public class TreeView extends Layer {
  private Paint mPaint;
  private int mTabW;
  private int mTxtH;
  private int mCurLayer;
  private Matrix mMatrix;
  private Matrix mDrawMatrix;
  private int[] mLocation = new int[2];
  private float mLastX;
  private float mLastY;
  private int mMode = 0;
  private float mLastDist;
  private boolean mIsScale;

  @Override
  protected void onAttach(View rootView) {
    super.onAttach(rootView);
    init();
    invalidate();
  }

  private int convertSize(int leng) {
    return (int) getSizeConverter().convert(getContext(), leng).getLength();
  }

  private ISizeConverter getSizeConverter() {
    return ISizeConverter.CONVERTER;
  }

  private void init() {
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setColor(Color.WHITE);
    mPaint.setTextSize(ScreenUtils.dp2px(getContext(), 12));
    mTabW = ScreenUtils.dp2px(getContext(), 20);
    Rect rect = new Rect();
    mPaint.getTextBounds("Aj", 0, 2, rect);
    mTxtH = rect.height() * 2;
//        setBackgroundColor(0x88000000);
    mDrawMatrix = new Matrix();
  }

    @Override
    protected void onAfterTraversal(View rootView) {
        super.onAfterTraversal(rootView);
        invalidate();
    }

    @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(0x88000000);
    canvas.setMatrix(mDrawMatrix);
    mCurLayer = -1;
    layerCount(canvas, getRootView());
  }

  private void layerCount(Canvas canvas, View view) {
//    if (view == null || view instanceof RootContainerView || getViewFilter().filter(view) == false) {
//      return;
//    }
    mCurLayer++;
    drawLayer(canvas, view);
    if (view instanceof ViewGroup) {
      ViewGroup vg = ((ViewGroup) view);
      for (int i = 0; i < vg.getChildCount(); i++) {
        View child = vg.getChildAt(i);
        layerCount(canvas, child);
      }
    }
    mCurLayer--;
  }

  private void drawLayer(Canvas canvas, View view) {
    canvas.translate(0, mTxtH);
    canvas.save();
    if (view.getVisibility() != VISIBLE) {
      mPaint.setColor(0xFFAAAAAA);
    } else {
      mPaint.setColor(Color.WHITE);
    }
    for (int i = 0; i < mCurLayer; i++) {
      canvas.translate(mTabW, 0);
      canvas.drawText("|", 0, 0, mPaint);
    }
    canvas.translate(mTabW, 0);
    String txt = getInfo(view);
    canvas.drawText(txt, 0, 0, mPaint);
    if (view instanceof ImageView) {
      float len = mPaint.measureText(txt);
      canvas.translate(len, 0);
      drawBitmap(canvas, ((ImageView) view));
      canvas.translate(-len, 0);
    }
    canvas.restore();
  }

  private void drawBitmap(Canvas canvas, ImageView view) {
    if (view == null) {
      return;
    }
    Drawable drawable = view.getDrawable();
    if (drawable instanceof BitmapDrawable) {
      Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
      if (bmp != null && !bmp.isRecycled()) {
        if (mMatrix == null) {
          mMatrix = new Matrix();
        } else {
          mMatrix.reset();
        }
        float scale = mTxtH * 1.0f / bmp.getHeight();
        float w = scale * bmp.getWidth();
        canvas.drawText(" -bmp(w:" + convertSize(bmp.getWidth()) + " h:" + convertSize(bmp.getHeight()) + ")", w, 0, mPaint);
        mMatrix.setScale(scale, scale);
        canvas.translate(0, -mTxtH >> 1);
        canvas.drawBitmap(bmp, mMatrix, mPaint);
        canvas.translate(0, mTxtH >> 1);
      }
    }

  }

  protected String getInfo(View view) {
    StringBuilder sb = new StringBuilder(100);
    sb.append(view.getClass().getName()).append(" ");

    sb.append("-(w:").append(convertSize(view.getWidth()));
    sb.append(" h:").append(convertSize(view.getHeight())).append(") ");

    view.getLocationOnScreen(mLocation);
    sb.append(" -loc(x:")
        .append(convertSize(mLocation[0]))
        .append("-")
        .append(convertSize(mLocation[0] + view.getWidth()))
        .append(" y:")
        .append(convertSize(mLocation[1]))
        .append("-")
        .append(convertSize(mLocation[1] + view.getHeight()))
        .append(")");

    sb.append(" -P(l:").append(convertSize(view.getPaddingLeft())).append(" t:").append(convertSize(view.getPaddingTop()))
        .append(" r:").append(convertSize(view.getPaddingRight())).append(" b:").append(convertSize(view.getPaddingBottom())).append(")");

    ViewGroup.LayoutParams param = view.getLayoutParams();
    if (param instanceof ViewGroup.MarginLayoutParams) {
      ViewGroup.MarginLayoutParams marginLayoutParams = ((ViewGroup.MarginLayoutParams) param);
      sb.append(" -M(l:").append(convertSize(marginLayoutParams.leftMargin)).append(" t:").append(convertSize(marginLayoutParams.topMargin))
          .append(" r:").append(convertSize(marginLayoutParams.rightMargin)).append(" b:").append(convertSize(marginLayoutParams.bottomMargin)).append(")");

    }
    int visible = view.getVisibility();
    String visStr = "";
    if (visible == VISIBLE) {
      visStr = "visible";
    } else if (visible == INVISIBLE) {
      visStr = "invisible";
    } else if (visible == GONE) {
      visStr = "gone";
    }

    if (view instanceof TextView) {
      String txt = ((TextView) view).getText().toString();
      sb.append(" -txt:").append(txt);
    }
    sb.append(" -visible:").append(visStr).append(" ");
    sb.append(" -extra:").append(view.getTag(InfoLayer.INFO_KEY));

    return sb.toString();
  }

  @Override
  protected boolean onBeforeInputEvent(View rootView, InputEvent event) {
    if (event instanceof MotionEvent) {
      onTouchEvent((MotionEvent) event);
      return true;
    }
    return false;
  }

  private boolean onTouchEvent(MotionEvent event) {

    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        mLastX = event.getX();
        mLastY = event.getY();
        mIsScale = false;
        mMode = 1;
        break;
      case MotionEvent.ACTION_UP:
        mMode = 0;
        break;
      case MotionEvent.ACTION_POINTER_UP:
        mMode -= 1;
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        mLastDist = spacing(event);
        mMode += 1;
        break;

      case MotionEvent.ACTION_MOVE:
        if (mMode >= 2) {
          float newDist = spacing(event);
          float factor = newDist / mLastDist;
          mDrawMatrix.postScale(factor, factor);
          mLastDist = newDist;
          mIsScale = true;
        } else {
          if (mIsScale) {
            break;
          }
          float curX = event.getX();
          float curY = event.getY();
          mDrawMatrix.postTranslate(curX - mLastX, curY - mLastY);
          mLastX = curX;
          mLastY = curY;
        }
        invalidate();
        break;
    }

    return true;
  }


  private float spacing(MotionEvent event) {
    float x = event.getX(0) - event.getX(1);
    float y = event.getY(0) - event.getY(1);
    return (float) Math.sqrt(x * x + y * y);
  }


  protected int px2dp(float px) {
    final float scale = getContext().getResources().getDisplayMetrics().density;
    return (int) (px / scale + 0.5f);
  }


}
