package org.fenixedu.academic.domain.curricularRules;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class RecommendedRoute extends RecommendedRoute_Base {

    protected RecommendedRoute() {
        super();
    }

    public RecommendedRoute(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup, final ExecutionInterval begin,
            final ExecutionInterval end) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
    }

    public void edit(CourseGroup courseGroup, ExecutionInterval begin, ExecutionInterval end) {
        super.edit(begin, end);
        super.setContextCourseGroup(courseGroup);
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    protected void removeOwnParameters() {
    }

    @Override
    public List<GenericPair<Object, Boolean>> getLabel() {
        return Collections.singletonList(new GenericPair<Object, Boolean>(
                BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.RecommendedRoute"), false));
    }

    @Override
    public boolean isVisible() {
        return false;
    }

}
