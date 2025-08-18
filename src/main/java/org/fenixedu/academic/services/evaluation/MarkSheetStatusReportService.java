package org.fenixedu.academic.services.evaluation;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

/**
 * @see {@link com.qubit.qubEdu.module.academicOffice.domain.grade.marksheet.report.MarkSheetStatusReportService}
 */
abstract public class MarkSheetStatusReportService {

    private static final int THREAD_BLOCK_SIZE = 10;

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

        final Set<CompetenceCourse> toProcess = collectCompetenceCourses(executionInterval);

        result.addAll(iterateCompetenceCourses(executionInterval, toProcess, seasons));

        return result;
    }

    static private Set<CompetenceCourse> collectCompetenceCourses(final ExecutionInterval interval) {

        final Set<CompetenceCourse> result = Sets.newHashSet();

        for (final ExecutionCourse executionCourse : interval.getAssociatedExecutionCoursesSet()) {
            result.addAll(executionCourse.getCompetenceCourses());
        }

        // improvement of evaluations approved in previous years
        for (final EnrolmentEvaluation evaluation : interval.getEnrolmentEvaluationsSet()) {
            result.add(evaluation.getEnrolment().getCurricularCourse().getCompetenceCourse());
        }

        return result;
    }

    private static List<CompetenceCourseSeasonReport> iterateCompetenceCourses(final ExecutionInterval interval,
            final Set<CompetenceCourse> competenceCourses, final Set<EvaluationSeason> seasons) {
        final List<Throwable> errors = synchronizedList(new ArrayList<>());
        final List<CompetenceCourseSeasonReport> result = synchronizedList(new ArrayList<>());
        final List<CompetenceCourse> toProcess = new ArrayList<>(competenceCourses);
        final ExecutorService executor =
                Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 4, 1));

        int startIndex = 0;
        while (startIndex < toProcess.size()) {
            final int endIndex = Math.min(startIndex + THREAD_BLOCK_SIZE, toProcess.size());
            final List<CompetenceCourse> block = toProcess.subList(startIndex, endIndex);
            executor.submit(() -> {
                try {
                    result.addAll(iterateCompetenceCoursesBlock(interval, block, seasons));
                } catch (Throwable e) {
                    errors.add(e.getCause() != null ? e.getCause() : e);
                }
            });

            startIndex = endIndex;
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            //ignore
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.stream().map(e -> e.getMessage()).distinct().collect(Collectors.joining("\n")));
        }

        return result;
    }

    @Atomic(mode = TxMode.READ)
    static private List<CompetenceCourseSeasonReport> iterateCompetenceCoursesBlock(final ExecutionInterval interval,
            final Collection<CompetenceCourse> toProcess, final Set<EvaluationSeason> seasons) {

        final List<CompetenceCourseSeasonReport> result = Lists.newArrayList();

        for (final CompetenceCourse iter : toProcess) {
            result.addAll(getReportsForCompetenceCourse(interval, iter, seasons));
        }

        return result;
    }

    static public List<CompetenceCourseSeasonReport> getReportsForCompetenceCourse(final ExecutionInterval interval,
            final CompetenceCourse toProcess, final Set<EvaluationSeason> seasons) {

        final List<CompetenceCourseSeasonReport> result = Lists.newArrayList();

        for (final EvaluationSeason season : seasons) {

            addNonEmptyReport(result, generateReport(interval, toProcess, season, (LocalDate) null));
        }

        return result;
    }

    static private void addNonEmptyReport(final List<CompetenceCourseSeasonReport> result,
            final CompetenceCourseSeasonReport report) {

        if (report != null && report.getTotalStudents().intValue() > 0) {
            result.add(report);
        }
    }

    static private CompetenceCourseSeasonReport generateReport(final ExecutionInterval interval, final CompetenceCourse toProcess,
            final EvaluationSeason season, final LocalDate evaluationDate) {

        // setMarksheetsTotal
        final Supplier<Stream<CompetenceCourseMarkSheet>> supplier =
                () -> CompetenceCourseMarkSheet.findBy(interval, toProcess, (ExecutionCourse) null, season, (DateTime) null,
                        (Set<Shift>) null, (CompetenceCourseMarkSheetStateEnum) null,
                        (CompetenceCourseMarkSheetChangeRequestStateEnum) null);
        if (supplier.get().anyMatch(ccm -> ccm.getEnrolmentEvaluationSet().isEmpty())) {
            return null;
        }

        final CompetenceCourseSeasonReport result = new CompetenceCourseSeasonReport(toProcess, season, interval, evaluationDate);

        // setNotEvaluatedStudents
        final AtomicInteger notEvaluatedStudents = new AtomicInteger(0);
        toProcess.getExecutionCoursesByExecutionPeriod(interval).stream().forEach(i -> notEvaluatedStudents.addAndGet(
                CompetenceCourseMarkSheet.getExecutionCourseEnrolmentsNotInAnyMarkSheet(interval, toProcess, i, season,
                        (LocalDate) null, Sets.newHashSet()).size()));
        result.setNotEvaluatedStudents(notEvaluatedStudents.get());

        final Set<Enrolment> enrolments = Sets.newHashSet();
        toProcess.getAssociatedCurricularCoursesSet().stream()
                .forEach(i -> enrolments.addAll(i.getEnrolmentsByAcademicInterval(interval.getAcademicInterval())));

        // improvement of evaluations approved in previous years
        for (final EnrolmentEvaluation evaluation : interval.getEnrolmentEvaluationsSet()) {
            if (evaluation.getEvaluationSeason() == season
                    && evaluation.getEnrolment().getCurricularCourse().getCompetenceCourse() == toProcess) {
                enrolments.add(evaluation.getEnrolment());
            }
        }

        // setEvaluatedStudents
        int evaluatedStudents = 0;
        for (final Enrolment enrolment : enrolments) {

            final Optional<EnrolmentEvaluation> evaluation = enrolment.getEnrolmentEvaluation(season, interval, (Boolean) null);
            if (evaluation.isPresent() && evaluation.get().getCompetenceCourseMarkSheet() != null) {
                evaluatedStudents = evaluatedStudents + 1;
            }
        }
        result.setEvaluatedStudents(evaluatedStudents);
        final long markSheetsTotal = supplier.get().count();
        result.setMarksheetsTotal(Long.valueOf(markSheetsTotal).intValue());

        // setMarksheetsToConfirm
        final long markSheetsToConfirm = supplier.get().filter(markSheet -> !markSheet.isConfirmed()).count();
        result.setMarksheetsToConfirm(Long.valueOf(markSheetsToConfirm).intValue());

        return result;
    }

}
