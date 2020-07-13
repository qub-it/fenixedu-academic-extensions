package org.fenixedu.academic.domain.curricularRules;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleConfigurationInitializer;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.PreviousYearsEnrolmentByYearExecutor.SkipCollectCurricularCoursesPredicate;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.UnavailableForEnrolmentRuleExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.UnavailableForEnrolmentRuleVerifier;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class UnavailableForEnrolmentRule extends UnavailableForEnrolmentRule_Base {

    protected UnavailableForEnrolmentRule() {
        super();
    }

    public UnavailableForEnrolmentRule(DegreeModule degreeModule, CourseGroup courseGroup, ExecutionInterval begin,
            ExecutionInterval end) {
        this();
        super.init(degreeModule, courseGroup, begin, end, CurricularRuleType.CUSTOM);
    }

    public void edit(CourseGroup courseGroup, ExecutionInterval begin, ExecutionInterval end) {
        super.edit(begin, end);
        super.setContextCourseGroup(courseGroup);
    }

    public List<GenericPair<Object, Boolean>> getLabel() {
        return Collections.singletonList(
                new GenericPair<Object, Boolean>(AcademicExtensionsUtil.bundle("label.UnavailableForEnrolmentRule"), false));
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
        return new UnavailableForEnrolmentRuleVerifier();
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new UnavailableForEnrolmentRuleExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    public static void initializeDomainListenersAndExtensions() {
        final SkipCollectCurricularCoursesPredicate predicate = (CourseGroup cg, EnrolmentContext ec) -> cg
                .getCurricularRules(ec.getExecutionYear()).stream().anyMatch(r -> r instanceof UnavailableForEnrolmentRule);
        CurricularRuleConfigurationInitializer.addPreviousYearsEnrolmentCoursesSkipPredicate(predicate);
    }

}
