package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.CreditsLimitWithPreviousApprovals;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleServices;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleExecutor.CurricularRuleApprovalExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.PreviousYearsEnrolmentByYearExecutor.SkipCollectCurricularCoursesPredicate;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseServices;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class CurricularRuleConfigurationInitializer {

    static private final Logger logger = LoggerFactory.getLogger(CurricularRuleConfigurationInitializer.class);

    static public class SkipCollectCurricularCoursesOrPredicate implements SkipCollectCurricularCoursesPredicate {
        private static Collection<SkipCollectCurricularCoursesPredicate> predicates = new HashSet<>();

        public void addPredicate(SkipCollectCurricularCoursesPredicate predicate) {
            predicates.add(predicate);
        }

        @Override
        public boolean skip(CourseGroup courseGroup, EnrolmentContext enrolmentContext) {
            return predicates.stream().anyMatch(p -> p.skip(courseGroup, enrolmentContext));
        }

    }

    static private SkipCollectCurricularCoursesOrPredicate PREVIOUS_YEARS_ENROLMENT_COURSES_SKIP_PREDICATE =
            new SkipCollectCurricularCoursesOrPredicate();

    public static void addPreviousYearsEnrolmentCoursesSkipPredicate(SkipCollectCurricularCoursesPredicate predicate) {
        PREVIOUS_YEARS_ENROLMENT_COURSES_SKIP_PREDICATE.addPredicate(predicate);
    }

    static public void init() {

        CurricularRuleExecutor.setCurricularRuleApprovalExecutor(CURRICULAR_RULE_APPROVAL_EXECUTOR);
        logger.info("CurricularRuleApprovalExecutor: Overriding default");

        PreviousYearsEnrolmentBySemesterExecutor
                .setSkipCollectCurricularCoursesPredicate(PREVIOUS_YEARS_ENROLMENT_COURSES_SKIP_PREDICATE);
        PreviousYearsEnrolmentByYearExecutor
                .setSkipCollectCurricularCoursesPredicate(PREVIOUS_YEARS_ENROLMENT_COURSES_SKIP_PREDICATE);

        addPreviousYearsEnrolmentCoursesSkipPredicate((cg, ctx) -> skipByCreditsLimitWithPreviousApprovals(cg, ctx));
    }

    static private Supplier<CurricularRuleApprovalExecutor> CURRICULAR_RULE_APPROVAL_EXECUTOR =
            () -> new CurricularRuleApprovalExecutor() {

                @Override
                public boolean isApproved(final EnrolmentContext enrolmentContext, final CurricularCourse curricularCourse) {
                    return isApproved(enrolmentContext, curricularCourse, (ExecutionSemester) null);
                }

                @Override
                public boolean isApproved(final EnrolmentContext enrolmentContext, final CurricularCourse curricularCourse,
                        final ExecutionSemester executionSemester) {

                    final StudentCurricularPlan plan = enrolmentContext.getStudentCurricularPlan();

                    if (FenixEduAcademicExtensionsConfiguration.getConfiguration().getCurricularRulesApprovalsAwareOfCompetenceCourse()) {

                        return CompetenceCourseServices.isCompetenceCourseApproved(plan, curricularCourse, executionSemester);

                    } else {
                        return plan.isApproved(curricularCourse, executionSemester);
                    }
                }
            };

    static private boolean skipByCreditsLimitWithPreviousApprovals(final CourseGroup courseGroup,
            final EnrolmentContext enrolmentContext) {

        final CurriculumGroup curriculumGroup = enrolmentContext.getStudentCurricularPlan().findCurriculumGroupFor(courseGroup);

        if (curriculumGroup == null) {
            return false;
        }

        final Registration registration = enrolmentContext.getRegistration();
        if (!RegistrationServices.isCurriculumAccumulated(registration)) {
            return false;
        }

        final List<? extends ICurricularRule> rules = CurricularRuleServices.getCurricularRules(curriculumGroup.getDegreeModule(),
                CreditsLimitWithPreviousApprovals.class, enrolmentContext.getExecutionPeriod());
        if (rules.isEmpty()) {
            return false;
        }

        final double ectsFromPrevious = CurricularRuleServices.calculateEctsCreditsFromPreviousGroups(enrolmentContext,
                (CreditsLimitWithPreviousApprovals) rules.iterator().next());

        final double minEctsToApprove = curriculumGroup.getDegreeModule().getMinEctsCredits();
        final double totalEcts =
                CurricularRuleServices.calculateTotalEctsInGroup(enrolmentContext, curriculumGroup) + ectsFromPrevious;

        return totalEcts >= minEctsToApprove;
    }

}
