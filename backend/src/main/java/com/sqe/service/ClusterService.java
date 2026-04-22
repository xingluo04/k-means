package com.sqe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqe.entity.*;
import com.sqe.mapper.*;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
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

    /* 聚类名称映射 */
    private static final String[] CLUSTER_NAMES = {
            "全面优秀型", "均衡发展型", "学术艺术型", "体能突出型", "待提升型"
    };

    /**
     * 执行聚类分析
     * 先汇总各维度评价数据，再执行K-means聚类
     */
    @Transactional
    public Map<String, Object> executeCluster(String semester, int clusterCount) {
        /* 汇总各维度评价数据到综合评价表 */
        aggregateEvaluations(semester);

        /* 获取该学期所有综合评价数据 */
        List<ComprehensiveEvaluation> evaluations = comprehensiveMapper.selectList(
                new LambdaQueryWrapper<ComprehensiveEvaluation>()
                        .eq(ComprehensiveEvaluation::getSemester, semester));

        if (evaluations.size() < clusterCount) {
            throw new RuntimeException("学生数量不足，无法进行" + clusterCount + "类聚类分析");
        }

        /* 数据标准化 */
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

        /* Z-Score标准化 */
        double[][] normalizedData = zScoreNormalize(rawData);

        /* 构建聚类数据点 */
        List<DoublePoint> points = new ArrayList<>();
        for (double[] row : normalizedData) {
            points.add(new DoublePoint(row));
        }

        /* 执行K-means++聚类 */
        KMeansPlusPlusClusterer<DoublePoint> clusterer =
                new KMeansPlusPlusClusterer<>(clusterCount, 300);
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

        /* 计算轮廓系数 */
        double silhouetteScore = calculateSilhouetteScore(normalizedData, clusters, points);

        /* 根据聚类中心的总分排序，分配聚类名称 */
        List<Integer> sortedIndices = sortClustersByTotalScore(clusters);

        /* 清除旧的聚类结果 */
        clusterResultMapper.delete(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getSemester, semester));

        /* 保存聚类结果并更新综合评价 */
        for (int clusterIdx = 0; clusterIdx < clusters.size(); clusterIdx++) {
            CentroidCluster<DoublePoint> cluster = clusters.get(clusterIdx);
            int nameIdx = sortedIndices.indexOf(clusterIdx);
            String clusterName = nameIdx < CLUSTER_NAMES.length ? CLUSTER_NAMES[nameIdx] : "类别" + (nameIdx + 1);

            List<DoublePoint> clusterPoints = cluster.getPoints();

            /* 找到属于该聚类的学生 */
            for (DoublePoint point : clusterPoints) {
                int originalIdx = points.indexOf(point);
                if (originalIdx >= 0 && originalIdx < evaluations.size()) {
                    ComprehensiveEvaluation eval = evaluations.get(originalIdx);
                    eval.setClusterLabel(nameIdx);
                    eval.setClusterName(clusterName);
                    eval.setSuggestion(generateSuggestion(eval, clusterName));
                    comprehensiveMapper.updateById(eval);
                }
            }

            /* 计算聚类统计信息 */
            ClusterResult result = new ClusterResult();
            result.setSemester(semester);
            result.setClusterCount(clusterCount);
            result.setSilhouetteScore(BigDecimal.valueOf(silhouetteScore).setScale(4, RoundingMode.HALF_UP));
            result.setClusterLabel(nameIdx);
            result.setClusterName(clusterName);
            result.setStudentCount(clusterPoints.size());

            /* 计算该聚类各维度平均分 */
            double[] avgScores = calculateClusterAvg(evaluations, points, clusterPoints);
            result.setAvgMoral(BigDecimal.valueOf(avgScores[0]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgAcademic(BigDecimal.valueOf(avgScores[1]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgPhysical(BigDecimal.valueOf(avgScores[2]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgArt(BigDecimal.valueOf(avgScores[3]).setScale(2, RoundingMode.HALF_UP));
            result.setAvgPractice(BigDecimal.valueOf(avgScores[4]).setScale(2, RoundingMode.HALF_UP));
            result.setDescription(generateClusterDescription(clusterName, avgScores));
            clusterResultMapper.insert(result);
        }

        /* 返回结果 */
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("silhouetteScore", BigDecimal.valueOf(silhouetteScore).setScale(4, RoundingMode.HALF_UP));
        resultMap.put("clusterCount", clusterCount);
        resultMap.put("studentCount", evaluations.size());
        resultMap.put("clusters", clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>().eq(ClusterResult::getSemester, semester)
                        .orderByAsc(ClusterResult::getClusterLabel)));
        return resultMap;
    }

    /* 汇总各维度评价数据 */
    private void aggregateEvaluations(String semester) {
        /* 获取该学期所有品德评价 */
        List<MoralEvaluation> morals = moralMapper.selectList(
                new LambdaQueryWrapper<MoralEvaluation>().eq(MoralEvaluation::getSemester, semester));
        Map<Long, BigDecimal> moralMap = morals.stream()
                .collect(Collectors.toMap(MoralEvaluation::getStudentId, MoralEvaluation::getTotalScore, (a, b) -> b));

        List<AcademicEvaluation> academics = academicMapper.selectList(
                new LambdaQueryWrapper<AcademicEvaluation>().eq(AcademicEvaluation::getSemester, semester));
        Map<Long, BigDecimal> academicMap = academics.stream()
                .collect(Collectors.toMap(AcademicEvaluation::getStudentId, AcademicEvaluation::getTotalScore, (a, b) -> b));

        List<PhysicalEvaluation> physicals = physicalMapper.selectList(
                new LambdaQueryWrapper<PhysicalEvaluation>().eq(PhysicalEvaluation::getSemester, semester));
        Map<Long, BigDecimal> physicalMap = physicals.stream()
                .collect(Collectors.toMap(PhysicalEvaluation::getStudentId, PhysicalEvaluation::getTotalScore, (a, b) -> b));

        List<ArtEvaluation> arts = artMapper.selectList(
                new LambdaQueryWrapper<ArtEvaluation>().eq(ArtEvaluation::getSemester, semester));
        Map<Long, BigDecimal> artMap = arts.stream()
                .collect(Collectors.toMap(ArtEvaluation::getStudentId, ArtEvaluation::getTotalScore, (a, b) -> b));

        List<PracticeEvaluation> practices = practiceMapper.selectList(
                new LambdaQueryWrapper<PracticeEvaluation>().eq(PracticeEvaluation::getSemester, semester));
        Map<Long, BigDecimal> practiceMap = practices.stream()
                .collect(Collectors.toMap(PracticeEvaluation::getStudentId, PracticeEvaluation::getTotalScore, (a, b) -> b));

        /* 合并所有学生ID */
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
            BigDecimal total = moral.add(academic).add(physical).add(art).add(practice)
                    .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);

            ComprehensiveEvaluation existing = comprehensiveMapper.selectOne(
                    new LambdaQueryWrapper<ComprehensiveEvaluation>()
                            .eq(ComprehensiveEvaluation::getStudentId, studentId)
                            .eq(ComprehensiveEvaluation::getSemester, semester));

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
                ce.setSemester(semester);
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

    /* Z-Score标准化 */
    private double[][] zScoreNormalize(double[][] data) {
        int rows = data.length;
        int cols = data[0].length;
        double[][] normalized = new double[rows][cols];

        for (int j = 0; j < cols; j++) {
            double mean = 0, std = 0;
            for (int i = 0; i < rows; i++) mean += data[i][j];
            mean /= rows;
            for (int i = 0; i < rows; i++) std += Math.pow(data[i][j] - mean, 2);
            std = Math.sqrt(std / rows);
            if (std == 0) std = 1;
            for (int i = 0; i < rows; i++) {
                normalized[i][j] = (data[i][j] - mean) / std;
            }
        }
        return normalized;
    }

    /* 计算轮廓系数 */
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

    /* 计算点到同簇其他点的平均距离 */
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

    /* 计算点到最近其他簇的平均距离 */
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

    /* 欧氏距离 */
    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    /* 按聚类中心总分排序 */
    private List<Integer> sortClustersByTotalScore(List<CentroidCluster<DoublePoint>> clusters) {
        List<double[]> centers = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            double[] center = clusters.get(i).getCenter().getPoint();
            centers.add(new double[]{i, Arrays.stream(center).sum()});
        }
        centers.sort((a, b) -> Double.compare(b[1], a[1]));
        return centers.stream().map(c -> (int) c[0]).collect(Collectors.toList());
    }

    /* 计算聚类各维度平均分 */
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

    /* 生成个性化建议 */
    private String generateSuggestion(ComprehensiveEvaluation eval, String clusterName) {
        StringBuilder sb = new StringBuilder();
        BigDecimal[] scores = {eval.getMoralScore(), eval.getAcademicScore(),
                eval.getPhysicalScore(), eval.getArtScore(), eval.getPracticeScore()};
        String[] dims = {"品德发展", "学业发展", "体能发展", "艺术素养", "实践创新"};

        /* 找出最强和最弱维度 */
        int maxIdx = 0, minIdx = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i].compareTo(scores[maxIdx]) > 0) maxIdx = i;
            if (scores[i].compareTo(scores[minIdx]) < 0) minIdx = i;
        }

        sb.append("您属于「").append(clusterName).append("」群体。");
        sb.append(dims[maxIdx]).append("表现突出（").append(scores[maxIdx]).append("分），");
        sb.append("建议重点加强").append(dims[minIdx]).append("方面的发展（当前").append(scores[minIdx]).append("分）。");

        if (scores[minIdx].compareTo(BigDecimal.valueOf(60)) < 0) {
            sb.append("该维度得分偏低，建议制定专项提升计划。");
        } else if (scores[minIdx].compareTo(BigDecimal.valueOf(75)) < 0) {
            sb.append("可通过多参加相关活动来提升该维度表现。");
        }
        return sb.toString();
    }

    /* 生成聚类描述 */
    private String generateClusterDescription(String name, double[] avgScores) {
        return String.format("「%s」群体特征：品德%.1f分，学业%.1f分，体能%.1f分，艺术%.1f分，实践%.1f分。",
                name, avgScores[0], avgScores[1], avgScores[2], avgScores[3], avgScores[4]);
    }

    /* 获取聚类结果 */
    public List<ClusterResult> getClusterResults(String semester) {
        return clusterResultMapper.selectList(
                new LambdaQueryWrapper<ClusterResult>()
                        .eq(ClusterResult::getSemester, semester)
                        .orderByAsc(ClusterResult::getClusterLabel));
    }
}
