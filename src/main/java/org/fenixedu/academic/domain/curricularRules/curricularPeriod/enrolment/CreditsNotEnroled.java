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
import java.util.Collections;
import java.util.Map;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResultMessage;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;

import pt.ist.fenixframework.Atomic;

public class CreditsNotEnroled extends CreditsNotEnroled_Base {

    protected CreditsNotEnroled() {
        super();
    }

    @Atomic
    static public CreditsNotEnroled create(final CurricularPeriodConfiguration configuration, final BigDecimal credits) {
        return create(configuration, credits, configuration.getCurricularPeriod().getAbsoluteOrderOfChild());
    }

    @Atomic
    static public CreditsNotEnroled create(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final Integer year) {

        final CreditsNotEnroled result = new CreditsNotEnroled();
        result.init(configuration, credits, (Integer) null /* semester */, year);
        return result;
    }

    protected void init(CurricularPeriodConfiguration configuration, BigDecimal credits, Integer semester, Integer year) {
        super.init(configuration, credits, semester);
        super.setYearMin(year);

        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getYearMin() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".yearMin.required");
        }
    }

    @Override
    public RuleResult execute(final EnrolmentContext enrolmentContext) {
        final DegreeCurricularPlan dcp = getDegreeCurricularPlan();
        final CurricularPeriod configured = CurricularPeriodServices.getCurricularPeriod(dcp, getYearMin());
        if (configured == null) {
            return createFalseConfiguration();
        }

        final ICurriculum curriculum = RegistrationServices.getCurriculum(enrolmentContext.getRegistration(),
                enrolmentContext.getExecutionPeriod().getExecutionYear());

        BigDecimal approved = getCreditsApproved(curriculum, configured);
        BigDecimal enroledAndEnroling = getCreditsEnroledAndEnroling(enrolmentContext, configured);
        BigDecimal total = approved.add(enroledAndEnroling);

        return total.compareTo(getCredits()) >= 0 ? createTrue() : createWarningLabelled(enroledAndEnroling, approved, total);
    }

    private BigDecimal getCreditsApproved(final ICurriculum curriculum, final CurricularPeriod curricularPeriod) {
        BigDecimal result = BigDecimal.ZERO;

        final Map<CurricularPeriod, BigDecimal> curricularPeriodCredits =
                CurricularPeriodServices.mapYearCredits(curriculum, getApplyToOptionals());
        final BigDecimal credits = curricularPeriodCredits.get(curricularPeriod);
        if (credits != null) {
            result = result.add(credits);
        }

        return result;
    }

    private BigDecimal getCreditsEnroledAndEnroling(final EnrolmentContext enrolmentContext,
            final CurricularPeriod curricularPeriod) {

        BigDecimal result = BigDecimal.ZERO;

        final Map<CurricularPeriod, BigDecimal> curricularPeriodCredits =
                CurricularPeriodServices.mapYearCredits(enrolmentContext, getApplyToOptionals(), (ExecutionSemester) null);
        final BigDecimal credits = curricularPeriodCredits.get(curricularPeriod);
        if (credits != null) {
            result = result.add(credits);
        }

        return result;
    }

    public RuleResult createWarningLabelled(final BigDecimal enroledAndEnroling, final BigDecimal approved,
            final BigDecimal total) {
        final String prefix = BundleUtil.getString(MODULE_BUNDLE, "label.CurricularPeriodRule.prefix",
                getConfiguration().getCurricularPeriod().getFullLabel());

        final String literalMessage =
                prefix + " " + getLabel() + " " + BundleUtil.getString(MODULE_BUNDLE, "label.CreditsNotEnroled.suffix",
                        enroledAndEnroling.toPlainString(), approved.toPlainString(), total.toPlainString());
        return RuleResult.createWarning(getDegreeModule(), Collections.singleton(new RuleResultMessage(literalMessage, false)));
    }

    @Override
    public String getLabel() {
        String label = "label." + this.getClass().getSimpleName();
        if (getApplyToOptionals() != null) {
            label += getApplyToOptionals() ? ".applyToOptionals" : ".applyToOptionals.not";
        }

        return BundleUtil.getString(MODULE_BUNDLE, label, getCredits().toString(), getYearMin().toString());
    }

}
