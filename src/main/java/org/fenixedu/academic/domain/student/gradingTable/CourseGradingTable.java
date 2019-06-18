package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
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
            DefaultGradingTable table = DefaultGradingTable.getDefaultGradingTable();
            if (table != null) {
                ectsGrade = table.getEctsGrade(grade);
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
        List<BigDecimal> sample = harvestSample();
        if (sample != null) {
            GradingTableGenerator.generateTableData(this, sample);
        } else {
            GradingTableGenerator.defaultData(this);
            setCopied(true);
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

    private List<BigDecimal> harvestSample() {
        List<BigDecimal> sample = new ArrayList<>();
        int coveredYears = 0;
        boolean sampleOK = false;
        for (ExecutionYear year = getExecutionYear().getPreviousExecutionYear(); year != null; year =
                year.getPreviousExecutionYear()) {
            for (final CurricularCourse curricularCourse : getCompetenceCourse().getAssociatedCurricularCoursesSet()) {
                if (!GradingTableSettings.getApplicableDegreeTypes().contains(curricularCourse.getDegreeType())) {
                    continue;
                }
                List<Enrolment> enrolmentsByExecutionYear = curricularCourse.getEnrolmentsByExecutionYear(year);
                for (Enrolment enrolment : enrolmentsByExecutionYear) {
                    if (!enrolment.isApproved()) {
                        continue;
                    }

                    if (!GradeScale.TYPE20.equals(enrolment.getGrade().getGradeScale())) {
                        continue;
                    }

                    Integer finalGrade = isNumeric(enrolment.getGrade()) ? enrolment.getGrade().getNumericValue()
                            .setScale(0, RoundingMode.HALF_UP).intValue() : 0;
                    if (finalGrade == 0) {
                        continue;
                    }
                    sample.add(new BigDecimal(finalGrade));
                }
            }

            if (++coveredYears >= GradingTableSettings.getMinimumPastYears()
                    && sample.size() >= GradingTableSettings.getMinimumSampleSize()) {
                sampleOK = true;
                break;
            }

            if (coveredYears == GradingTableSettings.getMaximumPastYears()) {
                break;
            }
        }
        return sampleOK ? sample : null;
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
}
