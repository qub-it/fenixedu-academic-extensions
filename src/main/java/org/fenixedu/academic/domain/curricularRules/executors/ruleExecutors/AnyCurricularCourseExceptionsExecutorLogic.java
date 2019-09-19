/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 *
 * 
 * This file is part of FenixEdu fenixedu-ulisboa-specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Specifications.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.AnyCurricularCourseExceptions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyCurricularCourseExceptionsExecutorLogic extends AbstractCurricularRuleExecutorLogic {

    static private final Logger logger = LoggerFactory.getLogger(AnyCurricularCourseExceptionsExecutorLogic.class);

    static public void configure() {
        CurricularRuleExecutorFactory.findExecutor(AnyCurricularCourseExceptions.class)
                .setLogic(new AnyCurricularCourseExceptionsExecutorLogic());
    }

    @Override
    protected String getCurricularRuleLabelKey() {
        return "label.anyCurricularCourseExceptions";
    }

    @Override
    public RuleResult executeEnrolmentVerificationWithRules(final ICurricularRule curricularRule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {

        // init result with wrong configuration message
        RuleResult result = createFalseConfiguration(sourceDegreeModuleToEvaluate.getDegreeModule());

        final CurricularCourse curricularCourseToEnrol = getCurricularCourseFromOptional(sourceDegreeModuleToEvaluate);
        if (curricularCourseToEnrol != null) {

            result = verifyOptionalsConfiguration((AnyCurricularCourseExceptions) curricularRule, sourceDegreeModuleToEvaluate,
                    curricularCourseToEnrol);

            if (result.isTrue()) {
                result = verifyCompetenceCourses((AnyCurricularCourseExceptions) curricularRule, sourceDegreeModuleToEvaluate,
                        curricularCourseToEnrol, enrolmentContext);
            }
        }

        return result;
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

    private RuleResult verifyOptionalsConfiguration(final AnyCurricularCourseExceptions rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol) {

        RuleResult result = RuleResult.createTrue(curricularCourseToEnrol);

        final Boolean optionalsConfiguration = rule.getOptionalsConfiguration();
        if (optionalsConfiguration != null) {

            final boolean isOptional = hasOneOptionalParentCourseGroup(curricularCourseToEnrol);

            if (optionalsConfiguration && !isOptional) {
                result = createResultFalse(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrol,
                        "curricularRules.ruleExecutors.AnyCurricularCourseExceptions.only.optional");
            }

            if (!optionalsConfiguration && isOptional) {
                result = createResultFalse(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrol,
                        "curricularRules.ruleExecutors.AnyCurricularCourseExceptions.only.mandatory");
            }
        }

        return result;
    }

    static private boolean hasOneOptionalParentCourseGroup(final CurricularCourse curricularCourseToEnrol) {
        for (final Context context : curricularCourseToEnrol.getParentContextsSet()) {
            if (context.getParentCourseGroup().getIsOptional()) {
                return true;
            }
        }

        return false;
    }

    private RuleResult verifyCompetenceCourses(final AnyCurricularCourseExceptions rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
            final EnrolmentContext enrolmentContext) {

        RuleResult result = createFalseConfiguration(sourceDegreeModuleToEvaluate.getDegreeModule());

        final CompetenceCourse competenceCourse = curricularCourseToEnrol.getCompetenceCourse();
        final DegreeCurricularPlan chosenDegreeCurricularPlan = curricularCourseToEnrol.getDegreeCurricularPlan();
        final StudentCurricularPlan studentCurricularPlan = enrolmentContext.getStudentCurricularPlan();

        if (isException(competenceCourse, chosenDegreeCurricularPlan, studentCurricularPlan)) {

            result = createResultFalse(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrol,
                    "curricularRules.ruleExecutors.AnyCurricularCourseExceptions.not.offered");
        } else {
            result = RuleResult.createTrue(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return result;
    }

    static public boolean isException(final CompetenceCourse competenceCourse,
            final DegreeCurricularPlan chosenDegreeCurricularPlan, final StudentCurricularPlan studentCurricularPlan) {

        boolean result = false;

        // can only be considered an exception if the user chose other DCP than his own
        if (chosenDegreeCurricularPlan != studentCurricularPlan.getDegreeCurricularPlan()) {

            result = Bennu.getInstance().getAnyCurricularCourseExceptionsConfiguration().getCompetenceCoursesSet()
                    .contains(competenceCourse);
        }

        return result;
    }

    static private RuleResult createResultFalse(final AnyCurricularCourseExceptions rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
            final String messageKey) {

        final String message = BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, messageKey,
                curricularCourseToEnrol.getName(), rule.getDegreeModuleToApplyRule().getName());

        return sourceDegreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
                sourceDegreeModuleToEvaluate.getDegreeModule(),
                message) : RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(), message);
    }

}
