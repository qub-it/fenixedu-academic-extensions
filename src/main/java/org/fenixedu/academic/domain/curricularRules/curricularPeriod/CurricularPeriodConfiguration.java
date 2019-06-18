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

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.RootCourseGroup;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.domain.Bennu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;

public class CurricularPeriodConfiguration extends CurricularPeriodConfiguration_Base {

    private static final Logger logger = LoggerFactory.getLogger(CurricularPeriodConfiguration.class);

    protected CurricularPeriodConfiguration() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    protected void init(final CurricularPeriod curricularPeriod) {
        setCurricularPeriod(curricularPeriod);
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getCurricularPeriod() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".curricularPeriod.required");
        }
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        //add more logical tests for checking deletion rules
        //if (getXPTORelation() != null)
        //{
        //    blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error." + this.getClass().getSimpleName() + ".cannot.be.deleted"));
        //}
    }

    @Atomic
    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.setBennu(null);
        super.setCurricularPeriod(null);
        for (; !getRuleEnrolmentSet().isEmpty(); getRuleEnrolmentSet().iterator().next().delete()) {
            ;
        }
        for (; !getRuleTransitionSet().isEmpty(); getRuleTransitionSet().iterator().next().delete()) {
            ;
        }
        deleteDomainObject();
    }

    @Atomic
    static public CurricularPeriodConfiguration create(final CurricularPeriod curricularPeriod) {
        final CurricularPeriodConfiguration result = new CurricularPeriodConfiguration();
        result.init(curricularPeriod);
        return result;
    }

    public RuleResult verifyRulesForEnrolment(final EnrolmentContext enrolmentContext) {
        final Registration registration = enrolmentContext.getRegistration();
        final RootCourseGroup degreeModule = getDegreeCurricularPlan().getRoot();

        if (getRuleEnrolmentSet().isEmpty()) {
            final RuleResult falseResult = CurricularPeriodRule.createFalseConfiguration(degreeModule, this);
            logger.debug("[REG][{}][{}]", registration.getNumber(), CurricularPeriodRule.getMessages(falseResult));

            return falseResult;
        }

        //check executive rules first
        for (final RuleEnrolment rule : getFilteredRuleEnrolment(true)) {

            final RuleResult ruleResult = rule.execute(enrolmentContext);
            if (ruleResult.isTrue()) {
                return ruleResult;
            }

            logger.debug("[RULE executive !true][{}] [REG][{}][{}]", rule.getExternalId(), registration.getNumber(),
                    CurricularPeriodRule.getMessages(rule.createFalseLabelled()));
        }

        RuleResult result = RuleResult.createTrue(degreeModule);
        for (final RuleEnrolment rule : getFilteredRuleEnrolment(false)) {

            final RuleResult ruleResult = rule.execute(enrolmentContext);
            if (!ruleResult.isTrue()) {
                result = result.and(ruleResult);
                logger.debug("[RULE !true][{}] [REG][{}][{}]", rule.getExternalId(), registration.getNumber(),
                        CurricularPeriodRule.getMessages(rule.createFalseLabelled()));
            }
        }

        return result;
    }

    public RuleResult verifyRulesForTransition(final Curriculum curriculum) {
        final Registration registration = curriculum.getStudentCurricularPlan().getRegistration();
        final RootCourseGroup degreeModule = getDegreeCurricularPlan().getRoot();

        if (getRuleTransitionSet().isEmpty()) {
            final RuleResult falseResult = CurricularPeriodRule.createFalseConfiguration(degreeModule, this);
            logger.debug("[REG][{}][{}]", registration.getNumber(), CurricularPeriodRule.getMessages(falseResult));
            return falseResult;
        }

        //check executive rules first
        for (final RuleTransition rule : getFilteredRuleTransition(true)) {

            final RuleResult ruleResult = rule.execute(curriculum);
            if (ruleResult.isTrue()) {
                return ruleResult;
            }

            logger.debug("[RULE executive !true][{}] [REG][{}][{}]", rule.getExternalId(), registration.getNumber(),
                    CurricularPeriodRule.getMessages(rule.createFalseLabelled()));
        }

        final Collection<RuleTransition> otherRules = getFilteredRuleTransition(false);
        if (otherRules.isEmpty()) {
            final RuleResult falseResult = CurricularPeriodRule.createFalseConfiguration(degreeModule, this);
            logger.debug("[REG][{}][{}]", registration.getNumber(), CurricularPeriodRule.getMessages(falseResult));
            return falseResult;
        }

        RuleResult result = RuleResult.createTrue(degreeModule);
        for (final RuleTransition rule : otherRules) {

            final RuleResult ruleResult = rule.execute(curriculum);
            if (!ruleResult.isTrue()) {
                result = result.and(ruleResult);
                logger.debug("[RULE !true][{}] [REG][{}][{}]", rule.getExternalId(), registration.getNumber(),
                        CurricularPeriodRule.getMessages(rule.createFalseLabelled()));
            }
        }

        return result;
    }

    private Collection<RuleTransition> getFilteredRuleTransition(boolean executive) {
        return getRuleTransitionSet().stream().filter(r -> r.isExecutive() == executive).collect(Collectors.toSet());
    }

    private Collection<RuleEnrolment> getFilteredRuleEnrolment(boolean executive) {
        return getRuleEnrolmentSet().stream().filter(r -> r.isExecutive() == executive).collect(Collectors.toSet());
    }

    public DegreeCurricularPlan getDegreeCurricularPlan() {
        CurricularPeriod curricularPeriod = getCurricularPeriod();
        for (; curricularPeriod.getParent() != null; curricularPeriod = curricularPeriod.getParent()) {
        }

        return curricularPeriod.getDegreeCurricularPlan();
    }

}
