package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.StudentStatuteCurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.services.StatuteServices;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class StudentStatuteCurricularRuleExecutor extends CurricularRuleExecutor {



    @Override
    protected boolean canBeEvaluated(ICurricularRule curricularRule, IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            EnrolmentContext enrolmentContext) {
        return true;
    }

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (!canApplyRule(enrolmentContext, curricularRule)) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final StudentStatuteCurricularRule rule = (StudentStatuteCurricularRule) curricularRule;

        if (!StatuteServices.findStatuteTypes(enrolmentContext.getRegistration(), enrolmentContext.getExecutionPeriod())
                .contains(rule.getStatuteType())) {
            return RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    AcademicExtensionsUtil.bundle(
                            "curricularRules.ruleExecutors.StudentStatuteCurricularRuleExecutor.statutes.is.required.to.enrol",
                            sourceDegreeModuleToEvaluate.getName(),
                            rule.getStatuteType().getCode() + " - " + rule.getStatuteType().getName().getContent()));
        }

        return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());

    }

}
