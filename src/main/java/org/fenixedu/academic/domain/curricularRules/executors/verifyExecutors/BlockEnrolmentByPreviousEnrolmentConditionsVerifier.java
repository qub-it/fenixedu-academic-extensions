package org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.curricularRules.BlockEnrolmentByPreviousEnrolmentConditions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;

public class BlockEnrolmentByPreviousEnrolmentConditionsVerifier extends VerifyRuleExecutor {

    @Deprecated
    @Override
    protected RuleResult verifyEnrolmentWithTemporaryEnrolment(ICurricularRule curricularRule, EnrolmentContext enrolmentContext,
            DegreeModule degreeModuleToVerify, CourseGroup parentCourseGroup) {
        return RuleResult.createNA(degreeModuleToVerify);
    }

    @Override
    protected RuleResult verifyEnrolmentWithRules(ICurricularRule curricularRule, EnrolmentContext enrolmentContext,
            DegreeModule degreeModuleToVerify, CourseGroup parentCourseGroup) {

        final BlockEnrolmentByPreviousEnrolmentConditions rule = (BlockEnrolmentByPreviousEnrolmentConditions) curricularRule;
        if (degreeModuleToVerify.isLeaf() && rule.hasPreviousEnrolmentMatchingConditions(enrolmentContext.getRegistration(),
                rule.expandCurricularCourses((CurricularCourse) degreeModuleToVerify), enrolmentContext.getExecutionPeriod())) {
            return RuleResult.createFalse(degreeModuleToVerify);
        }

        return RuleResult.createTrue(degreeModuleToVerify);

    }

}
