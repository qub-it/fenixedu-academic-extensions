package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.IntStream;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class CreditsInCurricularPeriod extends CreditsInCurricularPeriod_Base {

    protected CreditsInCurricularPeriod() {
        super();
    }

    static public CreditsInCurricularPeriod createForYearInterval(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer yearMin, final Integer yearMax) {

        return create(configuration, credits, (Integer) null /* semester */, yearMin, yearMax);
    }

    @Atomic
    static private CreditsInCurricularPeriod create(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final Integer semester, final Integer yearMin, final Integer yearMax) {

        final CreditsInCurricularPeriod result = new CreditsInCurricularPeriod();
        result.init(configuration, credits, semester, yearMin, yearMax);
        return result;
    }

    private void init(final CurricularPeriodConfiguration configuration, final BigDecimal credits, final Integer semester,
            final Integer yearMin, final Integer yearMax) {

        super.init(configuration, credits, semester);
        setYearMin(yearMin);
        setYearMax(yearMax);
        checkRules();
    }

    private void checkRules() {
        if (getYearMin() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".yearMin.required");
        }
    }

    private boolean isForYear() {
        return getYearMin() != null && getYearMax() != null && getYearMin().intValue() == getYearMax().intValue();
    }

    @Override
    public String getLabel() {

        final BigDecimal credits = getCredits();

        if (getSemester() != null) {
            return getStatuteTypesLabelPrefix()
                    + BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName() + ".semester",
                            credits.toString(), getSemester().toString(), getYearMin().toString());

        } else if (isForYear()) {
            return getStatuteTypesLabelPrefix() + BundleUtil.getString(MODULE_BUNDLE,
                    "label." + this.getClass().getSimpleName() + ".year", credits.toString(), getYearMin().toString());

        } else {
            final int configurationYear = getConfiguration().getCurricularPeriod().getChildOrder();
            final int degreeDuration = getConfiguration().getDegreeCurricularPlan().getDegreeDuration();
            final boolean simple =
                    credits.equals(BigDecimal.ZERO) && getYearMin() == configurationYear + 1 && getYearMax() == degreeDuration;

            return getStatuteTypesLabelPrefix()
                    + BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName() + (simple ? ".simple" : ""),
                            credits.toString(), getYearMin().toString(), getYearMax().toString());
        }
    }

    @Override
    public RuleResult execute(final EnrolmentContext enrolmentContext) {

        if (!isToEvaluate(enrolmentContext)) {
            return createNA();
        }

        final Map<Integer, BigDecimal> creditsByYear =
                CurricularPeriodServices.mapYearCreditsForPeriods(enrolmentContext, getAcademicPeriodOrdersSet());

        final BigDecimal total = IntStream.rangeClosed(getYearMin(), getYearMax()).boxed()
                .map(year -> creditsByYear.getOrDefault(year, BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.compareTo(getCredits()) <= 0 ? createTrue() : createFalseLabelled(total);
    }

    //TODO: generalize and move to parent
    private boolean isToEvaluate(EnrolmentContext enrolmentContext) {

        if (!hasValidStatute(enrolmentContext)) {
            return false;
        }

        if (getApplyToFlunkedStudents() != null && getApplyToFlunkedStudents().booleanValue() != RegistrationServices
                .isFlunkedUsingCurricularYear(enrolmentContext.getRegistration(), enrolmentContext.getExecutionYear())) {
            return false;
        }

        return true;

    }

}
