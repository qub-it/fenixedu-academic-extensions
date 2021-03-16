package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Map;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class EnrolmentPrecedenceInCurricularYear extends EnrolmentPrecedenceInCurricularYear_Base {

    protected EnrolmentPrecedenceInCurricularYear() {
        super();
    }

    @Atomic
    public static EnrolmentPrecedenceInCurricularYear create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer year, final Boolean applyToOptionals) {
        final EnrolmentPrecedenceInCurricularYear result = new EnrolmentPrecedenceInCurricularYear();
        result.initRule(configuration, credits, year, applyToOptionals);

        return result;
    }

    private void initRule(final CurricularPeriodConfiguration configuration, final BigDecimal credits, final Integer year,
            final Boolean applyToOptionals) {
        super.init(configuration, credits, null /*semester*/);
        super.setYearMin(year);
        super.setYearMax(year);
        super.setApplyToOptionals(applyToOptionals);
        checkRules();
    }

    private void checkRules() {
        if (getYearMin() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".yearMin.is.required");
        }

        if (getYearMax() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".yearMax.is.required");
        }

    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getCredits().toString(),
                getYearMin().toString());
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {
        if (isEnrolingInYearsAfter(enrolmentContext)) {
            return getYearCredits(mapApprovedCredits(enrolmentContext))
                    .add(getYearCredits(mapEnrolmentCredits(enrolmentContext, getApplyToOptionals())))
                    .compareTo(getValue()) >= 0 ? createTrue() : createFalseLabelled();

        }
        return createTrue();
    }

    private boolean isEnrolingInYearsAfter(final EnrolmentContext enrolmentContext) {
        return mapEnrolmentCredits(enrolmentContext, null).entrySet().stream()
                .anyMatch(e -> e.getKey().getChildOrder().intValue() > getYearMin().intValue()
                        && e.getValue().compareTo(BigDecimal.ZERO) > 0);
    }

    private BigDecimal getYearCredits(final Map<CurricularPeriod, BigDecimal> creditsByCurricularPeriod) {
        return creditsByCurricularPeriod.entrySet().stream()
                .filter(e -> e.getKey().getChildOrder().intValue() == getYearMin().intValue()).findFirst().map(e -> e.getValue())
                .orElse(BigDecimal.ZERO);
    }

    //TODO: cache
    private static Map<CurricularPeriod, BigDecimal> mapEnrolmentCredits(EnrolmentContext enrolmentContext,
            Boolean applyToOptionals) {
        return CurricularPeriodServices.mapYearCredits(enrolmentContext, applyToOptionals, null);
    }

    //TODO: cache
    private Map<CurricularPeriod, BigDecimal> mapApprovedCredits(EnrolmentContext enrolmentContext) {
        return CurricularPeriodServices.mapYearCredits(RegistrationServices.getCurriculum(enrolmentContext.getRegistration(),
                enrolmentContext.getExecutionPeriod().getExecutionYear()), getApplyToOptionals());
    }

}
