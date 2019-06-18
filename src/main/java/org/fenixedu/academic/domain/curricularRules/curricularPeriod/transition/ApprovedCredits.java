package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;

public class ApprovedCredits extends ApprovedCredits_Base {

    protected ApprovedCredits() {
        super();
    }

    @Atomic
    static public ApprovedCredits create(final CurricularPeriodConfiguration configuration, final BigDecimal credits) {
        final ApprovedCredits result = new ApprovedCredits();
        result.init(configuration, credits, null /*yearMin*/, null /*yearMax*/);

        return result;
    }

    @Atomic
    static public ApprovedCredits createForSemester(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final Integer semester) {

        final ApprovedCredits result = new ApprovedCredits();
        result.init(configuration, credits, null /*yearMin*/, null /*yearMax*/);
        result.setSemester(semester);

        return result;
    }

    @Atomic
    static public ApprovedCredits createForYearInterval(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer yearMin, final Integer yearMax) {

        final ApprovedCredits result = new ApprovedCredits();
        result.init(configuration, credits, yearMin, yearMax);
        return result;
    }

    @Override
    public String getLabel() {
        String label = "label." + this.getClass().getSimpleName();
        final List<String> args = Lists.newArrayList();

        if (isYearless()) {
            args.add(getCredits().toString());

        } else {

            final int yearMin = getYearMinFinal();
            final int yearMax = getYearMaxFinal();

            if (yearMin == yearMax) {
                label += ".year";
                args.add(getCredits().toString());
                args.add(String.valueOf(yearMin));

            } else {
                label += ".years";
                args.add(getCredits().toString());
                args.add(String.valueOf(yearMin));
                args.add(String.valueOf(yearMax));
            }
        }

        if (getApplyToOptionals() != null) {
            label += getApplyToOptionals() ? ".applyToOptionals" : ".applyToOptionals.not";
        }

        // TODO legidio, label with semester? or remove semester constructor?
        // in FP/IE, FlunkedCredits is being used by semester. Any other institution is using this ApprovedCredits with semester config?

        return BundleUtil.getString(MODULE_BUNDLE, label, args.toArray(new String[] {}));
    }

    private boolean isYearless() {
        return getYearMin() == null && getYearMax() == null;
    }

    private int getYearMinFinal() {
        return getYearMin() == null ? 1 : getYearMin();
    }

    private int getYearMaxFinal() {
        return getYearMax() == null ? getConfiguration().getCurricularPeriod().getChildOrder().intValue() - 1 : getYearMax();
    }

    public RuleResult executeYearless(final Curriculum input) {
        final Curriculum curriculum = prepareCurriculum(input);
        final BigDecimal total = curriculum.getSumEctsCredits();
        return total.compareTo(getCredits()) >= 0 ? createTrue() : createFalseLabelled(total);
    }

    @Override
    public RuleResult execute(final Curriculum input) {
        if (isYearless()) {
            return executeYearless(input);
        }

        final Curriculum curriculum = prepareCurriculum(input);

        final int yearMin = getYearMinFinal();
        final int yearMax = getYearMaxFinal();

        final Set<CurricularPeriod> configured = getCurricularPeriodsConfigured(yearMin, yearMax, false);
        if (configured == null) {
            return createFalseConfiguration();
        }

        final BigDecimal total = calculateTotalApproved(curriculum, configured);
        return total.compareTo(getCredits()) >= 0 ? createTrue() : createFalseLabelled(total);
    }

    private BigDecimal calculateTotalApproved(final Curriculum curriculum, final Set<CurricularPeriod> configured) {

        BigDecimal result = BigDecimal.ZERO;
        final Map<CurricularPeriod, BigDecimal> curricularPeriodCredits =
                CurricularPeriodServices.mapYearCredits(curriculum, getApplyToOptionals());
        final Set<CurricularPeriod> toInspect = configured.isEmpty() ? curricularPeriodCredits.keySet() : configured;

        for (final CurricularPeriod curricularPeriod : toInspect) {
            final BigDecimal approved = curricularPeriodCredits.get(curricularPeriod) != null ? curricularPeriodCredits
                    .get(curricularPeriod) : BigDecimal.ZERO;
            result = result.add(approved);
        }

        return result;
    }

}
