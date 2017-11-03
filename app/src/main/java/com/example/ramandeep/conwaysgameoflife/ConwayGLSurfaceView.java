package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ramandeep on 2017-09-21.
 */

public class ConwayGLSurfaceView extends GLSurfaceView {

    private ConwayRenderer conwayRenderer;
    private ConwayProcessor conwayProcessor;

    private ConcurrentLinkedQueue<Object[]> livingList;
    private ConcurrentLinkedQueue<Object[]> deadList;

    private AtomicInteger rowsAtomic;
    private CountDownLatch rowNumWait;
    private CountDownLatch displayUpdateLatch;

    private int[] defaultColumnSizes = {9, 12, 15, 18, 27, 36, 45, 54, 63, 72};

    private int columns = -1;
    private int rows = -1;

    private BackgroundTask initTask;
    private Runnable initializationRunnable;
    private Runnable pauseRenderRunnable;
    private Runnable resumeRenderRunnable;


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

        rowNumWait = new CountDownLatch(1);
        displayUpdateLatch = new CountDownLatch(0);
        rowsAtomic = new AtomicInteger();

        livingList = new ConcurrentLinkedQueue<>();
        deadList = new ConcurrentLinkedQueue<>();

        columns = defaultColumnSizes[defaultColumnSizes.length - 2];

        //performs the grid calculations
        conwayProcessor = new ConwayProcessor(context, livingList, deadList);
        conwayRenderer = new ConwayRenderer(context, livingList, deadList, columns, rowNumWait, rowsAtomic);//renders cells to screen

        initializationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    rowNumWait.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                rows = rowsAtomic.get();
                //now conwayProcessor can get initialized
                conwayProcessor.init(rows, columns);
                loadInitial(conwayObjectList.get(conwayIter));
                updateConwayIter();
                initialized = true;
                System.out.println("InitGLSV!");
            }
        };

        pauseRenderRunnable = new Runnable() {
            @Override
            public void run() {
                conwayRenderer.pauseRendering();
            }
        };

        resumeRenderRunnable = new Runnable() {
            @Override
            public void run() {
                conwayRenderer.resumeRendering();
            }
        };

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean showGrid = sharedPref.getBoolean("grid_visible", false);
        conwayRenderer.gridVisible(showGrid);
        conwayRenderer.setColor(sharedPref.getInt("cell_color", ColorPickerPreference.COLOR_GREEN));
        conwayProcessor.setDelay(sharedPref.getInt("frame_delay", SeekBarPreference.MAX_VALUE));

        setEGLContextClientVersion(2);
        setRenderer(conwayRenderer);
    }

    @Override
    public void onResume() {
        //
        super.onResume();
        if (!initialized) {
            initTask.submitRunnable(initializationRunnable);
        }
        conwayProcessor.onResume();
        resumeRendering();
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
        pauseRendering();
    }

    public void onDestroy() {
        initTask.close();
        conwayProcessor.onDestroy();
        System.out.println("GLSV onDestroy!");
    }

    //start button was clicked
    public void onClickStart() {
        resumeProcessing();
        resumeRendering();
    }

    //stop button clicked
    public void onClickStop() {
        pauseProcessing();
        pauseRendering();
    }

    //next button clicked
    //when this is clicked display a new conwayObject
    //on the surface
    public void onClickNext() {
        pauseRendering();
        pauseProcessing();
        clearUpdateQueues();
        loadNextConwayObject();
        updateConwayIter();
        resumeRendering();
    }

    private void clearUpdateQueues() {
        livingList.clear();
    }

    private void updateConwayIter() {
        conwayIter++;
        if (conwayIter >= conwayObjectList.size()) {
            conwayIter = 0;
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

    private void loadNextConwayObject() {
        conwayProcessor.loadNewObject(conwayObjectList.get(conwayIter));
    }

    private void pauseRendering() {
        this.queueEvent(pauseRenderRunnable);//stop updating frames
    }

    private void resumeRendering() {
        this.queueEvent(resumeRenderRunnable);//display frames
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
}
