import java.util.ArrayList;
import java.util.List;

public class RBFReconstruction {
    private final List<float[]> points;
    private double[][] A;
    private double[] lambda;

    public RBFReconstruction(List<float[]> points) {
        this.points = points;
        computeRBF();
    }

    private void computeRBF() {
        int n = points.size();
        A = new double[n][n];
        double[] y = new double[n];

        // Create RBF matrix A
        for (int i = 0; i < n; i++) {
            float[] pi = points.get(i);
            y[i] = 0; // For on-surface points, f(x) = 0

            for (int j = 0; j < n; j++) {
                float[] pj = points.get(j);
                double r = Math.sqrt(
                    Math.pow(pi[0] - pj[0], 2) +
                    Math.pow(pi[1] - pj[1], 2) +
                    Math.pow(pi[2] - pj[2], 2)
                );
                A[i][j] = rbfFunction(r);
            }
        }

        // Solve Aλ = y using Gaussian elimination
        lambda = gaussianElimination(A, y);
    }

    // Solve linear system using Gaussian elimination
    private double[] gaussianElimination(double[][] A, double[] y) {
        int n = y.length;
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            // select pivot
            int max = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(A[k][i]) > Math.abs(A[max][i])) {
                    max = k;
                }
            }
            double[] temp = A[i]; A[i] = A[max]; A[max] = temp;
            double t = y[i]; y[i] = y[max]; y[max] = t;

            // forward elimination
            for (int k = i + 1; k < n; k++) {
                double factor = A[k][i] / A[i][i];
                y[k] -= factor * y[i];
                for (int j = i; j < n; j++) {
                    A[k][j] -= factor * A[i][j];
                }
            }
        }

        // backward substitution
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (y[i] - sum) / A[i][i];
        }
        return x;
    }

    // Interpolate points using RBF
    public double evaluate(float[] p) {
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            float[] pi = points.get(i);
            double r = Math.sqrt(
                Math.pow(p[0] - pi[0], 2) +
                Math.pow(p[1] - pi[1], 2) +
                Math.pow(p[2] - pi[2], 2)
            );
            sum += lambda[i] * rbfFunction(r);
        }
        return sum;
    }

        // RBF function (Gaussian, Thin Plate Spline, etc.)
        private double rbfFunction(double r) {
            double c = 0.1;
            return Math.sqrt(r * r + c * c);
        }

    // Determine if point p is on the surface
    public boolean isOnSurface(float[] p) {
        double eval = Math.abs(evaluate(p));
        double threshold = 0.01;  // Data-dependent threshold
        return eval < threshold;
    }
    
    public void checkInterpolationAccuracy() {
        double totalError = 0.0;
        for (float[] p : points) {
            double f_p = evaluate(p);
            System.out.println("f(" + p[0] + ", " + p[1] + ", " + p[2] + ") = " + f_p);
            totalError += Math.abs(f_p);
        }
        System.out.println("Total error: " + totalError);
    }

    public List<float[]> generateSurfacePoints(float gridSize, float threshold) {
    List<float[]> surfacePoints = new ArrayList<>();
    
    for (float x = -1; x <= 1; x += gridSize) {
        for (float y = -1; y <= 1; y += gridSize) {
            for (float z = -1; z <= 1; z += gridSize) {
                float[] p = {x, y, z};
                double f_p = evaluate(p);
                
                if (Math.abs(f_p) < threshold) {
                    surfacePoints.add(p);
                }
            }
        }
    }
    return surfacePoints;
}
}
