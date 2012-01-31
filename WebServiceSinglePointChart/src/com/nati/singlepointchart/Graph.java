package com.nati.singlepointchart;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

public class Graph extends View
{
   private static final int gradC1 = Color.rgb(254, 254, 254);
   private static final int gradC2 = Color.rgb(154, 154, 154);

   protected Paint graphPaint;
   protected int mValidDataPoints;
   protected int mWidth;
   private int mHeight;
   protected int mXGridSpacing;
   private int mYGridSpacing;
   protected double mTMax;
   protected double mTMin;
   protected double mTRatio;
   protected double mYMax;
   protected double mYMin;
   private String[] mTLabels;
   private String[] mYLabels;
   protected float[] mXGridPoints;
   private float[] mYGridPoints;
   protected float mTextWidth;
   private double mTextHeight;
   protected double[][] mData;
   protected float[][] mScaledData;
   protected Bundle mOldState;

   public static final int[] Colors = { Color.BLUE, Color.GREEN, Color.RED,
         Color.CYAN };

   public Graph(Context context)
   {
      super(context);
      graphPaint = new Paint();
      mValidDataPoints = 0;
   }

   public Graph(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      graphPaint = new Paint();
      mXGridSpacing = 50;
      mYGridSpacing = 50;
      graphPaint.setTextSize(10);
      mTMax = 10;
      mTMin = 0;
      mTRatio = 1;
      mYMax = 1;
      mYMin = -1;
   }

   public void setText(String text)
   {
      requestLayout();
      invalidate();
   }

   public synchronized void setTScale(double min, double max)
   {
      mTMin = min;
      mTMax = max;
      updateGridDrawData();
      postUpdate();
   }

   private void postUpdate()
   {
      // This is called from threads other than the UI thread,
      // to force a re-draw of the graph.
      this.post(new Runnable()
      {
         @Override
         public void run()
         {
            invalidate();
         }
      });
   }

   public synchronized void setData(ArrayList<ArrayList<Double>> dataPoints)
   {
      if (mXGridPoints == null || dataPoints.size() == 0
            || dataPoints.get(0).size() == 0) return;
      if (mData == null || mScaledData == null
            || mData.length != dataPoints.size())
      {
         mData = new double[dataPoints.size()][(int) ((mXGridSpacing * ((mXGridPoints.length / 4) - 1)) / mTRatio)];
         mScaledData = new float[mData.length][mData[0].length];
      }
      // auto scale
      float max = Collections.max(dataPoints.get(0)).floatValue();
      float min = Collections.min(dataPoints.get(0)).floatValue();
      for (ArrayList<Double> list : dataPoints.subList(1, dataPoints.size()))
      {
         float listMax = Collections.max(list).floatValue();
         if (listMax > max) max = listMax;

         float listMin = Collections.min(list).floatValue();
         if (listMin < min) min = listMin;
      }
      // add a fuzz factor so scaling isn't to the edge of the graph
      max += Math.abs(max) * 0.1f;
      min -= Math.abs(min) * 0.1f;
      if (max != mYMax || min != mYMin /* || dataPoints.size() != mTMax */)
      {
         mYMax = max;
         mYMin = min;
         // mTMax = dataPoints.get(0).size();
         updateGridDrawData();
      }
      // insert new data points
      for (int i = 0; i < mData.length; i++)
      {
         for (int j = 0; j < mData[0].length; j++)
         {
            // Interpolate data into mData
            double fractionalIndex = (j / (double) (mData[0].length - 1))
                  * (dataPoints.get(i).size() - 1);
            int left = (int) Math.floor(fractionalIndex);
            int right = (int) Math.ceil(fractionalIndex);
            double fraction = fractionalIndex - left;
            double slope = 0;
            if (right != left)
               slope = dataPoints.get(i).get(right)
                     - dataPoints.get(i).get(left) / (right - left);
            mData[i][j] = (float) (dataPoints.get(i).get(left) + slope
                  * fraction);
            scaleData(i, j);
         }
      }
      mValidDataPoints = mData[0].length;
      postUpdate();
   }

   public double[][] getData()
   {
      double[][] temp = new double[mData.length][mValidDataPoints];
      for (int i = 0; i < temp.length; i++)
         for (int j = 0; j < temp[0].length; j++)
            temp[i][j] = mData[i][j];
      return temp;
   }

   public void clearData()
   {
      mValidDataPoints = 0;
      mTMax -= mTMin;
      mTMin = 0;
      if (mData != null && mData[0] != null && mScaledData != null
            && mScaledData[0] != null)
      {
         mData = new double[mData.length][mData[0].length];
         mScaledData = new float[mData.length][mData[0].length];
      }
      postUpdate();
   }

