package org.fenixedu.academic.domain.curricularRules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.AnyCurricularCourseRestrictionsExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Lists;

public class AnyCurricularCourseRestrictions extends AnyCurricularCourseRestrictions_Base {

    protected AnyCurricularCourseRestrictions() {
        super();
    }

    public AnyCurricularCourseRestrictions(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup,
            final ExecutionInterval begin, final ExecutionInterval end, final Set<CourseGroup> courseGroups) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
        edit(contextCourseGroup, courseGroups);
    }

    public void edit(final CourseGroup contextCourseGroup, final Set<CourseGroup> courseGroups) {
        setContextCourseGroup(contextCourseGroup);
        super.getCourseGroupsSet().clear();
        super.getCourseGroupsSet().addAll(courseGroups);
        checkRules();
    }

    private void checkRules() {
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new AnyCurricularCourseRestrictionsExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    protected void removeOwnParameters() {
        getCourseGroupsSet().clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<GenericPair<Object, Boolean>> getLabel() {
        final StringBuilder label = new StringBuilder();
        label.append(AcademicExtensionsUtil.bundle("label.AnyCurricularCourseRestrictions")).append(": ");

        if (!getCourseGroupsSet().isEmpty()) {
            label.append(AcademicExtensionsUtil.bundle("label.AnyCurricularCourseRestrictions.allowedCourseGroups",
                    getCourseGroupsDescription()));
        }

        if (getContextCourseGroup() != null) {
            label.append(", ");
            label.append(BundleUtil.getString(Bundle.BOLONHA, "label.inGroup"));
            label.append(" ");
            label.append(getContextCourseGroup().getOneFullName());
        }

        return Lists.newArrayList(new GenericPair<Object, Boolean>(label, false));
    }

    public String getCourseGroupsDescription() {
        return getCourseGroupsSet().isEmpty() ? "-" : getCourseGroupsSet().stream()
                .map(i -> "\"" + i.getNameI18N().getContent() + "\"").collect(Collectors.joining(" ou "));
    }

}
