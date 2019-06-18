package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EctsAndWeightProviderRegistry;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;

public class EctsAndWeightProviders {

    static public void init() {
        initForEnrolment();
        initForDismissal();
    }

    protected static void initForEnrolment() {

        EctsAndWeightProviderRegistry.setEctsProvider(Enrolment.class, x ->
        {
            final Enrolment enrolment = (Enrolment) x;

            final BigDecimal ectsOverride = CurriculumLineServices.getEctsCredits(enrolment);
            if (ectsOverride != null) {
                return ectsOverride;
            }

            return BigDecimal.valueOf(enrolment.getCurricularCourse().getEctsCredits(enrolment.getExecutionPeriod()));
        });

        EctsAndWeightProviderRegistry.setEctsForCurriculumProvider(Enrolment.class, x ->
        {
            return BigDecimal.valueOf(((Enrolment) x).getEctsCredits());
        });

        EctsAndWeightProviderRegistry.setWeightProvider(Enrolment.class, x ->
        {
            final Enrolment enrolment = (Enrolment) x;

            final BigDecimal weightOverride = CurriculumLineServices.getWeight(enrolment);
            if (weightOverride != null) {
                return weightOverride;
            }

            return enrolment.getCurricularCourse().getBaseWeight() != null ? new BigDecimal(
                    enrolment.getCurricularCourse().getBaseWeight()) : null;

        });

        EctsAndWeightProviderRegistry.setWeightForCurriculumProvider(Enrolment.class, x ->
        {
            final Enrolment enrolment = (Enrolment) x;

            if (enrolment.getEctsCreditsForCurriculum() != null && enrolment.getEctsCreditsForCurriculum().doubleValue() != 0) {
                return enrolment.getEctsCreditsForCurriculum();
            }

            if (enrolment.getWeigth() != null && enrolment.getWeigth().doubleValue() != 0) {
                return BigDecimal.valueOf(enrolment.getWeigth());
            }

            return BigDecimal.ZERO;

        });
    }

    private static void initForDismissal() {

        EctsAndWeightProviderRegistry.setEctsProvider(Dismissal.class, x ->
        {
            final Dismissal dismissal = (Dismissal) x;

            final BigDecimal ectsOverride = CurriculumLineServices.getEctsCredits(dismissal);
            if (ectsOverride != null) {
                return ectsOverride;
            }

            return BigDecimal
                    .valueOf(dismissal.getCurricularCourse().getEctsCredits(dismissal.getCredits().getExecutionPeriod()));
        });

        EctsAndWeightProviderRegistry.setEctsForCurriculumProvider(Dismissal.class, x ->
        {
            return BigDecimal.valueOf(((Dismissal) x).getEctsCredits());
        });

        EctsAndWeightProviderRegistry.setWeightProvider(Dismissal.class, x ->
        {
            final Dismissal dismissal = (Dismissal) x;

            final BigDecimal weightOverride = CurriculumLineServices.getWeight(dismissal);
            if (weightOverride != null) {
                return weightOverride;
            }

            final CurricularCourse curricularCourse = dismissal.getCurricularCourse();
            return curricularCourse != null && curricularCourse.getBaseWeight() != null ? BigDecimal
                    .valueOf(curricularCourse.getBaseWeight()) : null;

        });

        EctsAndWeightProviderRegistry.setWeightForCurriculumProvider(Dismissal.class, x ->
        {
            final Dismissal dismissal = (Dismissal) x;

            if (dismissal.getEctsCreditsForCurriculum() != null && dismissal.getEctsCreditsForCurriculum().doubleValue() != 0) {
                return dismissal.getEctsCreditsForCurriculum();
            }

            if (dismissal.getWeigth() != null && dismissal.getWeigth().doubleValue() != 0) {
                return BigDecimal.valueOf(dismissal.getWeigth());
            }

            return BigDecimal.ZERO;
        });

    }

}
