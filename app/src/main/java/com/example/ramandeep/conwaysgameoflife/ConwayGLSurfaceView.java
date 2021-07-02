package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ramandeep on 2017-09-21.
 */

public class ConwayGLSurfaceView extends GLSurfaceView implements ConwayProcessor.FrameUpdateListener,ConwayRenderer.DimensionMeasureListener {

    private ConwayRenderer conwayRenderer;
    private ConwayProcessor conwayProcessor;
    private ConcurrentLinkedQueue<byte[]> frameQueue;
    private CountDownLatch displayUpdateLatch;

    private final int[] defaultColumnSizes = {9, 12, 15, 18, 27, 36, 45, 54, 63, 72};

    private int columns = -1;
    private int rows = -1;

    private BackgroundTask initTask;

    private ArrayList<ConwayObject> conwayObjectList;
    private int conwayIter = 0;

    private boolean initialized = false;
    private boolean isProcessing = false;

    public ConwayGLSurfaceView(Context context) {
        super(context);
        if (!isInEditMode()) {
            init(context);
        }
    }

    public ConwayGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(context);
        }
    }

    private void init(Context context) {

        initTask = new BackgroundTask("Initialization Thread");
        //send the data here
        //from here send to glthread
        displayUpdateLatch = new CountDownLatch(0);
        frameQueue = new ConcurrentLinkedQueue<>();
        columns = defaultColumnSizes[defaultColumnSizes.length - 2];
        //performs the grid calculations
        conwayProcessor = new ConwayProcessor(context,this);
        conwayRenderer = new ConwayRenderer(context, columns,frameQueue,this);//renders cells to screen

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean showGrid = sharedPref.getBoolean("grid_visible", false);
        conwayRenderer.gridVisible(showGrid);
        conwayRenderer.setColor(sharedPref.getInt("cell_color", ColorPickerPreference.COLOR_GREEN));
        conwayProcessor.setDelay(sharedPref.getInt("frame_delay", SeekBarPreference.MAX_VALUE));

        setEGLContextClientVersion(2);
        setRenderer(conwayRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void OnFrameAvailable(byte[] data) {
        frameQueue.add(data);
        requestRender();
    }

    @Override
    public void onResume() {
        //
        super.onResume();
        conwayProcessor.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //pause calculations of any frames
        conwayPause();
    }

    private void conwayPause() {
        displayUpdateLatch = new CountDownLatch(2);
        conwayRenderer.setUpdateLatch(displayUpdateLatch);
        conwayProcessor.setUpdateLatch(displayUpdateLatch);
        conwayProcessor.onPause();
        isProcessing = false;
    }

    public void onDestroy() {
        initTask.close();
        conwayProcessor.onDestroy();
        System.out.println("GLSV onDestroy!");
    }

    //start button was clicked
    public void onClickStart() {
        resumeProcessing();
    }

    //stop button clicked
    public void onClickStop() {
        pauseProcessing();
    }

    //next button clicked
    //when this is clicked display a new conwayObject
    //on the surface
    public void onClickNext() {
        pauseProcessing();
        clearUpdateQueues();
        updateConwayIter(true);
        loadNextConwayObject(conwayIter);
        //i want to couple rendering and processing
        //1 frame rendered per frame processed
        //not like how i am doing now
    }

    private void clearUpdateQueues() {
        frameQueue.clear();
    }

    private void updateConwayIter(boolean incrementFlag) {
        conwayIter = incrementFlag? conwayIter + 1: conwayIter - 1 ;
        if (conwayIter >= conwayObjectList.size()) {
            conwayIter = 0;
        }
        else if(conwayIter < 0){
            conwayIter = conwayObjectList.size() - 1;
        }
    }

    private void loadInitial(ConwayObject conwayObject) {
        System.out.println("Loading Conway Object!");
        conwayProcessor.loadNewObject(conwayObject);
    }

    public void setConwayObjectList(ArrayList<ConwayObject> conwayObjectList) {
        this.conwayObjectList = conwayObjectList;
        conwayIter = 0;
    }

    private void loadNextConwayObject(int index) {
        conwayProcessor.loadNewObject(conwayObjectList.get(index));
    }

    private void resumeProcessing() {
        if(!isProcessing){
            conwayProcessor.startAuto();//start processing frames
            isProcessing = true;
        }
    }

    private void pauseProcessing() {
        isProcessing = false;
        conwayProcessor.stop();
    }

    public void setCellColor(int color) {
        conwayRenderer.setColor(color);
    }

    public void setFrameDelay(int frameDelay) {
        conwayProcessor.setDelay(frameDelay);
    }

    public void gridVisible(boolean aBoolean) {
        conwayRenderer.gridVisible(aBoolean);
    }

    @Override
    public void MaxRowsAvailable(final int maxRows) {
        if(initialized){
            return;
        }
        //takes time so needs a seperate thread
        initTask.submitRunnable(new Runnable() {
            @Override
            public void run() {
                rows = maxRows;
                //now conwayProcessor can get initialized
                //could grab a reference to the handler from here
                conwayProcessor.init(rows, columns);
                loadInitial(conwayObjectList.get(conwayIter));
                initialized = true;
                System.out.println("InitGLSV!");
            }
        });
    }

    public void onClickPrev() {
        pauseProcessing();
        clearUpdateQueues();
        updateConwayIter(false);
        loadNextConwayObject(conwayIter);
    }
}
