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
package org.fenixedu.academic.domain.enrolment;

import java.util.function.Supplier;

import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolment;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolment.EnrolmentManagerFactory;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanExtraEnrolmentManager;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanPropaeudeuticsEnrolmentManager;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanStandaloneEnrolmentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class EnrolmentManagerFactoryInitializer {

    private static Logger logger = LoggerFactory.getLogger(EnrolmentManagerFactoryInitializer.class);

    static public void init() {

        StudentCurricularPlanEnrolment.setEnrolmentManagerFactory(ENROLMENT_MANAGER_FACTORY);
        logger.info("Overriding default");
    }

    static private Supplier<EnrolmentManagerFactory> ENROLMENT_MANAGER_FACTORY = () -> new EnrolmentManagerFactory() {

        @Override
        public StudentCurricularPlanEnrolment createManager(final EnrolmentContext enrolmentContext) {

            if (enrolmentContext.isNormal()) {
                return new StudentCurricularPlanEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isImprovement()) {
                // qubExtension
                return new StudentCurricularPlanImprovementOfApprovedEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isSpecialSeason()) {
                // qubExtension
                return new StudentCurricularPlanEnrolmentInSpecialSeasonEvaluationManager(enrolmentContext);

            } else if (enrolmentContext.isExtra()) {
                return new StudentCurricularPlanExtraEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isPropaeudeutics()) {
                return new StudentCurricularPlanPropaeudeuticsEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isStandalone()) {
                return new StudentCurricularPlanStandaloneEnrolmentManager(enrolmentContext);
            }

            throw new DomainException("StudentCurricularPlanEnrolment");
        }

    };

}
