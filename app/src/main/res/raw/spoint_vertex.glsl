
attribute vec3 vPosition;
uniform mat4 mvpMatrix;
uniform float pointSize;

void main() {
    gl_Position = mvpMatrix*vec4(vPosition,1.0);
    gl_PointSize = pointSize;
}
