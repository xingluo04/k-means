package com.sqe.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.dto.SuggestionData;
import com.sqe.entity.*;
import com.sqe.mapper.*;
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
import java.util.stream.Collectors;

/**
 * K-means聚类分析服务
 */
@Service
public class ClusterService {

    @Autowired
    private ComprehensiveEvaluationMapper comprehensiveMapper;
    @Autowired
    private ClusterResultMapper clusterResultMapper;
    @Autowired
    private MoralEvaluationMapper moralMapper;
    @Autowired
    private AcademicEvaluationMapper academicMapper;
    @Autowired
    private PhysicalEvaluationMapper physicalMapper;
    @Autowired
    private ArtEvaluationMapper artMapper;
    @Autowired
    private PracticeEvaluationMapper practiceMapper;

    /* 权重常量：德育25%，智育50%，体育8%，美育8%，劳动教育9% */
    private static final BigDecimal W_MORAL = new BigDecimal("0.25");
    private static final BigDecimal W_ACADEMIC = new BigDecimal("0.50");
    private static final BigDecimal W_PHYSICAL = new BigDecimal("0.08");
    private static final BigDecimal W_ART = new BigDecimal("0.08");
    private static final BigDecimal W_PRACTICE = new BigDecimal("0.09");

    /**
     * 执行聚类分析
     */
    @Transactional
    public Map<String, Object> executeCluster(String academicYear, int clusterCount) {
        aggregateEvaluations(academicYear);

        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));

        if (evaluations.size() < clusterCount) {
            throw new RuntimeException("学生数量不足，无法进行" + clusterCount + "类聚类分析");
        }

        double[][] rawData = new double[evaluations.size()][5];
        for (int i = 0; i < evaluations.size(); i++) {
            ComprehensiveEvaluation e = evaluations.get(i);
            rawData[i] = new double[]{
                    e.getMoralScore().doubleValue(),
                    e.getAcademicScore().doubleValue(),
                    e.getPhysicalScore().doubleValue(),
                    e.getArtScore().doubleValue(),
                    e.getPracticeScore().doubleValue()
            };
        }

        double[][] normalizedData = zScoreNormalize(rawData);

        /* PCA降维：保留≥90%方差的主成分，滤除低方差维度的噪声 */
        double[][] pcaData = applyPCA(normalizedData, 0.90);

        List<DoublePoint> points = new ArrayList<>();
        for (double[] row : pcaData) {
            points.add(new DoublePoint(row));
        }

        JDKRandomGenerator rng = new JDKRandomGenerator();
        rng.setSeed(42L);
        KMeansPlusPlusClusterer<DoublePoint> clusterer =
                new KMeansPlusPlusClusterer<>(clusterCount, 300,
                        new EuclideanDistance(), rng);
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

        double silhouetteScore = calculateSilhouetteScore(pcaData, clusters, points);
        double dbi = calculateDaviesBouldinIndex(pcaData, clusters, points);
        double chi = calculateCalinskiHarabaszIndex(pcaData, clusters, points);

        // 预加载上一年综合评价数据（用于成长趋势分析）
        Map<Long, ComprehensiveEvaluation> prevYearMap = loadPreviousYearEvaluations(academicYear);

        // 计算百分位排名和绝对排名（基于原始5维数据）
        double[][] percentileRanks = calculatePercentileRanks(rawData);
        int[][] absoluteRanks = calculateAbsoluteRanks(rawData);

        clusterResultMapper.delete(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getAcademicYear, academicYear));

        // 先收集每个簇的原始5维均值，用于排序和命名（不受PCA降维影响）
        double[][] allAvgs = new double[clusters.size()][5];
        List<List<DoublePoint>> allPoints = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            List<DoublePoint> cp = clusters.get(i).getPoints();
            allPoints.add(cp);
            allAvgs[i] = calculateClusterAvg(evaluations, points, cp);
        }

        // 按原始5维均分从高到低排序，label=0为最优
        List<Integer> sortedIndices = sortClustersByAvgScore(allAvgs);

        // 预计算每个簇的PCA中心点到簇内平均距离（用于典型度分析）
        double[] clusterAvgDists = new double[clusters.size()];
        for (int c = 0; c < clusters.size(); c++) {
            double[] centroidPca = clusters.get(c).getCenter().getPoint();
            clusterAvgDists[c] = avgDistanceToCentroid(pcaData, buildLabelArray(clusters, points), c, centroidPca);
        }

        double[] thresholds = calculateDynamicThresholds(allAvgs);
        Set<String> usedNames = new HashSet<>();
        Map<Integer, String> labelToName = new HashMap<>();

        // Phase 1: 确定所有簇的名称（构建label→name映射）
        for (int clusterIdx = 0; clusterIdx < clusters.size(); clusterIdx++) {
            double[] avgScores = allAvgs[clusterIdx];
            String clusterName = determineClusterName(avgScores, allAvgs,
                    thresholds[0], thresholds[1], usedNames);
            int label = sortedIndices.indexOf(clusterIdx);
            labelToName.put(label, clusterName);
        }

        // Phase 2: 分配标签、生成建议、保存结果
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
                    eval.setSuggestion(generateStructuredSuggestion(
                            eval, clusterName, label, sortedIndices, allAvgs,
                            prevYearMap.get(eval.getStudentId()),
                            pcaData[originalIdx], centroidPca, clusterAvgDists[clusterIdx],
                            percentileRanks[originalIdx], absoluteRanks[originalIdx],
                            evaluations.size(), labelToName));
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
            result.setDescription(generateClusterDescription(clusterName, avgScores));
            clusterResultMapper.insert(result);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("silhouetteScore", BigDecimal.valueOf(silhouetteScore).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("daviesBouldinIndex", BigDecimal.valueOf(dbi).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("calinskiHarabaszIndex", BigDecimal.valueOf(chi).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("descriptiveStats", calculateDescriptiveStats(rawData));
        resultMap.put("clusterCount", clusterCount);
        resultMap.put("studentCount", evaluations.size());
        resultMap.put("clusters", clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getAcademicYear, academicYear)
                        .orderByAsc(ClusterResult::getClusterLabel)));
        return resultMap;
    }

    /* 汇总各维度评价数据到综合评价表，使用加权总分 */
    private void aggregateEvaluations(String academicYear) {
        List<MoralEvaluation> morals = moralMapper.selectList(
                new LambdaQueryWrapper<MoralEvaluation>().eq(MoralEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> moralMap = morals.stream()
                .collect(Collectors.toMap(MoralEvaluation::getStudentId, MoralEvaluation::getTotalScore, (a, b) -> b));

        List<AcademicEvaluation> academics = academicMapper.selectList(
                new LambdaQueryWrapper<AcademicEvaluation>().eq(AcademicEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> academicMap = academics.stream()
                .collect(Collectors.toMap(AcademicEvaluation::getStudentId, AcademicEvaluation::getTotalScore, (a, b) -> b));

        List<PhysicalEvaluation> physicals = physicalMapper.selectList(
                new LambdaQueryWrapper<PhysicalEvaluation>().eq(PhysicalEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> physicalMap = physicals.stream()
                .collect(Collectors.toMap(PhysicalEvaluation::getStudentId, PhysicalEvaluation::getTotalScore, (a, b) -> b));

        List<ArtEvaluation> arts = artMapper.selectList(
                new LambdaQueryWrapper<ArtEvaluation>().eq(ArtEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> artMap = arts.stream()
                .collect(Collectors.toMap(ArtEvaluation::getStudentId, ArtEvaluation::getTotalScore, (a, b) -> b));

        List<PracticeEvaluation> practices = practiceMapper.selectList(
                new LambdaQueryWrapper<PracticeEvaluation>().eq(PracticeEvaluation::getAcademicYear, academicYear));
        Map<Long, BigDecimal> practiceMap = practices.stream()
                .collect(Collectors.toMap(PracticeEvaluation::getStudentId, PracticeEvaluation::getTotalScore, (a, b) -> b));

        Set<Long> allStudentIds = new HashSet<>();
        allStudentIds.addAll(moralMap.keySet());
        allStudentIds.addAll(academicMap.keySet());
        allStudentIds.addAll(physicalMap.keySet());
        allStudentIds.addAll(artMap.keySet());
        allStudentIds.addAll(practiceMap.keySet());

        for (Long studentId : allStudentIds) {
            BigDecimal moral = moralMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal academic = academicMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal physical = physicalMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal art = artMap.getOrDefault(studentId, BigDecimal.ZERO);
            BigDecimal practice = practiceMap.getOrDefault(studentId, BigDecimal.ZERO);

            /* 加权综合总分 = 德育×0.25 + 智育×0.50 + 体育×0.08 + 美育×0.08 + 劳动教育×0.09 */
            BigDecimal total = moral.multiply(W_MORAL)
                    .add(academic.multiply(W_ACADEMIC))
                    .add(physical.multiply(W_PHYSICAL))
                    .add(art.multiply(W_ART))
                    .add(practice.multiply(W_PRACTICE))
                    .setScale(2, RoundingMode.HALF_UP);

            ComprehensiveEvaluation existing = comprehensiveMapper.selectOne(
                    new LambdaQueryWrapper<ComprehensiveEvaluation>()
                            .eq(ComprehensiveEvaluation::getStudentId, studentId)
                            .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));

            if (existing != null) {
                existing.setMoralScore(moral);
                existing.setAcademicScore(academic);
                existing.setPhysicalScore(physical);
                existing.setArtScore(art);
                existing.setPracticeScore(practice);
                existing.setTotalScore(total);
                comprehensiveMapper.updateById(existing);
            } else {
                ComprehensiveEvaluation ce = new ComprehensiveEvaluation();
                ce.setStudentId(studentId);
                ce.setAcademicYear(academicYear);
                ce.setMoralScore(moral);
                ce.setAcademicScore(academic);
                ce.setPhysicalScore(physical);
                ce.setArtScore(art);
                ce.setPracticeScore(practice);
                ce.setTotalScore(total);
                comprehensiveMapper.insert(ce);
            }
        }
    }

    private double[][] zScoreNormalize(double[][] data) {
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

    /* PCA降维：保留累计方差贡献率 >= varianceThreshold 的主成分，最少保留2个 */
    private double[][] applyPCA(double[][] data, double varianceThreshold) {
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

    private double calculateSilhouetteScore(double[][] normalizedData,
                                             List<CentroidCluster<DoublePoint>> clusters,
                                             List<DoublePoint> points) {
        if (clusters.size() <= 1) return 0;

        int[] labels = new int[points.size()];
        for (int c = 0; c < clusters.size(); c++) {
            for (DoublePoint p : clusters.get(c).getPoints()) {
                int idx = points.indexOf(p);
                if (idx >= 0) labels[idx] = c;
            }
        }

        double totalSilhouette = 0;
        for (int i = 0; i < normalizedData.length; i++) {
            double a = avgDistanceInCluster(normalizedData, labels, i);
            double b = minAvgDistanceToOtherCluster(normalizedData, labels, i, clusters.size());
            double s = (b - a) / Math.max(a, b);
            totalSilhouette += s;
        }
        return totalSilhouette / normalizedData.length;
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

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    private double squaredEuclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return sum;
    }

    private int[] buildLabelArray(List<CentroidCluster<DoublePoint>> clusters,
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

    private double avgDistanceToCentroid(double[][] data, int[] labels,
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

    /* Davies-Bouldin Index：簇内散度与簇间距离之比的平均值，越小越好 */
    private double calculateDaviesBouldinIndex(double[][] data,
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

    /* Calinski-Harabasz Index：簇间方差与簇内方差之比，越大越好 */
    private double calculateCalinskiHarabaszIndex(double[][] data,
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

    /* 按原始5维均分从高到低排序簇索引 */
    private List<Integer> sortClustersByAvgScore(double[][] allAvgs) {
        List<double[]> items = new ArrayList<>();
        for (int i = 0; i < allAvgs.length; i++) {
            items.add(new double[]{i, mean(allAvgs[i])});
        }
        items.sort((a, b) -> Double.compare(b[1], a[1]));
        return items.stream().map(c -> (int) c[0]).collect(Collectors.toList());
    }

    /**
     * 基于跨簇排名 + 绝对分数门槛来判定类别名称。
     * "突出/特长型"名称要求该维度绝对分 >= excellenceThreshold，
     * "品学兼优型"/"全面优秀型"要求相关维度 >= goodThreshold。
     * 同时使用簇间相对排名和绝对分数，避免低分簇被命名为"特长型"。
     */
    private String determineClusterName(double[] s, double[][] allAvgs,
                                         double lowThreshold, double highThreshold,
                                         Set<String> usedNames) {
        int K = allAvgs.length;
        int nDims = s.length;

        // 绝对分数门槛（百分制）
        final double EXCELLENCE = 60.0;  // "突出/特长"至少需达此分
        final double GOOD = 52.0;        // "良好"至少需达此分

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

        // 最佳维度：排名最高且绝对分达标，否则不推荐维度突出型名称
        int bestDim = 0;
        for (int d = 1; d < nDims; d++) {
            if (rank[d] < rank[bestDim] || (rank[d] == rank[bestDim] && s[d] > s[bestDim])) {
                bestDim = d;
            }
        }
        boolean bestDimQualifies = s[bestDim] >= EXCELLENCE && rank[bestDim] <= K / 2;

        int topN = Math.max(1, K / 3);

        List<String> candidates = new ArrayList<>();

        // === 顶层：综合排名前 1/3 ===
        if (meanRank <= topN) {
            boolean allGood = true;
            for (int d = 0; d < nDims; d++) {
                if (s[d] < GOOD) { allGood = false; break; }
            }
            boolean allTopHalf = true;
            for (int r : rank) {
                if (r > K / 2) { allTopHalf = false; break; }
            }
            // 全面优秀型：所有维度达标 + 排名都在前半 + 低变异
            if (allGood && allTopHalf && cv < 0.22) candidates.add("全面优秀型");
            // 品学兼优型：德育智育达标且排名在前半
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
            // === 底层 ===
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
            // === 中间层 ===
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

    private double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private double std(double[] arr) {
        double m = mean(arr);
        double sumSq = 0;
        for (double v : arr) sumSq += Math.pow(v - m, 2);
        return Math.sqrt(sumSq / arr.length);
    }

    private double[] calculateClusterAvg(List<ComprehensiveEvaluation> evaluations,
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

    /* ========== 个性化建议生成（结构化JSON） ========== */

    private String generateStructuredSuggestion(ComprehensiveEvaluation eval, String clusterName,
                                                 int currentLabel, List<Integer> sortedIndices,
                                                 double[][] allAvgs,
                                                 ComprehensiveEvaluation prevEval,
                                                 double[] pcaPoint, double[] pcaCentroid,
                                                 double clusterAvgDist, double[] pctRanks,
                                                 int[] absRanks, int totalStudents,
                                                 Map<Integer, String> labelToName) {
        SuggestionData data = new SuggestionData();
        double[] scores = {eval.getMoralScore().doubleValue(), eval.getAcademicScore().doubleValue(),
                eval.getPhysicalScore().doubleValue(), eval.getArtScore().doubleValue(),
                eval.getPracticeScore().doubleValue()};
        String[] dimKeys = {"moral", "academic", "physical", "art", "practice"};
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};

        data.setClusterName(clusterName);

        // 优势与薄弱维度
        data.setStrengths(buildStrengths(scores, dimKeys, dimLabels, pctRanks, absRanks, totalStudents));
        data.setWeaknesses(buildWeaknesses(scores, dimKeys, dimLabels, pctRanks, absRanks, totalStudents));
        data.setSeverityLevel(overallSeverity(scores));

        // 一句话总结
        data.setSummary(buildSummary(clusterName, data.getStrengths(), data.getWeaknesses()));

        // 行动建议
        data.setActionItems(buildActionItems(clusterName, data.getWeaknesses(), data.getStrengths()));

        // 成长趋势
        if (prevEval != null) {
            data.setTrend(buildTrendInfo(eval, prevEval, clusterName));
        }

        // 提升路径（含顶级群体的正向反馈）
        if (sortedIndices != null && allAvgs != null) {
            data.setPromotion(buildPromotionPath(currentLabel, sortedIndices, allAvgs, scores, labelToName));
        }

        // 典型度
        if (pcaPoint != null && pcaCentroid != null) {
            data.setTypicality(buildTypicality(pcaPoint, pcaCentroid, clusterAvgDist, scores, dimLabels));
        }

        // 年级相对位置
        data.setPercentileRanks(buildDimRanks(dimKeys, dimLabels, absRanks, totalStudents));

        return JSONUtil.toJsonStr(data);
    }

    /* 加载上一学年综合评价数据 */
    private Map<Long, ComprehensiveEvaluation> loadPreviousYearEvaluations(String academicYear) {
        String prevYear = previousAcademicYear(academicYear);
        if (prevYear == null) return Collections.emptyMap();
        List<ComprehensiveEvaluation> prevList = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, prevYear));
        Map<Long, ComprehensiveEvaluation> map = new HashMap<>();
        for (ComprehensiveEvaluation e : prevList) {
            map.put(e.getStudentId(), e);
        }
        return map;
    }

    private String previousAcademicYear(String year) {
        if ("2024-2025".equals(year)) return "2023-2024";
        if ("2023-2024".equals(year)) return null;
        // 通用解析: "20XX-20YY" → "20(XX-1)-20(YY-1)"
        try {
            String[] parts = year.split("-");
            int start = Integer.parseInt(parts[0]);
            return (start - 1) + "-" + start;
        } catch (Exception e) {
            return null;
        }
    }

    /* 计算每个学生在5个维度的百分位排名（0-100，越高越好） */
    private double[][] calculatePercentileRanks(double[][] rawData) {
        int n = rawData.length;
        int d = rawData[0].length;
        double[][] ranks = new double[n][d];
        for (int col = 0; col < d; col++) {
            // 提取列数据并排序
            double[] colData = new double[n];
            for (int i = 0; i < n; i++) colData[i] = rawData[i][col];
            double[] sorted = Arrays.copyOf(colData, n);
            Arrays.sort(sorted);
            // 计算每个原始值的百分位
            for (int i = 0; i < n; i++) {
                int pos = Arrays.binarySearch(sorted, colData[i]);
                while (pos > 0 && sorted[pos - 1] == sorted[pos]) pos--;
                ranks[i][col] = Math.round((pos / (double) (n - 1)) * 10000.0) / 100.0;
            }
        }
        return ranks;
    }

    /* 计算绝对排名（1-based，1为最高），处理并列 */
    private int[][] calculateAbsoluteRanks(double[][] rawData) {
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

    /* --- 优势维度 --- */
    private List<SuggestionData.DimAdvice> buildStrengths(double[] scores, String[] dimKeys,
                                                            String[] dimLabels, double[] pctRanks,
                                                            int[] absRanks, int total) {
        List<SuggestionData.DimAdvice> result = new ArrayList<>();
        // 找出前2高分维度
        Integer[] idx = {0, 1, 2, 3, 4};
        Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));
        for (int i = 0; i < 2; i++) {
            int d = idx[i];
            if (scores[d] >= 60) {
                SuggestionData.DimAdvice adv = new SuggestionData.DimAdvice();
                adv.setDimKey(dimKeys[d]);
                adv.setDimLabel(dimLabels[d]);
                adv.setScore(Math.round(scores[d] * 100.0) / 100.0);
                adv.setPercentile(pctRanks != null ? pctRanks[d] : 0);
                adv.setRank(absRanks != null ? absRanks[d] : 0);
                adv.setTotal(total);
                String level = severityLevel(scores[d]);
                adv.setLevel(level);
                adv.setLevelLabel(severityLabel(level));
                adv.setSuggestion(strengthSuggestion(d, level));
                result.add(adv);
            }
        }
        return result;
    }

    /* --- 薄弱维度 --- */
    private List<SuggestionData.DimAdvice> buildWeaknesses(double[] scores, String[] dimKeys,
                                                             String[] dimLabels, double[] pctRanks,
                                                             int[] absRanks, int total) {
        List<SuggestionData.DimAdvice> result = new ArrayList<>();
        Integer[] idx = {0, 1, 2, 3, 4};
        Arrays.sort(idx, (a, b) -> Double.compare(scores[a], scores[b]));
        for (int i = 0; i < 3; i++) {
            int d = idx[i];
            if (scores[d] < 75) {
                SuggestionData.DimAdvice adv = new SuggestionData.DimAdvice();
                adv.setDimKey(dimKeys[d]);
                adv.setDimLabel(dimLabels[d]);
                adv.setScore(Math.round(scores[d] * 100.0) / 100.0);
                adv.setPercentile(pctRanks != null ? pctRanks[d] : 0);
                adv.setRank(absRanks != null ? absRanks[d] : 0);
                adv.setTotal(total);
                String level = severityLevel(scores[d]);
                adv.setLevel(level);
                adv.setLevelLabel(severityLabel(level));
                adv.setSuggestion(weaknessSuggestion(d, level));
                result.add(adv);
            }
        }
        return result;
    }

    /* --- 一句话总结 --- */
    private String buildSummary(String clusterName, List<SuggestionData.DimAdvice> strengths,
                                 List<SuggestionData.DimAdvice> weaknesses) {
        StringBuilder sb = new StringBuilder();
        sb.append("你属于「").append(clusterName).append("」群体。");
        if (!strengths.isEmpty()) {
            SuggestionData.DimAdvice top = strengths.get(0);
            sb.append(top.getDimLabel()).append("是你的优势维度（").append(top.getScore()).append("分），");
        }
        if (!weaknesses.isEmpty()) {
            SuggestionData.DimAdvice bottom = weaknesses.get(0);
            sb.append(bottom.getDimLabel()).append("是当前最需要提升的维度（").append(bottom.getScore()).append("分）。");
        }
        return sb.toString();
    }

    /* --- 行动建议 --- */
    private List<String> buildActionItems(String clusterName,
                                            List<SuggestionData.DimAdvice> weaknesses,
                                            List<SuggestionData.DimAdvice> strengths) {
        List<String> items = new ArrayList<>();

        // 基于聚类类别的高阶建议
        if (clusterName.contains("待提升") || clusterName.contains("基础提升")) {
            items.add("制定每日学习计划，确保每天各维度都有固定投入时间");
            items.add("主动寻求老师或同学的帮助，建立学习互助小组");
            if (!weaknesses.isEmpty()) {
                items.add("优先攻克" + weaknesses.get(0).getDimLabel() + "短板，设定每周小目标逐步提升");
            }
        } else if (clusterName.contains("均衡")) {
            items.add("在保持均衡发展的基础上，选择一个感兴趣的方向重点突破");
            if (!strengths.isEmpty()) {
                items.add("发挥" + strengths.get(0).getDimLabel() + "优势，参加相关竞赛或活动锻炼能力");
            }
            if (!weaknesses.isEmpty()) {
                items.add("关注" + weaknesses.get(0).getDimLabel() + "发展，每天额外投入20分钟专项练习");
            }
        } else if (clusterName.contains("优秀") || clusterName.contains("品学兼优")) {
            items.add("挑战更高目标：参加学科竞赛、科研项目或担任学生干部");
            items.add("带动身边同学共同进步，发挥榜样引领作用");
            if (!weaknesses.isEmpty()) {
                items.add("注意补齐" + weaknesses.get(0).getDimLabel() + "短板，避免偏科影响长远发展");
            }
        } else if (clusterName.contains("突出") || clusterName.contains("特长")) {
            items.add("继续强化优势维度，向专业级水平发展");
            items.add("尝试将优势能力迁移到其他维度，带动全面发展");
            if (!weaknesses.isEmpty()) {
                items.add(weaknesses.get(0).getDimLabel() + "方面建议寻求针对性辅导，缩小与优势维度的差距");
            }
        } else {
            if (!weaknesses.isEmpty()) {
                items.add("建议重点关注" + weaknesses.get(0).getDimLabel() + "，制定专项提升计划");
            }
            items.add("保持各维度均衡发展，避免偏科");
        }

        // 薄弱维度的具体建议
        if (!weaknesses.isEmpty()) {
            SuggestionData.DimAdvice w = weaknesses.get(0);
            if (w.getLevel().equals("critical") || w.getLevel().equals("warning")) {
                items.add(w.getDimLabel() + "：" + weaknessSuggestion(findDimIndex(w.getDimKey()), w.getLevel()));
            }
        }
        return items;
    }

    /* --- 成长趋势 --- */
    private SuggestionData.TrendInfo buildTrendInfo(ComprehensiveEvaluation current,
                                                      ComprehensiveEvaluation prev,
                                                      String currentClusterName) {
        SuggestionData.TrendInfo trend = new SuggestionData.TrendInfo();
        trend.setPrevYear(prev.getAcademicYear());
        double totalChange = current.getTotalScore().doubleValue() - prev.getTotalScore().doubleValue();
        trend.setTotalChange(Math.round(totalChange * 100.0) / 100.0);

        String[] dimKeys = {"moral", "academic", "physical", "art", "practice"};
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};
        double[] curScores = {current.getMoralScore().doubleValue(), current.getAcademicScore().doubleValue(),
                current.getPhysicalScore().doubleValue(), current.getArtScore().doubleValue(),
                current.getPracticeScore().doubleValue()};
        double[] prevScores = {prev.getMoralScore().doubleValue(), prev.getAcademicScore().doubleValue(),
                prev.getPhysicalScore().doubleValue(), prev.getArtScore().doubleValue(),
                prev.getPracticeScore().doubleValue()};

        List<SuggestionData.DimChange> changes = new ArrayList<>();
        List<String> upDims = new ArrayList<>();
        List<String> downDims = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            double change = Math.round((curScores[d] - prevScores[d]) * 100.0) / 100.0;
            SuggestionData.DimChange dc = new SuggestionData.DimChange();
            dc.setDimKey(dimKeys[d]);
            dc.setDimLabel(dimLabels[d]);
            dc.setChange(change);
            dc.setImproved(change >= 0);
            changes.add(dc);
            if (change >= 2) upDims.add(dimLabels[d]);
            if (change <= -2) downDims.add(dimLabels[d]);
        }
        trend.setDimChanges(changes);

        // 聚类类别变化
        if (prev.getClusterName() != null && !prev.getClusterName().equals(currentClusterName)) {
            trend.setClusterChangedFrom(prev.getClusterName());
            trend.setClusterChangedTo(currentClusterName);
        }

        // 亮点描述
        StringBuilder highlight = new StringBuilder();
        boolean categoryUpgraded = trend.getClusterChangedFrom() != null;
        if (categoryUpgraded) {
            highlight.append("表现优异，已从「").append(trend.getClusterChangedFrom())
                    .append("」提升至「").append(trend.getClusterChangedTo()).append("」，实现类别跨越！");
        } else if (totalChange >= 5) {
            highlight.append("综合总分较去年提升").append(String.format("%.1f", totalChange)).append("分，进步显著！");
        } else if (totalChange >= 2) {
            highlight.append("综合总分较去年提升").append(String.format("%.1f", totalChange)).append("分，保持稳步上升。");
        } else if (totalChange <= -5) {
            highlight.append("综合总分较去年下降").append(String.format("%.1f", -totalChange)).append("分，需引起重视。");
        } else if (totalChange < 0) {
            highlight.append("综合总分较去年略有下降").append(String.format("%.1f", -totalChange)).append("分，建议保持学习投入。");
        } else {
            highlight.append("综合总分与去年基本持平，需寻求突破。");
        }
        if (!categoryUpgraded) {
            if (!upDims.isEmpty()) {
                highlight.append(String.join("、", upDims)).append("进步明显；");
            }
            if (!downDims.isEmpty()) {
                highlight.append(String.join("、", downDims)).append("有所下滑，需重点关注。");
            }
        }
        trend.setHighlight(highlight.toString());
        return trend;
    }

    /* --- 提升路径 --- */
    private SuggestionData.PromotionPath buildPromotionPath(int currentLabel,
                                                              List<Integer> sortedIndices,
                                                              double[][] allAvgs,
                                                              double[] studentScores,
                                                              Map<Integer, String> labelToName) {
        int curRank = sortedIndices.indexOf(currentLabel);
        if (curRank <= 0) {
            // 已是最优群体，返回正向反馈
            SuggestionData.PromotionPath path = new SuggestionData.PromotionPath();
            path.setTargetClusterName(labelToName.getOrDefault(currentLabel, ""));
            path.setGaps(Collections.emptyList());
            path.setDescription("你已在最优评价群体中，请继续保持并发挥榜样引领作用！");
            return path;
        }

        int targetLabel = sortedIndices.get(curRank - 1);
        double[] targetCentroid = allAvgs[targetLabel];
        String targetName = labelToName.getOrDefault(targetLabel, "更优群体");
        String[] dimKeys = {"moral", "academic", "physical", "art", "practice"};
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};

        // 找出需要提升的维度（学生分低于目标簇中心）
        List<SuggestionData.DimGap> allGaps = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            double gap = Math.round((targetCentroid[d] - studentScores[d]) * 100.0) / 100.0;
            if (gap > 1) {
                SuggestionData.DimGap dg = new SuggestionData.DimGap();
                dg.setDimKey(dimKeys[d]);
                dg.setDimLabel(dimLabels[d]);
                dg.setCurrentScore(Math.round(studentScores[d] * 100.0) / 100.0);
                dg.setTargetScore(Math.round(targetCentroid[d] * 100.0) / 100.0);
                dg.setGap(gap);
                allGaps.add(dg);
            }
        }

        if (allGaps.isEmpty()) return null;

        // 取差距最大的2个
        allGaps.sort((a, b) -> Double.compare(b.getGap(), a.getGap()));
        List<SuggestionData.DimGap> mainGaps = allGaps.subList(0, Math.min(2, allGaps.size()));

        // 生成提升描述
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < mainGaps.size(); i++) {
            SuggestionData.DimGap g = mainGaps.get(i);
            if (i > 0) desc.append("，");
            desc.append(g.getDimLabel()).append("再提升").append(String.format("%.0f", g.getGap())).append("分");
        }
        desc.append("，即可进入「").append(targetName).append("」群体");

        SuggestionData.PromotionPath path = new SuggestionData.PromotionPath();
        path.setTargetClusterName(targetName);
        path.setGaps(mainGaps);
        path.setDescription(desc.toString());
        return path;
    }

    /* --- 典型度分析 --- */
    private SuggestionData.Typicality buildTypicality(double[] pcaPoint, double[] pcaCentroid,
                                                        double clusterAvgDist,
                                                        double[] scores, String[] dimLabels) {
        double myDist = euclideanDistance(pcaPoint, pcaCentroid);
        double ratio = clusterAvgDist > 1e-10 ? myDist / clusterAvgDist : 1.0;
        ratio = Math.round(ratio * 100.0) / 100.0;

        SuggestionData.Typicality t = new SuggestionData.Typicality();
        t.setRatio(ratio);

        if (ratio < 0.6) {
            t.setLabel("典型代表");
            t.setDescription("你的发展模式与本群体高度一致，是群体的经典画像。");
        } else if (ratio <= 1.5) {
            t.setLabel("一般成员");
            t.setDescription("你的发展模式处于本群体的正常范围。");
        } else {
            t.setLabel("特殊个体");
            // 找出偏离最远的维度
            double maxDev = 0;
            int maxIdx = 0;
            for (int d = 0; d < Math.min(pcaPoint.length, scores.length); d++) {
                double dev = Math.abs(scores[d] - (pcaCentroid.length > d ? pcaCentroid[d] : scores[d]));
                if (dev > maxDev) { maxDev = dev; maxIdx = d; }
            }
            t.setOutlierDim(dimLabels[maxIdx]);
            t.setOutlierDirection(scores[maxIdx] > 60 ? "above" : "below");
            if ("above".equals(t.getOutlierDirection())) {
                t.setDescription("你的" + dimLabels[maxIdx] + "远超群体平均水平，是群体的亮点。建议在保持优势的同时注意其他维度均衡。");
            } else {
                t.setDescription("你的" + dimLabels[maxIdx] + "显著低于群体平均水平，是当前最需要关注的方向。");
            }
        }
        return t;
    }

    /* --- 年级相对位置 --- */
    private List<SuggestionData.DimRank> buildDimRanks(String[] dimKeys, String[] dimLabels,
                                                         int[] absRanks, int total) {
        List<SuggestionData.DimRank> list = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            SuggestionData.DimRank dr = new SuggestionData.DimRank();
            dr.setDimKey(dimKeys[d]);
            dr.setDimLabel(dimLabels[d]);
            dr.setPercentile(0);
            dr.setRank(absRanks != null ? absRanks[d] : 0);
            dr.setTotal(total);
            list.add(dr);
        }
        return list;
    }

    /* --- 工具方法 --- */

    private String severityLevel(double score) {
        if (score < 40) return "critical";
        if (score < 60) return "warning";
        if (score < 75) return "normal";
        if (score < 90) return "good";
        return "excellent";
    }

    private String severityLabel(String level) {
        switch (level) {
            case "critical": return "需紧急提升";
            case "warning": return "建议重点关注";
            case "normal": return "可进一步提升";
            case "good": return "保持优势";
            case "excellent": return "榜样标杆";
            default: return "一般";
        }
    }

    private String overallSeverity(double[] scores) {
        double min = Arrays.stream(scores).min().orElse(100);
        if (min < 40) return "critical";
        if (min < 60) return "warning";
        double avg = Arrays.stream(scores).average().orElse(100);
        if (avg >= 85) return "excellent";
        if (avg >= 70) return "good";
        return "normal";
    }

    private String strengthSuggestion(int dimIdx, String level) {
        String[] advanced = {"争取获得德育类荣誉或先进称号", "参加学科竞赛或科研项目，挑战更高学术目标",
                "参加校级以上体育比赛，争取更好成绩", "参加艺术展演或比赛，发展个人艺术特长",
                "参与社会实践项目或志愿服务，积累实践经验"};
        String[] maintain = {"继续保持品德修养方面的优异表现", "保持学业优势，稳步提升各科成绩",
                "坚持日常锻炼，保持良好体能状态", "坚持艺术练习，持续发展审美素养",
                "保持劳动实践的积极性，培养良好劳动习惯"};
        if (level.equals("excellent") || level.equals("good")) return advanced[dimIdx];
        return maintain[dimIdx];
    }

    private String weaknessSuggestion(int dimIdx, String level) {
        String[] urgent = {"每日进行品德反思与行为规范练习，主动承担班级事务",
                "制定严格的学习计划，每天至少投入2小时课外学习，必要时请教学长或老师",
                "每周至少进行3次体育锻炼，每次不少于30分钟，可加入学校体育社团",
                "每周参与至少1次艺术类活动（音乐/美术/书法等），培养审美兴趣",
                "积极参加班级劳动和社区志愿服务，每周至少参与1次劳动实践活动"};
        String[] focus = {"多关注社会热点与时事政治，积极参与班级集体活动",
                "课后增加自主学习时间，针对薄弱科目制定专项提升计划",
                "增加体育锻炼频率，选择感兴趣的体育项目坚持练习",
                "多参观艺术展览或参加文艺活动，提升艺术鉴赏能力",
                "主动参与家务劳动和学校组织的实践活动，培养动手能力"};
        if (level.equals("critical") || level.equals("warning")) return urgent[dimIdx];
        return focus[dimIdx];
    }

    private int findDimIndex(String dimKey) {
        switch (dimKey) {
            case "moral": return 0;
            case "academic": return 1;
            case "physical": return 2;
            case "art": return 3;
            case "practice": return 4;
            default: return 0;
        }
    }

    private String generateClusterDescription(String name, double[] avgScores) {
        return String.format("「%s」群体特征：德育%.1f分，智育%.1f分，体育%.1f分，美育%.1f分，劳动教育%.1f分。",
                name, avgScores[0], avgScores[1], avgScores[2], avgScores[3], avgScores[4]);
    }

    /* 基于簇均值的百分位数动态计算阈值，避免硬编码 */
    private double[] calculateDynamicThresholds(double[][] allAvgs) {
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

    /* 各维度描述性统计，标记低区分度维度(CV<0.10) */
    private Map<String, Object> calculateDescriptiveStats(double[][] rawData) {
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

    private double[][] buildRawData(List<ComprehensiveEvaluation> evaluations) {
        double[][] rawData = new double[evaluations.size()][5];
        for (int i = 0; i < evaluations.size(); i++) {
            ComprehensiveEvaluation e = evaluations.get(i);
            rawData[i] = new double[]{
                    e.getMoralScore().doubleValue(), e.getAcademicScore().doubleValue(),
                    e.getPhysicalScore().doubleValue(), e.getArtScore().doubleValue(),
                    e.getPracticeScore().doubleValue()
            };
        }
        return rawData;
    }

    private List<DoublePoint> toDoublePoints(double[][] data) {
        List<DoublePoint> pts = new ArrayList<>();
        for (double[] row : data) pts.add(new DoublePoint(row));
        return pts;
    }

    /* 遍历 K=minK..maxK，用三指标投票选出最优 K */
    public Map<String, Object> findOptimalK(String academicYear, int minK, int maxK) {
        aggregateEvaluations(academicYear);
        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getAcademicYear, academicYear));
        if (evaluations.size() < maxK) {
            throw new RuntimeException("学生数量不足，无法评估K=" + maxK);
        }
        double[][] rawData = buildRawData(evaluations);
        double[][] normalizedData = zScoreNormalize(rawData);
        double[][] pcaData = applyPCA(normalizedData, 0.90);
        List<DoublePoint> points = toDoublePoints(pcaData);
        int range = maxK - minK + 1;
        double[] silhouetteScores = new double[range];
        double[] dbiScores = new double[range];
        double[] chiScores = new double[range];
        for (int k = minK; k <= maxK; k++) {
            int idx = k - minK;
            JDKRandomGenerator rng = new JDKRandomGenerator();
            rng.setSeed(42L);
            KMeansPlusPlusClusterer<DoublePoint> clusterer =
                    new KMeansPlusPlusClusterer<>(k, 300,
                            new EuclideanDistance(), rng);
            List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);
            silhouetteScores[idx] = calculateSilhouetteScore(pcaData, clusters, points);
            dbiScores[idx] = calculateDaviesBouldinIndex(pcaData, clusters, points);
            chiScores[idx] = calculateCalinskiHarabaszIndex(pcaData, clusters, points);
        }
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
        result.put("kRange", Map.of("min", minK, "max", maxK));
        result.put("silhouetteByK", toKeyValueList(minK, silhouetteScores));
        result.put("dbiByK", toKeyValueList(minK, dbiScores));
        result.put("chiByK", toKeyValueList(minK, chiScores));
        result.put("voteDetails", Map.of(
                "silhouetteVote", bestSilK,
                "dbiVote", bestDbiK,
                "chiVote", bestChiK));
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
}
