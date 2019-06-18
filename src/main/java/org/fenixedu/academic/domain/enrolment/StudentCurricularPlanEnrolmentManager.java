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

import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.enrolment.period.AcademicEnrolmentPeriod;
import org.fenixedu.academic.domain.exceptions.DomainException;

public class StudentCurricularPlanEnrolmentManager
        extends org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolmentManager {

    public StudentCurricularPlanEnrolmentManager(final EnrolmentContext enrolmentContext) {
        super(enrolmentContext);
    }

    /**
     * Changes org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolmentManager
     */
    @Override
    protected void assertEnrolmentPreConditions() {

        if (isResponsiblePersonManager()) {
            return;
        }

        checkDebts();

        if (isResponsiblePersonAllowedToEnrolStudents() || isResponsibleInternationalRelationOffice()) {
            assertAcademicAdminOfficePreConditions();

        } else if (isResponsiblePersonStudent()) {
            assertStudentEnrolmentPreConditions();

        } else {
            assertOtherRolesPreConditions();
        }
    }

    /**
     * Changes org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolment
     */
    @Override
    protected void assertAcademicAdminOfficePreConditions() {

        checkEnrolmentWithoutRules();

        if (updateRegistrationAfterConclusionProcessPermissionEvaluated()) {
            return;
        }
    }

    @Override
    protected boolean isResponsiblePersonStudent() {
        return getResponsiblePerson().getStudent() != null;
    }

    /**
     * Changes org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolment
     */
    @Override
    protected void assertStudentEnrolmentPreConditions() {

        if (!getResponsiblePerson().getStudent().getActiveRegistrationsIn(getExecutionSemester()).contains(getRegistration())) {
            throw new DomainException("error.StudentCurricularPlan.student.is.not.allowed.to.perform.enrol");
        }

        if (getCurricularRuleLevel() != CurricularRuleLevel.ENROLMENT_WITH_RULES) {
            throw new DomainException("error.StudentCurricularPlan.invalid.curricular.rule.level");
        }

        if (AcademicEnrolmentPeriod.getEnrolmentPeriodsOpenOrUpcoming(getStudent()).stream()
                .noneMatch(i -> i.getDegreeCurricularPlansSet().contains(getStudentCurricularPlan().getDegreeCurricularPlan())
                        && i.getExecutionSemester() == getExecutionSemester())) {
            throw new DomainException("message.out.curricular.course.enrolment.period.default");
        }
    }

}
