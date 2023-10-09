package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.groups.PermissionService;
import org.fenixedu.academic.util.CurricularRuleLabelFormatter;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class ConditionedRouteExecutor extends CurricularRuleExecutor {

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

        if (isPersonAuthorized(enrolmentContext)) {
            return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final IDegreeModuleToEvaluate degreeModuleToEvaluate = searchDegreeModuleToEvaluate(enrolmentContext, curricularRule);
        if (degreeModuleToEvaluate.isEnroled()) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return createFalseResult(sourceDegreeModuleToEvaluate, degreeModuleToEvaluate);
    }

    @Override
    protected RuleResult executeEnrolmentPrefilter(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (isPersonAuthorized(enrolmentContext)) {
            return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        if (sourceDegreeModuleToEvaluate.getDegreeModule() != curricularRule.getDegreeModuleToApplyRule()) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return RuleResult.createImpossibleWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                CurricularRuleLabelFormatter.getLabel(curricularRule));

    }

    private RuleResult createFalseResult(final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            final IDegreeModuleToEvaluate degreeModuleToEvaluate) {
        return RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                AcademicExtensionsUtil.bundle(
                        "curricularRules.ruleExecutors.ConditionedRouteExecutor.route.choice.must.be.performed.by.academic.office",
                        degreeModuleToEvaluate.getDegreeModule().getName()));
    }

    private boolean isPersonAuthorized(EnrolmentContext enrolmentContext) {
        return AcademicAccessRule.isProgramAccessibleToFunction(AcademicOperationType.STUDENT_ENROLMENTS,
                enrolmentContext.getStudentCurricularPlan().getDegree(), enrolmentContext.getResponsiblePerson().getUser())
                || PermissionService.hasAccess("ACADEMIC_OFFICE_ENROLMENTS", enrolmentContext.getResponsiblePerson().getUser());
    }

}
