/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its copyright terms are bind to the legal agreement
 * regulating the FenixEdu@ULisboa software development project between Quorum Born IT and Serviços Partilhados da Universidade de
 * Lisboa: - Copyright © 2015 Quorum Born IT (until any Go-Live phase) - Copyright © 2015 Universidade de Lisboa (after any
 * Go-Live phase)
 *
 * Contributors: luis.egidio@qub-it.com
 *
 *
 * This file is part of FenixEdu Specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with FenixEdu Specifications.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.fenixedu.academic.domain.evaluation.season;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.evaluation.season.rule.BlockingTreasuryEventInDebt;
import org.fenixedu.academic.domain.evaluation.season.rule.EvaluationSeasonRule;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.services.PersonServices;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.core.AbstractDomainObjectServices;

abstract public class EvaluationSeasonServices {

    static public boolean isBlockingTreasuryEventInDebt(final EvaluationSeason season, final Enrolment enrolment) {

        if (enrolment != null && !EvaluationSeasonRule.find(season, BlockingTreasuryEventInDebt.class).isEmpty()) {

            final Registration registration = enrolment.getRegistration();
            final Person person = registration.getPerson();

            if (isEnrolmentsInEvaluationsDependOnAcademicalActsBlocked() && PersonServices.isAcademicalActsBlocked(person,
                    new LocalDate())) {
                return true;
            }

        }

        return false;
    }

    static public boolean isDeletable(final EvaluationSeason evaluationSeason) {
        return evaluationSeason.getEvaluationSet().size() == 0;
    }

    static public boolean getActive(final EvaluationSeason input) {
        return input != null && input.getActive();
    }

    static public Integer getSeasonOrder(final EvaluationSeason input) {
        // bulletproof in order to be bootstrap-safe
        return input.getSeasonOrder() == null ? 0 : input.getSeasonOrder();
    }

    static public void setSeasonOrder(final EvaluationSeason evaluationSeason, final Integer order) {
        evaluationSeason.setSeasonOrder(order);
    }

    @Atomic
    static public void delete(final EvaluationSeason evaluationSeason) {
        if (!isDeletable(evaluationSeason)) {
            throw new AcademicExtensionsDomainException("error.EvaluationSeason.not.empty.to.delete");
        }

        AbstractDomainObjectServices.deleteDomainObject(evaluationSeason);
    }

    private static boolean enrolmentsInEvaluationsDependOnAcademicalActsBlocked = true;

    public static boolean isEnrolmentsInEvaluationsDependOnAcademicalActsBlocked() {
        return enrolmentsInEvaluationsDependOnAcademicalActsBlocked;
    }

    public static void setEnrolmentsInEvaluationsDependOnAcademicalActsBlocked(
            boolean enrolmentsInEvaluationsDependOnAcademicalActsBlocked) {
        EvaluationSeasonServices.enrolmentsInEvaluationsDependOnAcademicalActsBlocked =
                enrolmentsInEvaluationsDependOnAcademicalActsBlocked;
    }

}
