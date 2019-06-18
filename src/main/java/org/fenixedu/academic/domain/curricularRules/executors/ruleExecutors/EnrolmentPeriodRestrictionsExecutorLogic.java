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

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.EnrolmentPeriodRestrictions;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrolmentPeriodRestrictionsExecutorLogic extends AbstractCurricularRuleExecutorLogic {

    static private final Logger logger = LoggerFactory.getLogger(EnrolmentPeriodRestrictionsExecutorLogic.class);

    static public void configure() {
        CurricularRuleExecutorFactory.findExecutor(EnrolmentPeriodRestrictions.class)
                .setLogic(new EnrolmentPeriodRestrictionsExecutorLogic());
    }

    @Override
    protected String getCurricularRuleLabelKey() {
        return "label.enrolmentPeriodRestrictions";
    }

    @Override
    public RuleResult executeEnrolmentVerificationWithRules(final ICurricularRule curricularRule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {

        final DegreeCurricularPlan dcp = enrolmentContext.getStudentCurricularPlan().getDegreeCurricularPlan();

        // TODO legidio Performance: think of a way to reduce number of rule executions
//        if (!sourceDegreeModuleToEvaluate.isLeaf()) {
//            return RuleResult.createNA(dcp.getRoot());
//        }

        RuleResult result = createFalseConfiguration(dcp.getRoot());

        final Registration registration = enrolmentContext.getRegistration();
        final int year = RegistrationServices
                .getCurricularYear(enrolmentContext.getRegistration(), enrolmentContext.getExecutionPeriod().getExecutionYear())
                .getResult();
        logger.debug("Verifying restrictions for Registration Nr. [{}] in [{}] curricular year", registration.getNumber(), year);

        final CurricularPeriodConfiguration configuration = CurricularPeriodServices.getCurricularPeriodConfiguration(dcp, year);
        if (configuration != null) {
            result = configuration.verifyRulesForEnrolment(enrolmentContext);
        }

        return result;
    }

}
