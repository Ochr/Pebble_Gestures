package com.example.nadir.pebblegestures;

import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    public static ArrayList <String> options = new ArrayList<String>();
    public static GestureListFragment gestureList = new GestureListFragment();
    private static final String NO_MAPPING = "No function assigned";
    public static ArrayList<ListViewItem> gestureItemList = new ArrayList<ListViewItem>();

    public static final String SERVICECMD = "com.android.music.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPLAY = "play";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";

    private static UUID WATCHAPP_UUID = UUID.fromString("77de8318-a909-4ecf-bad2-20ede942d820");

    private static final int
            /*
            KEY_BUTTON = 0,
            KEY_VIBRATE = 1,
            BUTTON_UP = 0,
            BUTTON_SELECT = 1,
            BUTTON_DOWN = 2,
            */

            KEY_MAKE_NEW_GESTURE = 0,
            KEY_NEW_GESTURE_ID = 1,
            KEY_NEW_GESTURE_DATA = 2,
            KEY_NEW_GESTURE_DATA_SIZE = 3,
            KEY_GESTURE = 4,
            KEY_OLD_GESTURE_ID = 5,
            KEY_OLD_GESTURE_DATA = 6,
            KEY_OLD_GESTURE_DATA_SIZE = 7,
            KEY_ON_START = 8;


    private Handler handler = new Handler();
    private PebbleKit.PebbleDataReceiver appMessageReciever;
    private boolean isFlashlightOn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        options.add(getString(R.string.play_pause));
        options.add(getString(R.string.next_track));
        options.add(getString(R.string.previous_track));
        options.add(getString(R.string.toggle_flashlight));
        options.add(getString(R.string.open_chrome));
        options.add(getString(R.string.volume_down));
        options.add(getString(R.string.volume_up));

