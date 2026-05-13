package com.sqe.service;

import com.sqe.entity.ComprehensiveEvaluation;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 簇命名服务 — 基于簇间排名和绝对分数阈值动态分配簇名称
 */
@Service
public class ClusterNamingService {

    /**
     * 基于跨簇排名 + 绝对分数门槛来判定类别名称
     */
    public String determineClusterName(double[] s, double[][] allAvgs,
                                        double lowThreshold, double highThreshold,
                                        Set<String> usedNames) {
        int K = allAvgs.length;
        int nDims = s.length;

        final double EXCELLENCE = 60.0;
        final double GOOD = 52.0;

        int[] rank = new int[nDims];
        for (int d = 0; d < nDims; d++) {
            int better = 0;
            for (double[] other : allAvgs) {
                if (other[d] > s[d]) better++;
            }
            rank[d] = better;
        }

        double myMean = mean(s);
        int meanRank = 0;
        for (double[] other : allAvgs) {
            if (mean(other) > myMean) meanRank++;
        }

        double cv = std(s) / myMean;
        String[] dimNames = {"品德突出型", "学术突出型", "体能突出型", "艺术特长型", "实践突出型"};

        int bestDim = 0;
        for (int d = 1; d < nDims; d++) {
            if (rank[d] < rank[bestDim] || (rank[d] == rank[bestDim] && s[d] > s[bestDim])) {
                bestDim = d;
            }
        }
        boolean bestDimQualifies = s[bestDim] >= EXCELLENCE && rank[bestDim] <= K / 2;

        int topN = Math.max(1, K / 3);

        List<String> candidates = new ArrayList<>();

        if (meanRank <= topN) {
            boolean allGood = true;
            for (int d = 0; d < nDims; d++) {
                if (s[d] < GOOD) { allGood = false; break; }
            }
            boolean allTopHalf = true;
            for (int r : rank) {
                if (r > K / 2) { allTopHalf = false; break; }
            }
            if (allGood && allTopHalf && cv < 0.22) candidates.add("全面优秀型");
            if (s[0] >= GOOD && s[1] >= GOOD && rank[0] <= K / 2 && rank[1] <= K / 2)
                candidates.add("品学兼优型");
            if (s[1] >= EXCELLENCE && s[3] >= GOOD && rank[1] <= topN && rank[3] <= topN)
                candidates.add("学术艺术型");
            if (s[0] >= EXCELLENCE && s[3] >= GOOD && rank[0] <= topN && rank[3] <= topN)
                candidates.add("德艺双馨型");
            if (s[2] >= EXCELLENCE && s[4] >= GOOD && rank[2] <= topN && rank[4] <= topN)
                candidates.add("体能实践型");
            if (bestDimQualifies) candidates.add(dimNames[bestDim]);
            candidates.add("均衡发展型");
        } else if (meanRank >= K - 1 - topN) {
            if (myMean < lowThreshold) candidates.add("待提升型");
            if (myMean < highThreshold) candidates.add("基础提升型");
            int worstDim = 0;
            for (int d = 1; d < nDims; d++) {
                if (rank[d] > rank[worstDim]) worstDim = d;
            }
            String[] weakLabels = {"德育待加强型", "学业待加强型", "体能待加强型", "美育待加强型", "实践待加强型"};
            if (rank[worstDim] >= K - 1 && s[worstDim] < GOOD) candidates.add(weakLabels[worstDim]);
            candidates.add("基础提升型");
            candidates.add("待提升型");
        } else {
            if (cv < 0.20) candidates.add("均衡发展型");
            if (s[1] >= EXCELLENCE && rank[1] <= topN) candidates.add("学术偏强型");
            if (s[0] >= GOOD && s[1] >= GOOD && rank[0] <= K / 2 && rank[1] <= K / 2)
                candidates.add("均衡发展型");
            if (bestDimQualifies) candidates.add(dimNames[bestDim]);
            candidates.add("均衡发展型");
            candidates.add("基础提升型");
        }

        for (String name : candidates) {
            if (!usedNames.contains(name)) {
                usedNames.add(name);
                return name;
            }
        }
        String fallback = "均衡发展型";
        int suffix = 2;
        while (usedNames.contains(fallback)) {
            fallback = "均衡发展型" + suffix;
            suffix++;
        }
        usedNames.add(fallback);
        return fallback;
    }

    /**
     * 按原始5维均分从高到低排序簇索引
     */
    public List<Integer> sortClustersByAvgScore(double[][] allAvgs) {
        List<double[]> items = new ArrayList<>();
        for (int i = 0; i < allAvgs.length; i++) {
            items.add(new double[]{i, mean(allAvgs[i])});
        }
        items.sort((a, b) -> Double.compare(b[1], a[1]));
        return items.stream().map(c -> (int) c[0]).collect(Collectors.toList());
    }

    /**
     * 基于簇均值的百分位数动态计算阈值
     */
    public double[] calculateDynamicThresholds(double[][] allAvgs) {
        double[] means = new double[allAvgs.length];
        for (int i = 0; i < allAvgs.length; i++) means[i] = mean(allAvgs[i]);
        Arrays.sort(means);
        int lowIdx = Math.max(0, allAvgs.length / 3);
        int highIdx = Math.min(allAvgs.length - 1, 2 * allAvgs.length / 3);
        double lowThreshold = means[lowIdx];
        double highThreshold = means[highIdx];
        lowThreshold = Math.max(50, Math.min(70, lowThreshold));
        highThreshold = Math.max(60, Math.min(80, highThreshold));
        return new double[]{lowThreshold, highThreshold};
    }

    /**
     * 计算簇在原始5维空间上的均值
     */
    public double[] calculateClusterAvg(List<ComprehensiveEvaluation> evaluations,
                                         List<DoublePoint> allPoints,
                                         List<DoublePoint> clusterPoints) {
        double[] avg = new double[5];
        int count = 0;
        for (DoublePoint point : clusterPoints) {
            int idx = allPoints.indexOf(point);
            if (idx >= 0 && idx < evaluations.size()) {
                ComprehensiveEvaluation e = evaluations.get(idx);
                avg[0] += e.getMoralScore().doubleValue();
                avg[1] += e.getAcademicScore().doubleValue();
                avg[2] += e.getPhysicalScore().doubleValue();
                avg[3] += e.getArtScore().doubleValue();
                avg[4] += e.getPracticeScore().doubleValue();
                count++;
            }
        }
        if (count > 0) {
            for (int i = 0; i < 5; i++) avg[i] /= count;
        }
        return avg;
    }

    public String generateClusterDescription(String name, double[] avgScores) {
        return String.format("「%s」群体特征：德育%.1f分，智育%.1f分，体育%.1f分，美育%.1f分，劳动教育%.1f分。",
                name, avgScores[0], avgScores[1], avgScores[2], avgScores[3], avgScores[4]);
    }

    public double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    public double std(double[] arr) {
        double m = mean(arr);
        double sumSq = 0;
        for (double v : arr) sumSq += Math.pow(v - m, 2);
        return Math.sqrt(sumSq / arr.length);
    }
}
