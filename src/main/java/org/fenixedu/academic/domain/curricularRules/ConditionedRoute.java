package org.fenixedu.academic.domain.curricularRules;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.ConditionedRouteExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Lists;

public class ConditionedRoute extends ConditionedRoute_Base {

    protected ConditionedRoute() {
        super();
    }

    public ConditionedRoute(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup, final ExecutionInterval begin,
            final ExecutionInterval end) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
    }

    public void edit(CourseGroup contextCourseGroup) {
        setContextCourseGroup(contextCourseGroup);
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new ConditionedRouteExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    protected void removeOwnParameters() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<GenericPair<Object, Boolean>> getLabel() {
        final StringBuilder label = new StringBuilder();
        label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.ConditionedRoute"));
        if (getContextCourseGroup() != null) {
            label.append(", ");
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.inGroup"));
            label.append(" ");
            label.append(getContextCourseGroup().getOneFullName());
        }

        return Lists.newArrayList(new GenericPair<Object, Boolean>(label, false));
    }

}
