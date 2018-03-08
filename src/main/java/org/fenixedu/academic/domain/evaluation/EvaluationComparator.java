package org.fenixedu.academic.domain.evaluation;

import java.util.Comparator;

import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;

/*
 * Based on original EvaluationConfiguration.EnrolmentComparator
 * 
 *
 */
public class EvaluationComparator implements Comparator<EnrolmentEvaluation> {

    @Override
    public int compare(final EnrolmentEvaluation left, final EnrolmentEvaluation right) {

        if (isFinalInAnyMarkSheet(left) && !isFinalInAnyMarkSheet(right)) {

            if (!isSpecialAuthorization(right)) {
                return 1;
            }
        }

        if (!isFinalInAnyMarkSheet(left) && isFinalInAnyMarkSheet(right)) {

            if (!isSpecialAuthorization(left)) {
                return -1;
            }
        }

        return compareBySpecialAuthorizationAndStateAndGradeAndEvaluationDate(left, right);
    }

    static private int compareBySpecialAuthorizationAndStateAndGradeAndEvaluationDate(final EnrolmentEvaluation left,
            final EnrolmentEvaluation right) {

        if (isSpecialAuthorization(left) && !isSpecialAuthorization(right)) {

            if (!isFinalInAnyMarkSheet(right)) {
                return 1;
            }
        }

        if (!isSpecialAuthorization(left) && isSpecialAuthorization(right)) {

            if (!isFinalInAnyMarkSheet(left)) {
                return -1;
            }
        }

        return compareByStateAndGradeAndEvaluationDate(left, right);
    }

    static private boolean isFinalInAnyMarkSheet(final EnrolmentEvaluation input) {
        return input != null && input.getCompetenceCourseMarkSheet() != null && input.isFinal();
    }

    static private boolean isSpecialAuthorization(final EnrolmentEvaluation input) {
        return input != null && input.getEvaluationSeason().isSpecialAuthorization();
    }

    static private int compareByStateAndGradeAndEvaluationDate(final EnrolmentEvaluation left, final EnrolmentEvaluation right) {
        final EnrollmentState leftEnrolmentState = left.getEnrollmentStateByGrade();
        final EnrollmentState rightEnrolmentState = right.getEnrollmentStateByGrade();

        if (leftEnrolmentState == EnrollmentState.APROVED && rightEnrolmentState == EnrollmentState.APROVED) {
            return compareByGradeAndEvaluationDate(left, right, true);
        } else if (leftEnrolmentState == EnrollmentState.APROVED) {
            return 1;
        } else if (rightEnrolmentState == EnrollmentState.APROVED) {
            return -1;
        } else {
            return compareByGradeAndEvaluationDate(left, right, false);
        }
    }

    static private int compareByGradeAndEvaluationDate(final EnrolmentEvaluation left, final EnrolmentEvaluation right,
            final boolean bothApproved) {

        int result = left.getGrade().compareTo(right.getGrade());
        if (result != 0) {
            return result;
        }

        if (bothApproved) {
            // TODO legidio, use EnrolmentEvaluationServices.getExamDateTime ?
            result = Comparator.comparing(EnrolmentEvaluation::getExamDateYearMonthDay).reversed().compare(left, right);
            if (result != 0) {
                return result;
            }
        }

        result = EvaluationSeasonServices.SEASON_ORDER_COMPARATOR.compare(left.getEvaluationSeason(),
                right.getEvaluationSeason());
        if (result != 0) {
            return result;
        }

        result = Comparator.comparing(EnrolmentEvaluation::getExternalId).reversed().compare(left, right);
        if (result != 0) {
            return result;
        }

        return result;
    }

}
