package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Collection;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class CreditsInEnrolmentPeriodForRegistrationProtocols extends CreditsInEnrolmentPeriodForRegistrationProtocols_Base {

    protected CreditsInEnrolmentPeriodForRegistrationProtocols() {
        super();
    }

    protected void init(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            Collection<RegistrationProtocol> protocols) {
        super.init(configuration, credits, null /*semester*/);
        getRegistrationProtocolsSet().addAll(protocols);
        checkRules();
    }

    private void checkRules() {
        if (getRegistrationProtocolsSet().isEmpty()) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".registrationProtocols.is.required");
        }
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {
        return hasValidRegistrationProtocol(enrolmentContext) ? super.execute(enrolmentContext) : createNA();
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getCredits().toString());
    }

    public static CreditsInEnrolmentPeriodForRegistrationProtocols create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, Collection<RegistrationProtocol> protocols) {
        final CreditsInEnrolmentPeriodForRegistrationProtocols result = new CreditsInEnrolmentPeriodForRegistrationProtocols();
        result.init(configuration, credits, protocols);

        return result;
    }
}
