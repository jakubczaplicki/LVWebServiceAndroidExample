package com.nati.singlepointchart;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SinglePoint extends Activity implements OnClickListener
{
   /** Called when the activity is first created. */

   private Chart mChart;
   private EditText mURLText;
   private updaterThread mThread;
   private EditText mValue;

   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      mChart = (Chart) findViewById(R.id.DataChart);
      mURLText = (EditText) findViewById(R.id.URLEditText);
      Button updateButton = (Button) findViewById(R.id.UpdateButton);
      mValue =   (EditText) findViewById(R.id.Value);
      mThread = new updaterThread();
      
      if(savedInstanceState != null)
      {
         //restore the graph data and URL if we have them from the last time we ran
         mChart.restoreState(savedInstanceState);
         mURLText.setText(savedInstanceState.getString("url"));
      }

      updateButton.setOnClickListener(this);
   }

   protected void onSaveInstanceState(Bundle outState)
   {
      //Save the graph data and URL text when the activity is destroyed
      super.onSaveInstanceState(outState);
      outState.putString("url", mURLText.getText().toString());
      mChart.saveState(outState);
   }

   public void onClick(View v)
   {
      if (mThread.isAlive())
      {
         mThread.mStop = true;
         try
         {
            mThread.join();
         } catch (InterruptedException e)
         {
         }
      }
      mThread = new updaterThread();
      mThread.start();
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      if (mThread.isAlive())
      {
         mThread.mStop = true;
         try
         {
            mThread.join();
         } catch (InterruptedException e)
         {
         }
      }
   }

   String value = "";
   
   // Instantiating the Handler associated with the main thread.
   private Handler messageHandler = new Handler() {

       @Override
       public void handleMessage(Message msg) {  
           switch(msg.what) {
	        //handle update
	        case 1:
	               mValue.setText(value);
	               mValue.invalidate();
	        break;
           }
       }   
   };

   class updaterThread extends Thread
   {
      public volatile Boolean mStop = false;

      public void run()
      {
         // This is all the same as before, except we will parse the double and
         // add
         // it to a chart.
         try
         {
            while (!mStop)
            {
               // First, open a connection to the machine where LabVIEW is
               // running
               URL url = new URL(mURLText.getText().toString());
               URLConnection connection = url.openConnection();
               // Set resonable timeouts
               connection.setConnectTimeout(5000);
               connection.setReadTimeout(5000);
               // Create an XML document builder with the default settings
               DocumentBuilder docbuilder = DocumentBuilderFactory.newInstance()
                     .newDocumentBuilder();
               // Parse XML from the web service into a DOM tree
               Document doc = docbuilder.parse(connection.getInputStream());
               // Now, pull out the value attribute of the first channel element
               value = doc.getElementsByTagName("channel")
                     .item(0)
                     .getAttributes()
                     .getNamedItem("value")
                     .getNodeValue();
               
               messageHandler.sendMessage(Message.obtain(messageHandler, 1));
               
               // Create an array of data to add to the chart. There is only one
               // channel of data right now, so just add one point.
               ArrayList<Double> data = new ArrayList<Double>();
               data.add(Double.parseDouble(value));
               // Add the data to the chart.
               mChart.addDataPoint(data);
            }
         } catch (Exception e)
         {
            // Show some pop-up text so you can see what went wrong.
            // Since this thread is not the UI thread, post a runnable to it
            // instead. Only the UI thread can display a toast.
            final String message = e.toString();
            getCurrentFocus().post(new Runnable()
            {
               @Override
               public void run()
               {
                  Toast.makeText(SinglePoint.this, message, Toast.LENGTH_LONG)
                        .show();
               }
            });

         }
      }
   }
}

