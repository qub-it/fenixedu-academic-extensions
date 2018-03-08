package org.fenixedu.academic.domain.evaluation;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.Evaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Shift;

import com.google.common.collect.Sets;

abstract public class EvaluationServices {

    // CourseEvaluationIgnoredInMarkSheet

    static private Function<Evaluation, Boolean> COURSE_EVALUATION_IGNORED_IN_MARK_SHEET = null;

    static public void setCourseEvaluationIgnoredInMarkSheet(final Function<Evaluation, Boolean> input) {
        COURSE_EVALUATION_IGNORED_IN_MARK_SHEET = input;
    }

    static public Boolean isCourseEvaluationIgnoredInMarkSheet(final Evaluation input) {
        return COURSE_EVALUATION_IGNORED_IN_MARK_SHEET == null ? Boolean.FALSE : COURSE_EVALUATION_IGNORED_IN_MARK_SHEET
                .apply(input);
    }

    // ExecutionCourseEvaluationsFinder

    static private BiFunction<ExecutionCourse, EvaluationSeason, Set<Evaluation>> EXECUTION_COURSE_EVALUATION_FINDER = null;

    static public void setExecutionCourseEvaluationsFinder(
            final BiFunction<ExecutionCourse, EvaluationSeason, Set<Evaluation>> input) {
        EXECUTION_COURSE_EVALUATION_FINDER = input;
    }

    static public Set<Evaluation> findExecutionCourseEvaluations(final ExecutionCourse execution, final EvaluationSeason season) {
        return EXECUTION_COURSE_EVALUATION_FINDER == null ? Sets.newHashSet() : EXECUTION_COURSE_EVALUATION_FINDER
                .apply(execution, season);
    }

    // CourseEvaluationShiftFinder

    static private Function<Evaluation, Set<Shift>> COURSE_EVALUATION_SHIFT_FINDER = null;

    static public void setCourseEvaluationShiftFinder(final Function<Evaluation, Set<Shift>> input) {
        COURSE_EVALUATION_SHIFT_FINDER = input;
    }

    static public Set<Shift> findCourseEvaluationShifts(final Evaluation input) {
        return COURSE_EVALUATION_SHIFT_FINDER == null ? Sets.newHashSet() : COURSE_EVALUATION_SHIFT_FINDER.apply(input);
    }

    // EnrolmentCourseEvaluationFinder

    static public interface IEnrolmentCourseEvaluationFinder {
        public Set<Evaluation> apply(final Enrolment enrolment, final EvaluationSeason season, final ExecutionSemester semester);
    }

    static private IEnrolmentCourseEvaluationFinder ENROLMENT_COURSE_EVALUATION_FINDER = null;

    static public void setEnrolmentCourseEvaluationFinder(final IEnrolmentCourseEvaluationFinder finder) {
        ENROLMENT_COURSE_EVALUATION_FINDER = finder;
    }

    static public Set<Evaluation> findEnrolmentCourseEvaluations(final Enrolment enrolment, final EvaluationSeason season,
            final ExecutionSemester semester) {
        return ENROLMENT_COURSE_EVALUATION_FINDER == null ? Sets.newHashSet() : ENROLMENT_COURSE_EVALUATION_FINDER
                .apply(enrolment, season, semester);
    }

    static public boolean isEnroledInAnyCourseEvaluation(final Enrolment enrolment, final EvaluationSeason season,
            final ExecutionSemester semester) {
        return !findEnrolmentCourseEvaluations(enrolment, season, semester).isEmpty();
    }

    static public boolean isEnroledInCourseEvaluation(final Enrolment enrolment, final EvaluationSeason season,
            final ExecutionSemester semester, final Evaluation evaluation) {
        return findEnrolmentCourseEvaluations(enrolment, season, semester).contains(evaluation);
    }

}
