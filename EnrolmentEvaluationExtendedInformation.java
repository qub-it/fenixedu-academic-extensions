package org.fenixedu.academic.domain.evaluation;

import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;

import pt.ist.fenixframework.FenixFramework;

public class EnrolmentEvaluationExtendedInformation extends EnrolmentEvaluationExtendedInformation_Base {

    public EnrolmentEvaluationExtendedInformation() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    static public void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(EnrolmentEvaluation.class, evaluation ->
        {
            if (evaluation.getExtendedInformation() != null) {
                evaluation.getExtendedInformation().delete();
            }
        });
    }

    private void delete() {

        super.setEnrolmentEvaluation(null);
        super.setBennu(null);

        super.deleteDomainObject();

    }

    static public EnrolmentEvaluationExtendedInformation findOrCreate(EnrolmentEvaluation evaluation) {
        return evaluation.getExtendedInformation() != null ? evaluation.getExtendedInformation() : create(evaluation);
    }

    static public EnrolmentEvaluationExtendedInformation create(EnrolmentEvaluation evaluation) {
        final EnrolmentEvaluationExtendedInformation result = new EnrolmentEvaluationExtendedInformation();
        result.init(evaluation);

        return result;
    }

    protected void init(EnrolmentEvaluation evaluation) {
        super.setEnrolmentEvaluation(evaluation);

        checkRules();

    }

    private void checkRules() {
        if (getEnrolmentEvaluation() == null) {
            throw new ULisboaSpecificationsDomainException(
                    "error.EnrolmentEvaluationExtendedInformation.enrolmentEvaluation.cannot.be.null");
        }
    }

}
