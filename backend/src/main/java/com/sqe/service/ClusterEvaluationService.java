package com.sqe.service;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 聚类质量评估服务 — 轮廓系数、DBI、CHI、描述性统计、ANOVA等
 */
@Service
public class ClusterEvaluationService {

    /* ---------- 距离方法 ---------- */

    public double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    public double squaredEuclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return sum;
    }

    public int[] buildLabelArray(List<CentroidCluster<DoublePoint>> clusters,
                                  List<DoublePoint> points) {
        int[] labels = new int[points.size()];
        for (int c = 0; c < clusters.size(); c++) {
            for (DoublePoint p : clusters.get(c).getPoints()) {
                int idx = points.indexOf(p);
                if (idx >= 0) labels[idx] = c;
            }
        }
        return labels;
    }

    public double avgDistanceToCentroid(double[][] data, int[] labels,
                                         int clusterId, double[] centroid) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < data.length; i++) {
            if (labels[i] == clusterId) {
                sum += euclideanDistance(data[i], centroid);
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    /* ---------- 轮廓系数 ---------- */

    public double calculateSilhouetteScore(double[][] data,
                                            List<CentroidCluster<DoublePoint>> clusters,
                                            List<DoublePoint> points) {
        if (clusters.size() <= 1) return 0;
        int[] labels = buildLabelArray(clusters, points);
        double totalSilhouette = 0;
        for (int i = 0; i < data.length; i++) {
            double a = avgDistanceInCluster(data, labels, i);
            double b = minAvgDistanceToOtherCluster(data, labels, i, clusters.size());
            double s = (b - a) / Math.max(a, b);
            totalSilhouette += s;
        }
        return totalSilhouette / data.length;
    }

    private double avgDistanceInCluster(double[][] data, int[] labels, int idx) {
        int cluster = labels[idx];
        double sum = 0;
        int count = 0;
        for (int i = 0; i < data.length; i++) {
            if (i != idx && labels[i] == cluster) {
                sum += euclideanDistance(data[idx], data[i]);
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private double minAvgDistanceToOtherCluster(double[][] data, int[] labels, int idx, int k) {
        double minAvg = Double.MAX_VALUE;
        for (int c = 0; c < k; c++) {
            if (c == labels[idx]) continue;
            double sum = 0;
            int count = 0;
            for (int i = 0; i < data.length; i++) {
                if (labels[i] == c) {
                    sum += euclideanDistance(data[idx], data[i]);
                    count++;
                }
            }
            if (count > 0) {
                minAvg = Math.min(minAvg, sum / count);
            }
        }
        return minAvg == Double.MAX_VALUE ? 0 : minAvg;
    }

    /* ---------- Davies-Bouldin Index ---------- */

    public double calculateDaviesBouldinIndex(double[][] data,
                                               List<CentroidCluster<DoublePoint>> clusters,
                                               List<DoublePoint> points) {
        int k = clusters.size();
        if (k <= 1) return 0;
        double[][] centroids = new double[k][];
        double[] scatter = new double[k];
        int[] labels = buildLabelArray(clusters, points);
        for (int c = 0; c < k; c++) {
            centroids[c] = clusters.get(c).getCenter().getPoint();
            scatter[c] = avgDistanceToCentroid(data, labels, c, centroids[c]);
        }
        double sum = 0;
        for (int i = 0; i < k; i++) {
            double maxRatio = 0;
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                double dist = euclideanDistance(centroids[i], centroids[j]);
                if (dist < 1e-10) dist = 1e-10;
                double ratio = (scatter[i] + scatter[j]) / dist;
                if (ratio > maxRatio) maxRatio = ratio;
            }
            sum += maxRatio;
        }
        return sum / k;
    }

    /* ---------- Calinski-Harabasz Index ---------- */

    public double calculateCalinskiHarabaszIndex(double[][] data,
                                                  List<CentroidCluster<DoublePoint>> clusters,
                                                  List<DoublePoint> points) {
        int n = data.length;
        int k = clusters.size();
        if (k <= 1 || n <= k) return 0;
        int dim = data[0].length;
        double[] overallMean = new double[dim];
        for (double[] row : data) {
            for (int d = 0; d < dim; d++) overallMean[d] += row[d];
        }
        for (int d = 0; d < dim; d++) overallMean[d] /= n;
        int[] labels = buildLabelArray(clusters, points);
        double[][] centroids = new double[k][];
        int[] clusterSizes = new int[k];
        for (int c = 0; c < k; c++) {
            centroids[c] = clusters.get(c).getCenter().getPoint();
            clusterSizes[c] = clusters.get(c).getPoints().size();
        }
        double ssb = 0;
        for (int c = 0; c < k; c++) {
            ssb += clusterSizes[c] * squaredEuclideanDistance(centroids[c], overallMean);
        }
        double ssw = 0;
        for (int c = 0; c < k; c++) {
            for (int i = 0; i < n; i++) {
                if (labels[i] == c) {
                    ssw += squaredEuclideanDistance(data[i], centroids[c]);
                }
            }
        }
        return (ssb / (k - 1)) / (ssw / (n - k));
    }

    /* ---------- 描述性统计 ---------- */

    public Map<String, Object> calculateDescriptiveStats(double[][] rawData) {
        String[] dimNames = {"moral", "academic", "physical", "art", "practice"};
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};
        List<Map<String, Object>> dims = new ArrayList<>();
        for (int j = 0; j < rawData[0].length; j++) {
            double[] col = new double[rawData.length];
            for (int i = 0; i < rawData.length; i++) col[i] = rawData[i][j];
            double meanVal = Arrays.stream(col).average().orElse(0);
            double variance = Arrays.stream(col).map(v -> Math.pow(v - meanVal, 2)).average().orElse(0);
            double stdVal = Math.sqrt(variance);
            double cv = meanVal > 0 ? stdVal / meanVal : 0;
            double minVal = Arrays.stream(col).min().orElse(0);
            double maxVal = Arrays.stream(col).max().orElse(0);
            Map<String, Object> dim = new LinkedHashMap<>();
            dim.put("dimension", dimNames[j]);
            dim.put("label", dimLabels[j]);
            dim.put("mean", Math.round(meanVal * 100.0) / 100.0);
            dim.put("std", Math.round(stdVal * 100.0) / 100.0);
            dim.put("cv", Math.round(cv * 10000.0) / 10000.0);
            dim.put("min", Math.round(minVal * 100.0) / 100.0);
            dim.put("max", Math.round(maxVal * 100.0) / 100.0);
            dim.put("lowDiscrimination", cv < 0.10);
            dims.add(dim);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dimensions", dims);
        return result;
    }

    /* ---------- 相关性矩阵 ---------- */

    public double[][] calculateCorrelationMatrix(double[][] data) {
        int d = data[0].length;
        double[][] corr = new double[d][d];
        double[] means = new double[d];
        double[] stds = new double[d];
        for (int j = 0; j < d; j++) {
            double sum = 0;
            for (int i = 0; i < data.length; i++) sum += data[i][j];
            means[j] = sum / data.length;
            double sumSq = 0;
            for (int i = 0; i < data.length; i++) sumSq += Math.pow(data[i][j] - means[j], 2);
            stds[j] = Math.sqrt(sumSq / (data.length - 1));
        }
        for (int c1 = 0; c1 < d; c1++) {
            for (int c2 = 0; c2 < d; c2++) {
                if (c1 == c2) {
                    corr[c1][c2] = 1.0;
                    continue;
                }
                double cov = 0;
                for (int i = 0; i < data.length; i++) {
                    cov += (data[i][c1] - means[c1]) * (data[i][c2] - means[c2]);
                }
                cov /= (data.length - 1);
                double denom = stds[c1] * stds[c2];
                corr[c1][c2] = denom > 1e-10 ? Math.round(cov / denom * 10000.0) / 10000.0 : 0;
            }
        }
        return corr;
    }
}
