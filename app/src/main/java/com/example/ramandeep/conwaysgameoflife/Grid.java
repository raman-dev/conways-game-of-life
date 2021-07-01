package com.example.ramandeep.conwaysgameoflife;


import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by Ramandeep on 2017-09-05.
 */

public class Grid extends RenderObject {

    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int VERTICES_PER_LINE = 2;
    private static final int INDICES_PER_LINE = 2;

    private int[] indices;

    private float[] verticalVerts;
    private float[] horizontalVerts;
    private float[] vertices;

    private float surfaceWidth;
    private float surfaceHeight;

    private int rows = -1;
    private int columns = -1;

    private float rowHeight = -1f;
    private float columnWidth = -1f;

    private float originX = 0f;
    private float originY = 0f;

    private int verticalVertexPointer;
    private int horizontalVertexPointer;
    private int indexCounter;

    private float brightness = 0.5f;
    private float[] color = {brightness,brightness,brightness,1f};
    private float[] black = {0f,0f,0f,1f};
    private boolean showGrid = true;

    public Grid(){
        initColorBuffer(color);
    }

    //not likely to be called because rows are unknown until surface creation
    public Grid(int rows, int columns){
        this.rows = rows;
        this.columns = columns;
        initColorBuffer(color);
    }

    public void setMaxDimensions(int width,int height,int rows,int columns){
        this.rows = rows;
        this.columns = columns;

        surfaceWidth = width;
        surfaceHeight = height;

        columnWidth = surfaceWidth/columns;
        rowHeight = columnWidth;//surfaceHeight/rows;

        verticalVerts = new float[POSITION_COMPONENT_COUNT*VERTICES_PER_LINE *(columns + 1)];
        horizontalVerts = new float[POSITION_COMPONENT_COUNT*VERTICES_PER_LINE * (rows + 1)];
        vertices = new float[POSITION_COMPONENT_COUNT*VERTICES_PER_LINE*(rows  + columns + 2)];
        indices = new int[INDICES_PER_LINE*(rows + columns + 2)];

        verticalVertexPointer = 0;
        horizontalVertexPointer = 0;
        indexCounter = 0;
    }

    public void generateGrid(){
        generateVerticalLines();
        generateHorizontalVertices();
        System.arraycopy(verticalVerts,0,vertices,0,verticalVerts.length);
        System.arraycopy(horizontalVerts,0,vertices,verticalVerts.length,horizontalVerts.length);
        initVertexBuffer(vertices);
        initIndexBuffer(indices);
    }

    public void generateVerticalLines(){
        //create vertical lines
        float left = originX - surfaceWidth/2;
        float bottom = originY - surfaceHeight/2;
        float top = -bottom;//bottom + rows*rowHeight;
        int i = 0;
        while(i <= columns){
            //2 vertices per line
            //3 components per vertex
            //x y z
            //top and bottom should remain fixed
            //and line should move left to right
            //bottom
            verticalVertexPointer = insertVertex(verticalVerts,verticalVertexPointer,left + i*columnWidth,bottom,0f);
            indices[indexCounter] = indexCounter;
            indexCounter++;
            //top
            verticalVertexPointer = insertVertex(verticalVerts,verticalVertexPointer,left + i*columnWidth,top,0f);
            indices[indexCounter] = indexCounter;
            indexCounter++;
            i++;
        }
    }

    public void generateHorizontalVertices(){
        int i = 0;
        float left = originX - surfaceWidth/2;
        float right = -left;
        float bottom = originY - surfaceHeight/2;

        while(i <= rows){
            //left
            horizontalVertexPointer = insertVertex(horizontalVerts,horizontalVertexPointer,left,bottom + i*rowHeight,0f);
            indices[indexCounter] = indexCounter;
            indexCounter++;
            //right
            horizontalVertexPointer = insertVertex(horizontalVerts,horizontalVertexPointer,right,bottom + i*rowHeight,0f);
            indices[indexCounter] = indexCounter;
            indexCounter++;
            i++;
        }
    }

    public void setVBOsAndAttributes(){
        buffers = new int[2];
        glGenBuffers(2,buffers,0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//index buffer
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexBuffer.capacity()*BYTES_PER_INT,indexBuffer,GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);
        glBufferData(GL_ARRAY_BUFFER,vertexBuffer.capacity()*BYTES_PER_FLOAT,vertexBuffer,GL_STATIC_DRAW);

        positionReference = glGetAttribLocation(program,"vPosition");
        colorReference = glGetUniformLocation(program,"mColor");
        mvpMatrixReference = glGetUniformLocation(program,"mvpMatrix");
    }

    @Override
    void draw() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);//index buffer
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);
        //                    location,         1-4                     , type   ,normalize,where can the next one be found, number of bytes till the first
        //                                                                                                         attribute
        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,POSITION_COMPONENT_COUNT*BYTES_PER_FLOAT,0);
        glEnableVertexAttribArray(positionReference);

        glUseProgram(program);
        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);
        glUniform4fv(colorReference,1,colorBuffer);
        glDrawElements(GL_LINES,indexBuffer.capacity(),GL_UNSIGNED_INT,0);
        glDisableVertexAttribArray(positionReference);
    }

    public void gridVisible(boolean showGrid) {
        this.showGrid = showGrid;
        if(showGrid){
            colorBuffer.clear();
            colorBuffer.put(color);
            colorBuffer.position(0);
        }else {
            colorBuffer.clear();
            colorBuffer.put(black);
            colorBuffer.position(0);
        }
    }
}
