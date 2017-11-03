attribute vec3 vPosition;
attribute vec4 vColor;

uniform mat4 mvpMatrix;
uniform float pointSize;

varying vec4 fragColor;

void main() {
    gl_Position = mvpMatrix*vec4(vPosition,1.0);
    gl_PointSize = pointSize;
    fragColor = vColor;
}
