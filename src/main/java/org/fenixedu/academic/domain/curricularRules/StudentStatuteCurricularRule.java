package org.fenixedu.academic.domain.curricularRules;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.StudentStatuteCurricularRuleExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.StudentStatuteCurricularRuleVerifier;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Lists;

public class StudentStatuteCurricularRule extends StudentStatuteCurricularRule_Base {

    protected StudentStatuteCurricularRule() {
        super();
    }

    public StudentStatuteCurricularRule(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup,
            final ExecutionSemester begin, final ExecutionSemester end, final StatuteType statuteType,
            final CurricularPeriod curricularPeriod) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
        edit(contextCourseGroup, statuteType, curricularPeriod);
    }

    public void edit(final CourseGroup contextCourseGroup, final StatuteType statuteType,
            final CurricularPeriod curricularPeriod) {
        setContextCourseGroup(contextCourseGroup);
        super.setStatuteType(statuteType);
        super.setCurricularPeriod(curricularPeriod);
        checkRules();
    }

    private void checkRules() {
        if (getStatuteType() == null) {
            throw new DomainException("error.StudentStatuteCurricularRule.statuteType.cannot.be.null");
        }
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new StudentStatuteCurricularRuleExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return new StudentStatuteCurricularRuleVerifier();
    }

    @Override
    protected void removeOwnParameters() {
        super.setStatuteType(null);
        super.setCurricularPeriod(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<GenericPair<Object, Boolean>> getLabel() {
        final StringBuilder label = new StringBuilder();
        label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.StudentStatuteCurricularRule")).append(": ")
                .append(getStatuteType().getCode() + " - " + getStatuteType().getName().getContent());
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

    @Override
    public boolean appliesToContext(final Context context) {
        return super.appliesToContext(context) && CurricularRuleServices.appliesToPeriod(context, getCurricularPeriod());
    }

}