   public void saveState(Bundle state)
   {
      state.putSerializable("graph.mData", mData);
      state.putInt("graph.mValidDataPoints", mValidDataPoints);
      state.putDouble("graph.mTMax", mTMax);
      state.putDouble("graph.mTMin", mTMin);
      state.putDouble("graph.mYMax", mYMax);
      state.putDouble("graph.mYMin", mYMin);
   }

   public void restoreState(Bundle state)
   {
      // We can't put in new data until onMeasure, so just save the bundle.
      mOldState = state;
   }

   protected void scaleData(int i, int j)
   {
      // scale data to screen coords
      double top = mHeight - mTextHeight - mYGridSpacing
            * ((mYGridPoints.length / 4) - 1);
      double bottom = mHeight - mTextHeight;
      mScaledData[i][j] = (float) (bottom - (1 - (mYMax - mData[i][j])
            / (mYMax - mYMin))
            * (bottom - top));
   }

   protected void updateGridDrawData()
   {
      if (mYLabels == null || mTLabels == null || mXGridPoints == null
            || mYGridPoints == null) return;
      graphPaint.setTextSize(10.0f);
      // Y labels
      for (int i = 0; i < mYGridPoints.length / 4; i++)
      {
         // mLabelBuffer.delete(0, mLabelBuffer.length());
         Double f = (mYMin + (mYMax - mYMin) * i
               / (mYGridPoints.length / 4 - 1));
         // float y = mHeight - textHeight - mGridSpacing * (i+1) -
         // (graphPaint.ascent() / 2);
         // mYLabels[i] = String.format("%3.3f", f);
         // mLabelFormatter.format("%3.3f", f);
         String label = f.toString();
         mYLabels[i] = label.substring(0, Math.min(label.length(), 5));
      }
      // X labels
      for (int i = 0; i < mXGridPoints.length / 4; i++)
      {
         // mLabelBuffer.delete(0, mLabelBuffer.length());
         Double f = (mTMin + (mTMax - mTMin) * i
               / (mXGridPoints.length / 4 - 1));
         // float x = mGridSpacing * (i+1) - (mGridXOffset % mGridSpacing) +
         // mLeftTextWidth;
         // mLabelFormatter.format("%3.3f", f);
         String label = f.toString();
         mTLabels[i] = label.substring(0, Math.min(label.length(), 5));
      }
      // Grid lines
      for (int i = 0, j = 0; i < (mXGridPoints.length); i += 4, j++)
      {
         mXGridPoints[i] = mXGridSpacing * j + mTextWidth;
         mXGridPoints[i + 1] = (float) (mHeight - getPaddingBottom() - mTextHeight);
         mXGridPoints[i + 2] = mXGridPoints[i];
         mXGridPoints[i + 3] = (float) (mHeight - mTextHeight - mYGridSpacing
               * ((mYGridPoints.length / 4) - 1));
      }
      for (int i = 0, j = 0; i < (mYGridPoints.length); i += 4, j++)
      {
         mYGridPoints[i] = getPaddingLeft() + mTextWidth;
         mYGridPoints[i + 1] = (float) (mHeight - mTextHeight - mYGridSpacing
               * j);
         mYGridPoints[i + 2] = getPaddingLeft() + mTextWidth + mXGridSpacing
               * ((mXGridPoints.length / 4) - 1);
         mYGridPoints[i + 3] = mYGridPoints[i + 1];
      }
      // Check arrays
      // if (mData == null)
      // mData = new float[(int) ((mWidth - mTextWidth) / mTRatio)];
      // if (mScaledData == null)
      // mScaledData = new float[mData.length][mData[0].length];
   }

   private void allocateData()
   {
      graphPaint.setTextSize(10.0f);
      // Get text height/width
      String measureString = "12345";
      float[] widths = new float[measureString.length()];
      graphPaint.getTextWidths(measureString, widths);
      mTextWidth = 0;
      for (float w : widths)
      {
         mTextWidth += w;
      }
      mTextHeight = graphPaint.getTextSize();
      // Figure out grid spacing based on constraining dimension
      mXGridSpacing = (int) (1.5 * mTextWidth);
      mYGridSpacing = (int) (2 * mTextHeight);
      // some grid lines
      int numXLines = (int) ((mWidth - mTextWidth * 1) / mXGridSpacing) + 1;
      int numYLines = (int) ((mHeight - mTextHeight * 2) / mYGridSpacing) + 1;
      // Y labels
      mYLabels = new String[numYLines];
      // T labels
      mTLabels = new String[numXLines];
      // Grid lines
      mXGridPoints = new float[numXLines * 4];
      mYGridPoints = new float[numYLines * 4];
      // Check arrays for old data
      updateFromOldState();
   }

