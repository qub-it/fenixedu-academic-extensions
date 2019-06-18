package org.fenixedu.academic.domain.curricularRules;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;

abstract public class CurricularRuleServices {

    /**
     * @see {@link org.fenixedu.academic.domain.degreeStructure.DegreeModule.getCurricularRules(CurricularRuleType,
     *      ExecutionSemester)}
     */
    static public List<? extends ICurricularRule> getCurricularRules(final DegreeModule source,
            final Class<? extends ICurricularRule> ruleClass, final ExecutionInterval interval) {

        return getCurricularRules(source, (CourseGroup) null, ruleClass, interval);
    }

    /**
     * @see {@link org.fenixedu.academic.domain.degreeStructure.DegreeModule.getCurricularRules(CurricularRuleType, CourseGroup,
     *      ExecutionYear)}
     */
    static public List<? extends ICurricularRule> getCurricularRules(final DegreeModule source, final CourseGroup parent,
            final Class<? extends ICurricularRule> ruleClass, final ExecutionInterval interval) {

        return source.getCurricularRulesSet().stream().filter(rule ->

        ruleClass.isAssignableFrom(rule.getClass())

                && isCurricularRuleValid(rule, interval)

                && (parent == null || rule.appliesToCourseGroup(parent))

        ).collect(Collectors.toList());
    }

    /**
     * @see {@link org.fenixedu.academic.domain.degreeStructure.DegreeModule.isCurricularRuleValid(ICurricularRule,
     *      ExecutionSemester)}
     */
    static private boolean isCurricularRuleValid(final ICurricularRule rule, final ExecutionInterval interval) {
        return interval == null || (interval instanceof ExecutionSemester ? rule.isValid((ExecutionSemester) interval) : rule
                .isValid((ExecutionYear) interval));
    }

    static protected boolean appliesToPeriod(final Context context, final CurricularPeriod period) {
        return period == null || period == context.getCurricularPeriod();
    }

    static public double calculateTotalEctsInGroup(final EnrolmentContext enrolmentContext,
            final CurriculumGroup curriculumGroup) {
        double result = calculateCreditsConcluded(enrolmentContext, curriculumGroup);
        result += calculateEnroledEctsCredits(enrolmentContext, curriculumGroup);

        return result;
    }

    static public Double calculateCreditsConcluded(EnrolmentContext enrolmentContext, final CurriculumModule curriculumModule) {
        return enrolmentContext.isToEvaluateRulesByYear() ? curriculumModule
                .getCreditsConcluded(enrolmentContext.getExecutionYear()) : curriculumModule
                        .getCreditsConcluded(enrolmentContext.getExecutionPeriod().getExecutionYear());
    }

    /**
     * @see CreditsLimitExecutor.calculateEnroledEctsCredits(EnrolmentContext, CurriculumModule)
     */
    static public Double calculateEnroledEctsCredits(EnrolmentContext enrolmentContext, final CurriculumModule curriculumModule) {
        return enrolmentContext.isToEvaluateRulesByYear() ? curriculumModule.getEnroledEctsCredits(enrolmentContext
                .getExecutionYear()) : curriculumModule.getEnroledEctsCredits(enrolmentContext.getExecutionPeriod());
    }

    static public double calculateEctsCreditsFromPreviousGroups(final EnrolmentContext enrolmentContext,
            final CreditsLimitWithPreviousApprovals rule) {

        double result = 0;

        if (rule != null) {
            final ExecutionYear executionYear = enrolmentContext.isToEvaluateRulesByYear() ? enrolmentContext
                    .getExecutionYear() : enrolmentContext.getExecutionPeriod().getExecutionYear();

            for (final CourseGroup group : rule.getPreviousGroupsSet()) {
                final DegreeCurricularPlan dcp = group.getParentDegreeCurricularPlan();
                final StudentCurricularPlan scp = enrolmentContext.getRegistration().getStudentCurricularPlan(dcp);

                if (scp != null) {
                    final CurriculumGroup otherGroup = scp.findCurriculumGroupFor(group);
                    if (otherGroup != null) {
                        result += otherGroup.getCreditsConcluded(executionYear);
                    }
                }
            }
        }

        return result;
    }

}
