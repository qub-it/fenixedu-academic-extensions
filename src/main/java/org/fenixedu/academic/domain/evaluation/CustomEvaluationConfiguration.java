package org.fenixedu.academic.domain.evaluation;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.bennu.core.domain.Bennu;

public class CustomEvaluationConfiguration extends CustomEvaluationConfiguration_Base {

    private static Comparator<EnrolmentEvaluation> ENROLMENT_EVALUATION_ORDER = new EvaluationComparator();

    private static Predicate<Enrolment> ENROLMENT_FILTER = (e) -> false;

    static public void setEnrolmentFilter(Predicate<Enrolment> predicate) {
        ENROLMENT_FILTER = predicate;
    }

    public CustomEvaluationConfiguration() {
        setRoot(Bennu.getInstance());
    }

    @Override
    public Optional<EnrolmentEvaluation> getFinalEnrolmentEvaluation(Enrolment enrolment) {
        final Predicate<EnrolmentEvaluation> isFinal = EnrolmentEvaluation::isFinal;
        final Predicate<EnrolmentEvaluation> isSpecialAuthorization = getSpecialAuthorizationFilter(enrolment);
        return enrolment.getEvaluationsSet().stream().filter(isFinal.and(isSpecialAuthorization)).max(ENROLMENT_EVALUATION_ORDER);
    }

    @Override
    public Optional<EnrolmentEvaluation> getFinalEnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season) {
        final Predicate<EnrolmentEvaluation> isFinal = EnrolmentEvaluation::isFinal;
        final Predicate<EnrolmentEvaluation> isSeason = e -> e.getEvaluationSeason().equals(season);
        return enrolment.getEvaluationsSet().stream().filter(isFinal.and(isSeason)).max(ENROLMENT_EVALUATION_ORDER);
    }

    @Override
    public Optional<EnrolmentEvaluation> getEnrolmentEvaluationForConclusionDate(Enrolment enrolment) {
        Predicate<EnrolmentEvaluation> isFinal = EnrolmentEvaluation::isFinal;
        Predicate<EnrolmentEvaluation> isImprovement = e -> e.getEvaluationSeason().isImprovement();
        Predicate<EnrolmentEvaluation> hasExam = e -> e.getExamDateYearMonthDay() != null;
        final Predicate<EnrolmentEvaluation> isSpecialAuthorization = getSpecialAuthorizationFilter(enrolment);
        return enrolment.getEvaluationsSet().stream()
                .filter(isFinal.and(isSpecialAuthorization).and(isImprovement.negate()).and(hasExam))
                .max(ENROLMENT_EVALUATION_ORDER);
    }

    private Predicate<EnrolmentEvaluation> getSpecialAuthorizationFilter(Enrolment enrolment) {
        return isToApplySpecialAuthorization(enrolment) ? x -> x.getEvaluationSeason().getSpecialAuthorization() : x -> true;
    }

    protected boolean isToApplySpecialAuthorization(Enrolment enrolment) {
        return ENROLMENT_FILTER.test(enrolment);
    }

    @Override
    public Optional<EnrolmentEvaluation> getCurrentEnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season) {
        Predicate<EnrolmentEvaluation> isSeason = e -> e.getEvaluationSeason().equals(season);
        return enrolment.getEvaluationsSet().stream().filter(isSeason).max(EnrolmentEvaluation.COMPARATORY_BY_WHEN);
    }

}
