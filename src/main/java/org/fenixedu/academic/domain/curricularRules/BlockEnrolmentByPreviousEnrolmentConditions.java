package org.fenixedu.academic.domain.curricularRules;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.BlockEnrolmentByPreviousEnrolmentConditionsExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.BlockEnrolmentByPreviousEnrolmentConditionsVerifier;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class BlockEnrolmentByPreviousEnrolmentConditions extends BlockEnrolmentByPreviousEnrolmentConditions_Base {

    protected BlockEnrolmentByPreviousEnrolmentConditions() {
        super();
    }

    public BlockEnrolmentByPreviousEnrolmentConditions(DegreeModule degreeModule, CourseGroup courseGroup,
            ExecutionInterval begin, ExecutionInterval end, EnrollmentState previousEnrolmentState) {
        this();
        super.init(degreeModule, courseGroup, begin, end, CurricularRuleType.CUSTOM);
        super.setPreviousEnrolmentState(previousEnrolmentState);
        checkRules();
    }

    public void edit(CourseGroup courseGroup, ExecutionInterval begin, ExecutionInterval end,
            EnrollmentState previousEnrolmentState) {
        super.edit(begin, end);
        super.setContextCourseGroup(courseGroup);
        super.setPreviousEnrolmentState(previousEnrolmentState);
        checkRules();
    }

    private void checkRules() {
        if (getPreviousEnrolmentState() == null) {
            throw new DomainException("error.BlockEnrolmentByPreviousEnrolmentConditions.previousEnrolmentState.cannot.be.null");
        }
    }

    public List<GenericPair<Object, Boolean>> getLabel() {
        return Collections.singletonList(new GenericPair<Object, Boolean>(AcademicExtensionsUtil.bundle(
                "label.BlockEnrolmentByPreviousEnrolmentConditions", getPreviousEnrolmentState().getDescription()), false));
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
        return new BlockEnrolmentByPreviousEnrolmentConditionsVerifier();
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new BlockEnrolmentByPreviousEnrolmentConditionsExecutor().execute(this, sourceDegreeModuleToEvaluate,
                enrolmentContext);
    }
    
    public Collection<CurricularCourse> expandCurricularCourses(final CurricularCourse curricularCourse) {
        return curricularCourse.getCompetenceCourse() != null ? curricularCourse.getCompetenceCourse()
                .getAssociatedCurricularCoursesSet() : Collections.singleton(curricularCourse);
    }

    public boolean hasPreviousEnrolmentMatchingConditions(final Registration registration,
            final Collection<CurricularCourse> curricularCourses, final ExecutionInterval maxInterval) {
        return registration.getStudentCurricularPlansSet().stream().flatMap(scp -> scp.getEnrolmentsSet().stream())
                .filter(e -> e.getExecutionInterval().isBefore(maxInterval)
                        && curricularCourses.stream().anyMatch(cc -> e.hasDegreeModule(cc)))
                .anyMatch(e -> e.getEnrollmentState() == getPreviousEnrolmentState());
    }

}