/*
        gestureItemList.add(new ListViewItem(getResources().getDrawable(R.drawable.frame), getString(R.string.flick_up), NO_MAPPING, 0));
        gestureItemList.add(new ListViewItem(getResources().getDrawable(R.drawable.frame), getString(R.string.flick_down), NO_MAPPING, 1));
        gestureItemList.add(new ListViewItem(getResources().getDrawable(R.drawable.frame), getString(R.string.flick_forward), NO_MAPPING, 2));
        gestureItemList.add(new ListViewItem(getResources().getDrawable(R.drawable.frame), getString(R.string.flick_backward), NO_MAPPING, 3));
*/
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, gestureList, "gesture_list")
                    .commit();
        }

        if(appMessageReciever == null) {

            appMessageReciever = new PebbleKit.PebbleDataReceiver(WATCHAPP_UUID) {
                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                    // Always ACK
                    PebbleKit.sendAckToPebble(context, transactionId);

                    if(data.getInteger(KEY_NEW_GESTURE_ID) != null) {

                        final int gestureID = data.getInteger(KEY_NEW_GESTURE_ID).intValue();
                        //add new gesture to list
                        addToGestureList("Customized Gesture "+gestureID, gestureID);
                        final int dataSize = data.getInteger(KEY_NEW_GESTURE_DATA_SIZE).intValue();
                        //store data size of byte array
                        storeDataSize(dataSize, gestureID);
                        final byte[] bytes = data.getBytes(KEY_NEW_GESTURE_DATA);
                        //store data
                        storeBytes(bytes, gestureID);

                    }

                    if (data.getInteger(KEY_GESTURE) != null) {
                        final int gestureID = data.getInteger(KEY_GESTURE).intValue();
                        //perform mapped function
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                selectedGestureFunction(gestureItemList.get(gestureID).description);
                            }
                        });
                    }
                    if (data.getInteger(KEY_ON_START) != null) {
                        //send pebble data
                        PebbleDictionary send_data = new PebbleDictionary();
                        //for each byte array stored in phone memory
                        for(int i = 0; i < gestureItemList.size(); i++)
                        {
                            int dataSize = getDataSize(i);
                            byte[] storedBytes = getByteData(dataSize, i);
                            send_data.addInt32(KEY_OLD_GESTURE_ID, i);
                            send_data.addInt32(KEY_OLD_GESTURE_DATA_SIZE, dataSize);
                            send_data.addBytes(KEY_OLD_GESTURE_DATA, storedBytes);
                            PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, send_data);
                        }
                    }
                }
            };
            // Add AppMessage capabilities
            PebbleKit.registerReceivedDataHandler(this, appMessageReciever);
        }
    }

    public void sendNewGestureCmd(View view) {
        PebbleDictionary send_data = new PebbleDictionary();
        send_data.addInt32(KEY_MAKE_NEW_GESTURE, 0);
        PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, send_data);
    }

    //selects the gesture function based on the gestures option
    public void selectedGestureFunction(String option)
    {
        if(option.toLowerCase().compareTo(options.get(0).toLowerCase()) == 0)
            controlMusic(CMDTOGGLEPAUSE);
        else if(option.toLowerCase().compareTo(options.get(1).toLowerCase()) == 0)
            controlMusic(CMDNEXT);
        else if(option.toLowerCase().compareTo(options.get(2).toLowerCase()) == 0)
            controlMusic(CMDPREVIOUS);
        else if(option.toLowerCase().compareTo(options.get(3).toLowerCase()) == 0)
            toggleFlashlight(findViewById(R.id.container));
        else if(option.toLowerCase().compareTo(options.get(4).toLowerCase()) == 0)
            openChrome();
        else if(option.toLowerCase().compareTo(options.get(5).toLowerCase()) == 0)
            controlVolume(KeyEvent.KEYCODE_VOLUME_DOWN);
        else if(option.toLowerCase().compareTo(options.get(6).toLowerCase()) == 0)
            controlVolume(KeyEvent.KEYCODE_VOLUME_UP);
    }

    public void deleteCustomGestureList(View view)
    {
        for(int i=0; i < gestureItemList.size(); i++)
        {
            try
            {
                gestureItemList.remove(i);
                deleteFile("gesture_file1_"+i);
                deleteFile("gesture_file_"+i);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        onResume();
    }


    public void addToGestureList(String gestureName, int gestureID) {
        gestureItemList.add(new ListViewItem(getResources().getDrawable(R.drawable.frame), gestureName, NO_MAPPING, gestureID));
    }

    public void storeBytes(byte[] bytes, int gestureId)
    {
        String filename = "gesture_file_"+gestureId;
        FileOutputStream fos = null;
        try {
           fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(bytes);
            fos.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public void storeDataSize(int datasize, int gestureId)
    {
        String filename1 = "gesture_file1_"+gestureId;
        try {
            FileOutputStream fos1 = openFileOutput(filename1, Context.MODE_PRIVATE);
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(datasize);
            byte[] bytes = bb.array();
            fos1.write(bytes);
            fos1.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public byte[] getByteData(int datasize, int gestureId)
    {
        String filename = "gesture_file_"+gestureId;
        try {
            FileInputStream fis = openFileInput(filename);
            byte[] bytes = null;
            fis.read(bytes, 0, datasize);
            fis.close();
            return bytes;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public int getDataSize(int gestureId)
    {
        String filename = "gesture_file1_"+gestureId;
        try {
            FileInputStream fis = openFileInput(filename);
            byte[] bytes = null;
            fis.read(bytes, 0, 4);
            ByteBuffer wrapped = ByteBuffer.wrap(bytes);
            int num = wrapped.getInt();
            fis.close();
            return num;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }


    //Functions Below
    ///////
    public void openChrome() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://google.com"));
        intent.setPackage("com.android.chrome");
        startActivity(intent);
    }
    public void controlVolume(final int keyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(keyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public static Camera cam = null;
    public void toggleFlashlight(View view) {
        if (isFlashlightOn)
            flashLightOff(view);
        else
            flashLightOn(view);
    }
    public void flashLightOn(View view) {

        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                cam = Camera.open();
                Camera.Parameters p = cam.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                cam.setParameters(p);
                cam.startPreview();
                isFlashlightOn = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void flashLightOff(View view) {
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                cam.stopPreview();
                cam.release();
                cam = null;
                isFlashlightOn = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOff",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void controlMusic(String cmd) {
        Intent i = new Intent(SERVICECMD);
        i.putExtra(CMDNAME , cmd );
        sendBroadcast(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class GestureListFragment extends ListFragment {
        //Global Constants
        private static final String NO_MAPPING = "No function assigned";
        public ListViewAdapter listViewAdapt;
        public  List<ListViewItem> mItems;        // ListView items list

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // initialize the items list
            mItems = new ArrayList<ListViewItem>();
            Resources resources = getResources();
            /*
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.flick_up), NO_MAPPING,0));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.flick_down), NO_MAPPING,1));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.flick_forward), NO_MAPPING,2));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.flick_backward), NO_MAPPING,3));
            */
            // initialize and set the list adapter
            listViewAdapt = new ListViewAdapter(getActivity(), mItems);
            setListAdapter(listViewAdapt);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // remove the dividers from the ListView of the ListFragment
            getListView().setDivider(null);
        }

        @Override
        public void onResume()
        {
            super.onResume();
            listViewAdapt.clear();
            listViewAdapt.addAll(gestureItemList);
            listViewAdapt.notifyDataSetChanged();
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            // retrieve theListView item
            ListViewItem item = mItems.get(position);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new FunctionListFragment(position), "function_list")
                    .commit();
        }
    }

    public static class FunctionListFragment extends ListFragment {

        private static final String NO_MAPPING = "No function assigned";
        private List<ListViewItem> mItems;        // ListView items list
        private int mGesturePos;
        public FunctionListFragment() {}
        public FunctionListFragment(int gesturePos) {
            mGesturePos = gesturePos;
        }
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // initialize the items list
            mItems = new ArrayList<ListViewItem>();
            Resources resources = getResources();

            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.play_pause), "play/pause music", 0));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.previous_track), "Go to prev track",1));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.next_track), "Go to next track", 2));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.toggle_flashlight), "Turn flashlight on or off", 3));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.open_chrome), "Open chrome browser", 4));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.volume_down), "",5));
            mItems.add(new ListViewItem(resources.getDrawable(R.drawable.frame), getString(R.string.volume_up), "", 6));



            // initialize and set the list adapter
            setListAdapter(new ListViewAdapter(getActivity(), mItems));
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // remove the dividers from the ListView of the ListFragment
            getListView().setDivider(null);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            // retrieve theListView item
            ListViewItem item = mItems.get(position);
            gestureItemList.get(mGesturePos).description = item.title;
            ((ArrayAdapter) gestureList.getListAdapter()).notifyDataSetChanged();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, gestureList, "gesture_list")
                    .commit();
        }
    }
}



