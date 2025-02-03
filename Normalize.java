import java.util.List;

public class Normalize {
    
    public static void normalizePoints(List<float[]> points) {
        if (points.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

        // Calculate minimum and maximum values
        for (float[] p : points) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            minZ = Math.min(minZ, p[2]);
            maxZ = Math.max(maxZ, p[2]);
        }

        // Normalize each coordinate to the range [-1,1]
        for (float[] p : points) {
            p[0] = (p[0] - minX) / (maxX - minX) * 2 - 1;
            p[1] = (p[1] - minY) / (maxY - minY) * 2 - 1;
            p[2] = (p[2] - minZ) / (maxZ - minZ) * 2 - 1;
        }
    }
}
