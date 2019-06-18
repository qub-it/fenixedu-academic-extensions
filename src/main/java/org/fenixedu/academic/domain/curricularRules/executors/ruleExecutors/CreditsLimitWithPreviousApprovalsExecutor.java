package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CreditsLimitWithPreviousApprovals;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleServices;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnroledCurriculumModuleWrapper;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;

public class CreditsLimitWithPreviousApprovalsExecutor extends CurricularRuleExecutor {

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule)) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final CreditsLimit creditsLimit = (CreditsLimit) sourceDegreeModuleToEvaluate.getCurriculumGroup()
                .getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, enrolmentContext.getExecutionYear());
        if (creditsLimit == null) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final CreditsLimitWithPreviousApprovals rule = (CreditsLimitWithPreviousApprovals) curricularRule;

        final IDegreeModuleToEvaluate degreeModuleToEvaluate = searchDegreeModuleToEvaluate(enrolmentContext, rule);
        if (degreeModuleToEvaluate.isEnroled()) {

            final EnroledCurriculumModuleWrapper moduleEnroledWrapper = (EnroledCurriculumModuleWrapper) degreeModuleToEvaluate;
            final CurriculumModule curriculumModule = moduleEnroledWrapper.getCurriculumModule();

            final double ectsFromPrevious = CurricularRuleServices.calculateEctsCreditsFromPreviousGroups(enrolmentContext, rule);
            final Double ectsCredits = CurricularRuleServices.calculateCreditsConcluded(enrolmentContext, curriculumModule)
                    + CurricularRuleServices.calculateEnroledEctsCredits(enrolmentContext, curriculumModule)
                    + calculateEctsCreditsFromToEnrolCurricularCourses(enrolmentContext, curriculumModule) + ectsFromPrevious;

            if (creditsLimit.creditsExceedMaximum(ectsCredits)) {
                if (sourceDegreeModuleToEvaluate.isEnroled() && sourceDegreeModuleToEvaluate.isLeaf()) {
                    return createImpossibleResult(creditsLimit, sourceDegreeModuleToEvaluate, ectsCredits);
                } else {
                    return createFalseRuleResult(creditsLimit, sourceDegreeModuleToEvaluate, ectsCredits);
                }
            } else {
                return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
            }

        } else { // is enrolling now
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }
    }

    /**
     * @see CreditsLimitExecutor.calculateEctsCreditsFromToEnrolCurricularCourses(EnrolmentContext, CurriculumModule)
     */
    private Double calculateEctsCreditsFromToEnrolCurricularCourses(final EnrolmentContext enrolmentContext,
            final CurriculumModule parentCurriculumModule) {
        final ExecutionSemester executionSemester = enrolmentContext.getExecutionPeriod();

        BigDecimal result = BigDecimal.ZERO;
        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : enrolmentContext.getDegreeModulesToEvaluate()) {
            if (degreeModuleToEvaluate.isEnroling()
                    && parentCurriculumModule.hasCurriculumModule(degreeModuleToEvaluate.getCurriculumGroup())) {
                result = result.add(BigDecimal.valueOf(degreeModuleToEvaluate.getEctsCredits(executionSemester)));
            }
        }

        return Double.valueOf(result.doubleValue());
    }

    /**
     * @see CreditsLimitExecutor.createFalseRuleResult(CreditsLimit, IDegreeModuleToEvaluate, Double)
     */
    private RuleResult createFalseRuleResult(final CreditsLimit rule, final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            final Double ectsCredits) {
        if (rule.getMinimumCredits().equals(rule.getMaximumCredits())) {
            return RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    "curricularRules.ruleExecutors.CreditsLimitExecutor.limit.not.fulfilled",
                    rule.getDegreeModuleToApplyRule().getName(), rule.getMinimumCredits().toString(), ectsCredits.toString());
        } else {
            return RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    "curricularRules.ruleExecutors.CreditsLimitExecutor.limits.not.fulfilled",
                    rule.getDegreeModuleToApplyRule().getName(), rule.getMinimumCredits().toString(),
                    rule.getMaximumCredits().toString(), ectsCredits.toString());
        }
    }

    // author = "legidio", comment = "Deprecated: grades must be set before enrolment periods"
    @Override
    @Deprecated
    protected RuleResult executeEnrolmentWithRulesAndTemporaryEnrolment(final ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {
        return executeEnrolmentVerificationWithRules(curricularRule, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    /**
     * @see CreditsLimitExecutor.createImpossibleResult(CreditsLimit, IDegreeModuleToEvaluate, Double)
     */
    private RuleResult createImpossibleResult(final CreditsLimit rule, final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            final Double ectsCredits) {
        if (rule.getMinimumCredits().equals(rule.getMaximumCredits())) {
            return RuleResult.createImpossible(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    "curricularRules.ruleExecutors.CreditsLimitExecutor.limit.not.fulfilled",
                    rule.getDegreeModuleToApplyRule().getName(), rule.getMinimumCredits().toString(), ectsCredits.toString());
        } else {
            return RuleResult.createImpossible(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    "curricularRules.ruleExecutors.CreditsLimitExecutor.limits.not.fulfilled",
                    rule.getDegreeModuleToApplyRule().getName(), rule.getMinimumCredits().toString(),
                    rule.getMaximumCredits().toString(), ectsCredits.toString());
        }
    }

    @Override
    protected RuleResult executeEnrolmentInEnrolmentEvaluation(final ICurricularRule curricularRule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {
        return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    @Override
    protected boolean canBeEvaluated(ICurricularRule curricularRule, IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            EnrolmentContext enrolmentContext) {
        return true;
    }

    @Override
    protected boolean canApplyRule(EnrolmentContext enrolmentContext, ICurricularRule curricularRule) {
        return super.canApplyRule(enrolmentContext, curricularRule)
                && RegistrationServices.isCurriculumAccumulated(enrolmentContext.getRegistration());
    }

}
