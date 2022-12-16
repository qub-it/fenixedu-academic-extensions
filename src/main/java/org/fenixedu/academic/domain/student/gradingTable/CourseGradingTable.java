package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.ExternalEnrolment;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.CallableWithoutException;

public class CourseGradingTable extends CourseGradingTable_Base {

    public CourseGradingTable() {
        super();
    }

    @Override
    public void delete() {
        setCompetenceCourse(null);
        setCurriculumLine(null);
        super.delete();
    }

    public static Stream<CourseGradingTable> findAll() {
        return Bennu.getInstance().getGradingTablesSet().stream().filter(CourseGradingTable.class::isInstance)
                .map(CourseGradingTable.class::cast);
    }

    public static Set<CourseGradingTable> find(final ExecutionYear ey) {
        return find(ey, false);
    }

    public static Set<CourseGradingTable> find(final ExecutionYear ey, final boolean includeLegacy) {
        return ey.getGradingTablesSet().stream().filter(CourseGradingTable.class::isInstance).map(CourseGradingTable.class::cast)
                .filter(cgt -> (includeLegacy || cgt.getCurriculumLine() == null)).collect(Collectors.toSet());
    }

    public static CourseGradingTable find(final ExecutionYear ey, final CompetenceCourse cc) {
        return cc == null ? null : cc.getCourseGradingTablesSet().stream().filter(cgt -> cgt.getCurriculumLine() == null)
                .filter(cgt -> cgt.getExecutionYear() == ey).findAny().orElse(null);
    }

    public static CourseGradingTable find(final CurriculumLine line) {
        final String grade =
                ((ICurriculumEntry) line).getGrade().isEmpty() ? "-" : ((ICurriculumEntry) line).getGrade().getValue();
        //Return the table associated with this line if and only if it has a valid value for the final grade
        if (line.getCourseGradingTable() != null && line.getCourseGradingTable().getEctsGrade(grade) != null) {
            return line.getCourseGradingTable();
        }

        ExecutionYear year = line.getExecutionYear();
        if (line instanceof Enrolment) {
            final EnrolmentEvaluation evaluation = ((Enrolment) line).getFinalEnrolmentEvaluation();

            if (evaluation != null) {
                year = evaluation.getExecutionPeriod().getExecutionYear();
            }
        }

        return find(year, line.getCurricularCourse().getCompetenceCourse());
    }

    public static String getEctsGrade(final ICurriculumEntry entry) {
        final String grade = entry.getGrade().isEmpty() ? "-" : entry.getGrade().getValue();
        String ectsGrade = null;
        if (entry instanceof ExternalEnrolment) {
            final ExternalEnrolment externalEnrolment = (ExternalEnrolment) entry;
            if (externalEnrolment.getEctsGrade() != null && !externalEnrolment.getEctsGrade().isEmpty()) {
                ectsGrade = externalEnrolment.getEctsGrade().getValue();
            } else {
                ectsGrade = DefaultGradingTable.getDefaultGradingTable().getEctsGrade(grade);
            }
        } else if (entry instanceof CurriculumLine) {
            CurriculumLine line = (CurriculumLine) entry;
            if (line.getCurricularCourse() != null) {
                CourseGradingTable table = find(line);
                if (table != null) {
                    ectsGrade = table.getEctsGrade(grade);
                }
            }
        }
        return ectsGrade != null ? ectsGrade : "-";
    }

    public static boolean isApplicable(final CurriculumLine line) {
        return GradingTableSettings.getApplicableDegreeTypes().contains(line.getCurricularCourse().getDegreeType());
    }

    public static Set<CourseGradingTable> generate(final ExecutionYear executionYear) {
        Set<CourseGradingTable> allTables = new HashSet<>();
        for (CompetenceCourse cc : Bennu.getInstance().getCompetenceCoursesSet()) {
            if (cc.hasActiveScopesInExecutionYear(executionYear)) {
                CourseGradingTable table = CourseGradingTable.find(executionYear, cc);
                if (table == null) {

                    CallableWithoutException<CourseGradingTable> workerLogic =
                            new CallableWithoutException<CourseGradingTable>() {
                                @Override
                                public CourseGradingTable call() {
                                    CourseGradingTable table = new CourseGradingTable();
                                    table.setExecutionYear(executionYear);
                                    table.setCompetenceCourse(cc);
                                    table.compileData();

                                    return table;
                                }
                            };
                    GeneratorWorker<CourseGradingTable> worker = new GeneratorWorker<>(workerLogic);
                    worker.start();
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    allTables.add(worker.getTable());
                } else {
                    allTables.add(table);
                }
            }
        }
        return allTables;
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        List<Enrolment> harvestEnrolmentsUsedInSample = harvestEnrolmentsUsedInSample();

        // harvestEnrolmentsUsedInSample will be the empty list if requirements weren't met.
        List<BigDecimal> sample = harvestEnrolmentsUsedInSample.stream().map(e -> e.getGrade()).filter(e -> isNumeric(e))
                .map(e -> e.getNumericValue().setScale(0, RoundingMode.HALF_UP)).collect(Collectors.toList());

        final String harvestEnrolmentsUsedInSampleData = harvestEnrolmentsUsedInSample.stream().map(e -> enrolmentStringData(e))
                .reduce((a, c) -> a + "\n" + c).orElseGet(() -> "");
        setStudentSampleData(harvestEnrolmentsUsedInSampleData);

        if (sample.isEmpty()) { // Using the default table
            GradingTableGenerator.defaultData(this);
            setCopied(true);
        } else {
            GradingTableGenerator.generateTableDataImprovement(this, sample);
        }
        checkUniquenessOfTable();
    }

