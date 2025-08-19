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
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

public class CompetenceCourseSeasonReport extends AbstractSeasonReport {

    private CompetenceCourse competenceCourse;

    private ExecutionInterval executionInterval;

    private Integer notEvaluatedStudents = 0;

    private Integer evaluatedStudents = 0;

    private Integer marksheetsTotal = 0;

    private Integer editionMarksheets = 0;

    private Integer submittedMarksheets = 0;

    private Integer confirmedMarksheets = 0;

    private Integer marksheetsToConfirm = 0;

    public CompetenceCourseSeasonReport(final CompetenceCourse competenceCourse, final EvaluationSeason season,
            final ExecutionInterval executionInterval, final LocalDate evaluationDate) {
        super(season, evaluationDate);
        this.competenceCourse = competenceCourse;
        this.executionInterval = executionInterval;
    }

    public CompetenceCourse getCompetenceCourse() {
        return competenceCourse;
    }

    @Override
    public Collection<Person> getResponsibles() {
        final Collection<Person> result = new HashSet<Person>();

        for (final ExecutionCourse executionCourse : getCompetenceCourse()
                .getExecutionCoursesByExecutionPeriod(getExecutionSemester())) {
            for (final Professorship professorship : executionCourse.getProfessorshipsSet()) {
                if (professorship.isResponsibleFor()) {
                    result.add(professorship.getPerson());
                }
            }
        }

        return result;
    }

    @Override
    public Integer getNotEvaluatedStudents() {
        return notEvaluatedStudents;
    }

    public void setNotEvaluatedStudents(Integer notEvaluatedStudents) {
        this.notEvaluatedStudents = notEvaluatedStudents;
    }

    @Override
    public Integer getEvaluatedStudents() {
        return evaluatedStudents;
    }

    public void setEvaluatedStudents(Integer evaluatedStudents) {
        this.evaluatedStudents = evaluatedStudents;
    }

    @Override
    public Integer getMarksheetsTotal() {
        return marksheetsTotal;
    }

    public void setMarksheetsTotal(final Integer input) {
        this.marksheetsTotal = input;
    }

    public Integer getEditionMarksheets() {
        return editionMarksheets;
    }

    public void setEditionMarksheets(final Integer editionMarksheets) {
        this.editionMarksheets = editionMarksheets;
    }

    public Integer getSubmittedMarksheets() {
        return submittedMarksheets;
    }

    public void setSubmittedMarksheets(final Integer submittedMarksheets) {
        this.submittedMarksheets = submittedMarksheets;
    }

    public Integer getConfirmedMarksheets() {
        return confirmedMarksheets;
    }

    public void setConfirmedMarksheets(final Integer confirmedMarksheets) {
        this.confirmedMarksheets = confirmedMarksheets;
    }

    public Integer getMarksheetsToConfirm() {
        return marksheetsToConfirm;
    }

    public void setMarksheetsToConfirm(Integer marksheetsToConfirm) {
        this.marksheetsToConfirm = marksheetsToConfirm;
    }

    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return executionInterval;
    }

    @Override
    public ExecutionInterval getExecutionInterval() {
        return executionInterval;
    }

    public String getExecutionCourses() {

        final List<String> nameSame = Lists.newLinkedList();
        final List<String> nameDifferent = Lists.newLinkedList();

        if (getCompetenceCourse() != null && getExecutionSemester() != null) {

            for (final ExecutionCourse iter : getCompetenceCourse()
                    .getExecutionCoursesByExecutionPeriod(getExecutionSemester())) {

                if (iter.getName().equals(getCompetenceCourse().getName())) {
                    nameSame.add(getDegrees(iter));
                } else {
                    nameDifferent.add(getDegrees(iter) + " [" + iter.getName() + "]");
                }
            }
        }

        String result = "";
        if (!nameSame.isEmpty()) {
            result += nameSame.stream().collect(Collectors.joining("; "));
        }
        if (!nameDifferent.isEmpty()) {
            result += nameDifferent.stream().collect(Collectors.joining("; "));
        }

        return result;
    }

    private String getDegrees(final ExecutionCourse i) {
        return i.getAssociatedCurricularCoursesSet().stream().map(x -> x.getDegree().getCode()).collect(Collectors.joining("; "));
    }

}
