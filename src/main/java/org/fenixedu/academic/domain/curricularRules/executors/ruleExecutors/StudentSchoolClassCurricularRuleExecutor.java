package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.StudentSchoolClassCurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

public class StudentSchoolClassCurricularRuleExecutor extends CurricularRuleExecutor {



    @Override
    protected boolean canBeEvaluated(ICurricularRule curricularRule, IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            EnrolmentContext enrolmentContext) {
        return true;
    }

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (sourceDegreeModuleToEvaluate.isEnroled() || !canApplyRule(enrolmentContext, curricularRule)
                || !sourceDegreeModuleToEvaluate.isLeaf()
                || sourceDegreeModuleToEvaluate.getExecutionInterval() != enrolmentContext.getExecutionPeriod()) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        if (sourceDegreeModuleToEvaluate instanceof EnroledOptionalEnrolment) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final StudentSchoolClassCurricularRule schoolClassCurricularRule = (StudentSchoolClassCurricularRule) curricularRule;
        final Registration registration = enrolmentContext.getRegistration();
        final ExecutionInterval executionInterval = sourceDegreeModuleToEvaluate.getExecutionInterval();
        final CurricularCourse curricularCourse =
                sourceDegreeModuleToEvaluate instanceof OptionalDegreeModuleToEnrol ? ((OptionalDegreeModuleToEnrol) sourceDegreeModuleToEvaluate)
                        .getCurricularCourse() : (CurricularCourse) sourceDegreeModuleToEvaluate.getDegreeModule();

        if (schoolClassCurricularRule.getSchoolClassMustContainCourse()) {

            int curricularYear =
                    RegistrationServices.getCurricularYear(registration, executionInterval.getExecutionYear()).getResult();
            if (sourceDegreeModuleToEvaluate.getContext().getCurricularYear().equals(curricularYear)) {

                if (registration.findSchoolClass(executionInterval).stream().flatMap(sc -> sc.getAssociatedShiftsSet().stream())
                        .map(Shift::getExecutionCourse)
                        .noneMatch(ec -> ec.getAssociatedCurricularCoursesSet().contains(curricularCourse))) {

                    return RuleResult.createFalseWithLiteralMessage(curricularCourse, AcademicExtensionsUtil.bundle(
                            "curricularRules.ruleExecutors.StudentSchoolClassCurricularRuleExecutor.error.schoolClassMustContainCourse",
                            executionInterval.getName(), curricularCourse.getCode(), curricularCourse.getName()));
                }
            }
        }

        final Optional<SchoolClass> registrationSchoolClass = registration.findSchoolClass(executionInterval);

        if (schoolClassCurricularRule.getCourseMustHaveFreeShifts()) {
            final Set<Shift> shifts =
                    getShiftsToCheckIfFull(registrationSchoolClass.orElse(null), curricularCourse, executionInterval);

            if (isAllShiftsOfLoadTypeFull(registration, shifts)) {
                return RuleResult.createFalseWithLiteralMessage(curricularCourse, AcademicExtensionsUtil.bundle(
                        "curricularRules.ruleExecutors.StudentSchoolClassCurricularRuleExecutor.error.courseMustHaveFreeShifts",
                        curricularCourse.getCode(), curricularCourse.getName()));
            }
        }

        if (StringUtils.isNotBlank(schoolClassCurricularRule.getSchoolClassNames())
                && (registrationSchoolClass.isEmpty() || schoolClassCurricularRule.getSchoolClassesSplitted()
                        .noneMatch(name -> name.equals(registrationSchoolClass.get().getName())))) {

            return RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                    AcademicExtensionsUtil.bundle(
                            "curricularRules.ruleExecutors.StudentSchoolClassCurricularRuleExecutor.error.registrationNotForSchoolClass",
                            curricularCourse.getCode(), curricularCourse.getName()));
        }

        return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    private static Set<Shift> getShiftsToCheckIfFull(final SchoolClass schoolClass,
            final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {

        if (schoolClass != null) {
            final Set<Shift> shifts = schoolClass.getAssociatedShiftsSet().stream()
                    .filter(s -> s.getExecutionCourse().getAssociatedCurricularCoursesSet().contains(curricularCourse))
                    .collect(Collectors.toSet());

            if (!shifts.isEmpty()) {
                return shifts;
            }
        }

        return curricularCourse.getExecutionCoursesByExecutionPeriod(executionInterval).stream()
                .flatMap(ec -> ec.getShiftsSet().stream()).collect(Collectors.toSet());
    }

    private boolean isAllShiftsOfLoadTypeFull(final Registration registration, final Set<Shift> shifts) {

        final Map<CourseLoadType, List<Shift>> shiftsByCourseLoad =
                shifts.stream().collect(Collectors.groupingBy(Shift::getCourseLoadType));

        for (final CourseLoadType courseLoadType : shiftsByCourseLoad.keySet()) {
            final List<Shift> shiftsForType = shiftsByCourseLoad.get(courseLoadType);
            final ExecutionCourse executionCourse = shiftsForType.iterator().next().getExecutionCourse();

            if (registration.findEnrolledShiftFor(executionCourse, courseLoadType).isEmpty()
                    && shiftsForType.stream().noneMatch(s -> s.isFreeFor(registration))) {

                return true;
            }
        }

        return false;
    }

    /**
     * Evaluates if the attends complies with allAvailableShiftsMustBeEnrolled rule.
     *
     * @param attends the attends to evaluate
     * @return true if the attends complies with the rule or  false otherwise
     */
    public static boolean evaluateAllAvailableShiftsMustBeEnrolled(final Attends attends) {
        if (attends == null || attends.getEnrolment() == null) {
            return true;
        }

        final ExecutionInterval executionInterval = attends.getExecutionInterval();
        boolean allAvailableShiftsMustBeEnrolled =
                StudentSchoolClassCurricularRule.findForEnrolment(attends.getEnrolment(), executionInterval)
                        .anyMatch(StudentSchoolClassCurricularRule::getAllAvailableShiftsMustBeEnrolled);

        if (allAvailableShiftsMustBeEnrolled) {
            final ExecutionCourse executionCourse = attends.getExecutionCourse();

            final Registration registration = attends.getRegistration();
            if (executionCourse.getShiftsSet().stream().map(Shift::getCourseLoadType).distinct()
                    .anyMatch(clt -> registration.findEnrolledShiftFor(executionCourse, clt).isEmpty())) {
                return false;
            }
        }

        return true;
    }

}
