package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GradingTableGenerator {

    public static void defaultData(GradingTable table) {
        table.addMark("10.0", "E");
        table.addMark("11.0", "E");
        table.addMark("12.0", "D");
        table.addMark("13.0", "D");
        table.addMark("14.0", "C");
        table.addMark("15.0", "C");
        table.addMark("16.0", "B");
        table.addMark("17.0", "B");
        table.addMark("18.0", "A");
        table.addMark("19.0", "A");
        table.addMark("20.0", "A");
    }

    public static void generateTableData(GradingTable table, List<BigDecimal> sample) {
        Map<BigDecimal, Integer> gradeDistro = new LinkedHashMap<BigDecimal, Integer>();
        Map<BigDecimal, BigDecimal> heapedGradeDistro = new LinkedHashMap<BigDecimal, BigDecimal>();
        BigDecimal sampleSize = new BigDecimal(sample.size());
        gradeDistro.put(new BigDecimal("10.0"), 0);
        gradeDistro.put(new BigDecimal("11.0"), 0);
        gradeDistro.put(new BigDecimal("12.0"), 0);
        gradeDistro.put(new BigDecimal("13.0"), 0);
        gradeDistro.put(new BigDecimal("14.0"), 0);
        gradeDistro.put(new BigDecimal("15.0"), 0);
        gradeDistro.put(new BigDecimal("16.0"), 0);
        gradeDistro.put(new BigDecimal("17.0"), 0);
        gradeDistro.put(new BigDecimal("18.0"), 0);
        gradeDistro.put(new BigDecimal("19.0"), 0);
        gradeDistro.put(new BigDecimal("20.0"), 0);

        // 1. Grades distributions
        for (BigDecimal grade : sample) {
            grade = grade.setScale(1);
            gradeDistro.put(grade, (gradeDistro.get(grade) + 1));
        }

        // 2. Heaped grades distribution
        BigDecimal heap = BigDecimal.ZERO;
        for (Entry<BigDecimal, Integer> step : gradeDistro.entrySet()) {
            BigDecimal grade = step.getKey();
            BigDecimal count = new BigDecimal(step.getValue());
            BigDecimal share = count.divide(sampleSize, 5, BigDecimal.ROUND_HALF_EVEN);
            heap = heap.add(share);
            heapedGradeDistro.put(grade, heap.setScale(3, BigDecimal.ROUND_HALF_EVEN));
        }

        // 3. Apply algorithm and return table
        final GradeDistributionConverter carla = new GradeDistributionConverter();
        final Map<BigDecimal, String> tableMap = carla.process(heapedGradeDistro);
        for (BigDecimal mark : tableMap.keySet()) {
            table.addMark(mark, tableMap.get(mark));
        }
    }

    private static class GradeDistributionConverter {
        private Map<String, BigDecimal> distro = new LinkedHashMap<String, BigDecimal>();
        private static String[] ectsGrades = { "E", "D", "C", "B", "A" };
        private int gradePointer = 0;

        GradeDistributionConverter() {
            distro.put("E", new BigDecimal("0.10"));
            distro.put("D", new BigDecimal("0.35"));
            distro.put("C", new BigDecimal("0.65"));
            distro.put("B", new BigDecimal("0.90"));
            distro.put("A", new BigDecimal("1.00"));
        }

        Map<BigDecimal, String> process(Map<BigDecimal, BigDecimal> heapedGradeDistro) {
            final Map<BigDecimal, String> tableMap = new LinkedHashMap<BigDecimal, String>();
            boolean firstCycle = true;
            BigDecimal previousHeap = BigDecimal.ZERO;
            for (Entry<BigDecimal, BigDecimal> tuple : heapedGradeDistro.entrySet()) {
                if (firstCycle) {
                    if (tuple.getValue().compareTo(distro.get(ectsGrades[gradePointer]).multiply(new BigDecimal(2))) >= 0) {
                        gradePointer++;
                    }
                    tableMap.put(tuple.getKey(), ectsGrades[gradePointer]);
                    previousHeap = tuple.getValue();
                    firstCycle = false;
                } else if (gradePointer == 4) {
                    tableMap.put(tuple.getKey(), ectsGrades[gradePointer]);
                    previousHeap = tuple.getValue();
                } else {
                    BigDecimal threshold = distro.get(ectsGrades[gradePointer]);
                    BigDecimal nextThreshold = distro.get(ectsGrades[gradePointer + 1]);
                    if (tuple.getValue().compareTo(nextThreshold) >= 0) {
                        while (tuple.getValue().compareTo(nextThreshold) >= 0 && gradePointer < (ectsGrades.length - 2)) {
                            gradePointer++;
                            nextThreshold = distro.get(ectsGrades[gradePointer + 1]);
                        }
                        tableMap.put(tuple.getKey(), ectsGrades[++gradePointer]);
                    } else if (tuple.getValue().compareTo(threshold) >= 0) {
                        BigDecimal lowerSeparation = threshold.subtract(previousHeap);
                        BigDecimal higherSeparation = tuple.getValue().subtract(threshold);
                        if (lowerSeparation.compareTo(higherSeparation) < 0) {
                            tableMap.put(tuple.getKey(), ectsGrades[++gradePointer]);
                        } else if (lowerSeparation.compareTo(higherSeparation) >= 0) {
                            tableMap.put(tuple.getKey(), ectsGrades[gradePointer++]);
                        }
                    } else {
                        tableMap.put(tuple.getKey(), ectsGrades[gradePointer]);
                    }
                    previousHeap = tuple.getValue();
                }
            }
            return tableMap;
        }
    }

}
