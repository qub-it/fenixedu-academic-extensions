package org.fenixedu.academic.domain.curricularRules;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.BlockEnrolmentInAdvancedCurricularCoursesExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class BlockEnrolmentInAdvancedCurricularCourses extends BlockEnrolmentInAdvancedCurricularCourses_Base {

    protected BlockEnrolmentInAdvancedCurricularCourses() {
        super();
    }

    public BlockEnrolmentInAdvancedCurricularCourses(DegreeModule degreeModule, CourseGroup courseGroup, ExecutionInterval begin,
            ExecutionInterval end) {
        this();
        super.init(degreeModule, courseGroup, begin, end, CurricularRuleType.CUSTOM);
    }

    public void edit(CourseGroup courseGroup, ExecutionInterval begin, ExecutionInterval end) {
        super.edit(begin, end);
        super.setContextCourseGroup(courseGroup);
    }

    public List<GenericPair<Object, Boolean>> getLabel() {
        return Collections.singletonList(new GenericPair<Object, Boolean>(
                AcademicExtensionsUtil.bundle("label.BlockEnrolmentInAdvancedCurricularCourses"), false));
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    protected void removeOwnParameters() {

    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new BlockEnrolmentInAdvancedCurricularCoursesExecutor().execute(this, sourceDegreeModuleToEvaluate,
                enrolmentContext);
    }

}
