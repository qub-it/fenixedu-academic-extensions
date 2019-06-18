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
package org.fenixedu.academic.domain.curricularRules.curricularPeriod;

import java.math.BigDecimal;
import java.util.Collection;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

abstract public class RuleTransition extends RuleTransition_Base {

    protected RuleTransition() {
        super();
    }

    protected void init(final CurricularPeriodConfiguration configuration, final BigDecimal value, final Integer yearMin,
            final Integer yearMax) {
        super.init(value);
        setConfigurationTransition(configuration);
        setYearMin(yearMin);
        setYearMax(yearMax);
        checkRules();
    }

    public BigDecimal getCredits() {
        return super.getValue();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getConfigurationTransition() == null && getParentRuleTransition() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".configuration.or.parent.rule.is.required");
        }
    }

    protected boolean isForYear() {
        return getYearMin() != null && getYearMax() != null && getYearMin().intValue() == getYearMax().intValue();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        //add more logical tests for checking deletion rules
        //if (getXPTORelation() != null)
        //{
        //    blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error." + this.getClass().getSimpleName() + ".cannot.be.deleted"));
        //}

        if (getConfigurationTransition() != null) {
            blockers.add(
                    BundleUtil.getString(Bundle.APPLICATION, "error." + this.getClass().getSimpleName() + ".cannot.be.deleted"));
        }
    }

    @Override
    @Atomic
    public void delete() {
        super.setConfigurationTransition(null);
        super.setParentRuleTransition(null);
        super.setStatuteTypeForRuleTransition(null);

        while (!getChildrenSet().isEmpty()) {
            getChildrenSet().iterator().next().delete();
        }

        super.delete();
    }

    @Override
    protected CurricularPeriodConfiguration getConfiguration() {
        return getParentRuleTransition() != null ? getParentRuleTransition().getConfiguration() : getConfigurationTransition();
    }

    @Override
    protected DegreeCurricularPlan getDegreeCurricularPlan() {
        return getConfiguration().getDegreeCurricularPlan();
    }

    public void addChildRule(RuleTransition ruleTransition) {
        getChildrenSet().add(ruleTransition);
        ruleTransition.setConfigurationTransition(null);
        ruleTransition.checkRules();
    }

    abstract public RuleResult execute(final Curriculum curriculum);

    protected Curriculum prepareCurriculum(final Curriculum input) {
        Curriculum result = input;

        final Registration registration = input.getStudentCurricularPlan().getRegistration();

        if (getAllowToCollectAllCurricularPlans() && RegistrationServices.canCollectAllPlansForCurriculum(registration)) {
            result = RegistrationServices.getAllPlansCurriculum(registration, input.getExecutionYear());
        }

        if (getSemester() != null) {
            result = RegistrationServices.filterCurricularYearEntriesBySemester(result, getSemester());
        }

        return result;
    }

    @Override
    public void copyConfigurationTo(CurricularPeriodRule target) {
        super.copyConfigurationTo(target);

        final RuleTransition ruleTransition = (RuleTransition) target;
        ruleTransition.setAllowToCollectAllCurricularPlans(getAllowToCollectAllCurricularPlans());
        ruleTransition.setCodesCSV(getCodesCSV());
        ruleTransition.setStatuteTypeForRuleTransition(getStatuteTypeForRuleTransition());

        for (final RuleTransition child : getChildrenSet()) {
            ruleTransition.addChildRule((RuleTransition) child.cloneRule());
        }
    }

}
