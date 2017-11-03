package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Int2;
import android.renderscript.RenderScript;
import android.renderscript.Type;

import com.example.ramandeep.conwaysgameex5.ScriptC_ConwayProcessScript;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ramandeep on 2017-09-21.
 */

public class ConwayProcessor {

    private static final int MAX_FRAMES_GENERATEED = 50;
    private boolean initialized = false;

    private ConcurrentLinkedQueue<Object[]> livingList;
    private ConcurrentLinkedQueue<Object[]> deadList;

    private String threadName = "ConwayProcessThread";
    private HandlerThread thread;
    private Handler threadHandler;

    private Runnable initializationRunnable;
    private Runnable AutoProcessRunnable;
    private Runnable DisplayNewInputRunnable;
    private Runnable DisplayAfterPauseRunnable;


    private long delay = 65;

    private RenderScript rsContext;
    private ScriptC_ConwayProcessScript processor;
    private Allocation inputAllocation;
    private Allocation outputAllocation;

    private byte[] input;
    private byte[] output;

    private int rows;
    private int columns;
    private int cellCount;
    private int centerRow;
    private int centerColumn;

    private HashSet<Integer> living;
    private HashSet<Integer> stillAlive;
    private HashSet<Integer> dead;
    private HashSet<Integer> prevFrame;
    private HashSet<Integer> newFrame;

    private boolean switchFlag = false;
    private boolean newObject = false;
    private boolean running = false;
    private boolean ranOnce = false;

    private int framesWaiting = 0;

    private CountDownLatch displayUpdateLatch;

    public ConwayProcessor(Context context, ConcurrentLinkedQueue<Object[]> livingList, ConcurrentLinkedQueue<Object[]> deadList) {
        this.livingList = livingList;
        this.deadList = deadList;

        thread = new HandlerThread(threadName);
        thread.start();
        threadHandler = new Handler(thread.getLooper());

        rsContext = RenderScript.create(context);
        processor = new ScriptC_ConwayProcessScript(rsContext);
    }

