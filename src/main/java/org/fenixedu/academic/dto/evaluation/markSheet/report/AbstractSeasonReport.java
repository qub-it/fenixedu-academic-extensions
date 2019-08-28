/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 *
 * 
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.dto.evaluation.markSheet.report;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Person;
import org.joda.time.LocalDate;

public abstract class AbstractSeasonReport {

    private EvaluationSeason season;

    private LocalDate evaluationDate;

    protected AbstractSeasonReport(final EvaluationSeason season, final LocalDate evaluationDate) {
        this.season = season;
        this.evaluationDate = evaluationDate;
    }

    public Set<String> getResponsibleNames() {
        final Set<String> result = new HashSet<String>();

        for (final Person person : getResponsibles()) {
            result.add(person.getName());
        }

        return result;

    }

    public Set<String> getResponsibleEmails() {
        final Set<String> result = new HashSet<String>();

        for (final Person person : getResponsibles()) {
            if (person.hasDefaultEmailAddress()) {
                result.add(person.getDefaultEmailAddressValue());
            }
        }

        return result;
    }

    public Integer getTotalStudents() {
        return getEvaluatedStudents() + getNotEvaluatedStudents();
    }

    public EvaluationSeason getSeason() {
        return season;
    }

    public LocalDate getEvaluationDate() {
        return this.evaluationDate;
    }

    abstract public Integer getNotEvaluatedStudents();

    abstract public Integer getEvaluatedStudents();

    abstract public Integer getMarksheetsTotal();

    abstract public Integer getMarksheetsToConfirm();

    abstract public Collection<Person> getResponsibles();

    @Deprecated
    abstract public ExecutionInterval getExecutionSemester();

    abstract public ExecutionInterval getExecutionInterval();

}
