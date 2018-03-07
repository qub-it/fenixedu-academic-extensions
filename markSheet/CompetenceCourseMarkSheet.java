/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: luis.egidio@qub-it.com
 *
 * 
 * This file is part of FenixEdu Specifications.
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

package org.fenixedu.academic.domain.evaluation.markSheet;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.Evaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.Holiday;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.curriculum.EnrolmentEvaluationContext;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.evaluation.EvaluationComparator;
import org.fenixedu.academic.domain.evaluation.EvaluationServices;
import org.fenixedu.academic.domain.evaluation.config.MarkSheetSettings;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonPeriod;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonPeriod.EvaluationSeasonPeriodType;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.academic.domain.evaluation.season.rule.EvaluationSeasonRule;
import org.fenixedu.academic.domain.evaluation.season.rule.GradeScaleValidator;
import org.fenixedu.academic.domain.evaluation.services.EnrolmentEvaluationServices;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.services.EnrolmentServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.ulisboa.specifications.domain.studentCurriculum.CurriculumAggregator;
import org.fenixedu.ulisboa.specifications.domain.studentCurriculum.CurriculumAggregatorEntry;
import org.fenixedu.ulisboa.specifications.domain.studentCurriculum.CurriculumAggregatorServices;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CompetenceCourseMarkSheet extends CompetenceCourseMarkSheet_Base {

    static public Comparator<String> COMPARATOR_FOR_STUDENT_NAME = new Comparator<String>() {

        @Override
        public int compare(final String o1, final String o2) {
            return collator.get().compare(normalize(o1), normalize(o2));
        }

        final Supplier<Collator> collator = () -> {
            final Collator result = Collator.getInstance(I18N.getLocale());
            return result;
        };

        private String normalize(final String input) {
            return input == null ? "" : input.replaceAll("\\s+", "").toUpperCase();
        }
    };

    static final private Logger logger = LoggerFactory.getLogger(CompetenceCourseMarkSheet.class);

    private Set<CurriculumAggregator> curriculumAggregators = null;
    private Set<CurriculumAggregatorEntry> curriculumAggregatorEntries = null;

    protected CompetenceCourseMarkSheet() {
        super();
    }

    protected void init(final ExecutionSemester executionSemester, final CompetenceCourse competenceCourse,
            final ExecutionCourse executionCourse, final EvaluationSeason evaluationSeason, final Evaluation courseEvaluation,
            final LocalDate evaluationDate, GradeScale gradeScale, final Person certifier, final Set<Shift> shifts,
            final LocalDate expireDate) {

        setExecutionSemester(executionSemester);
        setCompetenceCourse(competenceCourse);
        setExecutionCourse(executionCourse);
        setEvaluationSeason(evaluationSeason);
        if (courseEvaluation != null) {
            setCourseEvaluation(courseEvaluation);
            setEvaluationDate(new LocalDate(courseEvaluation.getEvaluationDate()));
        } else {
            setEvaluationDate(evaluationDate);
        }
        setGradeScale(gradeScale);
        setCertifier(certifier);
        getShiftSet().addAll(shifts);
        setExpireDate(expireDate);
        checkRules();
    }

    private void checkRules() {

        if (getExecutionSemester() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.executionSemester.required");
        }

        if (getCompetenceCourse() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.competenceCourse.required");
        }

        if (getExecutionCourse() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.executionCourse.required");
        }

        if (getEvaluationSeason() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.evaluationSeason.required");
        }

        if (getEvaluationDate() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.evaluationDate.required");
        }

        if (getCertifier() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.certifier.required");
        }

        if (getGradeScale() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.gradeScale.required");
        }

        for (final EnrolmentEvaluation enrolmentEvaluation : getEnrolmentEvaluationSet()) {
            if (enrolmentEvaluation.getGradeScale() != getGradeScale()) {
                throw new AcademicExtensionsDomainException(
                        "error.CompetenceCourseMarkSheet.marksheet.already.contains.evaluations.with.another.grade.scale");
            }
        }

        if (getEnrolmentEvaluationSet().isEmpty() && getExecutionCourseEnrolmentsNotInAnyMarkSheet().isEmpty()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheet.no.enrolments.found.for.grade.submission");
        }

        checkRulesEvaluationDate();
    }

    private void checkRulesEvaluationDate() {
        checkIfEvaluationDateIsWorkingDay();
        checkIfEvaluationDateIsInExamsPeriod();
        checkIfEvaluationsDateIsEqualToMarkSheetEvaluationDate();
    }

    private void checkIfEvaluationDateIsWorkingDay() {
        if (getEvaluationDate().getDayOfWeek() == DateTimeConstants.SUNDAY || Holiday.isHoliday(getEvaluationDate())) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.evaluationDateNotInWorkingDay",
                    getEvaluationDate().toString());
        }
    }

    private void checkIfEvaluationDateIsInExamsPeriod() {
        final Set<EvaluationSeasonPeriod> periods = getExamsPeriods();

        if (periods.isEmpty()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheet.evaluationDateNotInExamsPeriod.undefined",
                    EvaluationSeasonServices.getDescriptionI18N(getEvaluationSeason()).getContent(),
                    getExecutionSemester().getQualifiedName());
        }

        for (final EvaluationSeasonPeriod iter : periods) {
            if (iter.isContainingDate(getEvaluationDate())) {
                return;
            }
        }

        throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.evaluationDateNotInExamsPeriod",
                getEvaluationDate().toString(), EvaluationSeasonPeriod.getIntervalsDescription(periods));
    }

    protected void checkIfIsGradeSubmissionAvailable() {

        if (getExpireDate() != null) {

            if (getExpireDate().isBefore(new LocalDate())) {
                throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.notInGradeSubmissionPeriod.expired");
            }

            return;
        }

        final Set<EvaluationSeasonPeriod> periods = getGradeSubmissionPeriods();

        if (periods.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.notInGradeSubmissionPeriod.undefined",
                    EvaluationSeasonServices.getDescriptionI18N(getEvaluationSeason()).getContent(),
                    getExecutionSemester().getQualifiedName());
        }

        for (final EvaluationSeasonPeriod iter : periods) {

            if (iter.isContainingDate(new LocalDate())) {
                return;
            }
        }

        throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.notInGradeSubmissionPeriod",
                EvaluationSeasonPeriod.getIntervalsDescription(periods));
    }

    private void checkIfEvaluationsDateIsEqualToMarkSheetEvaluationDate() {
        for (final EnrolmentEvaluation iter : getEnrolmentEvaluationSet()) {
            // TODO legidio, use EnrolmentEvaluationServices.getExamDateTime ?
            if (!iter.getExamDateYearMonthDay().toLocalDate().isEqual(getEvaluationDate())) {
                throw new AcademicExtensionsDomainException(
                        "error.CompetenceCourseMarkSheet.evaluations.examDate.must.be.equal.marksheet.evaluationDate");
            }
        }
    }

    @Atomic
    public void edit(final LocalDate evaluationDate, final GradeScale gradeScale, final Person certifier,
            final LocalDate expireDate) {

        if (!isEdition()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheet.markSheet.can.only.be.updated.in.edition.state");
        }

        getEnrolmentEvaluationSet().forEach(e -> {
            e.setExamDateYearMonthDay(evaluationDate == null ? null : evaluationDate.toDateTimeAtStartOfDay().toYearMonthDay());
            e.setPersonResponsibleForGrade(certifier);
        });

        init(getExecutionSemester(), getCompetenceCourse(), getExecutionCourse(), getEvaluationSeason(), getCourseEvaluation(),
                evaluationDate, gradeScale, certifier, getShiftSet(), expireDate);

        checkRules();
    }

    void editExpireDate(LocalDate expireDate) {
        super.setExpireDate(expireDate);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {

        if (!isEdition()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheet.markSheet.can.only.be.deleted.in.edition.state");
        }

        setExecutionSemester(null);
        setCompetenceCourse(null);
        setExecutionCourse(null);
        setEvaluationSeason(null);
        setCourseEvaluation(null);
        setCertifier(null);
        getShiftSet().clear();
        removeEnrolmentEvaluationData();

        final Iterator<CompetenceCourseMarkSheetStateChange> stateIterator = getStateChangeSet().iterator();
        while (stateIterator.hasNext()) {
            final CompetenceCourseMarkSheetStateChange stateChange = stateIterator.next();
            stateIterator.remove();
            stateChange.delete();
        }

        final Iterator<CompetenceCourseMarkSheetChangeRequest> changeRequestIterator = getChangeRequestsSet().iterator();
        while (changeRequestIterator.hasNext()) {
            final CompetenceCourseMarkSheetChangeRequest changeRequest = changeRequestIterator.next();
            changeRequestIterator.remove();
            changeRequest.delete();
        }

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        deleteDomainObject();
    }

    private void removeEnrolmentEvaluationData() {
        for (final EnrolmentEvaluation evaluation : getEnrolmentEvaluationSet()) {
            removeEnrolmentEvaluationData(evaluation);
        }
    }

    public Set<EvaluationSeasonPeriod> getGradeSubmissionPeriods() {
        return getEvaluationSeasonPeriods(EvaluationSeasonPeriodType.GRADE_SUBMISSION);
    }

    public Set<EvaluationSeasonPeriod> getExamsPeriods() {
        return getEvaluationSeasonPeriods(EvaluationSeasonPeriodType.EXAMS);
    }

    private Set<EvaluationSeasonPeriod> getEvaluationSeasonPeriods(final EvaluationSeasonPeriodType periodType) {
        final Set<EvaluationSeasonPeriod> result = Sets.<EvaluationSeasonPeriod> newHashSet();

        for (final EvaluationSeasonPeriod iter : getExecutionSemester().getEvaluationSeasonPeriodSet()) {
            if (iter.getPeriodType() == periodType && iter.getSeason() == getEvaluationSeason()
                    && !Sets.intersection(iter.getExecutionDegrees(), getExecutionDegrees()).isEmpty()) {
                result.add(iter);
            }
        }

        return result;
    }

    private Set<ExecutionDegree> getExecutionDegrees() {
        return getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .map(i -> i.getExecutionDegreeFor(getExecutionSemester().getAcademicInterval())).collect(Collectors.toSet());
    }

    // @formatter: off
    /************
     * SERVICES
     ************/
    // @formatter: on

    @Atomic
    public static CompetenceCourseMarkSheet create(final ExecutionSemester semester, final CompetenceCourse competence,
            final ExecutionCourse execution, final EvaluationSeason season, final Evaluation courseEvaluation,
            final LocalDate evaluationDate, final Person certifier, final Set<Shift> shifts, final boolean byTeacher) {

        final CompetenceCourseMarkSheet result = new CompetenceCourseMarkSheet();
        final GradeScale gradeScale = competence.getGradeScale() == null ? GradeScale.TYPE20 : competence.getGradeScale();
        result.init(semester, competence, execution, season, courseEvaluation, evaluationDate, gradeScale, certifier, shifts,
                null);

        if (byTeacher && !EvaluationSeasonServices.isSupportsEmptyGrades(season)
                && findBy(semester, competence, execution, season, result.getEvaluationDateTime(), shifts,
                        (CompetenceCourseMarkSheetStateEnum) null, (CompetenceCourseMarkSheetChangeRequestStateEnum) null)
                                .anyMatch(i -> i != result)) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.duplicated",
                    season.getName().getContent());
        }

        CompetenceCourseMarkSheetStateChange.createEditionState(result, byTeacher, null);
        return result;
    }

    public static Stream<CompetenceCourseMarkSheet> findBy(final ExecutionCourse executionCourse) {

        final Set<CompetenceCourseMarkSheet> result = Sets.newHashSet();

        if (executionCourse != null) {

            for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
                result.addAll(curricularCourse.getCompetenceCourse().getCompetenceCourseMarkSheetSet());
            }
        }

        return result.stream().filter(c -> c.getExecutionSemester() == executionCourse.getExecutionPeriod());
    }

    public static Stream<CompetenceCourseMarkSheet> findBy(final ExecutionSemester executionSemester,
            final CompetenceCourse competenceCourse, final ExecutionCourse executionCourse,
            final EvaluationSeason evaluationSeason, final DateTime evaluationDateTime, final Set<Shift> shifts,
            final CompetenceCourseMarkSheetStateEnum markSheetState,
            final CompetenceCourseMarkSheetChangeRequestStateEnum changeRequestState) {

        final Set<CompetenceCourseMarkSheet> result = Sets.newHashSet();
        if (executionSemester != null) {
            result.addAll(executionSemester.getCompetenceCourseMarkSheetSet());
        }

        return result.stream()

                .filter(c -> competenceCourse == null || c.getCompetenceCourse() == competenceCourse)

                .filter(c -> executionCourse == null || c.getExecutionCourse() == executionCourse)

                .filter(c -> evaluationSeason == null || c.getEvaluationSeason() == evaluationSeason)

                .filter(c -> evaluationDateTime == null || c.getEvaluationDateTime().equals(evaluationDateTime))

                .filter(c -> shifts == null || shifts.isEmpty() || Sets.symmetricDifference(shifts, c.getShiftSet()).isEmpty())

                .filter(c -> markSheetState == null || c.isInState(markSheetState))

                .filter(c -> changeRequestState == null
                        || c.getChangeRequestsSet().stream().anyMatch(r -> r.getState() == changeRequestState));
    }

    private CompetenceCourseMarkSheetStateChange getFirstStateChange() {
        return getStateChangeSet().stream().min(CompetenceCourseMarkSheetStateChange::compareTo).get();
    }

    private CompetenceCourseMarkSheetStateChange getStateChange() {
        return getStateChangeSet().stream().max(CompetenceCourseMarkSheetStateChange::compareTo).get();
    }

    public boolean isEdition() {
        return getStateChange().isEdition();
    }

    public boolean isSubmitted() {
        return getStateChange().isSubmitted();
    }

    public boolean isConfirmed() {
        return getStateChange().isConfirmed();
    }

    public DateTime getCreationDate() {
        return getFirstStateChange().getDate();
    }

    public Person getCreator() {
        return getFirstStateChange().getResponsible();
    }

    public String getState() {
        return getStateChange().getState().getDescriptionI18N().getContent();
    }

    public DateTime getStateDate() {
        return getStateChange().getDate();
    }

    public String getEvaluationDatePresentation() {
        return getEvaluationDatePresentation(getEvaluationDateTime());
    }

    public DateTime getEvaluationDateTime() {
        return getEvaluationDateTime(getCourseEvaluation(), getEvaluationDate());
    }

    static public String getEvaluationDatePresentation(final DateTime input) {
        return input.toString(!input.toString().contains(
                "T00:00") ? EnrolmentEvaluationServices.EVALUATION_DATE_TIME_FORMAT : EnrolmentEvaluationServices.EVALUATION_DATE_FORMAT);
    }

    static public DateTime getEvaluationDateTime(final Evaluation courseEvaluation, final LocalDate evaluationDate) {
        return getEvaluationDateTime(courseEvaluation == null ? null : new DateTime(courseEvaluation.getEvaluationDate()),
                evaluationDate);
    }

    static public DateTime getEvaluationDateTime(final DateTime evaluationDateTime, final LocalDate evaluationDate) {
        if (evaluationDateTime != null) {
            return evaluationDateTime;
        } else {
            return evaluationDate.toDateTimeAtStartOfDay();
        }
    }

    public boolean hasCourseEvaluationDate() {
        final Evaluation courseEvaluation = getCourseEvaluation();
        return courseEvaluation != null && courseEvaluation.getEvaluationDate() != null;
    }

    @Atomic
    public void markAsPrinted() {
        super.setPrinted(true);
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionSemester().getExecutionYear();
    }

    public String getShiftsDescription() {
        return getShiftSet().stream().map(i -> i.getNome()).collect(Collectors.joining(", "));
    }

    /**
     * Tests for final and temporary evaluations are made in filterEnrolmentsForGradeSubmission
     * 
     * @see {@link com.qubit.qubEdu.module.academicOffice.presentation.actions.teacher.MarkSheetTeacherManagementDispatchAction.getEnrolmentsNotInAnyMarkSheet}
     */
    static private Set<Enrolment> getEnrolmentsNotInAnyMarkSheet(final ExecutionSemester semester,
            final CompetenceCourse competence, final ExecutionCourse execution, final EvaluationSeason season,
            final LocalDate evaluationDate, final Set<Shift> shifts) {

        return filterEnrolmentsForGradeSubmission(collectEnrolmentsForGradeSubmission(semester, competence, execution), semester,
                season, evaluationDate, shifts);
    }

    /**
     * Collects enrolments based on mark sheet parameters (competence, semester, execution course)
     * Does not deal with anual competence courses, this is done in getExecutionCourseEnrolmentsNotInAnyMarkSheet
     */
    static private Set<Enrolment> collectEnrolmentsForGradeSubmission(final ExecutionSemester semester,
            final CompetenceCourse competence, final ExecutionCourse execution) {

        final Set<Enrolment> result = Sets.newHashSet();

        for (final ExecutionCourse iter : competence.getExecutionCoursesByExecutionPeriod(semester)) {

            if (execution == null || iter == execution) {

                for (final CurricularCourse curricularCourse : iter.getAssociatedCurricularCoursesSet()) {

                    for (final CurriculumModule curriculumModule : curricularCourse.getCurriculumModulesSet()) {

                        if (!curriculumModule.isEnrolment()) {
                            continue;
                        }

                        result.add((Enrolment) curriculumModule);
                    }
                }

                // also look at execution course atends
                for (final Attends attends : iter.getAttendsSet()) {
                    final Enrolment enrolment = attends.getEnrolment();
                    if (enrolment != null) {
                        result.add(enrolment);
                    }
                }
            }
        }

        return result;
    }

    public Set<Enrolment> getExecutionCourseEnrolmentsNotInAnyMarkSheet() {
        // We need static services for report purposes
        final Set<Enrolment> result = Sets.newHashSet(getExecutionCourseEnrolmentsNotInAnyMarkSheet(getExecutionSemester(),
                getCompetenceCourse(), getExecutionCourse(), getEvaluationSeason(), getEvaluationDate(), getShiftSet()));

        // mark sheet has no course evaluation, nothing to be done
        if (getCourseEvaluation() == null) {
            return result;
        }

        for (final Iterator<Enrolment> iterator = result.iterator(); iterator.hasNext();) {
            final Enrolment enrolment = iterator.next();

            // student hasn't enroled in any course evaluation (manual creation of EnrolmentEvaluation
            if (!EvaluationServices.isEnroledInAnyCourseEvaluation(enrolment, getEvaluationSeason(), getExecutionSemester())) {
                continue;
            }

            final Set<Evaluation> courseEvaluations =
                    EvaluationServices.findEnrolmentCourseEvaluations(enrolment, getEvaluationSeason(), getExecutionSemester());
            if (courseEvaluations.contains(getCourseEvaluation())) {

                // student enroled in mark sheet's course evaluation
                continue;

            } else {

                // student enroled in other course evaluation but it's type is marked as to be ignored in this mark sheet filtering
                if (courseEvaluations.stream().anyMatch(
                        courseEvaluation -> EvaluationServices.isCourseEvaluationIgnoredInMarkSheet(courseEvaluation))) {
                    continue;
                }
            }

            iterator.remove();
        }

        return result;
    }

    /**
     * Algorithm entry point
     * Deals with anual competence courses
     */
    static public Set<Enrolment> getExecutionCourseEnrolmentsNotInAnyMarkSheet(final ExecutionSemester semester,
            final CompetenceCourse competence, final ExecutionCourse execution, final EvaluationSeason season,
            final LocalDate evaluationDate, final Set<Shift> shifts) {

        final Set<Enrolment> result = Sets.newHashSet();

        for (final Enrolment enrolment : getEnrolmentsNotInAnyMarkSheet(semester, competence, execution, season, evaluationDate,
                shifts)) {

            if (competence.isAnual() && semester == semester.getExecutionYear().getLastExecutionPeriod()) {

                final ExecutionCourse otherExecutionCourse =
                        enrolment.getExecutionCourseFor(semester.getExecutionYear().getFirstExecutionPeriod());
                if (otherExecutionCourse != null && otherExecutionCourse.getAssociatedCurricularCoursesSet()
                        .containsAll(execution.getAssociatedCurricularCoursesSet())) {

                    if (enrolment.getAttendsByExecutionCourse(otherExecutionCourse) != null) {
                        result.add(enrolment);
                    }
                }

            } else {
                if (enrolment.getAttendsByExecutionCourse(execution) != null) {
                    result.add(enrolment);
                }
            }
        }

        return result;
    }

    /**
     * @see {@link com.qubit.qubEdu.module.academicOffice.domain.evaluation.EvaluationSeason.getEnrolmentsForGradeSubmission}
     */
    static private Set<Enrolment> filterEnrolmentsForGradeSubmission(final Set<Enrolment> enrolments,
            final ExecutionSemester semester, final EvaluationSeason season, final LocalDate evaluationDate,
            final Set<Shift> shifts) {

        final Set<Enrolment> result = Sets.newHashSet();

        for (final Enrolment enrolment : enrolments) {

            if (enrolment.isAnnulled()) {
                continue;
            }

            if (!EvaluationSeasonServices.isDifferentEvaluationSemesterAccepted(season) && !enrolment.isValid(semester)) {
                continue;
            }

            if (!shifts.isEmpty() && !EnrolmentServices.containsAnyShift(enrolment, semester, shifts)) {
                continue;
            }

            // inspect enrolment evaluation of the given season
            final Optional<EnrolmentEvaluation> inspect = enrolment.getEnrolmentEvaluation(season, semester, null);
            final EnrolmentEvaluation evaluation = inspect.isPresent() ? inspect.get() : null;
            if (evaluation != null) {

                // isEvaluatedInSeason
                if (!evaluation.isTemporary()) {
                    continue;
                }

                if (evaluation.isAnnuled()) {
                    continue;
                }

                // isEnroledInSeason
                if (evaluation.isTemporary()) {

                    // already included in a mark sheet
                    if (evaluation.getCompetenceCourseMarkSheet() != null) {
                        continue;
                    }

                    if (EvaluationSeasonServices.isRequiredEnrolmentEvaluation(season)) {
                        // is manual, enrolment must be included
                        result.add(enrolment);
                        continue;
                    }
                }

            } else if (EvaluationSeasonServices.isRequiredEnrolmentEvaluation(season)) {
                // is manual, evaluation should exist
                continue;
            }

            // the real business logic
            if (isEnrolmentCandidateForEvaluation(enrolment, evaluationDate, semester, season)) {
                result.add(enrolment);
            }
        }

        return result;
    }

    /**
     * Filters according to evaluation season parameters, rules, etc
     */
    static private boolean isEnrolmentCandidateForEvaluation(final Enrolment enrolment, final LocalDate evaluationDate,
            final ExecutionSemester semester, final EvaluationSeason season) {

        // only evaluations before the input evaluation date should be investigated
        final Collection<EnrolmentEvaluation> evaluations = getAllFinalEnrolmentEvaluations(enrolment, evaluationDate);
        final EnrolmentEvaluation latestEvaluation = getLatestEnrolmentEvaluation(evaluations);
        final boolean isApproved = latestEvaluation != null && latestEvaluation.isApproved();

        // this evaluation season is for not approved enrolments
        if (!EvaluationSeasonServices.isForApprovedEnrolments(season) && isApproved) {
            return false;
        }

        // this evaluation season is for approved enrolments
        if (EvaluationSeasonServices.isForApprovedEnrolments(season) && !isApproved) {
            return false;
        }

        if (EvaluationSeasonServices.hasStudentStatuteBlocking(season, enrolment, semester)) {
            return false;
        }

        if (EvaluationSeasonServices.hasPreviousSeasonBlockingGrade(season, latestEvaluation)) {
            return false;
        }

        if (!EvaluationSeasonServices.hasRequiredPreviousSeasonMinimumGrade(season, evaluations)) {
            return false;
        }

        if (EvaluationSeasonServices.isRequiredPreviousSeasonEvaluation(season)) {
            if (latestEvaluation == null) {
                return false;
            }

            boolean exclude = true;

            final EvaluationSeason previousSeason = EvaluationSeasonServices.getPreviousSeason(season);
            if (previousSeason != null) {

                // WARNING: we should be using EnrolmentEvaluation.find API, but for simplicity reasons we're dealing with this "manually" 
                for (final EnrolmentEvaluation iter : evaluations) {
                    if (iter.getEvaluationSeason() == previousSeason) {
                        exclude = false;
                        break;
                    }
                }
            }

            if (exclude) {
                return false;
            }
        }

        if (EvaluationSeasonServices.isBlockingTreasuryEventInDebt(season, enrolment, semester)) {
            return false;
        }

        if (!CurriculumAggregatorServices.isCandidateForEvaluation(season, enrolment)) {
            return false;
        }

        return true;
    }

    /**
     * Returns final evaluations that took place before the evaluation date
     */
    static private Collection<EnrolmentEvaluation> getAllFinalEnrolmentEvaluations(final Enrolment enrolment,
            final LocalDate evaluationDate) {

        final Collection<EnrolmentEvaluation> evaluations = enrolment.getAllFinalEnrolmentEvaluations();

        if (evaluationDate != null) {
            for (final Iterator<EnrolmentEvaluation> iterator = evaluations.iterator(); iterator.hasNext();) {
                final EnrolmentEvaluation enrolmentEvaluation = iterator.next();

                // TODO legidio, use EnrolmentEvaluationServices.getExamDateTime ?
                final YearMonthDay examDate = enrolmentEvaluation.getExamDateYearMonthDay();
                if (examDate != null && !examDate.isBefore(evaluationDate)) {
                    iterator.remove();
                }
            }
        }

        return evaluations;
    }

    static private EnrolmentEvaluation getLatestEnrolmentEvaluation(final Collection<EnrolmentEvaluation> evaluations) {
        return ((evaluations == null || evaluations.isEmpty()) ? null : evaluations.stream().max(new EvaluationComparator())
                .get());
    }

    public boolean isGradeValueAccepted(final String gradeValue) {

        if (!Strings.isNullOrEmpty(gradeValue)) {

            final GradeScaleValidator validator = getGradeScaleValidator();
            if (validator == null) {
                return getGradeScale().belongsTo(gradeValue);
            } else {
                return validator.isGradeValueAccepted(gradeValue);
            }
        }

        return false;
    }

    public String getGradeScaleDescription() {
        final GradeScaleValidator validator = getGradeScaleValidator();
        return validator == null ? getGradeScale().getDescription() : validator.getRuleDescription().getContent();
    }

    public GradeScaleValidator getGradeScaleValidator() {
        final SortedSet<GradeScaleValidator> result = Sets.newTreeSet(DomainObjectUtil.COMPARATOR_BY_ID);

        for (final GradeScaleValidator validator : EvaluationSeasonRule.find(getEvaluationSeason(), GradeScaleValidator.class)) {

            if (validator.getGradeScale() != getGradeScale()) {
                continue;
            }

            final Set<DegreeType> markSheetDegreeTypes = getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                    .map(c -> c.getDegree().getDegreeType()).collect(Collectors.toSet());
            if (Sets.intersection(markSheetDegreeTypes, validator.getDegreeTypeSet()).isEmpty()) {
                continue;
            }

            if (hasCurriculumAggregationData()) {

                if (!validator.getAppliesToCurriculumAggregatorEntry()) {
                    continue;
                }

                if (!isAggregationConsistentForGradeValueScale()) {
                    continue;
                }

            } else {

                if (validator.getAppliesToCurriculumAggregatorEntry()) {
                    continue;
                }
            }

            result.add(validator);
        }

        if (result.size() > 1) {
            logger.warn("Mark sheet {} has more than one GradeScaleValidator configured, returning the oldest", this);
        }

        return result.isEmpty() ? null : result.first();
    }

    private boolean isAggregationConsistentForGradeValueScale() {
        return getAggregationGradeValueScale() != null;
    }

    /**
     * Strange method, just to make sure we have consistency across the execution course's curricular courses.
     */
    private Integer getAggregationGradeValueScale() {
        Integer result = null;

        final Set<CurriculumAggregator> aggregators = getCurriculumAggregators();
        final Set<CurriculumAggregatorEntry> entries = getCurriculumAggregatorEntries();

        // let's find a candidate 
        final Integer temp = !aggregators.isEmpty() ? aggregators.iterator().next()
                .getGradeValueScale() : !entries.isEmpty() ? entries.iterator().next().getGradeValueScale() : null;

        // let's check consistency
        if (temp != null && aggregators.stream().allMatch(i -> i.getGradeValueScale() == temp)
                && entries.stream().allMatch(i -> i.getGradeValueScale() == temp.intValue())) {
            result = temp;
        }

        if (result == null) {
            logger.warn("Unable to find GradeValueScale for {}", this);
        }

        return result;
    }

    /**
     * Strange method, just to make sure we have consistency across the execution course's curricular courses.
     */
    private Boolean getAggregationSupportsTeacherConfirmation() {
        Boolean result = null;

        final Set<CurriculumAggregatorEntry> entries = getCurriculumAggregatorEntries();

        // let's find a candidate 
        final Boolean temp = !entries.isEmpty() ? entries.iterator().next().getSupportsTeacherConfirmation() : null;

        // let's check consistency
        if (temp != null && entries.stream().allMatch(i -> i.getSupportsTeacherConfirmation() == temp.booleanValue())) {
            result = temp;
        }

        if (result == null) {
            logger.warn("Unable to find SupportsTeacherConfirmation for {}", this);
        }

        return result;
    }

    private boolean hasCurriculumAggregationData() {
        return !getCurriculumAggregators().isEmpty() || !getCurriculumAggregatorEntries().isEmpty();
    }

    private boolean hasCurriculumAggregationDataInspected() {
        return this.curriculumAggregators != null && this.curriculumAggregatorEntries != null;
    }

    private Set<CurriculumAggregator> getCurriculumAggregators() {
        if (!hasCurriculumAggregationDataInspected()) {
            setCurriculumAggregationData();
        }

        return this.curriculumAggregators;
    }

    private Set<CurriculumAggregatorEntry> getCurriculumAggregatorEntries() {
        if (!hasCurriculumAggregationDataInspected()) {
            setCurriculumAggregationData();
        }

        return this.curriculumAggregatorEntries;
    }

    private void setCurriculumAggregationData() {
        final Entry<Set<CurriculumAggregator>, Set<CurriculumAggregatorEntry>> data = getCurriculumAggregationData();

        this.curriculumAggregators = data == null ? Sets.newHashSet() : data.getKey();
        this.curriculumAggregatorEntries = data == null ? Sets.newHashSet() : data.getValue();
    }

    /**
     * Ugly, but that's life sometimes.
     */
    private Entry<Set<CurriculumAggregator>, Set<CurriculumAggregatorEntry>> getCurriculumAggregationData() {
        final Map<Set<CurriculumAggregator>, Set<CurriculumAggregatorEntry>> collected = Maps.newHashMap();

        if (CurriculumAggregatorServices.isAggregationsActive(getExecutionYear())) {

            // try to find aggregation context for each associated curricular course
            final Set<Context> contexts = getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                    .filter(c -> !c.getEnrolmentsByExecutionPeriod(getExecutionSemester()).isEmpty())
                    .map(i -> CurriculumAggregatorServices.getContext(i, getExecutionSemester(), (CourseGroup) null))
                    .filter(i -> i != null).collect(Collectors.toSet());

            if (!contexts.isEmpty()) {
                // we don't know if we are dealing with aggregators or entries...
                // the CurricularCourses may even be configured with both status...
                // so...let's search everything.
                final Set<CurriculumAggregator> aggregators = contexts.stream()
                        .map(i -> CurriculumAggregatorServices.getAggregator(i, getExecutionYear()))
                        .filter(i -> i != null && i.isCandidateForEvaluation(getEvaluationSeason())).collect(Collectors.toSet());
                final Set<CurriculumAggregatorEntry> entries =
                        contexts.stream().map(i -> CurriculumAggregatorServices.getAggregatorEntry(i, getExecutionYear()))
                                .filter(i -> i != null).collect(Collectors.toSet());

                collected.put(aggregators, entries);
            }
        }

        // using a map entry just for the pairing...
        return collected.isEmpty() ? null : collected.entrySet().iterator().next();
    }

    @Atomic
    public void confirm(boolean byTeacher) {

        if (!isSubmitted()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.must.be.submitted.to.confirm");
        }

        if (getEnrolmentEvaluationSet().isEmpty()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheet.enrolmentEvaluations.required.to.confirm.markSheet");
        }

        if (byTeacher && !getSupportsTeacherConfirmation()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.unauthorized.teacher.confirmation",
                    getEvaluationSeason().getName().getContent());
        }

        // unfortunately this is relevant for the evaluation comparator used by Enrolment.getFinalEnrolmentEvaluation
        CompetenceCourseMarkSheetStateChange.createConfirmedState(this, byTeacher, null);

        for (final EnrolmentEvaluation evaluation : getEnrolmentEvaluationSet()) {

            // it hasn't been touched since last confirmation
            if (evaluation.isFinal()) {
                continue;
            }

            //TODO: force evaluation checksum generation
            evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
            evaluation.setGradeAvailableDateYearMonthDay(new YearMonthDay());

            // depends on EnrolmentEvaluationState
            EnrolmentEvaluationServices.onStateChange(evaluation);
            EnrolmentServices.updateState(evaluation.getEnrolment());
            CurriculumAggregatorServices.updateAggregatorEvaluationTriggeredByEntry(evaluation);
        }
    }

    public boolean getSupportsTeacherConfirmation() {
        if (EvaluationSeasonServices.isSupportsTeacherConfirmation(getEvaluationSeason())) {
            return true;
        }

        final Boolean aggregationSupportsTeacherConfirmation = getAggregationSupportsTeacherConfirmation();
        if (aggregationSupportsTeacherConfirmation != null && aggregationSupportsTeacherConfirmation) {
            return true;
        }

        return false;
    }

    static public void setEnrolmentEvaluationData(final CompetenceCourseMarkSheet markSheet, final EnrolmentEvaluation evaluation,
            final String gradeValue, final GradeScale gradeScale) {

        // avoid concurrent mark sheet editions
        if (evaluation.getCompetenceCourseMarkSheet() != null && evaluation.getCompetenceCourseMarkSheet() != markSheet) {
            return;
        }

        // avoid making untouched grades unavailable
        if (gradeValue.equals(evaluation.getGradeValue())) {
            return;
        }

        // grade available date is set upon mark sheet confirmation
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);

        evaluation.setGrade(Grade.createGrade(gradeValue, gradeScale));
        evaluation.setGradeScale(gradeScale);
        evaluation.setWhenDateTime(new DateTime());

        evaluation.setCompetenceCourseMarkSheet(markSheet);
        evaluation.setExamDateYearMonthDay(markSheet.getEvaluationDateTime().toYearMonthDay());
        evaluation.setPersonResponsibleForGrade(markSheet.getCertifier());
        evaluation.setPerson(Authenticate.getUser().getPerson());
        evaluation.setContext(EnrolmentEvaluationContext.MARK_SHEET_EVALUATION);

        // this was once performed in revertToEdition; depends on EnrolmentEvaluationState
        EnrolmentEvaluationServices.onStateChange(evaluation);
        EnrolmentServices.updateState(evaluation.getEnrolment());
        CurriculumAggregatorServices.updateAggregatorEvaluationTriggeredByEntry(evaluation);
    }

    static public void removeEnrolmentEvaluationData(final EnrolmentEvaluation evaluation) {
        // unnecessary computation
        if (!evaluation.hasGrade()) {
            return;
        }

        // before refactor, this was once performed in revertToEdition
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        evaluation.setGradeAvailableDateYearMonthDay((YearMonthDay) null);

        // additional cleanup
        evaluation.setGrade(Grade.createEmptyGrade());
        evaluation.setWhenDateTime((DateTime) null);
        evaluation.setCompetenceCourseMarkSheet(null);
        evaluation.setExamDateYearMonthDay((YearMonthDay) null);
        evaluation.setPersonResponsibleForGrade((Person) null);
        evaluation.setPerson((Person) null);
        evaluation.setContext((EnrolmentEvaluationContext) null);

        // before refactor, this was once performed in revertToEdition; depends on EnrolmentEvaluationState
        EnrolmentEvaluationServices.onStateChange(evaluation);
        // FIXME hack for bypass evaluation method type issues
        if (FenixFramework.isDomainObjectValid(evaluation)) {
            EnrolmentServices.updateState(evaluation.getEnrolment());
            CurriculumAggregatorServices.updateAggregatorEvaluationTriggeredByEntry(evaluation);
        }
    }

    @Atomic
    public void submit(boolean byTeacher) {

        if (!isEdition()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.must.be.edition.to.confirm");
        }

        final CompetenceCourseMarkSheetStateChange stateChange =
                CompetenceCourseMarkSheetStateChange.createSubmitedState(this, byTeacher, null);

        final CompetenceCourseMarkSheetSnapshot snapshot =
                CompetenceCourseMarkSheetSnapshot.create(stateChange, getCompetenceCourse().getCode(),
                        getCompetenceCourse().getNameI18N().toLocalizedString(), getExecutionSemester().getQualifiedName(),
                        getEvaluationSeason().getName(), getCertifier().getName(), getEvaluationDate(), getEvaluationDateTime());

        for (final EnrolmentEvaluation evaluation : getSortedEnrolmentEvaluations()) {
            final Registration registration = evaluation.getRegistration();
            final Degree degree = evaluation.getStudentCurricularPlan().getDegree();
            snapshot.addEntry(registration.getNumber(), registration.getName(), evaluation.getGrade(), degree.getCode(),
                    degree.getNameI18N().toLocalizedString(),
                    EnrolmentServices.getShiftsDescription(evaluation.getEnrolment(), evaluation.getExecutionPeriod()));
        }

        snapshot.finalize();
    }

    @Atomic
    public void revertToEdition(boolean byTeacher, String reason) {

        if (isEdition()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.already.in.edition");
        }

        CompetenceCourseMarkSheetStateChange.createEditionState(this, byTeacher, reason);
    }

    public String getCheckSum() {
        if (isEdition()) {
            return null;
        }

        return getLastSnapshot().get().getCheckSum();
    }

    public String getFormattedCheckSum() {
        if (isEdition()) {
            return null;
        }

        return getLastSnapshot().get().getFormattedCheckSum();
    }

    public SortedSet<EnrolmentEvaluation> getSortedEnrolmentEvaluations() {

        final Comparator<EnrolmentEvaluation> byStudentName = (x, y) -> CompetenceCourseMarkSheet.COMPARATOR_FOR_STUDENT_NAME
                .compare(x.getRegistration().getStudent().getName(), y.getRegistration().getStudent().getName());

        final SortedSet<EnrolmentEvaluation> result =
                Sets.newTreeSet(byStudentName.thenComparing(DomainObjectUtil.COMPARATOR_BY_ID));

        result.addAll(getEnrolmentEvaluationSet());

        return result;

    }

    private Optional<CompetenceCourseMarkSheetStateChange> getLastStateBy(CompetenceCourseMarkSheetStateEnum type) {
        return getStateChangeSet().stream().filter(s -> s.getState() == type)
                .max(CompetenceCourseMarkSheetStateChange::compareTo);

    }

    public Optional<CompetenceCourseMarkSheetSnapshot> getLastSnapshot() {
        final Optional<CompetenceCourseMarkSheetStateChange> lastStateChange =
                getLastStateBy(CompetenceCourseMarkSheetStateEnum.findSubmited());

        return Optional.of(lastStateChange.isPresent() ? lastStateChange.get().getSnapshot() : null);
    }

    public List<CompetenceCourseMarkSheetSnapshot> getSnapshots() {
        return getStateChangeSet().stream().sorted()
                .filter(s -> s.getState() == CompetenceCourseMarkSheetStateEnum.findSubmited()).map(s -> s.getSnapshot())
                .collect(Collectors.toList());

    }

    public Collection<CompetenceCourseMarkSheetSnapshot> getPreviousSnapshots() {

        final Collection<CompetenceCourseMarkSheetSnapshot> snapshots = getSnapshots();
        if (snapshots.isEmpty()) {
            return Collections.emptySet();
        }

        final CompetenceCourseMarkSheetSnapshot lastSnapshot = getLastSnapshot().get();
        return snapshots.stream().filter(s -> s != lastSnapshot).collect(Collectors.toSet());

    }

    public boolean isInState(CompetenceCourseMarkSheetStateEnum markSheetState) {
        return getStateChange().getState() == markSheetState;
    }

    public boolean isCertifierExecutionCourseResponsible() {
        final Professorship professorship = getExecutionCourse().getProfessorship(getCertifier());
        return professorship != null && professorship.isResponsibleFor();
    }

    public boolean getLimitTeacherView() {
        final Person logged = Authenticate.getUser().getPerson();
        final boolean teacherCanView = getCertifier() == logged || getCreator() == logged
                || MarkSheetSettings.getInstance().getAllowTeacherToChooseCertifier();
        return !teacherCanView;
    }

    public CompetenceCourseMarkSheetChangeRequest getLastChangeRequest() {
        final Optional<CompetenceCourseMarkSheetChangeRequest> result =
                getChangeRequestsSet().stream().max(CompetenceCourseMarkSheetChangeRequest.COMPARATOR_BY_REQUEST_DATE);

        return result.isPresent() ? result.get() : null;
    }

    public CompetenceCourseMarkSheetChangeRequest getLastPendingChangeRequest() {
        final Optional<CompetenceCourseMarkSheetChangeRequest> result =
                getPendingChangeRequests().stream().max(CompetenceCourseMarkSheetChangeRequest.COMPARATOR_BY_REQUEST_DATE);

        return result.isPresent() ? result.get() : null;

    }

    public Set<CompetenceCourseMarkSheetChangeRequest> getPendingChangeRequests() {
        return getChangeRequestsSet().stream().filter(i -> i.isPending()).collect(Collectors.toSet());
    }

    public SortedSet<CompetenceCourseMarkSheetChangeRequest> getSortedChangeRequests() {

        final SortedSet<CompetenceCourseMarkSheetChangeRequest> result =
                Sets.newTreeSet(CompetenceCourseMarkSheetChangeRequest.COMPARATOR_BY_REQUEST_DATE.reversed());

        result.addAll(getChangeRequestsSet());

        return result;
    }

}
