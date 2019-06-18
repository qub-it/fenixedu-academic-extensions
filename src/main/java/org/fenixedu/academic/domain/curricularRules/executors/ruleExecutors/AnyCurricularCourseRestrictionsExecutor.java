package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.curricularRules.AnyCurricularCourseRestrictions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Sets;

public class AnyCurricularCourseRestrictionsExecutor extends CurricularRuleExecutor {

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

    // author = "legidio", comment = "Deprecated: grades must be set before enrolment periods"
    @Override
    @Deprecated
    protected RuleResult executeEnrolmentWithRulesAndTemporaryEnrolment(final ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {
        return executeEnrolmentVerificationWithRules(curricularRule, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule)) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final AnyCurricularCourseRestrictions rule = (AnyCurricularCourseRestrictions) curricularRule;

        final CurricularCourse curricularCourseToEnrol = getCurricularCourseFromOptional(sourceDegreeModuleToEvaluate);
        if (curricularCourseToEnrol != null) {

            if (isAllowedCourseGroup(rule, curricularCourseToEnrol)) {
                return createResultFalse(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrol,
                        "curricularRules.ruleExecutors.AnyCurricularCourseRestrictions.only.allowedCourseGroups");
            }
        }

        return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());

    }

    private boolean isAllowedCourseGroup(final AnyCurricularCourseRestrictions rule,
            final CurricularCourse curricularCourseToEnrol) {
        return Sets.intersection(rule.getCourseGroupsSet(), curricularCourseToEnrol.getAllParentCourseGroups()).isEmpty();
    }

    /**
     * Similar code in {@link AnyCurricularCourseExecutor}, explicitly assuming we're dealing with optionals
     */
    static private CurricularCourse getCurricularCourseFromOptional(final IDegreeModuleToEvaluate input) {
        CurricularCourse result = null;

        if (input.isEnroling()) {
            final OptionalDegreeModuleToEnrol toEnrol = (OptionalDegreeModuleToEnrol) input;
            result = toEnrol.getCurricularCourse();

        } else if (input.isEnroled()) {
            final EnroledOptionalEnrolment enroled = (EnroledOptionalEnrolment) input;
            result = (CurricularCourse) enroled.getCurriculumModule().getDegreeModule();
        }

        return result;
    }

    static private RuleResult createResultFalse(final AnyCurricularCourseRestrictions rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
            final String messageKey) {

        final String message = BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, messageKey, curricularCourseToEnrol.getName(),
                rule.getDegreeModuleToApplyRule().getName(), rule.getCourseGroupsDescription());

        return sourceDegreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
                sourceDegreeModuleToEvaluate.getDegreeModule(),
                message) : RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(), message);
    }

}
