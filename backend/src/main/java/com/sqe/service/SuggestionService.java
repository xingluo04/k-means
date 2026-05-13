package com.sqe.service;

import cn.hutool.json.JSONUtil;
import com.sqe.dto.SuggestionData;
import com.sqe.entity.ComprehensiveEvaluation;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 个性化发展建议生成服务
 */
@Service
public class SuggestionService {

    /**
     * 生成结构化个性化发展建议（JSON格式）
     */
    public String generateStructuredSuggestion(ComprehensiveEvaluation eval, String clusterName,
                                                int currentLabel, List<Integer> sortedIndices,
                                                double[][] allAvgs,
                                                ComprehensiveEvaluation prevEval,
                                                double[] pcaPoint, double[] pcaCentroid,
                                                double clusterAvgDist, double[] pctRanks,
                                                int[] absRanks, int totalStudents,
                                                Map<Integer, String> labelToName,
                                                ClusterEvaluationService evalService) {
        SuggestionData data = new SuggestionData();
        double[] scores = {eval.getMoralScore().doubleValue(), eval.getAcademicScore().doubleValue(),
                eval.getPhysicalScore().doubleValue(), eval.getArtScore().doubleValue(),
                eval.getPracticeScore().doubleValue()};
        String[] dimKeys = {"moral", "academic", "physical", "art", "practice"};
        String[] dimLabels = {"德育", "智育", "体育", "美育", "劳动教育"};

        data.setClusterName(clusterName);

        data.setStrengths(buildStrengths(scores, dimKeys, dimLabels, pctRanks, absRanks, totalStudents));
        data.setWeaknesses(buildWeaknesses(scores, dimKeys, dimLabels, pctRanks, absRanks, totalStudents));
        data.setSeverityLevel(overallSeverity(scores));

        data.setSummary(buildSummary(clusterName, data.getStrengths(), data.getWeaknesses()));

        data.setActionItems(buildActionItems(clusterName, data.getWeaknesses(), data.getStrengths()));

        if (prevEval != null) {
            data.setTrend(buildTrendInfo(eval, prevEval, clusterName));
        }

        if (sortedIndices != null && allAvgs != null) {
            data.setPromotion(buildPromotionPath(currentLabel, sortedIndices, allAvgs, scores, labelToName));
        }

        if (pcaPoint != null && pcaCentroid != null) {
            data.setTypicality(buildTypicality(pcaPoint, pcaCentroid, clusterAvgDist, scores, dimLabels, evalService));
        }

        data.setPercentileRanks(buildDimRanks(dimKeys, dimLabels, absRanks, totalStudents, pctRanks));

        return JSONUtil.toJsonStr(data);
    }

    /* --- 优势维度 --- */
    public List<SuggestionData.DimAdvice> buildStrengths(double[] scores, String[] dimKeys,
                                                           String[] dimLabels, double[] pctRanks,
                                                           int[] absRanks, int total) {
        List<SuggestionData.DimAdvice> result = new ArrayList<>();
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
    public List<SuggestionData.DimAdvice> buildWeaknesses(double[] scores, String[] dimKeys,
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
    public String buildSummary(String clusterName, List<SuggestionData.DimAdvice> strengths,
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
    public List<String> buildActionItems(String clusterName,
                                           List<SuggestionData.DimAdvice> weaknesses,
                                           List<SuggestionData.DimAdvice> strengths) {
        List<String> items = new ArrayList<>();

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

        if (!weaknesses.isEmpty()) {
            SuggestionData.DimAdvice w = weaknesses.get(0);
            if (w.getLevel().equals("critical") || w.getLevel().equals("warning")) {
                items.add(w.getDimLabel() + "：" + weaknessSuggestion(findDimIndex(w.getDimKey()), w.getLevel()));
            }
        }
        return items;
    }

    /* --- 成长趋势 --- */
    public SuggestionData.TrendInfo buildTrendInfo(ComprehensiveEvaluation current,
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

        if (prev.getClusterName() != null && !prev.getClusterName().equals(currentClusterName)) {
            trend.setClusterChangedFrom(prev.getClusterName());
            trend.setClusterChangedTo(currentClusterName);
        }

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
    public SuggestionData.PromotionPath buildPromotionPath(int currentLabel,
                                                            List<Integer> sortedIndices,
                                                            double[][] allAvgs,
                                                            double[] studentScores,
                                                            Map<Integer, String> labelToName) {
        int curRank = sortedIndices.indexOf(currentLabel);
        if (curRank <= 0) {
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

        allGaps.sort((a, b) -> Double.compare(b.getGap(), a.getGap()));
        List<SuggestionData.DimGap> mainGaps = allGaps.subList(0, Math.min(2, allGaps.size()));

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
    public SuggestionData.Typicality buildTypicality(double[] pcaPoint, double[] pcaCentroid,
                                                      double clusterAvgDist,
                                                      double[] scores, String[] dimLabels,
                                                      ClusterEvaluationService evalService) {
        double myDist = evalService.euclideanDistance(pcaPoint, pcaCentroid);
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
    public List<SuggestionData.DimRank> buildDimRanks(String[] dimKeys, String[] dimLabels,
                                                       int[] absRanks, int total, double[] pctRanks) {
        List<SuggestionData.DimRank> list = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            SuggestionData.DimRank dr = new SuggestionData.DimRank();
            dr.setDimKey(dimKeys[d]);
            dr.setDimLabel(dimLabels[d]);
            dr.setPercentile(pctRanks != null ? pctRanks[d] : 0);
            dr.setRank(absRanks != null ? absRanks[d] : 0);
            dr.setTotal(total);
            list.add(dr);
        }
        return list;
    }

    /* --- 严重度等级 --- */
    public String severityLevel(double score) {
        if (score < 40) return "critical";
        if (score < 60) return "warning";
        if (score < 75) return "normal";
        if (score < 90) return "good";
        return "excellent";
    }

    public String severityLabel(String level) {
        switch (level) {
            case "critical": return "需紧急提升";
            case "warning": return "建议重点关注";
            case "normal": return "可进一步提升";
            case "good": return "保持优势";
            case "excellent": return "榜样标杆";
            default: return "一般";
        }
    }

    public String overallSeverity(double[] scores) {
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

    public String weaknessSuggestion(int dimIdx, String level) {
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
}
