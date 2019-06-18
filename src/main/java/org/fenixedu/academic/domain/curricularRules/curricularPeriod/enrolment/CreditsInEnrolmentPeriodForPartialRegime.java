package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;

import pt.ist.fenixframework.Atomic;

public class CreditsInEnrolmentPeriodForPartialRegime extends CreditsInEnrolmentPeriodForPartialRegime_Base {

    protected CreditsInEnrolmentPeriodForPartialRegime() {
        super();
    }

    @Atomic
    static public CreditsInEnrolmentPeriodForPartialRegime create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer semester, final Integer year) {

        final CreditsInEnrolmentPeriodForPartialRegime result = new CreditsInEnrolmentPeriodForPartialRegime();
        result.init(configuration, credits, semester);
        result.setYearMin(year);
        result.setYearMax(year);

        return result;
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {
        return enrolmentContext.getRegistration()
                .isPartialRegime(enrolmentContext.getExecutionYear()) ? super.execute(enrolmentContext) : createNA();

    }

}
