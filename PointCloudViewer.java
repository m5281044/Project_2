import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class PointCloudViewer {
    private long window;
    private int vao;
    private int vbo;
    private int program;

    // Loaded points (x, y, z, nx, ny, nz)
    private final List<float[]> points = new ArrayList<>();
    private List<int[]> triangles = new ArrayList<>();
        private RBFReconstruction rbf;
    
        //MVP matrices
        private Matrix4f projectionMatrix;
        private Matrix4f modelMatrix;
    
        private Camera camera = new Camera();
    
        public static void main(String[] args) {
            new PointCloudViewer().run(args[0]);
        }
    
        public void run(String filename) {
            initWindow();
            loadPointCloud(filename);
            initShaders();
            initBuffers();
            initMatrices(); // Create projection and other matrices
            loop();
            cleanup();
        }
    
        /**
        * Initialize GLFW window
        */
        private void initWindow() {
            GLFWErrorCallback.createPrint(System.err).set();
            if (!glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW");
            }
    
            //Mac Core Profile compatibility
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    
            window = glfwCreateWindow(800, 600, "PointCloudViewer", NULL, NULL);
            if (window == NULL) {
                throw new RuntimeException("Failed to create GLFW window");
            }
    
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1); // vsync
            glfwShowWindow(window);
    
            GL.createCapabilities();
            glViewport(0, 0, 800, 600);
            glClearColor(1, 1, 1, 1);
            glEnable(GL_DEPTH_TEST);
            
            // Get screen size
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    
            // Center the window on the screen
            glfwSetWindowPos(window,(vidmode.width() - 800) / 2,(vidmode.height() - 600) / 2);
        }
    
        /**
        * Load point cloud
        */
        private void loadPointCloud(String filename) {
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\s+");
                    if (parts.length >= 6) {
                        float x  = Float.parseFloat(parts[0]);
                        float y  = Float.parseFloat(parts[1]);
                        float z  = Float.parseFloat(parts[2]);
                        float nx = Float.parseFloat(parts[3]);
                        float ny = Float.parseFloat(parts[4]);
                        float nz = Float.parseFloat(parts[5]);
                        points.add(new float[]{x, y, z, nx, ny, nz});
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    
            //Normalize
            Normalize.normalizePoints(points);
            //for (float[] p : points) {
            //    System.out.println("x: " + p[0] + " y: " + p[1] + " z: " + p[2]);
            //}
            System.out.println("Loaded points = " + points.size());
            // RBF Reconstruction
            rbf = new RBFReconstruction(points);
            rbf.checkInterpolationAccuracy();
            List<float[]> surfacePoints = rbf.generateSurfacePoints(0.05f, 0.01f);
            System.out.println("Extracted " + surfacePoints.size() + " surface points.");
            // **MeshGenerator** (Dosen't work well)
            triangles = MeshGenerator.generateTriangles(surfacePoints);
            System.out.println("Generated " + triangles.size() + " triangles.");
    
            // Transfer mesh data to OpenGL
            initMeshBuffers(triangles);

    }

        /**
         * Compile & link shaders
         */
    private void initShaders() {
        // Vertex shader: Apply MVP transformation
        String vertexShaderSource =
            "#version 330 core\n" +
            "layout(location = 0) in vec3 aPos;\n" +
            "layout(location = 1) in vec3 aNormal\n;" +
            "uniform mat4 MVP;\n" + 
            "uniform mat4 modelMatrix;\n" +
            "out vec3 Normal;\n" +
            "out vec3 FragPos;\n" +
            "void main(){\n" +
            "   FragPos = vec3(modelMatrix * vec4(aPos, 1.0));\n" +
            "   Normal = mat3(transpose(inverse(modelMatrix))) * aNormal;\n" +
            "   gl_Position = MVP * vec4(aPos, 1.0);\n" +
            "}";

        // Fragment shader: Gray color
        String fragmentShaderSource =
            "#version 330 core\n" +
            "out vec4 FragColor;\n" +
            "void main(){\n" +
            "    FragColor = vec4(0.5, 0.5, 0.5, 1.0);\n" +
            "}";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertexShaderSource);
        glCompileShader(vs);
        checkShaderCompileStatus(vs, "VertexShader");

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragmentShaderSource);
        glCompileShader(fs);
        checkShaderCompileStatus(fs, "FragmentShader");

        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        checkProgramLinkStatus(program);

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

        /**
         * Create VAO / VBO and transfer data
         */
    private void initBuffers() {
        // Transfer only position data (x, y, z)
        float[] vertices = new float[points.size() * 6];
        for (int i = 0; i < points.size(); i++) {
            vertices[i * 6]     = points.get(i)[0]; // x
            vertices[i * 6 + 1] = points.get(i)[1]; // y
            vertices[i * 6 + 2] = points.get(i)[2]; // z
            vertices[i * 6 + 3] = points.get(i)[3]; // nx
            vertices[i * 6 + 4] = points.get(i)[4]; // ny
            vertices[i * 6 + 5] = points.get(i)[5]; // nz
        }
    

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        memFree(vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private int ebo; // Index buffer
    private int[] indices;

    private void initMeshBuffers(List<int[]> triangles) {
        // Create index array
        indices = new int[triangles.size() * 3];
        for (int i = 0; i < triangles.size(); i++) {
            indices[i * 3] = triangles.get(i)[0];
            indices[i * 3 + 1] = triangles.get(i)[1];
            indices[i * 3 + 2] = triangles.get(i)[2];
        }

        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
    
        memFree(indexBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    /**
     * Initialize projection, view, and model matrices
     * (Using JOML library)
     */
    private void initMatrices() {
        float fov = (float)Math.toRadians(45.0);
        float aspect = 800f / 600f;
        float near = 0.1f;
        float far = 100f;
        projectionMatrix = new Matrix4f().perspective(fov, aspect, near, far);

        // Model matrix (Update this for rotation and scaling)
        modelMatrix = new Matrix4f().identity();
    }

    /**
     * Main loop
     */
    private void loop() {
        glUseProgram(program);
        int mvpLocation = glGetUniformLocation(program, "MVP");

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); 
        glPointSize(5.0f);

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            camera.processInput(window);        //key operation
            camera.handleMouseInput(window);    //mouse operation
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Matrix4f viewMatrix = camera.getTransform();
            Matrix4f mvp = new Matrix4f();
            projectionMatrix.mul(viewMatrix, mvp);
            mvp.mul(modelMatrix);

            glUseProgram(program);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                mvp.get(fb);
                glUniformMatrix4fv(mvpLocation, false, fb);
            }

            glBindVertexArray(vao);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glDrawElements(GL_POINTS, indices.length, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);


            glfwSwapBuffers(window);
        }
    }
    
    
    /**
     * Cleanup process
     */
    private void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback ec = glfwSetErrorCallback(null);
        if (ec != null) {
            ec.free();
        }
    }

    private void checkShaderCompileStatus(int shaderId, String shaderName) {
        int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            String infoLog = glGetShaderInfoLog(shaderId);
            throw new RuntimeException(shaderName + " compile error:\n" + infoLog);
        }
    }

    private void checkProgramLinkStatus(int programId) {
        int status = glGetProgrami(programId, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            String infoLog = glGetProgramInfoLog(programId);
            throw new RuntimeException("Program link error:\n" + infoLog);
        }
    }
}
    