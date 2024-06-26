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
package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class CreditsInEnrolmentPeriod extends CreditsInEnrolmentPeriod_Base {

    protected CreditsInEnrolmentPeriod() {
        super();
        setIncludeEnrolments(false);
    }

    @Atomic
    static public CreditsInEnrolmentPeriod createForSemester(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer semester) {
        return create(configuration, credits, semester);
    }

    @Atomic
    static public CreditsInEnrolmentPeriod create(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final boolean includeEnrolments) {
        final CreditsInEnrolmentPeriod result = create(configuration, credits, /* semester */(Integer) null);
        result.setIncludeEnrolments(includeEnrolments);

        return result;
    }

    @Atomic
    static public CreditsInEnrolmentPeriod create(final CurricularPeriodConfiguration configuration, final BigDecimal credits) {
        return create(configuration, credits, /* semester */(Integer) null);
    }

    @Atomic
    static private CreditsInEnrolmentPeriod create(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final Integer semester) {

        final CreditsInEnrolmentPeriod result = new CreditsInEnrolmentPeriod();
        result.init(configuration, credits, semester);
        return result;
    }

    @Override
    public String getLabel() {
        if (getSemester() != null) {
            return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName() + ".semester",
                    getCredits().toString(), getSemester().toString());

        } else {
            return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getCredits().toString());
        }
    }

    @Override
    public RuleResult execute(final EnrolmentContext enrolmentContext) {
        if (getSemester() != null) {

            if (getSemester().intValue() != enrolmentContext.getExecutionPeriod().getChildOrder().intValue()) {
                return createNA();
            }

            return executeBySemester(enrolmentContext);
        }

        return executeByYear(enrolmentContext);
    }

    private RuleResult executeByYear(final EnrolmentContext enrolmentContext) {
        BigDecimal total = BigDecimal.ZERO;

        final Set<DegreeModule> processedDegreeModules = Sets.newHashSet();

        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : getEnroledAndEnroling(enrolmentContext,
                dme -> isValidForAcademicPeriods(dme))) {

            processedDegreeModules.add(degreeModuleToEvaluate.getDegreeModule());

            final BigDecimal credits = BigDecimal.valueOf(degreeModuleToEvaluate.getEctsCredits());
            total = total.add(credits);
        }

        if (getIncludeEnrolments()) {
            //deprecated mode: 
            //applies to EnrolmentModel.SEMESTER and validation for full year (total credits in semester S2 cannot exceed total in year=S1+S2)
            //applies to EnrolmentModel.YEAR and validation for full year with multiple enrolment periods (e.g. Period 1 = S1+S2 and Period 2 = S2 OR Period 1 = S1 and Period 2 = S2) 

            // must include approved and flunked enrolments from the other periods

            for (final Enrolment enrolment : enrolmentContext.getStudentCurricularPlan().getEnrolmentsSet()) {

                if (enrolment.isAnnulled() || processedDegreeModules.contains(enrolment.getCurricularCourse())) {
                    continue;
                }

                if (enrolment.getExecutionYear() == enrolmentContext.getExecutionYear()
                        && enrolment.getExecutionPeriod() != enrolmentContext.getExecutionPeriod()) {
                    total = total.add(enrolment.getEctsCreditsForCurriculum());
                }
            }

        } else {

            //must include flunked and approved enrolments
            if (enrolmentContext.isToEvaluateRulesByYear()) {

                for (final Enrolment enrolment : enrolmentContext.getStudentCurricularPlan()
                        .getEnrolmentsByExecutionYear(enrolmentContext.getExecutionYear())) {

                    if (enrolment.isAnnulled() || processedDegreeModules.contains(enrolment.getCurricularCourse())
                            || !isValidForAcademicPeriods(enrolment.getCurricularCourse().getCompetenceCourse(),
                                    enrolment.getExecutionInterval())) {
                        continue;
                    }

                    total = total.add(enrolment.getEctsCreditsForCurriculum());
                }
            }
        }

        return total.compareTo(getCredits()) <= 0 ? createTrue() : createFalseLabelled(total);
    }

    @Deprecated
    private RuleResult executeBySemester(final EnrolmentContext enrolmentContext) {
        BigDecimal total = BigDecimal.ZERO;

        final Predicate<IDegreeModuleToEvaluate> filter = i -> i.isAnnualCurricularCourse(
                enrolmentContext.getExecutionYear()) ? i.getExecutionInterval().getExecutionYear() == enrolmentContext
                        .getExecutionYear() : i.getExecutionInterval() == enrolmentContext.getExecutionPeriod();

        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : getEnroledAndEnroling(enrolmentContext, filter)) {

            BigDecimal credits = BigDecimal.valueOf(degreeModuleToEvaluate.getEctsCredits());

            if (degreeModuleToEvaluate.isAnnualCurricularCourse(enrolmentContext.getExecutionYear())) {
                credits = credits.divide(new BigDecimal(2));
            }

            total = total.add(credits);
        }

        return total.compareTo(getCredits()) <= 0 ? createTrue() : createFalseLabelled(total);
    }

}
