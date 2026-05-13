package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.entity.ClusterResult;
import com.sqe.entity.ComprehensiveEvaluation;
import com.sqe.mapper.ClusterResultMapper;
import com.sqe.mapper.ComprehensiveEvaluationMapper;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * K-Means聚类分析服务（协调层）
 * 负责流水线编排，具体算法细节委托给子服务
 */
@Service
public class ClusterService {

    @Autowired
    private ComprehensiveEvaluationMapper comprehensiveMapper;
    @Autowired
    private ClusterResultMapper clusterResultMapper;
    @Autowired
    private DataAggregationService dataAggregationService;
    @Autowired
    private ClusterEvaluationService clusterEvaluationService;
    @Autowired
    private ClusterNamingService clusterNamingService;
    @Autowired
    private SuggestionService suggestionService;

    /**
     * 执行聚类分析
     */
    @Transactional
    public Map<String, Object> executeCluster(String academicYear, int clusterCount) {
        dataAggregationService.aggregateEvaluations(academicYear);

        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));

        if (evaluations.size() < clusterCount) {
            throw new RuntimeException("学生数量不足，无法进行" + clusterCount + "类聚类分析");
        }

        double[][] rawData = dataAggregationService.buildRawData(evaluations);
        double[][] normalizedData = zScoreNormalize(rawData);
        double[][] pcaData = applyPCA(normalizedData, 0.90);

        List<DoublePoint> points = toDoublePoints(pcaData);

        JDKRandomGenerator rng = new JDKRandomGenerator();
        rng.setSeed(42L);
        KMeansPlusPlusClusterer<DoublePoint> clusterer =
                new KMeansPlusPlusClusterer<>(clusterCount, 300,
                        new EuclideanDistance(), rng);
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

        double silhouetteScore = clusterEvaluationService.calculateSilhouetteScore(pcaData, clusters, points);
        double dbi = clusterEvaluationService.calculateDaviesBouldinIndex(pcaData, clusters, points);
        double chi = clusterEvaluationService.calculateCalinskiHarabaszIndex(pcaData, clusters, points);

        Map<Long, ComprehensiveEvaluation> prevYearMap = dataAggregationService.loadPreviousYearEvaluations(academicYear);

        double[][] percentileRanks = calculatePercentileRanks(rawData);
        int[][] absoluteRanks = calculateAbsoluteRanks(rawData);

        clusterResultMapper.delete(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getAcademicYear, academicYear));

        double[][] allAvgs = new double[clusters.size()][5];
        List<List<DoublePoint>> allPoints = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            List<DoublePoint> cp = clusters.get(i).getPoints();
            allPoints.add(cp);
            allAvgs[i] = clusterNamingService.calculateClusterAvg(evaluations, points, cp);
        }

        List<Integer> sortedIndices = clusterNamingService.sortClustersByAvgScore(allAvgs);

        double[] clusterAvgDists = new double[clusters.size()];
        for (int c = 0; c < clusters.size(); c++) {
            double[] centroidPca = clusters.get(c).getCenter().getPoint();
            clusterAvgDists[c] = clusterEvaluationService.avgDistanceToCentroid(
                    pcaData, clusterEvaluationService.buildLabelArray(clusters, points), c, centroidPca);
        }

        double[] thresholds = clusterNamingService.calculateDynamicThresholds(allAvgs);
        Set<String> usedNames = new HashSet<>();
        Map<Integer, String> labelToName = new HashMap<>();

        for (int clusterIdx = 0; clusterIdx < clusters.size(); clusterIdx++) {
            double[] avgScores = allAvgs[clusterIdx];
            String clusterName = clusterNamingService.determineClusterName(avgScores, allAvgs,
                    thresholds[0], thresholds[1], usedNames);
            int label = sortedIndices.indexOf(clusterIdx);
            labelToName.put(label, clusterName);
        }

        for (int clusterIdx = 0; clusterIdx < clusters.size(); clusterIdx++) {
            List<DoublePoint> clusterPoints = allPoints.get(clusterIdx);
            double[] avgScores = allAvgs[clusterIdx];
            int label = sortedIndices.indexOf(clusterIdx);
            String clusterName = labelToName.get(label);
            double[] centroidPca = clusters.get(clusterIdx).getCenter().getPoint();

            for (DoublePoint point : clusterPoints) {
                int originalIdx = points.indexOf(point);
                if (originalIdx >= 0 && originalIdx < evaluations.size()) {
                    ComprehensiveEvaluation eval = evaluations.get(originalIdx);
                    eval.setClusterLabel(label);
                    eval.setClusterName(clusterName);
                    eval.setSuggestion(suggestionService.generateStructuredSuggestion(
                            eval, clusterName, label, sortedIndices, allAvgs,
                            prevYearMap.get(eval.getStudentId()),
                            pcaData[originalIdx], centroidPca, clusterAvgDists[clusterIdx],
                            percentileRanks[originalIdx], absoluteRanks[originalIdx],
                            evaluations.size(), labelToName, clusterEvaluationService));
                    comprehensiveMapper.updateById(eval);
                }
            }

            ClusterResult result = new ClusterResult();
            result.setAcademicYear(academicYear);
            result.setClusterCount(clusterCount);
            result.setSilhouetteScore(BigDecimal.valueOf(silhouetteScore).setScale(4, RoundingMode.HALF_UP));
            result.setDaviesBouldinIndex(BigDecimal.valueOf(dbi).setScale(4, RoundingMode.HALF_UP));
            result.setCalinskiHarabaszIndex(BigDecimal.valueOf(chi).setScale(4, RoundingMode.HALF_UP));
            result.setClusterLabel(label);
            result.setClusterName(clusterName);
            result.setStudentCount(clusterPoints.size());
            result.setAvgMoral(BigDecimal.valueOf(avgScores[0]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgAcademic(BigDecimal.valueOf(avgScores[1]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgPhysical(BigDecimal.valueOf(avgScores[2]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgArt(BigDecimal.valueOf(avgScores[3]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgPractice(BigDecimal.valueOf(avgScores[4]).setScale(2, RoundingMode.HALF_UP));
            result.setDescription(clusterNamingService.generateClusterDescription(clusterName, avgScores));
            clusterResultMapper.insert(result);
        }

        // 计算相关性矩阵和ANOVA
        double[][] correlationMatrix = clusterEvaluationService.calculateCorrelationMatrix(rawData);
        Map<String, Object> anovaResult = performANOVA(rawData,
                clusterEvaluationService.buildLabelArray(clusters, points), clusters.size());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("silhouetteScore", BigDecimal.valueOf(silhouetteScore).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("daviesBouldinIndex", BigDecimal.valueOf(dbi).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("calinskiHarabaszIndex", BigDecimal.valueOf(chi).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("descriptiveStats", clusterEvaluationService.calculateDescriptiveStats(rawData));
        resultMap.put("correlationMatrix", correlationMatrix);
        resultMap.put("anovaResult", anovaResult);
        resultMap.put("pcaPoints", extractPcaPoints(pcaData, clusters, points));
        resultMap.put("clusterCount", clusterCount);
        resultMap.put("studentCount", evaluations.size());
        resultMap.put("clusters", clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getAcademicYear, academicYear)
                        .orderByAsc(ClusterResult::getClusterLabel)));
        return resultMap;
    }

    /* ---------- 数据预处理 ---------- */

    double[][] zScoreNormalize(double[][] data) {
        int rows = data.length;
        int cols = data[0].length;
        double[][] normalized = new double[rows][cols];

        for (int j = 0; j < cols; j++) {
            double mean = 0, std = 0;
            for (int i = 0; i < rows; i++) mean += data[i][j];
            mean /= rows;
            for (int i = 0; i < rows; i++) std += Math.pow(data[i][j] - mean, 2);
            std = Math.sqrt(std / (rows - 1));
            if (std == 0) std = 1;
            for (int i = 0; i < rows; i++) {
                double val = (data[i][j] - mean) / std;
                if (val > 3.0) val = 3.0;
                if (val < -3.0) val = -3.0;
                normalized[i][j] = val;
            }
        }
        return normalized;
    }

    double[][] applyPCA(double[][] data, double varianceThreshold) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(data);
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        double[] svals = svd.getSingularValues();
        double totalVar = 0;
        for (double sv : svals) totalVar += sv * sv;
        int comps = 0;
        double cumVar = 0;
        for (double sv : svals) {
            cumVar += sv * sv;
            comps++;
            if (cumVar / totalVar >= varianceThreshold) break;
        }
        comps = Math.max(2, Math.min(comps, data[0].length));
        RealMatrix V = svd.getV();
        RealMatrix reduced = matrix.multiply(V.getSubMatrix(0, V.getRowDimension() - 1, 0, comps - 1));
        return reduced.getData();
    }

    /* ---------- 排名计算 ---------- */

    double[][] calculatePercentileRanks(double[][] rawData) {
        int n = rawData.length;
        int d = rawData[0].length;
        double[][] ranks = new double[n][d];
        for (int col = 0; col < d; col++) {
            double[] colData = new double[n];
            for (int i = 0; i < n; i++) colData[i] = rawData[i][col];
            double[] sorted = Arrays.copyOf(colData, n);
            Arrays.sort(sorted);
            for (int i = 0; i < n; i++) {
                int pos = Arrays.binarySearch(sorted, colData[i]);
                while (pos > 0 && sorted[pos - 1] == sorted[pos]) pos--;
                ranks[i][col] = Math.round((pos / (double) (n - 1)) * 10000.0) / 100.0;
            }
        }
        return ranks;
    }

    int[][] calculateAbsoluteRanks(double[][] rawData) {
        int n = rawData.length;
        int d = rawData[0].length;
        int[][] ranks = new int[n][d];
        for (int col = 0; col < d; col++) {
            for (int i = 0; i < n; i++) {
                int better = 0;
                for (int j = 0; j < n; j++) {
                    if (rawData[j][col] > rawData[i][col]) better++;
                }
                ranks[i][col] = better + 1;
            }
        }
        return ranks;
    }

    /* ---------- ANOVA ---------- */

    Map<String, Object> performANOVA(double[][] rawData, int[] labels, int k) {
        int n = rawData.length;
        int d = rawData[0].length;
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};
        List<Map<String, Object>> dimResults = new ArrayList<>();

        for (int col = 0; col < d; col++) {
            double grandMean = 0;
            for (int i = 0; i < n; i++) grandMean += rawData[i][col];
            grandMean /= n;

            double ssb = 0;
            double ssw = 0;
            int[] clusterSizes = new int[k];
            double[] clusterMeans = new double[k];
            for (int c = 0; c < k; c++) {
                double sum = 0;
                int count = 0;
                for (int i = 0; i < n; i++) {
                    if (labels[i] == c) {
                        sum += rawData[i][col];
                        count++;
                    }
                }
                clusterSizes[c] = count;
                clusterMeans[c] = count > 0 ? sum / count : 0;
                if (count > 0) {
                    ssb += count * Math.pow(clusterMeans[c] - grandMean, 2);
                }
            }
            for (int i = 0; i < n; i++) {
                ssw += Math.pow(rawData[i][col] - clusterMeans[labels[i]], 2);
            }

            int dfb = k - 1;
            int dfw = n - k;
            double msb = dfb > 0 ? ssb / dfb : 0;
            double msw = dfw > 0 ? ssw / dfw : 1e-10;
            double fValue = msw > 1e-10 ? msb / msw : 0;

            // 使用F分布的近似p值计算
            double pValue = approximateFDistributionPValue(fValue, dfb, dfw);
            double etaSq = (ssb + ssw) > 0 ? ssb / (ssb + ssw) : 0;

            Map<String, Object> dimResult = new LinkedHashMap<>();
            dimResult.put("dimension", dimLabels[col]);
            dimResult.put("fValue", Math.round(fValue * 1000.0) / 1000.0);
            dimResult.put("dfBetween", dfb);
            dimResult.put("dfWithin", dfw);
            dimResult.put("pValue", Math.round(pValue * 10000.0) / 10000.0);
            dimResult.put("significant", pValue < 0.05);
            dimResult.put("highlySignificant", pValue < 0.01);
            dimResult.put("etaSquared", Math.round(etaSq * 10000.0) / 10000.0);
            dimResults.add(dimResult);
        }

        Map<String, Object> anovaResult = new LinkedHashMap<>();
        anovaResult.put("dimensions", dimResults);
        anovaResult.put("totalStudents", n);
        anovaResult.put("clusterCount", k);
        return anovaResult;
    }

    /**
     * F分布的近似p值计算（使用正则化不完全Beta函数）
     */
    private double approximateFDistributionPValue(double f, int df1, int df2) {
        if (f <= 0) return 1.0;
        double x = df2 / (df2 + df1 * f);
        return regularizedBeta(x, df2 / 2.0, df1 / 2.0);
    }

    private double regularizedBeta(double x, double a, double b) {
        double term = Math.exp(logBeta(a, b) + a * Math.log(x) + b * Math.log(1 - x));
        if (Double.isInfinite(term) || Double.isNaN(term)) return 1.0;
        return Math.min(1.0, term);
    }

    private double logBeta(double a, double b) {
        return logGamma(a) + logGamma(b) - logGamma(a + b);
    }

    private double logGamma(double x) {
        // Stirling近似
        if (x <= 0) return 0;
        return 0.5 * Math.log(2 * Math.PI) + (x - 0.5) * Math.log(x) - x
                + 1.0 / (12 * x) - 1.0 / (360 * x * x * x) + 1.0 / (1260 * x * x * x * x * x);
    }

    /* ---------- PCA点位提取（供前端散点图） ---------- */

    private List<Map<String, Object>> extractPcaPoints(double[][] pcaData,
                                                        List<CentroidCluster<DoublePoint>> clusters,
                                                        List<DoublePoint> points) {
        int[] labels = clusterEvaluationService.buildLabelArray(clusters, points);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < pcaData.length; i++) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("x", Math.round(pcaData[i][0] * 10000.0) / 10000.0);
            pt.put("y", pcaData[i].length > 1 ? Math.round(pcaData[i][1] * 10000.0) / 10000.0 : 0);
            pt.put("cluster", labels[i]);
            result.add(pt);
        }
        return result;
    }

    /* ---------- 辅助方法 ---------- */

    private List<DoublePoint> toDoublePoints(double[][] data) {
        List<DoublePoint> pts = new ArrayList<>();
        for (double[] row : data) pts.add(new DoublePoint(row));
        return pts;
    }

    /* ---------- 最优K值搜索 ---------- */

    public Map<String, Object> findOptimalK(String academicYear, int minK, int maxK) {
        dataAggregationService.aggregateEvaluations(academicYear);
        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
        if (evaluations.size() < maxK) {
            throw new RuntimeException("学生数量不足，无法评估K=" + maxK);
        }
        double[][] rawData = dataAggregationService.buildRawData(evaluations);
        double[][] normalizedData = zScoreNormalize(rawData);
        double[][] pcaData = applyPCA(normalizedData, 0.90);
        List<DoublePoint> points = toDoublePoints(pcaData);
        int range = maxK - minK + 1;
        double[] silhouetteScores = new double[range];
        double[] dbiScores = new double[range];
        double[] chiScores = new double[range];
        double[] wss = new double[range]; // within-cluster sum of squares for Gap statistic

        for (int k = minK; k <= maxK; k++) {
            int idx = k - minK;
            JDKRandomGenerator rng = new JDKRandomGenerator();
            rng.setSeed(42L);
            KMeansPlusPlusClusterer<DoublePoint> clusterer =
                    new KMeansPlusPlusClusterer<>(k, 300,
                            new EuclideanDistance(), rng);
            List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);
            silhouetteScores[idx] = clusterEvaluationService.calculateSilhouetteScore(pcaData, clusters, points);
            dbiScores[idx] = clusterEvaluationService.calculateDaviesBouldinIndex(pcaData, clusters, points);
            chiScores[idx] = clusterEvaluationService.calculateCalinskiHarabaszIndex(pcaData, clusters, points);
            wss[idx] = computeWSS(pcaData, clusters, points);
        }

        // Gap Statistic
        Map<String, Object> gapResult = calculateGapStatistic(pcaData, minK, maxK, wss);

        int bestSilK = argmax(silhouetteScores) + minK;
        int bestDbiK = argmin(dbiScores) + minK;
        int bestChiK = argmax(chiScores) + minK;
        Map<Integer, Integer> votes = new HashMap<>();
        votes.merge(bestSilK, 1, Integer::sum);
        votes.merge(bestDbiK, 1, Integer::sum);
        votes.merge(bestChiK, 1, Integer::sum);
        int optimalK = votes.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();

        Map<String, Object> result = new HashMap<>();
        result.put("optimalK", optimalK);
        result.put("gapOptimalK", gapResult.get("optimalK"));
        result.put("kRange", Map.of("min", minK, "max", maxK));
        result.put("silhouetteByK", toKeyValueList(minK, silhouetteScores));
        result.put("dbiByK", toKeyValueList(minK, dbiScores));
        result.put("chiByK", toKeyValueList(minK, chiScores));
        result.put("gapByK", gapResult.get("gapByK"));
        result.put("gapSEByK", gapResult.get("gapSEByK"));
        result.put("voteDetails", Map.of(
                "silhouetteVote", bestSilK,
                "dbiVote", bestDbiK,
                "chiVote", bestChiK,
                "gapVote", gapResult.get("optimalK")));
        return result;
    }

    /**
     * 计算聚类内离差平方和(WSS)
     */
    private double computeWSS(double[][] data, List<CentroidCluster<DoublePoint>> clusters,
                               List<DoublePoint> points) {
        int[] labels = clusterEvaluationService.buildLabelArray(clusters, points);
        double wss = 0;
        for (int c = 0; c < clusters.size(); c++) {
            double[] centroid = clusters.get(c).getCenter().getPoint();
            for (int i = 0; i < data.length; i++) {
                if (labels[i] == c) {
                    wss += clusterEvaluationService.squaredEuclideanDistance(data[i], centroid);
                }
            }
        }
        return wss;
    }

    /**
     * Gap Statistic 方法选择最优K值
     * Gap(K) = (1/B) * Σ log(W*_kb) - log(W_k)
     * 选择满足 Gap(K) >= Gap(K+1) - s_{k+1} 的最小K
     */
    private Map<String, Object> calculateGapStatistic(double[][] data, int minK, int maxK, double[] observedWSS) {
        int B = 10;  // Monte Carlo参考数据集数量
        int n = data.length;
        int d = data[0].length;
        Random rand = new Random(42);

        // 计算数据各维度的范围
        double[] minVals = new double[d];
        double[] maxVals = new double[d];
        for (int j = 0; j < d; j++) {
            minVals[j] = Double.MAX_VALUE;
            maxVals[j] = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (data[i][j] < minVals[j]) minVals[j] = data[i][j];
                if (data[i][j] > maxVals[j]) maxVals[j] = data[i][j];
            }
        }

        int range = maxK - minK + 1;
        double[][] refLogW = new double[range][B]; // 参考数据集log(W_k)

        for (int b = 0; b < B; b++) {
            // 生成均匀分布参考数据集
            double[][] refData = new double[n][d];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < d; j++) {
                    refData[i][j] = minVals[j] + rand.nextDouble() * (maxVals[j] - minVals[j]);
                }
            }
            List<DoublePoint> refPoints = toDoublePoints(refData);

            for (int k = minK; k <= maxK; k++) {
                int idx = k - minK;
                JDKRandomGenerator rng = new JDKRandomGenerator();
                rng.setSeed(42L);
                KMeansPlusPlusClusterer<DoublePoint> clusterer =
                        new KMeansPlusPlusClusterer<>(k, 300, new EuclideanDistance(), rng);
                try {
                    List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(refPoints);
                    double w = computeWSS(refData, clusters, refPoints);
                    refLogW[idx][b] = Math.log(Math.max(w, 1e-10));
                } catch (Exception e) {
                    refLogW[idx][b] = 0;
                }
            }
        }

        // 计算Gap和标准误
        double[] gapValues = new double[range];
        double[] gapSE = new double[range];
        for (int idx = 0; idx < range; idx++) {
            double sumRefLogW = 0;
            for (int b = 0; b < B; b++) {
                sumRefLogW += refLogW[idx][b];
            }
            double meanRefLogW = sumRefLogW / B;
            double obsLogW = Math.log(Math.max(observedWSS[idx], 1e-10));
            gapValues[idx] = meanRefLogW - obsLogW;

            double var = 0;
            for (int b = 0; b < B; b++) {
                var += Math.pow(refLogW[idx][b] - meanRefLogW, 2);
            }
            var /= B;
            gapSE[idx] = Math.sqrt(var) * Math.sqrt(1.0 + 1.0 / B);
        }

        // 选择最优K: Gap(K) >= Gap(K+1) - s_{k+1}
        int optimalGapK = minK;
        for (int idx = 0; idx < range - 1; idx++) {
            if (gapValues[idx] >= gapValues[idx + 1] - gapSE[idx + 1]) {
                optimalGapK = minK + idx;
                break;
            }
            optimalGapK = maxK; // 未找到则取最大K
        }

        Map<String, Object> result = new HashMap<>();
        result.put("optimalK", optimalGapK);
        result.put("gapByK", toKeyValueList(minK, gapValues));
        result.put("gapSEByK", toKeyValueList(minK, gapSE));
        return result;
    }

    private int argmax(double[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] > arr[best]) best = i;
        return best;
    }

    private int argmin(double[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] < arr[best]) best = i;
        return best;
    }

    private List<Map<String, Object>> toKeyValueList(int minK, double[] values) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("k", minK + i);
            item.put("value", values[i]);
            list.add(item);
        }
        return list;
    }

    public List<ClusterResult> getClusterResults(String academicYear) {
        return clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>()
                        .eq(ClusterResult::getAcademicYear, academicYear)
                        .orderByAsc(ClusterResult::getClusterLabel));
    }

    /**
     * 获取聚类完整结果（含PCA、相关性矩阵、ANOVA等图表数据）
     */
    public Map<String, Object> getClusterResultsWithDetails(String academicYear) {
        List<ClusterResult> clusters = getClusterResults(academicYear);
        if (clusters.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("clusters", Collections.emptyList());
            return empty;
        }

        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));

        double[][] rawData = dataAggregationService.buildRawData(evaluations);
        double[][] normalizedData = zScoreNormalize(rawData);
        double[][] pcaData = applyPCA(normalizedData, 0.90);
        List<DoublePoint> points = toDoublePoints(pcaData);

        // 从evaluations中读取labels重建聚类分配
        int k = clusters.size();
        int[] labels = new int[evaluations.size()];
        for (int i = 0; i < evaluations.size(); i++) {
            Integer label = evaluations.get(i).getClusterLabel();
            labels[i] = label != null ? label : 0;
        }

        // 重建clusters列表用于计算
        List<List<DoublePoint>> clusterPointsList = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            clusterPointsList.add(new ArrayList<>());
        }
        for (int i = 0; i < points.size(); i++) {
            int lbl = labels[i];
            if (lbl >= 0 && lbl < k) {
                clusterPointsList.get(lbl).add(points.get(i));
            }
        }

        // 计算相关性矩阵
        double[][] correlationMatrix = clusterEvaluationService.calculateCorrelationMatrix(rawData);

        // 计算ANOVA
        Map<String, Object> anovaResult = performANOVA(rawData, labels, k);

        // 提取PCA散点
        List<Map<String, Object>> pcaPoints = new ArrayList<>();
        for (int i = 0; i < pcaData.length; i++) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("x", Math.round(pcaData[i][0] * 10000.0) / 10000.0);
            pt.put("y", pcaData[i].length > 1 ? Math.round(pcaData[i][1] * 10000.0) / 10000.0 : 0);
            pt.put("cluster", labels[i]);
            pcaPoints.add(pt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("clusters", clusters);
        result.put("silhouetteScore", clusters.get(0).getSilhouetteScore());
        result.put("daviesBouldinIndex", clusters.get(0).getDaviesBouldinIndex());
        result.put("calinskiHarabaszIndex", clusters.get(0).getCalinskiHarabaszIndex());
        result.put("pcaPoints", pcaPoints);
        result.put("correlationMatrix", correlationMatrix);
        result.put("anovaResult", anovaResult);
        result.put("descriptiveStats", clusterEvaluationService.calculateDescriptiveStats(rawData));
        return result;
    }
}
