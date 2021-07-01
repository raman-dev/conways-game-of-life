package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glViewport;

/**
 * Created by Ramandeep on 2017-09-21.
 */

class ConwayRenderer implements GLSurfaceView.Renderer{

    private final DimensionMeasureListener mDimensionMeasureListener;
    private CountDownLatch displayUpdateLatch;

    private Grid grid;
    private Point points;
    //shader source for grid vertex
    private String gridVertexSource;
    //shader source for simple point fragment
    //can be used as grid source
    private String spointFragmentSource;

    private String maPointVertexAltSource;
    private String maPointFragmentSource;

    private int columns = -1;
    private int rows = -1;

    private float[] viewMatrix;
    private float[] projectionMatrix;
    private float[] mvpMatrix;

    private float[] red = {1f,0f,0f,1f};
    private float[] green = {0f,1f,0f,1f};
    private float[] blue = {0f,0f,1f,1f};
    private float[] purple= {0.686f,0f,0.749f,1f};

    public ConwayRenderer(Context context, int columns, ConcurrentLinkedQueue<byte[]> frameQueue, DimensionMeasureListener mDimensionMeasureListener) {

        this.columns = columns;
        this.mDimensionMeasureListener = mDimensionMeasureListener;

        viewMatrix = new float[16];
        projectionMatrix = new float[16];
        mvpMatrix = new float[16];

        grid = new Grid();
        points = new Point(frameQueue);

        gridVertexSource = RenderObject.readSourceFromRaw(context,R.raw.grid_vertex);
        spointFragmentSource = RenderObject.readSourceFromRaw(context,R.raw.spoint_fragment);

        maPointVertexAltSource = RenderObject.readSourceFromRaw(context,R.raw.ma_alt_point_vertex);
        maPointFragmentSource = RenderObject.readSourceFromRaw(context,R.raw.ma_point_frag);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        glClearColor(0f,0f,0f,1f);
        System.out.println("glSurfaceView onSurfaceCreated!");


        grid.setShaderFromSource(GL_VERTEX_SHADER,gridVertexSource);
        grid.setShaderFromSource(GL_FRAGMENT_SHADER,spointFragmentSource);
        grid.createProgram();

        points.setShaderFromSource(GL_VERTEX_SHADER,maPointVertexAltSource);
        points.setShaderFromSource(GL_FRAGMENT_SHADER,maPointFragmentSource);
        points.createProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        System.out.println("glSurfaceView onSurfaceChanged!");

        float colWidth = ((float)width)/columns;

        //surfaceHeight/columnWidth
        rows = Math.round(height/colWidth);
        //make the other threads wait until
        //number of rows have been calculated
        mDimensionMeasureListener.MaxRowsAvailable(rows);
        RenderObject.computeOrthoMVP(width,height,-10f,10f,viewMatrix,projectionMatrix,mvpMatrix);
        prepareGrid(width,height);
        points.setPointGridDimensions(width,height,rows,columns);
        points.setPointSize(colWidth);
        points.setMvpMatrix(mvpMatrix);
        points.generatePointGrid();
        points.setAttributeAndVBO();
        glLineWidth(2f);

        if(displayUpdateLatch != null){
            //ticks down
            displayUpdateLatch.countDown();
        }

        glViewport(0,0,width,height);
    }

    private void prepareGrid(int width, int height) {
        //set dimensions
        grid.setMaxDimensions(width,height,rows,columns);
        grid.generateGrid();//generate vertices and indices for the grid
        grid.setMvpMatrix(mvpMatrix);//set the model-view projection matrix
        grid.setVBOsAndAttributes();//send the data to the gpu
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT);
        points.draw();
        grid.draw();

    }

    public void setUpdateLatch(CountDownLatch updateLatch) {
        this.displayUpdateLatch = updateLatch;
    }

    public void gridVisible(boolean showGrid) {
        grid.gridVisible(showGrid);
    }

    //set color of the cells
    public void setColor(int color) {
        float[] cellColor;
        switch(color){
            case ColorPickerPreference.COLOR_RED:
                cellColor = red;
                break;
            case ColorPickerPreference.COLOR_GREEN:
                cellColor = green;
                break;
            case ColorPickerPreference.COLOR_BLUE:
                cellColor = blue;
                break;
            case ColorPickerPreference.COLOR_PURPLE:
                cellColor = purple;
                break;
            default:
                cellColor = green;
        }
        points.setCellColor(cellColor);
    }

    public interface DimensionMeasureListener {
        void MaxRowsAvailable(int rows);
    }
}
