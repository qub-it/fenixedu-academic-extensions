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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheet;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.academic.dto.evaluation.markSheet.report.CompetenceCourseSeasonReport;
import org.fenixedu.academic.dto.evaluation.markSheet.report.ExecutionCourseSeasonReport;
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

        final Stream<CompetenceCourse> allCompetenceCourses = Stream.concat(
                executionInterval.getAssociatedExecutionCoursesSet().stream().flatMap(eC -> eC.getCompetenceCourses().stream()),
                executionInterval.getEnrolmentEvaluationsSet().stream()
                        .map(eE -> eE.getEnrolment().getCurricularCourse().getCompetenceCourse()));

        return iterateCompetenceCourses(executionInterval, allCompetenceCourses.collect(Collectors.toSet()), seasons);
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
            throw new RuntimeException(errors.stream().map(Throwable::getMessage).distinct().collect(Collectors.joining("\n")));
        }

        return result;
    }

    @Atomic(mode = TxMode.READ)
    static private List<CompetenceCourseSeasonReport> iterateCompetenceCoursesBlock(final ExecutionInterval interval,
            final Collection<CompetenceCourse> toProcess, final Set<EvaluationSeason> seasons) {
        return toProcess.stream().flatMap(iter -> getReportsForCompetenceCourse(interval, iter, seasons).stream()).toList();
    }

    static public List<CompetenceCourseSeasonReport> getReportsForCompetenceCourse(final ExecutionInterval interval,
            final CompetenceCourse toProcess, final Set<EvaluationSeason> seasons) {
        return seasons.stream().map(s -> generateReport(interval, toProcess, s)).filter(r -> r.getTotalStudents() > 0)
                .collect(Collectors.toList());
    }

    static private CompetenceCourseSeasonReport generateReport(final ExecutionInterval interval, final CompetenceCourse toProcess,
            final EvaluationSeason season) {
        final CompetenceCourseSeasonReport result = new CompetenceCourseSeasonReport(toProcess, season, interval, null);

        // setNotEvaluatedStudents
        final AtomicInteger notEvaluatedStudents = new AtomicInteger();
        toProcess.getExecutionCoursesByExecutionPeriod(interval).forEach(i -> notEvaluatedStudents.addAndGet(
                CompetenceCourseMarkSheet.getExecutionCourseEnrolmentsNotInAnyMarkSheet(interval, toProcess, i, season, null,
                        Set.of()).size()));
        result.setNotEvaluatedStudents(notEvaluatedStudents.get());

        final Set<Enrolment> enrolments = Sets.newHashSet();
        toProcess.getAssociatedCurricularCoursesSet()
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
            final Optional<EnrolmentEvaluation> evaluation = enrolment.getEnrolmentEvaluation(season, interval, null);
            if (evaluation.isPresent() && evaluation.get().getCompetenceCourseMarkSheet() != null) {
                evaluatedStudents++;
            }
        }
        result.setEvaluatedStudents(evaluatedStudents);

        for (CompetenceCourseMarkSheet ccm : CompetenceCourseMarkSheet.findBy(interval, toProcess, null, season, null, null, null,
                null).collect(Collectors.toSet())) {
            if (ccm.getEnrolmentEvaluationSet().isEmpty() && ccm.getExecutionCourseEnrolmentsNotInAnyMarkSheet().isEmpty()) {
                continue;
            }
            if (ccm.isConfirmed()) {
                result.incMarksheetsConfirmed();
            } else if (ccm.isEdition()) {
                result.incMarksheetsInEdition();
            } else if (ccm.isSubmitted()) {
                result.incMarksheetsSubmitted();
            }
        }

        return result;
    }

}
