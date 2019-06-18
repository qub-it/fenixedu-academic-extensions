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
package org.fenixedu.academic.domain.curricularRules;

import java.util.List;
import java.util.Optional;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.EnrolmentPeriodRestrictionsExecutorLogic;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.RootCourseGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;

public class EnrolmentPeriodRestrictionsInitializer {

    private static final Logger logger = LoggerFactory.getLogger(EnrolmentPeriodRestrictionsInitializer.class);

    static public void init() {
        for (final DegreeCurricularPlan degreeCurricularPlan : Bennu.getInstance().getDegreeCurricularPlansSet()) {
            createEnrolmentPeriodRestrictions(degreeCurricularPlan);
        }

        EnrolmentPeriodRestrictionsExecutorLogic.configure();
    }

    @Atomic
    static private void createEnrolmentPeriodRestrictions(final DegreeCurricularPlan input) {
        final RootCourseGroup root = input.getRoot();
        List<? extends ICurricularRule> rules =
                root.getCurricularRules(CurricularRuleType.ENROLMENT_PERIOD_RESTRICTIONS, (ExecutionYear) null);

        if (rules.isEmpty()) {

            new EnrolmentPeriodRestrictions(root, getBeginExecutionSemester(input, root));
            logger.info("Created {} for DCP {}", EnrolmentPeriodRestrictions.class.getSimpleName(), input.getPresentationName());

        } else if (rules.size() > 1) {

            logger.error("Possible error: found {} {} rules for DCP {}", rules.size(),
                    EnrolmentPeriodRestrictions.class.getSimpleName(), input.getPresentationName());

        }
    }

    static private ExecutionSemester getBeginExecutionSemester(final DegreeCurricularPlan dcp, final RootCourseGroup root) {
        final Optional<Context> first = root.getChildContextsSet().stream()
                .sorted((o1, o2) -> o1.getBeginExecutionPeriod().compareTo(o2.getBeginExecutionPeriod())).findFirst();

        final ExecutionSemester result =
                first.isPresent() ? first.get().getBeginExecutionPeriod() : ExecutionSemester.readFirstExecutionSemester();
        return result;
    }

}
