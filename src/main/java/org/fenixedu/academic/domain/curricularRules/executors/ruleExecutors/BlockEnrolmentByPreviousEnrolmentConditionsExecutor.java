package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.curricularRules.BlockEnrolmentByPreviousEnrolmentConditions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class BlockEnrolmentByPreviousEnrolmentConditionsExecutor extends CurricularRuleExecutor {

    @Override
    protected RuleResult executeEnrolmentWithRulesAndTemporaryEnrolment(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return executeEnrolmentVerificationWithRules(curricularRule, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate degreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule) || !degreeModuleToEvaluate.isLeaf()) {
            return RuleResult.createNA(degreeModuleToEvaluate.getDegreeModule());
        }

        final BlockEnrolmentByPreviousEnrolmentConditions rule = (BlockEnrolmentByPreviousEnrolmentConditions) curricularRule;
        if (rule.hasPreviousEnrolmentMatchingConditions(enrolmentContext.getRegistration(),
                getCurricularCoursesToCheck(degreeModuleToEvaluate), degreeModuleToEvaluate.getExecutionInterval())) {
            final String message = AcademicExtensionsUtil.bundle(
                    "curricularRules.ruleExecutors.BlockEnrolmentByPreviousEnrolmentConditionsExecutor.error.found.previous.enrolments.matching.conditions",
                    degreeModuleToEvaluate.getName(), rule.getPreviousEnrolmentState().getDescription());
            return degreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
                    degreeModuleToEvaluate.getDegreeModule(),
                    message) : RuleResult.createFalseWithLiteralMessage(degreeModuleToEvaluate.getDegreeModule(), message);
        }

        return RuleResult.createTrue(degreeModuleToEvaluate.getDegreeModule());

    }

    @Override
    protected RuleResult executeEnrolmentPrefilter(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule) || !sourceDegreeModuleToEvaluate.isLeaf()) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final BlockEnrolmentByPreviousEnrolmentConditions rule = (BlockEnrolmentByPreviousEnrolmentConditions) curricularRule;
        if (rule.hasPreviousEnrolmentMatchingConditions(enrolmentContext.getRegistration(),
                getCurricularCoursesToCheck(rule, sourceDegreeModuleToEvaluate),
                sourceDegreeModuleToEvaluate.getExecutionInterval())) {
            return RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
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

    private Collection<CurricularCourse> getCurricularCoursesToCheck(BlockEnrolmentByPreviousEnrolmentConditions rule,
            IDegreeModuleToEvaluate degreeModuleToEvaluate) {
        final Set<CurricularCourse> result = new HashSet<>();

        if (degreeModuleToEvaluate instanceof OptionalDegreeModuleToEnrol) {
            result.addAll(
                    rule.expandCurricularCourses(((OptionalDegreeModuleToEnrol) degreeModuleToEvaluate).getCurricularCourse()));
        } else if (degreeModuleToEvaluate instanceof EnroledOptionalEnrolment) {
            result.add(((EnroledOptionalEnrolment) degreeModuleToEvaluate).getOptionalCurricularCourse());
        }

        result.addAll(rule.expandCurricularCourses(((CurricularCourse) degreeModuleToEvaluate.getDegreeModule())));

        return result;
    }

}
