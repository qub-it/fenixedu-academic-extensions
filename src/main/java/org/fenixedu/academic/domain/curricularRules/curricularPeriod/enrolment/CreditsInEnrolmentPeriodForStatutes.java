package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class CreditsInEnrolmentPeriodForStatutes extends CreditsInEnrolmentPeriodForStatutes_Base {

    protected CreditsInEnrolmentPeriodForStatutes() {
        super();
    }

    @Atomic
    public static CreditsInEnrolmentPeriodForStatutes create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits) {
        final CreditsInEnrolmentPeriodForStatutes result = new CreditsInEnrolmentPeriodForStatutes();
        result.init(configuration, credits, null /*semester*/);

        return result;
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {
        return hasValidStatute(enrolmentContext) ? super.execute(enrolmentContext) : createNA();
    }

    @Override
    public String getLabel() {
        return getStatuteTypesLabelPrefix() + BundleUtil.getString(MODULE_BUNDLE,
                "label." + this.getClass().getSimpleName() + (getSemester() != null ? ".semester" : ""), getCredits().toString());
    }

}
