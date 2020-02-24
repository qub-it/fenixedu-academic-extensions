package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;

// We are using the procedure outlined in the "Proposta Aplicacao Classificacoes A-E Vdraft". It works fine in the general case
// where the grades are well distributed, but it breaks down in some corner/edge cases where the sample is small and/or badly
// distributed (either top heavy or bottom heavy).
// This is because the algorithm does not state if it's "first-past-the-post" or not. In the case that FPTP logic is applied, we
// can get into a corner case with a grade distribution of (20,1)(10,29).
// In this case, the upper bound for the B class is 10 (M_A-1) but the lower bound is 11 (b+1, with b = 10).
// Although this logic favors students (as we start from A -> E), this does not produce balanced grading that reflects the spirit
// of the ECTS grading system. Hence, we implemented a threshold oriented logic that does not use first past the post, but best
// fit.
//
// 11-09-2019 Diogo Sousa

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

    public static void generateTableDataImprovement(GradingTable table, List<BigDecimal> sample) {
        Map<BigDecimal, LongAdder> gradeDistro = new LinkedHashMap<BigDecimal, LongAdder>();
        Map<BigDecimal, BigDecimal> heapedGradeDistro = new LinkedHashMap<BigDecimal, BigDecimal>();
        BigDecimal sampleSize = new BigDecimal(sample.size());
        gradeDistro.put(new BigDecimal("20.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("19.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("18.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("17.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("16.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("15.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("14.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("13.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("12.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("11.0"), new LongAdder());
        gradeDistro.put(new BigDecimal("10.0"), new LongAdder());

        // 1. Grades distributions
        for (BigDecimal grade : sample) {
            grade = grade.setScale(1);              // This adds the .0 to the value.
            gradeDistro.get(grade).increment();     // Every grade should be present.
        }

        // 2. Heaped grades distribution
        BigDecimal heap = BigDecimal.ZERO;
        for (Entry<BigDecimal, LongAdder> step : gradeDistro.entrySet()) {
            BigDecimal grade = step.getKey();
            BigDecimal count = new BigDecimal(step.getValue().longValue());
            // Calculates every grade cumulative percentage point and adds it.
            BigDecimal share = count.divide(sampleSize, 5, BigDecimal.ROUND_HALF_EVEN);
            heap = heap.add(share);
            // Scale = 3
            heapedGradeDistro.put(grade, heap.setScale(3, BigDecimal.ROUND_HALF_EVEN));
        }

        // Sets S_21 to ZERO
        heapedGradeDistro.put(new BigDecimal(21).setScale(1), BigDecimal.ZERO);

        // 3. Apply algorithm and return table
        final GradeDistributionConverter ectsGradeConverter = new GradeDistributionConverter();
        final Map<BigDecimal, String> tableMap = ectsGradeConverter.process(heapedGradeDistro);
        for (Entry<BigDecimal, String> mark : tableMap.entrySet()) { // EntrySet removes the need for another get call.
            table.addMark(mark.getKey(), mark.getValue());
        }
    }

    private static class GradeDistributionConverter {

        private Map<String, BigDecimal> distro = new LinkedHashMap<String, BigDecimal>();
        private static final String[] ectsGrades = { "A", "B", "C", "D", "E" };
        private static final BigDecimal[] thresholds = createThresholds();
        private static final BigDecimal TWENTY = new BigDecimal(20).setScale(1);

        private static BigDecimal[] createThresholds() {
            BigDecimal[] results = new BigDecimal[4];
            results[0] = new BigDecimal("0.10").setScale(3);
            results[1] = new BigDecimal("0.35").setScale(3);
            results[2] = new BigDecimal("0.65").setScale(3);
            results[3] = new BigDecimal("0.90").setScale(3);
            return results;
        }

        GradeDistributionConverter() {
            distro.put("A", new BigDecimal("0.10"));
            distro.put("B", new BigDecimal("0.35"));
            distro.put("C", new BigDecimal("0.65"));
            distro.put("D", new BigDecimal("0.90"));
            distro.put("E", new BigDecimal("1.00"));
        }

//      Consider that the A, B and C grades are not given out when the upper bound is 20. In an extreme scenario, at least the
//      D and E grades must be present in the grading scale as they have no special clause.
//        
//      The logic here is to minimize the error between the accumulated percentage of grades and the thresholds.
//      We can also see that apart from the A class, the upper bound for B, C, D and E can be defined using the lower bound of
//      the previous class. Therefore, we only need to store that value and seed the A class with 20.
//      If the A class is ineligible, 21 will be present there.

        Map<BigDecimal, String> process(Map<BigDecimal, BigDecimal> heapedGradeDistro) {
            final Map<BigDecimal, String> tableMap = new LinkedHashMap<BigDecimal, String>();
            Map<String, Integer> lowerBound = new HashMap<>();
            Map<String, Integer> upperBound = new HashMap<>();
            int gradeIndex = 0;
            upperBound.put("A", 20); // Upper bound for A class is either 20 or 21 for every case.
            lowerBound.put("E", 10); // Lower bound for E will always be 10 in the worst case scenario. If upperBound(E) < 10, E isn't used.

            for (BigDecimal threshold : thresholds) {
                for (Entry<BigDecimal, BigDecimal> tuple : heapedGradeDistro.entrySet()) {
                    BigDecimal currentGrade = tuple.getKey();
                    BigDecimal currentPercentage = tuple.getValue();
                    if (currentPercentage.compareTo(threshold) >= 0) {
                        // If currentGrade == 20, this will retrieve { 21 : 0 }
                        BigDecimal previousPercentage = heapedGradeDistro.get(currentGrade.add(BigDecimal.ONE).setScale(1));
                        BigDecimal leftHandSide = currentPercentage.subtract(threshold).abs();
                        BigDecimal rightHandSide = previousPercentage.subtract(threshold).abs();
                        if (leftHandSide.compareTo(rightHandSide) <= 0) {
                            lowerBound.put(ectsGrades[gradeIndex], currentGrade.intValue());
                        } else {
                            if (gradeIndex < 3 && currentGrade.equals(TWENTY)) { // D does not care about the 20 check
                                lowerBound.put(ectsGrades[gradeIndex], 21);
                                upperBound.put(ectsGrades[gradeIndex], 21);
                            } else {
                                lowerBound.put(ectsGrades[gradeIndex], currentGrade.intValue() + 1);
                            }
                        }
                        break; // Necessary evil
                    }
                }
                gradeIndex++;
            }
            // Starts at B as A will either have M_A = 20 or M_A = 21
            for (int i = 1; i < ectsGrades.length; i++) {
                int previousClassLowerBound = lowerBound.get(ectsGrades[i - 1]);
                upperBound.put(ectsGrades[i], previousClassLowerBound - 1);
            }

            for (String grade : ectsGrades) {

                for (int i = lowerBound.get(grade); i <= upperBound.get(grade); i++) {
                    tableMap.put(new BigDecimal(i).setScale(1), grade);
                }
            }
            return tableMap;
        }

        /**
         * THIS METHOD IS NO LONGER USED: this logic favors the students. We switched to a more balanced logic.
         * 
         * The logic for this algorithm is based on stacking the grades so there are no gaps in the letters. Whenever we "close" a
         * letter, everything on the stack receives that letter and we move on to the next. This is based on the
         * observation that whenever we calculate a value (S_i), we are deciding if the letter border will include or exclude the
         * current grade.
         * 
         * The heapedGradeDistro comes ordered from 20 -> 10, with 21 at the end. 21 is present in order to prevent having to
         * create special logic for the case of 20 having more than 10%, 35% and 65% students.
         * Starting with 20, in descending order, we find the first grade value that will exceed the current threshold level (10%,
         * 35%, etc) and we compare it with the cumulative percentage from the grade immediately above (grade + 1).
         * 
         * We are looking to minimize the difference between those two percentages. If the current grade has a smaller difference,
         * it is put on the stack and receives the current letter grade. It is set as the lower bound of that letter. If not,
         * there are two cases:
         * 
         * - For the letters A to C, if the grade is 20, that letter will not be given out. The current grade then gets put on the
         * stack and we advance the letters + threshold. If not, then it will be put on the stack after the current letter has
         * been
         * applied, as it will be the upper bound for the NEXT letter.
         * - In the D and E range, it does not matter if it's 20 or not. The letter gets put on the stack after the current one is
         * applied.
         * 
         * After it's decided when the grade will be put on the stack (before/after the current letter), the letter is applied
         * by emptying the stack.
         * 
         * When we reach the letter E, every grade left gets put on the stack. This includes any grade that didn't make the D
         * threshold. A final call empties the stack of any grade that is left, giving it the letter E.
         * 
         * @author Diogo Sousa
         */
        @SuppressWarnings("unused")
        Map<BigDecimal, String> oldProcess(Map<BigDecimal, BigDecimal> heapedGradeDistro) {

            final Map<BigDecimal, String> tableMap = new LinkedHashMap<BigDecimal, String>();
            int gradeIndex = 0;
            Queue<BigDecimal> pendingAssignment = new ArrayDeque<>(10);
            BigDecimal threshold = distro.get(ectsGrades[gradeIndex]);
            boolean addCurrent = false;

            for (Entry<BigDecimal, BigDecimal> tuple : heapedGradeDistro.entrySet()) {
                BigDecimal currentGrade = tuple.getKey();
                BigDecimal currentPercentage = tuple.getValue();
                if (gradeIndex == 4) { // We have reached E grade
                    pendingAssignment.add(currentGrade);
                    continue;
                }
                if (currentPercentage.compareTo(threshold) >= 0) {
                    // If currentGrade == 20, this will retrieve { 21 : 0 }
                    BigDecimal previousPercentage = heapedGradeDistro.get(currentGrade.add(BigDecimal.ONE).setScale(1));
                    BigDecimal leftHandSide = currentPercentage.subtract(threshold).abs();
                    BigDecimal rightHandSide = previousPercentage.subtract(threshold).abs();
                    if (leftHandSide.compareTo(rightHandSide) <= 0) {
                        // This grade gets included in the ranking
                        pendingAssignment.add(currentGrade);
                    } else {
                        if (gradeIndex < 3 && currentGrade.equals(TWENTY)) { // D does not care about the 20 check
                            pendingAssignment.add(currentGrade);
                            gradeIndex++; // This grade does not get assigned
                            continue;
                        }
                        addCurrent = true;
                    }
                    while (!pendingAssignment.isEmpty()) {
                        tableMap.put(pendingAssignment.poll(), ectsGrades[gradeIndex]);
                    }
                    if (addCurrent) {
                        addCurrent = false;
                        pendingAssignment.add(currentGrade);
                    }
                    gradeIndex++;
                } else {
                    // Put it in the queue for later assignment
                    pendingAssignment.add(currentGrade);
                }
            }
            // If there is anything left
            while (!pendingAssignment.isEmpty()) {
                tableMap.put(pendingAssignment.poll(), ectsGrades[gradeIndex]);
            }
            // Remove the 21 entry that comes in the heapedGradeDistro.entrySet()
            tableMap.remove(new BigDecimal(21).setScale(1));
            return tableMap;
        }
    }
}
