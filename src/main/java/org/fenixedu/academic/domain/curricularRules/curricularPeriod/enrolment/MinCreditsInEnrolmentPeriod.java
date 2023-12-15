package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import static java.util.Optional.ofNullable;
import static org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices.getCurricularPeriod;
import static org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices.mapYearCredits;
import static org.fenixedu.academicextensions.util.AcademicExtensionsUtil.BUNDLE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResultMessage;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class MinCreditsInEnrolmentPeriod extends MinCreditsInEnrolmentPeriod_Base {

    private static final BigDecimal YEAR_CREDITS = BigDecimal.valueOf(60);

    protected MinCreditsInEnrolmentPeriod() {
        super();
    }

    @Override
    public RuleResult execute(final EnrolmentContext enrolmentContext) {

        if (!hasValidRegime(enrolmentContext)) {
            return createNA();
        }

        if (!hasValidStatute(enrolmentContext)) {
            return createNA();
        }

        final BigDecimal missingCredits = getMissingCredits(enrolmentContext);
        final BigDecimal enroledCredits = getEnroledCredits(enrolmentContext);

        if (missingCredits.compareTo(BigDecimal.ZERO) == 0 || enroledCredits.compareTo(missingCredits) >= 0
                || enroledCredits.compareTo(getCredits()) >= 0) {
            return createTrue();
        }

        return createWarning(missingCredits.min(getCredits()), enroledCredits);
    }

    private BigDecimal getMissingCredits(final EnrolmentContext context) {
        final Map<CurricularPeriod, BigDecimal> creditsByYear = mapYearCredits(getCurriculum(context));

        return getCurricularPeriodsForYears(context).stream().map(cp -> getMissingCreditsFor(context, creditsByYear, cp))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ICurriculum getCurriculum(final EnrolmentContext context) {
        final ICurriculum curriculum = RegistrationServices.getCurriculum(context.getRegistration(), context.getExecutionYear());

        if (context.isToEvaluateRulesByYear()) {
            return curriculum;
        }

        final Set<ICurriculumEntry> filteredEntries = curriculum.getCurricularYearEntries().stream().filter(e -> {
            final CurricularPeriod period = getCurricularPeriod((CurriculumLine) e);
            return period.getChildOrder().intValue() == context.getExecutionPeriod().getChildOrder()
                    && period.getAcademicPeriod().equals(context.getExecutionPeriod().getAcademicPeriod());
        }).collect(Collectors.toSet());

        return new Curriculum(context.getStudentCurricularPlan().getRoot(), context.getExecutionYear(), Collections.emptySet(),
                Collections.emptySet(), filteredEntries);
    }

    private BigDecimal getMissingCreditsFor(EnrolmentContext context, final Map<CurricularPeriod, BigDecimal> creditsByYear,
            CurricularPeriod period) {
        final BigDecimal approvedCredits = creditsByYear.getOrDefault(period, BigDecimal.ZERO);
        return getEnrolmentPeriodCreditsBaseline(context).subtract(approvedCredits).max(BigDecimal.ZERO);
    }

    private BigDecimal getEnrolmentPeriodCreditsBaseline(final EnrolmentContext context) {
        return context.isToEvaluateRulesByYear() ? YEAR_CREDITS : YEAR_CREDITS.multiply(BigDecimal
                .valueOf(context.getExecutionPeriod().getAcademicPeriod().getWeight()).setScale(1, RoundingMode.HALF_UP));
    }

    private BigDecimal getEnroledCredits(final EnrolmentContext context) {
        final Collection<CurricularPeriod> curricularPeriods = getCurricularPeriodsForYears(context);
        final Map<CurricularPeriod, BigDecimal> creditsByYear = mapYearCredits(context, null);

        return creditsByYear.entrySet().stream().filter(e -> curricularPeriods.contains(e.getKey())).map(e -> e.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Collection<CurricularPeriod> getCurricularPeriodsForYears(final EnrolmentContext context) {
        final DegreeCurricularPlan curricularPlan = context.getStudentCurricularPlan().getDegreeCurricularPlan();
        final int minYear = ofNullable(getYearMin()).map(v -> v.intValue()).orElse(1);
        final int maxYear = ofNullable(getYearMax()).map(v -> v.intValue()).orElseGet(() -> curricularPlan.getDurationInYears());

        return getCurricularPeriodsConfigured(minYear, maxYear, false);
    }

    public RuleResult createWarning(final BigDecimal minToEnrol, BigDecimal enroledCredits) {
        final String prefix = BundleUtil.getString(BUNDLE, "label.CurricularPeriodRule.prefix",
                getConfiguration().getCurricularPeriod().getFullLabel());

        final String literalMessage =
                prefix + getPartialRegimeLabelPrefix() + getStatuteTypesLabelPrefix() + " " + BundleUtil.getString(BUNDLE,
                        "label.MinCreditsInEnrolmentPeriod", minToEnrol.toPlainString(), enroledCredits.toPlainString());

        return RuleResult.createWarning(getDegreeModule(), Collections.singleton(new RuleResultMessage(literalMessage, false)));
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(BUNDLE, "label.MinCreditsInEnrolmentPeriod");
    }

    @Atomic
    public static MinCreditsInEnrolmentPeriod create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits) {
        final MinCreditsInEnrolmentPeriod result = new MinCreditsInEnrolmentPeriod();
        result.init(configuration, credits, null);

        return result;
    }

}
