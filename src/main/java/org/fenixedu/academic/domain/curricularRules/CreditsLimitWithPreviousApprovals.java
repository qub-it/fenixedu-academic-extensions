package org.fenixedu.academic.domain.curricularRules;

import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CreditsLimitWithPreviousApprovalsExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Lists;

public class CreditsLimitWithPreviousApprovals extends CreditsLimitWithPreviousApprovals_Base {

    protected CreditsLimitWithPreviousApprovals() {
        super();
    }

    public CreditsLimitWithPreviousApprovals(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup,
            final ExecutionInterval begin, final ExecutionInterval end, final Set<CourseGroup> previousGroups) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
        edit(contextCourseGroup, previousGroups);
    }

    public void edit(final CourseGroup contextCourseGroup, final Set<CourseGroup> previousGroups) {
        setContextCourseGroup(contextCourseGroup);
        getPreviousGroupsSet().clear();
        getPreviousGroupsSet().addAll(previousGroups);
        checkRules();
    }

    private void checkRules() {
        if (getPreviousGroupsSet() == null) {
            throw new DomainException("error.CreditsLimitWithPreviousApprovals.required.PreviousGroups");
        }
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new CreditsLimitWithPreviousApprovalsExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    protected void removeOwnParameters() {
        getPreviousGroupsSet().clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<GenericPair<Object, Boolean>> getLabel() {
        final StringBuilder label = new StringBuilder();
        label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.CreditsLimitWithPreviousApprovals"));
        if (getContextCourseGroup() != null) {
            label.append(", ");
            label.append(BundleUtil.getString(Bundle.BOLONHA, "label.inGroup"));
            label.append(" ");
            label.append(getContextCourseGroup().getOneFullName());
        }

        return Lists.newArrayList(new GenericPair<Object, Boolean>(label, false));
    }

    @Override
    public boolean isVisible() {
        return false;
    }

}