    private void checkUniquenessOfTable() {
        if (CourseGradingTable.find(getExecutionYear()).stream()
                .anyMatch(t -> t != this && t.getCompetenceCourse() == getCompetenceCourse())) {
            throw new AcademicExtensionsDomainException("error.CourseGradingTable.already.exists",
                    getExecutionYear().getQualifiedName(),
                    getCompetenceCourse().getCode() + " - " + getCompetenceCourse().getName());
        }
    }

    private boolean isNumeric(final Grade grade) {
        if (grade == null) {
            return false;
        }
        try {
            Double.parseDouble(grade.getValue());
            if (grade.getNumericValue() != null) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String enrolmentStringData(Enrolment e) {
        Integer studentNumber = e.getStudent().getNumber();
        String studentName = e.getStudent().getName();
        String competenceCourseCode = e.getCurricularCourse().getCompetenceCourse().getCode();
        String executionYearName = e.getExecutionYear().getQualifiedName();
        Integer finalGrade =
                isNumeric(e.getGrade()) ? e.getGrade().getNumericValue().setScale(0, RoundingMode.HALF_UP).intValue() : 0;

        return String.format("%s\t%s\t%s\t%s\t%s", studentNumber, studentName, competenceCourseCode, executionYearName,
                finalGrade);
    }

    private List<Enrolment> harvestEnrolmentsUsedInSample() {
        List<Enrolment> sample = new ArrayList<>();
        int coveredYears = 0;
        boolean sampleReady = false;
        ExecutionYear samplingYear = getExecutionYear()
                .getPrevious() instanceof ExecutionYear ? ((ExecutionYear) getExecutionYear().getPrevious()) : null;

        GradeScale gradeScale = null;
        while (samplingYear != null) {
            for (final CurricularCourse curricularCourse : getCompetenceCourse().getAssociatedCurricularCoursesSet()) {
                if (!GradingTableSettings.getApplicableDegreeTypes().contains(curricularCourse.getDegreeType())) {
                    continue;
                }
                List<Enrolment> enrolmentsByExecutionYear = curricularCourse.getEnrolmentsByExecutionYear(samplingYear);
                for (Enrolment enrolment : enrolmentsByExecutionYear) {

                    if (!enrolment.isApproved()) {
                        continue;
                    }

                    if (!enrolment.getGrade().isNumeric()) {
                        continue;
                    }

                    if (gradeScale != null) {
                        if (enrolment.getGrade().getGradeScale() != gradeScale) {
                            throw new DomainException(
                                    "error.CourseGradingTable.harvestEnrolmentsUsedInSample.gradeScale.mismatch");
                        }
                    } else {
                        gradeScale = enrolment.getGrade().getGradeScale();
                    }

                    sample.add(enrolment);
                }
            }
            coveredYears++; // A full year has been covered

            // A sample is OK if we covered:
            // 1. More than the minimum number of years
            // 2. Got enough entries
            // 3. Didn't exceed the maximum years (3-5)
            sampleReady = coveredYears >= GradingTableSettings.getMinimumPastYears()
                    && sample.size() >= GradingTableSettings.getMinimumSampleSize()
                    && coveredYears <= GradingTableSettings.getMaximumPastYears();

            // Updates the year for the next iteration
            // If the maximum number of years has been met or the sample is ready, sets the year to null and stops the cycle.
            samplingYear = !(coveredYears >= GradingTableSettings.getMaximumPastYears()) && !sampleReady
                    && samplingYear.getPrevious() instanceof ExecutionYear ? ((ExecutionYear) samplingYear.getPrevious()) : null;
        }
        // This now returns an empty list when unable to collect a sample.
        return sampleReady ? sample : Collections.emptyList();
    }

}
