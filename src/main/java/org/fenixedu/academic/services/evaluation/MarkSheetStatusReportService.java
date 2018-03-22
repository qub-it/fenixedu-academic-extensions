package org.fenixedu.academic.services.evaluation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheet;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetChangeRequestStateEnum;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetStateEnum;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.academic.dto.evaluation.markSheet.report.CompetenceCourseSeasonReport;
import org.fenixedu.academic.dto.evaluation.markSheet.report.ExecutionCourseSeasonReport;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * @see {@link com.qubit.qubEdu.module.academicOffice.domain.grade.marksheet.report.MarkSheetStatusReportService}
 */
abstract public class MarkSheetStatusReportService {

    static public List<ExecutionCourseSeasonReport> getReportsForExecutionCourse(final ExecutionCourse executionCourse) {

        final List<ExecutionCourseSeasonReport> result = Lists.newArrayList();

        for (final EvaluationSeason season : EvaluationSeasonServices.findByActive(true).collect(Collectors.toList())) {

            final Multimap<LocalDate, CompetenceCourseSeasonReport> reportsByEvaluationDate = ArrayListMultimap.create();
            for (final CompetenceCourseSeasonReport report : iterateCompetenceCourses(executionCourse.getExecutionPeriod(),
                    executionCourse.getCompetenceCourses(), Sets.newHashSet(season))) {

                reportsByEvaluationDate.put(report.getEvaluationDate(), report);
            }

            for (final Map.Entry<LocalDate, Collection<CompetenceCourseSeasonReport>> entry : reportsByEvaluationDate.asMap()
                    .entrySet()) {

                final ExecutionCourseSeasonReport report =
                        new ExecutionCourseSeasonReport(executionCourse, season, entry.getKey(), entry.getValue());
                if (report.getTotalStudents().intValue() > 0) {
                    result.add(report);
                }
            }
        }

        return result;
    }

    static public List<CompetenceCourseSeasonReport> getReportsForCompetenceCourses(final ExecutionInterval executionInterval) {
        return getReportsForCompetenceCourses(executionInterval,
                EvaluationSeasonServices.findByActive(true).collect(Collectors.toSet()));
    }

    static public List<CompetenceCourseSeasonReport> getReportsForCompetenceCourses(final ExecutionInterval executionInterval,
            final Set<EvaluationSeason> seasons) {

        final List<CompetenceCourseSeasonReport> result = Lists.newArrayList();

        final ExecutionSemester semester =
                ExecutionInterval.assertExecutionIntervalType(ExecutionSemester.class, executionInterval);
        final Set<CompetenceCourse> toProcess = collectCompetenceCourses(semester);

        result.addAll(iterateCompetenceCourses(semester, toProcess, seasons));

        return result;
    }

    static private Set<CompetenceCourse> collectCompetenceCourses(final ExecutionSemester semester) {

        final Set<CompetenceCourse> result = Sets.newHashSet();

        for (final ExecutionCourse executionCourse : semester.getAssociatedExecutionCoursesSet()) {
            result.addAll(executionCourse.getCompetenceCourses());
        }

        // improvement of evaluations approved in previous years
        for (final EnrolmentEvaluation evaluation : semester.getEnrolmentEvaluationsSet()) {
            result.add(evaluation.getEnrolment().getCurricularCourse().getCompetenceCourse());
        }

        return result;
    }

    static private List<CompetenceCourseSeasonReport> iterateCompetenceCourses(final ExecutionSemester semester,
            final Set<CompetenceCourse> toProcess, final Set<EvaluationSeason> seasons) {

        final List<CompetenceCourseSeasonReport> result = Lists.newArrayList();

        for (final CompetenceCourse iter : toProcess) {
            result.addAll(getReportsForCompetenceCourse(semester, iter, seasons));
        }

        return result;
    }

    static public List<CompetenceCourseSeasonReport> getReportsForCompetenceCourse(final ExecutionSemester semester,
            final CompetenceCourse toProcess, final Set<EvaluationSeason> seasons) {

        final List<CompetenceCourseSeasonReport> result = Lists.newArrayList();

        for (final EvaluationSeason season : seasons) {

            addNonEmptyReport(result, generateReport(semester, toProcess, season, (LocalDate) null));
        }

        return result;
    }

    static private void addNonEmptyReport(final List<CompetenceCourseSeasonReport> result,
            final CompetenceCourseSeasonReport report) {

        if (report.getTotalStudents().intValue() > 0) {
            result.add(report);
        }
    }

    static private CompetenceCourseSeasonReport generateReport(final ExecutionSemester semester, final CompetenceCourse toProcess,
            final EvaluationSeason season, final LocalDate evaluationDate) {

        final CompetenceCourseSeasonReport result = new CompetenceCourseSeasonReport(toProcess, season, semester, evaluationDate);

        // setNotEvaluatedStudents
        final AtomicInteger notEvaluatedStudents = new AtomicInteger(0);
        toProcess.getExecutionCoursesByExecutionPeriod(semester).stream()
                .forEach(i -> notEvaluatedStudents
                        .addAndGet(CompetenceCourseMarkSheet.getExecutionCourseEnrolmentsNotInAnyMarkSheet(semester, toProcess, i,
                                season, (LocalDate) null, Sets.newHashSet()).size()));
        result.setNotEvaluatedStudents(notEvaluatedStudents.get());

        final Set<Enrolment> enrolments = Sets.newHashSet();
        toProcess.getAssociatedCurricularCoursesSet().stream()
                .forEach(i -> enrolments.addAll(i.getEnrolmentsByAcademicInterval(semester.getAcademicInterval())));

        // improvement of evaluations approved in previous years
        for (final EnrolmentEvaluation evaluation : semester.getEnrolmentEvaluationsSet()) {
            if (evaluation.getEvaluationSeason() == season
                    && evaluation.getEnrolment().getCurricularCourse().getCompetenceCourse() == toProcess) {
                enrolments.add(evaluation.getEnrolment());
            }
        }

        // setEvaluatedStudents
        int evaluatedStudents = 0;
        for (final Enrolment enrolment : enrolments) {

            final Optional<EnrolmentEvaluation> evaluation = enrolment.getEnrolmentEvaluation(season, semester, (Boolean) null);
            if (evaluation.isPresent() && evaluation.get().getCompetenceCourseMarkSheet() != null) {
                evaluatedStudents = evaluatedStudents + 1;
            }
        }
        result.setEvaluatedStudents(evaluatedStudents);

        // setMarksheetsTotal
        final Supplier<Stream<CompetenceCourseMarkSheet>> supplier = () -> CompetenceCourseMarkSheet.findBy(semester, toProcess,
                (ExecutionCourse) null, season, (DateTime) null, (Set<Shift>) null, (CompetenceCourseMarkSheetStateEnum) null,
                (CompetenceCourseMarkSheetChangeRequestStateEnum) null);
        final long markSheetsTotal = supplier.get().count();
        result.setMarksheetsTotal(Long.valueOf(markSheetsTotal).intValue());

        // setMarksheetsToConfirm
        final long markSheetsToConfirm = supplier.get().filter(markSheet -> !markSheet.isConfirmed()).count();
        result.setMarksheetsToConfirm(Long.valueOf(markSheetsToConfirm).intValue());

        return result;
    }

}
