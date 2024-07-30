package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class BlockEnrolmentInAdvancedCurricularCoursesExecutor extends CurricularRuleExecutor {

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate degreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule) || !degreeModuleToEvaluate.isLeaf()) {
            return RuleResult.createNA(degreeModuleToEvaluate.getDegreeModule());
        }

        final int curricularYear = RegistrationServices
                .getCurricularYear(enrolmentContext.getRegistration(), enrolmentContext.getExecutionYear()).getResult();
        if (degreeModuleToEvaluate.getContext() != null
                && degreeModuleToEvaluate.getContext().getCurricularYear().intValue() <= curricularYear) {
            return RuleResult.createTrue(degreeModuleToEvaluate.getDegreeModule());
        }

        final String message = AcademicExtensionsUtil.bundle(
                "curricularRules.ruleExecutors.BlockEnrolmentInAdvancedCurricularCoursesExecutor.error.cannot.enrol.in.advanced.curricular.courses",
                degreeModuleToEvaluate.getName());
        return degreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
                degreeModuleToEvaluate.getDegreeModule(),
                message) : RuleResult.createFalseWithLiteralMessage(degreeModuleToEvaluate.getDegreeModule(), message);

    }

    @Override
    protected RuleResult executeEnrolmentPrefilter(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule) || !sourceDegreeModuleToEvaluate.isLeaf()) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final int curricularYear = RegistrationServices
                .getCurricularYear(enrolmentContext.getRegistration(), enrolmentContext.getExecutionYear()).getResult();
        return sourceDegreeModuleToEvaluate.getContext() != null
                && sourceDegreeModuleToEvaluate.getContext().getCurricularYear().intValue() <= curricularYear ? RuleResult
                        .createTrue(sourceDegreeModuleToEvaluate.getDegreeModule()) : RuleResult
                                .createFalse(sourceDegreeModuleToEvaluate.getDegreeModule());
    }



    @Override
    protected boolean canBeEvaluated(ICurricularRule curricularRule, IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            EnrolmentContext enrolmentContext) {
        return true;
    }

}
