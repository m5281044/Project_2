import java.util.ArrayList;
import java.util.List;

public class MeshGenerator {
    public static List<int[]> generateTriangles(List<float[]> surfacePoints) {
        List<int[]> triangles = new ArrayList<>();
        
        // Could not fully complete due to lack of understanding
        // A simple method to manually connect nearby points as a workaround

        for (int i = 0; i < surfacePoints.size() - 2; i+=3) {
            triangles.add(new int[]{i, i+1, i+2});
        }
        
        return triangles;
    }
}