   protected void updateFromOldState()
   {
      if (mOldState == null) return;

      double[][] oldData = null;
      int oldValidDataPoints = 0;
      double oldTMax = 0, oldTMin = 0, oldYMax = 0, oldYMin = 0;

      oldData = (double[][]) mOldState.getSerializable("graph.mData");
      oldValidDataPoints = mOldState.getInt("graph.mValidDataPoints");
      oldTMin = mOldState.getDouble("graph.mTMin");
      oldTMax = mOldState.getDouble("graph.mTMax");
      oldYMax = mOldState.getDouble("graph.mYMax");
      oldYMin = mOldState.getDouble("graph.mYMin");
      mOldState = null;

      if (oldValidDataPoints != 0 && oldData != null && oldData[0] != null)
      {
         mData = new double[oldData.length][(int) ((mXGridSpacing * ((mXGridPoints.length / 4) - 1)) / mTRatio)];
         mScaledData = new float[mData.length][mData[0].length];

         mTMin = oldTMin;
         mTMax = oldTMax;// oldTMin + mData[0].length;
         mYMax = oldYMax;
         mYMin = oldYMin;
         mValidDataPoints = Math.min(oldValidDataPoints, mData[0].length);

         for (int i = 0; i < oldData.length; i++)
            for (int j = 0; j < mValidDataPoints; j++)
            {
               mData[i][j] = oldData[i][j];
               scaleData(i, j);
            }
      }
   }

   @Override
   public synchronized void onDraw(Canvas canvas)
   {
      super.onDraw(canvas);
      graphPaint.setColor(Color.WHITE);
      graphPaint.setStyle(Style.FILL);
      graphPaint.setDither(true);
      graphPaint.setShader(new LinearGradient(getPaddingLeft(),
            getPaddingTop(), getPaddingLeft(), mHeight, gradC1, gradC2,
            TileMode.CLAMP));
      // draw the background
      canvas.drawRect(getPaddingLeft(), getPaddingTop(), mWidth, mHeight,
            graphPaint);
      graphPaint.setShader(null);
      graphPaint.setStrokeWidth(5);
      graphPaint.setColor(Color.BLUE);
      // Y labels
      graphPaint.setColor(Color.DKGRAY);
      graphPaint.setTextSize(10.0f);
      for (int i = 0; i < mYLabels.length; i++)
      {
         double y = mHeight - mTextHeight - mYGridSpacing * (i)
               - (graphPaint.ascent() / 2);
         canvas.drawText(mYLabels[i], getPaddingLeft(), (float) y, graphPaint);
      }
      // X labels
      for (int i = 0; i < mTLabels.length; i++)
      {
         double x = mXGridSpacing * (i) + mTextWidth;
         canvas.drawText(mTLabels[i], (float) (x - mTextWidth / 2), mHeight,
               graphPaint);
      }
      graphPaint.setColor(Color.BLUE);
      graphPaint.setStrokeWidth(0);
      canvas.drawLines(mXGridPoints, graphPaint);
      canvas.drawLines(mYGridPoints, graphPaint);
      // Finally, the data lines
      if (mScaledData == null) return;
      graphPaint.setStrokeWidth(4);
      graphPaint.setStyle(Style.FILL_AND_STROKE);
      for (int i = 0; i < mData.length; i++)
      {
         graphPaint.setColor(Colors[i % Colors.length]);
         float x = mTextWidth;
         for (int j = 0; j < mValidDataPoints - 1; j++)
         {
            canvas.drawLine(x, mScaledData[i][j], x + (float) mTRatio,
                  mScaledData[i][j + 1], graphPaint);
            x += mTRatio;
         }
      }
   }

   @Override
   protected synchronized void onMeasure(int widthMeasureSpec,
         int heightMeasureSpec)
   {
      // come up with our width and height
      // int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
      int widthSize = MeasureSpec.getSize(widthMeasureSpec);
      // int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
      int heightSize = MeasureSpec.getSize(heightMeasureSpec);

      mWidth = widthSize;
      mHeight = heightSize;
      setMeasuredDimension(mWidth, mHeight);
      allocateData();
      updateGridDrawData();
   }

}
