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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.joda.time.LocalDate;

public class ExecutionCourseSeasonReport extends AbstractSeasonReport {

    private ExecutionCourse executionCourse;

    private List<CompetenceCourseSeasonReport> entries = new ArrayList<CompetenceCourseSeasonReport>();

    public ExecutionCourseSeasonReport(final ExecutionCourse executionCourse, final EvaluationSeason season,
            final LocalDate evaluationDate, final Collection<CompetenceCourseSeasonReport> entries) {
        super(season, evaluationDate);
        this.executionCourse = executionCourse;
        this.entries.addAll(entries);
    }

    @Override
    public Collection<Person> getResponsibles() {
        final Collection<Person> result = new HashSet<Person>();

        for (final Professorship professorship : getExecutionCourse().getProfessorshipsSet()) {
            if (professorship.isResponsibleFor()) {
                result.add(professorship.getPerson());
            }
        }

        return result;
    }

    @Override
    public Integer getNotEvaluatedStudents() {
        int result = 0;

        for (final CompetenceCourseSeasonReport entry : this.entries) {
            result += entry.getNotEvaluatedStudents().intValue();
        }

        return result;
    }

    @Override
    public Integer getEvaluatedStudents() {
        int result = 0;

        for (final CompetenceCourseSeasonReport entry : this.entries) {
            result += entry.getEvaluatedStudents().intValue();
        }

        return result;
    }

    @Override
    public Integer getMarksheetsTotal() {
        int result = 0;

        for (final CompetenceCourseSeasonReport entry : this.entries) {
            result += entry.getMarksheetsTotal().intValue();
        }

        return result;
    }

    @Override
    public Integer getMarksheetsToConfirm() {
        int result = 0;

        for (final CompetenceCourseSeasonReport entry : this.entries) {
            result += entry.getMarksheetsToConfirm().intValue();
        }

        return result;
    }

    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return getExecutionCourse().getExecutionPeriod();
    }

    @Override
    public ExecutionInterval getExecutionInterval() {
        return getExecutionCourse().getExecutionInterval();
    }

    public ExecutionCourse getExecutionCourse() {
        return this.executionCourse;
    }

}