    public void init(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;

        cellCount = rows * columns;

        centerRow = rows/2;
        if (rows % 2 == 0) {
            centerRow--;
        }
        centerColumn = columns/2;
        if (columns % 2 == 0) {
            centerColumn--;
        }
        input = new byte[cellCount];
        output = new byte[cellCount];

        living = new HashSet<>();
        stillAlive = new HashSet<>();
        dead = new HashSet<>();
        prevFrame = new HashSet<>();
        newFrame = new HashSet<>();

        //allocation
        Type type = Type.createX(rsContext, Element.I8(rsContext),cellCount);
        inputAllocation = Allocation.createTyped(rsContext,type);
        outputAllocation = Allocation.createTyped(rsContext,type);
        //ready the processor
        processor.set_rows(rows);
        processor.set_columns(columns);
        processor.set_totalCells(cellCount);
        System.out.println("rows,columns = "+rows +","+columns);
        //still need to create a byte array of data to feed inputAllocation

        AutoProcessRunnable = new Runnable() {
            @Override
            public void run() {
                //process the grid
                processGrid();
                updateLocations(output);
                threadHandler.postDelayed(AutoProcessRunnable,delay);
            }
        };

        //we know that input is has data that is up to date so display input
        DisplayNewInputRunnable = new Runnable() {
            @Override
            public void run() {
                updateLocations(input);
            }
        };

        //displaying after pause
        //it could be processing and then it is paused
        //or it could be not processing and then it is paused
        //or it could have never started processing and then it is paused
        DisplayAfterPauseRunnable = new Runnable(){
            @Override
            public void run(){
                if(displayUpdateLatch != null){
                    try {
                        displayUpdateLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //if the grid has not been processed then display input
                if(!ranOnce){
                    updateLocations(input);
                }else{
                    //if it has run atleast once then
                    if(switchFlag){
                        outputAllocation.copyTo(output);
                    }else{
                        inputAllocation.copyTo(output);
                    }
                    updateLocations(output);
                }
            }
        };

        initialized = true;
    }

    private void processGrid() {
        if(!switchFlag){
            processor.set_inAlloc(inputAllocation);
            processor.forEach_nextGen2(inputAllocation,outputAllocation);
            outputAllocation.copyTo(output);
        }else{
            processor.set_inAlloc(outputAllocation);
            processor.forEach_nextGen2(outputAllocation,inputAllocation);
            inputAllocation.copyTo(output);
        }
        switchFlag = !switchFlag;
        ranOnce = true;
    }

    private void updateLocations(byte[] output) {
        newFrame.clear();
        living.clear();//clear living
        stillAlive.clear();
        //create a set of all living locations
        for (int i = 0; i < output.length; i++) {
            if(output[i] == 1){
                newFrame.add(i);
            }
        }
        living.addAll(newFrame);
        stillAlive.addAll(newFrame);
        dead.addAll(prevFrame);

        living.removeAll(dead);//only new living cells
        stillAlive.retainAll(prevFrame);//still living from previous generation
        dead.removeAll(stillAlive);
        sendLocations();
        //after sending save all living cells
        dead.clear();
        prevFrame.clear();
        prevFrame.addAll(newFrame);
    }

    private void sendLocations() {
        livingList.add(living.toArray());
        deadList.add(dead.toArray());
    }

    public void onResume() {
        System.out.println("ConwayProcessor Resumed!");
        if(initialized){
            //if initialized then show
            threadHandler.post(DisplayAfterPauseRunnable);
        }
    }

    public void startAuto(){
        if(initialized) {
            threadHandler.post(AutoProcessRunnable);
        }
    }

    public void onPause() {
        System.out.println("ConwayProcessor Paused/Stopped!");
        stop();
        prevFrame.clear();
    }

    public void onDestroy(){
        threadCleanUp();
        destroyRenderScriptData();
        initialized = false;
    }

    private void destroyRenderScriptData() {
        inputAllocation.destroy();
        outputAllocation.destroy();
        processor.destroy();
        rsContext.destroy();
    }

    private void threadCleanUp() {
        if(threadHandler!=null){
            threadHandler.removeCallbacks(null);
        }
        if(thread !=null){
            thread.quit();
            thread = null;
            System.out.println(threadName + " Ended.");
        }
    }

    public void stop() {
        threadHandler.removeCallbacks(AutoProcessRunnable);
        threadHandler.removeCallbacks(null);
    }

    private void insertObject(ConwayObject conwayObject){
        //insert the conwayObject into the current grid
        //from the bottom left corner
        Int2 dimensions = conwayObject.dimensions;
        if (dimensions.x > rows || dimensions.y > columns) {
            System.out.println("ConwayObj too big for current grid");
            return;
        }
        //assume nothing is rendering
        byte[] data = conwayObject.data;
        Arrays.fill(input,(byte)0);//0 the input array
        int dataCenterRow = dimensions.x/2;
        int dataCenterCol = dimensions.y/2;

        if(dimensions.x%2 == 0){
            dataCenterRow--;
        }
        if(dimensions.y%2==0){
            dataCenterCol--;
        }

        int topLeftX = centerRow - dataCenterRow;
        int topLeftY = centerColumn - dataCenterCol;

        int startIndex = topLeftX*columns + topLeftY;
        int i = 0;
        int dataIndex = 0;
        //copy the contents of data array to the center of the input array
        while(i < dimensions.x){
            System.arraycopy(data,dataIndex,input,startIndex,dimensions.y);
            startIndex+=columns;
            dataIndex+=dimensions.y;
            i++;
        }
    }

    public void loadNewObject(ConwayObject conwayObject) {
        ranOnce = false;
        insertObject(conwayObject);
        if(!switchFlag){
            inputAllocation.copyFrom(input);
        }else{
            outputAllocation.copyFrom(input);
        }
        threadHandler.post(DisplayNewInputRunnable);
    }

    public void setUpdateLatch(CountDownLatch updateLatch) {
        this.displayUpdateLatch = updateLatch;
    }

    public void setDelay(int frameDelay) {
        delay = frameDelay;
    }
}
