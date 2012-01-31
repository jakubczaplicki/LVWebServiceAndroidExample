package com.nati.singlepointchart;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.util.AttributeSet;

public class Chart extends Graph
{

   public Chart(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   public synchronized void addDataPoint(ArrayList<Double> points)
   {
      if(mXGridPoints == null) return;
      boolean needUpdate = false;
      if (mData == null || mScaledData == null || mData.length != points.size())
      {
         mData = new double[points.size()][(int) ((mXGridSpacing * ((mXGridPoints.length / 4) - 1)) / mTRatio)];
         mScaledData = new float[mData.length][mData[0].length];
         mValidDataPoints = 0;
      }
      if ((mValidDataPoints) >= mData[0].length)
      {
         // start shifting data points
         for (int i = 0; i < mData.length; i++)
            for (int j = 0; j < mData[0].length - 1; j++)
               mData[i][j] = mData[i][j + 1];
         mValidDataPoints = mData[0].length - 1;
         mTMin++;
         mTMax++;
         needUpdate = true;
      }
      for (int i = 0; i < mData.length; i++)
         mData[i][mValidDataPoints] = points.get(i);
      mValidDataPoints++;
      // On the first point, go ahead and do an initial auto scale
      if (mValidDataPoints == 1)
      {
         mYMax = Collections.max(points) + (Collections.max(points) * 0.1f);
         mYMin = Collections.min(points) - (Collections.min(points) * 0.1f);
         mTMax = mData[0].length;
         updateGridDrawData();
      }
      else
      {
         // auto scale if needed
         double min = mData[0][0], max = mData[0][0];
         for (int i = 0; i < mData.length; i++)
            for (int j = 1; j < mValidDataPoints; j++)
            {
               if (mData[i][j] > max) max = mData[i][j];
               if (mData[i][j] < min) min = mData[i][j];
            }
         max += Math.abs(max) * 0.1f;
         min -= Math.abs(min) * 0.1f;
         if (max != mYMax || min != mYMin)
         {
            mYMax = max;
            mYMin = min;
            updateGridDrawData();
            needUpdate = false;
         }
      }
      if (needUpdate) updateGridDrawData();
      for (int i = 0; i < mData.length; i++)
         for (int j = 0; j < mValidDataPoints; j++)
            scaleData(i, j);
      // We need this so other threads can call addDataPoint.
      this.post(new Runnable()
      {
         @Override
         public void run()
         {
            invalidate();
         }
      });
   }
   
   @Override
   protected void updateFromOldState()
   {
      if (mOldState == null) return;

      double[][] oldData = null;
      int oldValidDataPoints = 0;
      double oldTMin = 0, oldYMax = 0, oldYMin = 0;

      oldData = (double[][]) mOldState.getSerializable("graph.mData");
      oldValidDataPoints = mOldState.getInt("graph.mValidDataPoints");
      oldTMin = mOldState.getDouble("graph.mTMin");
      oldYMax = mOldState.getDouble("graph.mYMax");
      oldYMin = mOldState.getDouble("graph.mYMin");
      mOldState = null;

      if (oldValidDataPoints != 0 && oldData != null && oldData[0] != null)
      {
         mData = new double[oldData.length][(int) ((mXGridSpacing * ((mXGridPoints.length / 4) - 1)) / mTRatio)];
         mScaledData = new float[mData.length][mData[0].length];

         int diff = oldValidDataPoints - mData[0].length;
         int start = Math.max(0, diff);
         mTMin = oldTMin + start;
         mTMax = oldTMin + mData[0].length + start;
         mYMax = oldYMax;
         mYMin = oldYMin;

         for (int i = 0; i < oldData.length; i++)
            for (int j = start; j < oldValidDataPoints; j++)
            {
               mData[i][j - start] = oldData[i][j];
               scaleData(i, j - start);
            }
         mValidDataPoints = Math.min(oldValidDataPoints, mData[0].length);
      }
   }
   
}
