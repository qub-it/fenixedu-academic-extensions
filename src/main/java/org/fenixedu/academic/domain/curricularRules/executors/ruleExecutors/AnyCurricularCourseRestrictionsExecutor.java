package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.AnyCurricularCourseRestrictions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Sets;

public class AnyCurricularCourseRestrictionsExecutor extends CurricularRuleExecutor {

    @Override
    protected boolean canBeEvaluated(ICurricularRule curricularRule, IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate,
            EnrolmentContext enrolmentContext) {
        return true;
    }

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        final AnyCurricularCourseRestrictions rule = (AnyCurricularCourseRestrictions) curricularRule;

        final CurricularCourse curricularCourseToEnrol = getCurricularCourseFromOptional(sourceDegreeModuleToEvaluate);
        if (curricularCourseToEnrol != null) {

            if (isAllowedCourseGroup(rule, curricularCourseToEnrol,
                    sourceDegreeModuleToEvaluate.getExecutionInterval().getExecutionYear())) {
                return createResultFalse(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrol,
                        "curricularRules.ruleExecutors.AnyCurricularCourseRestrictions.only.allowedCourseGroups");
            }
        }

        return RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    @Override
    protected RuleResult executeEnrolmentPrefilter(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (sourceDegreeModuleToEvaluate instanceof OptionalDegreeModuleToEnrol) {
            final RuleResult result =
                    executeEnrolmentVerificationWithRules(curricularRule, sourceDegreeModuleToEvaluate, enrolmentContext);
            return result.isTrue() ? result : RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    private boolean isAllowedCourseGroup(final AnyCurricularCourseRestrictions rule, final CurricularCourse courseToEnrol,
            final ExecutionYear executionYear) {
        final Collection<CourseGroup> parentGroups = courseToEnrol.getParentContextsByExecutionYear(executionYear).stream()
                .map(ctx -> ctx.getParentCourseGroup()).collect(Collectors.toSet());
        final Set<CourseGroup> ancestorGroups =
                Stream.concat(parentGroups.stream(), parentGroups.stream().flatMap(cg -> cg.getAllParentCourseGroups().stream()))
                        .collect(Collectors.toSet());

        return Sets.intersection(rule.getCourseGroupsSet(), ancestorGroups).isEmpty();
    }

    /**
     * Similar code in {@link AnyCurricularCourseExecutor}, explicitly assuming we're dealing with optionals
     */
    static private CurricularCourse getCurricularCourseFromOptional(final IDegreeModuleToEvaluate input) {
        CurricularCourse result = null;

        if (input.isEnroling()) {
            final OptionalDegreeModuleToEnrol toEnrol = (OptionalDegreeModuleToEnrol) input;
            result = toEnrol.getCurricularCourse();

        } else if (input.isEnroled()) {
            final EnroledOptionalEnrolment enroled = (EnroledOptionalEnrolment) input;
            result = (CurricularCourse) enroled.getCurriculumModule().getDegreeModule();
        }

        return result;
    }

    static private RuleResult createResultFalse(final AnyCurricularCourseRestrictions rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
            final String messageKey) {

        final String message = BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, messageKey, curricularCourseToEnrol.getName(),
                rule.getDegreeModuleToApplyRule().getName(), rule.getCourseGroupsDescription());

        return sourceDegreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
                sourceDegreeModuleToEvaluate.getDegreeModule(),
                message) : RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(), message);
    }

}
