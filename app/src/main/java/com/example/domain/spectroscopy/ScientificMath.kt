package com.example.domain.spectroscopy

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure numerical mathematics utilities for spectroscopy:
 * - Savitzky-Golay filter coefficients calculation for arbitrary window and order
 * - Matrix inversion and polynomial fitting
 * - Asymmetric Least Squares (AsLS) Whittaker smoother
 * - Convex hull / Rubberband baseline
 */
object ScientificMath {

    /**
     * Compute Savitzky-Golay convolution coefficients for arbitrary odd window size and polynomial order.
     * derivativeOrder: 0 for smoothing, 1 for 1st derivative, 2 for 2nd derivative.
     * Returns an array of weights of length [windowSize].
     */
    fun computeSavitzkyGolayCoefficients(
        windowSize: Int,
        polyOrder: Int,
        derivativeOrder: Int = 0
    ): DoubleArray {
        val m = windowSize / 2
        val n = 2 * m + 1
        val k = polyOrder.coerceIn(1, n - 1)

        // Matrix A: n x (k+1), A[i, j] = i^j where i in -m..m
        val a = Array(n) { DoubleArray(k + 1) }
        for (i in -m..m) {
            val rowIdx = i + m
            var p = 1.0
            for (j in 0..k) {
                a[rowIdx][j] = p
                p *= i
            }
        }

        // Compute ATA = A^T * A of dimension (k+1) x (k+1)
        val ata = Array(k + 1) { DoubleArray(k + 1) }
        for (r in 0..k) {
            for (c in 0..k) {
                var sum = 0.0
                for (row in 0 until n) {
                    sum += a[row][r] * a[row][c]
                }
                ata[r][c] = sum
            }
        }

        // Invert ATA using Gauss-Jordan elimination
        val invAta = invertSquareMatrix(ata) ?: return DoubleArray(n) { 1.0 / n }

        // Row of interest corresponding to derivative order
        val targetRow = derivativeOrder.coerceIn(0, k)
        val weights = DoubleArray(n)

        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0..k) {
                sum += invAta[targetRow][j] * a[i][j]
            }
            weights[i] = sum
        }

        // Multiply by factorial for derivative scaling
        val factorial = when (derivativeOrder) {
            1 -> 1.0
            2 -> 2.0
            else -> 1.0
        }
        if (factorial != 1.0) {
            for (i in 0 until n) {
                weights[i] *= factorial
            }
        }

        return weights
    }

    /**
     * Invert a small square matrix (up to 6x6) using Gauss-Jordan elimination with partial pivoting.
     */
    fun invertSquareMatrix(matrix: Array<DoubleArray>): Array<DoubleArray>? {
        val n = matrix.size
        val a = Array(n) { i -> DoubleArray(2 * n) { j -> if (j < n) matrix[i][j] else if (j - n == i) 1.0 else 0.0 } }

        for (i in 0 until n) {
            var pivotRow = i
            var maxVal = abs(a[i][i])
            for (k in i + 1 until n) {
                if (abs(a[k][i]) > maxVal) {
                    maxVal = abs(a[k][i])
                    pivotRow = k
                }
            }

            if (maxVal < 1e-12) return null // Singular matrix

            // Swap rows
            if (pivotRow != i) {
                val temp = a[i]
                a[i] = a[pivotRow]
                a[pivotRow] = temp
            }

            val pivot = a[i][i]
            for (j in 0 until 2 * n) {
                a[i][j] /= pivot
            }

            for (k in 0 until n) {
                if (k != i) {
                    val factor = a[k][i]
                    for (j in 0 until 2 * n) {
                        a[k][j] -= factor * a[i][j]
                    }
                }
            }
        }

        val result = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                result[i][j] = a[i][j + n]
            }
        }
        return result
    }

    /**
     * Fit a 1D polynomial of specified degree to (x, y) coordinates using ordinary least squares.
     * Returns the polynomial coefficients [c0, c1, c2, ...] such that P(x) = c0 + c1*x + c2*x^2 + ...
     */
    fun fitPolynomial(x: DoubleArray, y: DoubleArray, degree: Int): DoubleArray {
        val n = x.size
        val deg = degree.coerceIn(1, minOf(5, n - 1))

        // Normalize X to [-1, 1] to prevent ill-conditioning with high powers
        val xMin = x.minOrNull() ?: 0.0
        val xMax = x.maxOrNull() ?: 1.0
        val xSpan = if (xMax - xMin > 0) xMax - xMin else 1.0

        val u = DoubleArray(n) { (x[it] - xMin) / xSpan }

        val a = Array(deg + 1) { DoubleArray(deg + 1) }
        val b = DoubleArray(deg + 1)

        for (i in 0..deg) {
            for (j in 0..deg) {
                var sum = 0.0
                for (p in 0 until n) {
                    sum += u[p].pow(i + j)
                }
                a[i][j] = sum
            }
            var bSum = 0.0
            for (p in 0 until n) {
                bSum += y[p] * u[p].pow(i)
            }
            b[i] = bSum
        }

        val invA = invertSquareMatrix(a) ?: DoubleArray(deg + 1) { y.average() / (deg + 1) }.let { return it }
        val uCoeffs = DoubleArray(deg + 1)
        for (i in 0..deg) {
            var sum = 0.0
            for (j in 0..deg) {
                sum += invA[i][j] * b[j]
            }
            uCoeffs[i] = sum
        }

        // Return evaluation function wrapper
        return uCoeffs // Note: evaluated using normalized u = (x - xMin) / xSpan
    }

    fun evaluateNormalizedPolynomial(uCoeffs: DoubleArray, x: Double, xMin: Double, xSpan: Double): Double {
        val u = if (xSpan > 0) (x - xMin) / xSpan else 0.0
        var res = 0.0
        var p = 1.0
        for (c in uCoeffs) {
            res += c * p
            p *= u
        }
        return res
    }

    /**
     * Asymmetric Least Squares (AsLS) Baseline Estimator (Eilers and Boelens, 2005).
     * Solves (W + lambda * D^T * D) * z = W * y iteratively.
     * Uses a fast band-diagonal matrix solver for O(N) execution time.
     */
    fun computeAsymmetricLeastSquaresBaseline(
        y: DoubleArray,
        smoothnessLambda: Double = 100000.0,
        asymmetryP: Double = 0.01,
        maxIterations: Int = 10
    ): DoubleArray {
        val n = y.size
        if (n < 5) return y.clone()

        val z = y.clone()
        val w = DoubleArray(n) { 1.0 }

        // Second difference matrix D has diagonal elements [1, -2, 1]
        // D^T * D is a pentadiagonal symmetric matrix with diagonal bandwidth 2:
        // Diagonals: d0 (main), d1 (offset 1), d2 (offset 2)
        val d0 = DoubleArray(n)
        val d1 = DoubleArray(n - 1)
        val d2 = DoubleArray(n - 2)

        for (i in 0 until n) {
            var val0 = 0.0
            if (i < n - 2) val0 += 1.0
            if (i > 0 && i < n - 1) val0 += 4.0
            if (i > 1) val0 += 1.0
            d0[i] = val0 * smoothnessLambda
        }
        for (i in 0 until n - 1) {
            var val1 = 0.0
            if (i < n - 2) val1 += -2.0
            if (i > 0) val1 += -2.0
            d1[i] = val1 * smoothnessLambda
        }
        for (i in 0 until n - 2) {
            d2[i] = 1.0 * smoothnessLambda
        }

        val a0 = DoubleArray(n)
        val a1 = DoubleArray(n - 1)
        val a2 = DoubleArray(n - 2)
        val rhs = DoubleArray(n)

        for (iter in 0 until maxIterations) {
            // Update weights: w_i = p if y_i > z_i else (1 - p)
            for (i in 0 until n) {
                w[i] = if (y[i] > z[i]) asymmetryP else (1.0 - asymmetryP)
                rhs[i] = w[i] * y[i]
                a0[i] = w[i] + d0[i]
            }
            for (i in 0 until n - 1) a1[i] = d1[i]
            for (i in 0 until n - 2) a2[i] = d2[i]

            // Solve pentadiagonal system A * z = rhs using Cholesky LDL^T factorization
            solvePentadiagonal(a0, a1, a2, rhs, z)
        }

        return z
    }

    /**
     * Solves a symmetric positive-definite pentadiagonal system in O(N) using LDL^T decomposition.
     */
    private fun solvePentadiagonal(
        diag: DoubleArray,
        sub1: DoubleArray,
        sub2: DoubleArray,
        b: DoubleArray,
        x: DoubleArray
    ) {
        val n = diag.size
        val d = DoubleArray(n)
        val l1 = DoubleArray(n - 1)
        val l2 = DoubleArray(n - 2)

        // Factorization LDL^T
        d[0] = diag[0]
        if (n > 1) {
            l1[0] = sub1[0] / d[0]
            d[1] = diag[1] - l1[0] * l1[0] * d[0]
        }

        for (i in 2 until n) {
            l2[i - 2] = sub2[i - 2] / d[i - 2]
            l1[i - 1] = (sub1[i - 1] - l1[i - 2] * l2[i - 2] * d[i - 2]) / d[i - 1]
            d[i] = diag[i] - l1[i - 1] * l1[i - 1] * d[i - 1] - l2[i - 2] * l2[i - 2] * d[i - 2]
            if (d[i] <= 0.0) d[i] = 1e-6 // Regularization
        }

        // Forward substitution L * y = b
        val y = DoubleArray(n)
        y[0] = b[0]
        if (n > 1) y[1] = b[1] - l1[0] * y[0]
        for (i in 2 until n) {
            y[i] = b[i] - l1[i - 1] * y[i - 1] - l2[i - 2] * y[i - 2]
        }

        // Diagonal solve D * v = y
        for (i in 0 until n) {
            y[i] /= d[i]
        }

        // Backward substitution L^T * x = v
        x[n - 1] = y[n - 1]
        if (n > 1) x[n - 2] = y[n - 2] - l1[n - 2] * x[n - 1]
        for (i in n - 3 downTo 0) {
            x[i] = y[i] - l1[i] * x[i + 1] - l2[i] * x[i + 2]
        }
    }

    /**
     * Compute Rubberband baseline (lower convex envelope) piecewise linear interpolation.
     */
    fun computeRubberbandBaseline(
        x: DoubleArray,
        y: DoubleArray,
        numSegments: Int = 24
    ): DoubleArray {
        val n = x.size
        if (n < 4) return y.clone()

        // 1. Find local minima in segments
        val segmentSize = (n / numSegments).coerceAtLeast(3)
        val anchorIndices = mutableListOf<Int>()
        anchorIndices.add(0)

        var idx = 0
        while (idx < n) {
            val end = (idx + segmentSize).coerceAtMost(n)
            var minVal = Double.MAX_VALUE
            var minIdx = idx
            for (j in idx until end) {
                if (y[j] < minVal) {
                    minVal = y[j]
                    minIdx = j
                }
            }
            if (minIdx !in anchorIndices) {
                anchorIndices.add(minIdx)
            }
            idx = end
        }
        if (anchorIndices.last() != n - 1) {
            anchorIndices.add(n - 1)
        }
        anchorIndices.sort()

        // 2. Compute lower convex hull of anchor points (Graham scan / monotonic chain for lower hull)
        val hull = mutableListOf<Int>()
        for (i in anchorIndices) {
            while (hull.size >= 2) {
                val p1 = hull[hull.size - 2]
                val p2 = hull[hull.size - 1]
                // Cross product to check turn: (x2 - x1)*(y3 - y1) - (y2 - y1)*(x3 - x1)
                val cross = (x[p2] - x[p1]) * (y[i] - y[p1]) - (y[p2] - y[p1]) * (x[i] - x[p1])
                if (cross <= 0) {
                    // Left turn or collinear for lower hull, pop p2
                    hull.removeAt(hull.size - 1)
                } else {
                    break
                }
            }
            hull.add(i)
        }

        // 3. Linearly interpolate between hull vertices across all x points
        val baseline = DoubleArray(n)
        for (h in 0 until hull.size - 1) {
            val startIdx = hull[h]
            val endIdx = hull[h + 1]
            val x0 = x[startIdx]
            val y0 = y[startIdx]
            val x1 = x[endIdx]
            val y1 = y[endIdx]
            val span = if (x1 - x0 != 0.0) x1 - x0 else 1.0

            for (i in startIdx..endIdx) {
                val t = (x[i] - x0) / span
                baseline[i] = y0 + t * (y1 - y0)
            }
        }

        return baseline
    }

    /**
     * Apply Savitzky-Golay filter to a 1D array.
     * Handles odd window sizes and polynomial orders with parameter validation.
     * derivativeOrder: 0 for smoothing, 1 for 1st derivative, 2 for 2nd derivative.
     * For derivatives, deltaX scales by 1 / (deltaX^derivativeOrder).
     */
    fun applySavitzkyGolayFilter(
        y: DoubleArray,
        windowSize: Int,
        polyOrder: Int,
        derivativeOrder: Int = 0,
        averageDeltaX: Double = 1.0
    ): DoubleArray {
        val n = y.size
        // Validate parameters
        val safeWin = if (windowSize % 2 == 0) windowSize + 1 else windowSize
        val clampedWin = safeWin.coerceIn(3, (n - 1) or 1)
        val clampedPoly = polyOrder.coerceIn(1, clampedWin - 1)

        val weights = computeSavitzkyGolayCoefficients(clampedWin, clampedPoly, derivativeOrder)
        val m = clampedWin / 2
        val result = DoubleArray(n)

        // Scale factor for derivative by physical step size Δx
        val stepScale = when (derivativeOrder) {
            1 -> if (abs(averageDeltaX) > 1e-9) 1.0 / averageDeltaX else 1.0
            2 -> if (abs(averageDeltaX) > 1e-9) 1.0 / (averageDeltaX * averageDeltaX) else 1.0
            else -> 1.0
        }

        // Interior points convolution
        for (i in m until (n - m)) {
            var sum = 0.0
            for (j in -m..m) {
                sum += weights[j + m] * y[i + j]
            }
            result[i] = sum * stepScale
        }

        // Boundary handling: mirror reflection for smooth edge continuation
        for (i in 0 until m) {
            var sum = 0.0
            for (j in -m..m) {
                val idx = i + j
                val yVal = if (idx in 0 until n) y[idx] else y[abs(idx).coerceAtMost(n - 1)]
                sum += weights[j + m] * yVal
            }
            result[i] = sum * stepScale
        }
        for (i in (n - m) until n) {
            var sum = 0.0
            for (j in -m..m) {
                val idx = i + j
                val yVal = if (idx in 0 until n) y[idx] else y[(2 * (n - 1) - idx).coerceIn(0, n - 1)]
                sum += weights[j + m] * yVal
            }
            result[i] = sum * stepScale
        }

        return result
    }

    /**
     * Numerical integration using the trapezoidal rule.
     * ∫ y dx ≈ Σ 0.5 * (y[i] + y[i+1]) * |x[i+1] - x[i]|
     */
    fun integrateTrapezoidal(
        x: DoubleArray,
        y: DoubleArray,
        startIdx: Int = 0,
        endIdx: Int = x.size - 1
    ): Double {
        val start = startIdx.coerceIn(0, x.lastIndex)
        val end = endIdx.coerceIn(start, x.lastIndex)
        if (end <= start) return 0.0

        var sum = 0.0
        for (i in start until end) {
            val dx = abs(x[i + 1] - x[i])
            val avgY = (y[i] + y[i + 1]) / 2.0
            sum += avgY * dx
        }
        return sum
    }

    /**
     * Compute L2 Euclidean Vector Norm: ||y||_2 = sqrt(sum(y_i^2))
     */
    fun computeVectorNorm(y: DoubleArray): Double {
        var sumSq = 0.0
        for (v in y) {
            sumSq += v * v
        }
        return sqrt(sumSq)
    }

    /**
     * Standard Normal Variate (SNV) transformation: (y - mean) / stdDev
     */
    fun computeStandardNormalVariate(y: DoubleArray): DoubleArray {
        val n = y.size
        if (n <= 1) return y.clone()
        val mean = y.average()
        var varianceSum = 0.0
        for (v in y) {
            val diff = v - mean
            varianceSum += diff * diff
        }
        val stdDev = sqrt(varianceSum / (n - 1)).coerceAtLeast(1e-9)
        return DoubleArray(n) { (y[it] - mean) / stdDev }
    }
}
