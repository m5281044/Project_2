import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.AxisAngle4f;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    private final Matrix4f transform;
    private float speed = 0.05f;
    private float sensitivity = 0.2f;

    private boolean dragging = false;
    private double lastX, lastY;

    public Camera() {
        transform = new Matrix4f().identity();
        Vector3f eye = new Vector3f(0.0f, 0.0f, -2.0f);
        Vector3f center = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        transform.lookAt(eye, center, up);
        transform.invert();
    }

    public Matrix4f getTransform() {
        return new Matrix4f(transform);
    }

    public void translate(Vector3f translation) {
        transform.translate(translation);
    }

    public void rotate(Vector3f axis, float angle) {
        AxisAngle4f aa = new AxisAngle4f(angle, axis);
        Matrix4f rotMat = new Matrix4f().rotate(aa);
        transform.mul(rotMat);
    }

    public void processInput(long window) {
        Vector3f move = new Vector3f();
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) move.z -= speed;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) move.z += speed;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) move.x -= speed;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) move.x += speed;
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) move.y -= speed;
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) move.y += speed;
        translate(move);
    }

    public void handleMouseInput(long window) {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);
        double x = xpos[0];
        double y = ypos[0];

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            if (!dragging) {
                dragging = true;
                lastX = x;
                lastY = y;
            } else {
                float dx = (float) (x - lastX) * sensitivity * 0.01f;
                float dy = (float) (y - lastY) * sensitivity * 0.01f;
                
                rotate(new Vector3f(0, 1, 0), dx);
                rotate(new Vector3f(1, 0, 0), dy);

                lastX = x;
                lastY = y;
            }
        } else {
            dragging = false;
        }
    }
}
