package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;

/**
 * Created by Ramandeep on 2017-09-05.
 */

public abstract class RenderObject {
    public static final int BYTES_PER_FLOAT = 4;
    public static final int BYTES_PER_INT = 4;

    protected FloatBuffer mvpMatrixBuffer;//model-view-projection matrix
    protected FloatBuffer vertexBuffer; //vertices
    protected FloatBuffer vertexAndColorBuffer; //vertices and per vertex color interleaved
    protected IntBuffer indexBuffer; //indices
    protected FloatBuffer colorBuffer; //colors ;

    protected int[] buffers;

    protected int program;
    private int vertexShader;
    private int fragmentShader;

    protected int mvpMatrixReference = -1;
    protected int positionReference = -1;
    protected int vertexColorReference = -1;//for per vertex colors
    protected int colorReference = -1;
    protected int pointSizeReference = -1;

     protected void createProgram() {
        program = glCreateProgram();//get an available program id
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        glLinkProgram(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

     protected void setShaderFromSource(int shaderType, String shaderSource) {
        switch (shaderType) {
            case GL_VERTEX_SHADER:
                vertexShader = loadShader(GL_VERTEX_SHADER, shaderSource);
                break;
            case GL_FRAGMENT_SHADER:
                fragmentShader = loadShader(GL_FRAGMENT_SHADER, shaderSource);
                break;
        }
    }

    private int loadShader(int type, String shaderSource) {
        int shaderReference = glCreateShader(type);
        glShaderSource(shaderReference, shaderSource);
        glCompileShader(shaderReference);

        int[] compileResult = new int[1];
        //check compile status
        glGetShaderiv(shaderReference, GL_COMPILE_STATUS, compileResult, 0);
        if (compileResult[0] < 0) {
            System.out.println("Shader not compiled!");
            glDeleteShader(shaderReference);
        } else {
            //System.out.println("Shader Compiled.");
        }
        return shaderReference;
    }

    protected void initVertexBuffer(float[] vertices) {
        vertexBuffer = getNativeOrderFloatBuffer(vertices.length);
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    protected void initVertexAndColorBuffer(float[] values) {
        vertexAndColorBuffer = getNativeOrderFloatBuffer(values.length);
        vertexAndColorBuffer.put(values);
        vertexAndColorBuffer.position(0);
    }

    protected void initColorBuffer(float[] color) {
        colorBuffer = getNativeOrderFloatBuffer(color.length);
        colorBuffer.put(color);
        colorBuffer.position(0);
    }

     protected void initIndexBuffer(int[] indices) {
        indexBuffer = getNativeOrderIntBuffer(indices.length);
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

     protected void setMvpMatrix(float[] mvpMatrix) {
        mvpMatrixBuffer = getNativeOrderFloatBuffer(mvpMatrix.length);
        mvpMatrixBuffer.put(mvpMatrix);
        mvpMatrixBuffer.position(0);
    }

    //genericize  buffers into a map of name,value pairs in a map
     protected void putFloatBuffer(String name, float[] values) {
    }

     protected void putIndexBuffer(String name, int[] values) {
    }

     FloatBuffer getNativeOrderFloatBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT);
        byteBuffer.order(ByteOrder.nativeOrder());//native byte order
        return byteBuffer.asFloatBuffer();
    }

    IntBuffer getNativeOrderIntBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_INT);
        byteBuffer.order(ByteOrder.nativeOrder());//native byte order
        return byteBuffer.asIntBuffer();
    }

    public static int insertVertex(float[] vertices, int currentLocation, float x, float y, float z) {
        vertices[currentLocation] = x;
        vertices[currentLocation + 1] = y;
        vertices[currentLocation + 2] = z;
        return currentLocation + 3;
    }

    public static int insertColor(float[] values, int currentLocation, float r, float g, float b, float a) {
        values[currentLocation] = r;
        values[currentLocation + 1] = g;
        values[currentLocation + 2] = b;
        values[currentLocation + 3] = a;
        return currentLocation + 4;
    }

    public static int insertColor(float[] values, int currentLocation, float[] color) {
        values[currentLocation] = color[0];
        values[currentLocation + 1] = color[1];
        values[currentLocation + 2] = color[2];
        values[currentLocation + 3] = color[3];
        return currentLocation + 4;
    }

    abstract void draw();

    protected void drawAlt() {
    }

     public static void computeOrthoMVP(int width, int height, float near, float far, float[] viewMatrix, float[] projectionMatrix, float[] mvpMatrix) {
        float left = width / 2f;
        float right = -left;
        float bottom = -height / 2f;
        float top = -bottom;
        //                              eye         center   up
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, near, far);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

     public static String readSourceFromRaw(Context context, int resourceId) {
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceId)));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try {
            line = br.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

}
