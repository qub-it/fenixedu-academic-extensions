package org.fenixedu.academic.domain.evaluation.markSheet;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.FenixDigestUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.ulisboa.specifications.dto.evaluation.markSheet.MarkBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class CompetenceCourseMarkSheetSnapshot extends CompetenceCourseMarkSheetSnapshot_Base {

    protected CompetenceCourseMarkSheetSnapshot() {
        super();
    }

    protected void init(final CompetenceCourseMarkSheetStateChange stateChange, final String competenceCourseCode,
            final LocalizedString competenceCourseName, final String executionSemester, final LocalizedString evaluationSeason,
            final String certifier, final LocalDate evaluationDate, final DateTime evaluationDateTime) {

        setStateChange(stateChange);
        setCompetenceCourseCode(competenceCourseCode);
        setCompetenceCourseName(competenceCourseName);
        setExecutionSemester(executionSemester);
        setEvaluationSeason(evaluationSeason);
        setCertifier(certifier);
        setEvaluationDate(evaluationDate);
        setEvaluationDateTime(evaluationDateTime);
        checkRules();
    }

    private void checkRules() {

        if (getStateChange().getState() != CompetenceCourseMarkSheetStateEnum.findSubmited()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheetSnapshot.stateChange.must.be.of.type.submited");
        }

        if (getStateChange() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.stateChange.required");
        }

        if (getCompetenceCourseName() == null || getCompetenceCourseName().isEmpty()) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheetSnapshot.competenceCourseName.required");
        }

        if (StringUtils.isEmpty(getExecutionSemester())) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.executionSemester.required");
        }

        if (getEvaluationSeason() == null || getEvaluationSeason().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.evaluationSeason.required");
        }

        if (StringUtils.isEmpty(getCertifier())) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.certifier.required");
        }

        if (getEvaluationDate() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.evaluationDate.required");
        }

    }

    @Atomic
    public void delete() {
        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        final Iterator<CompetenceCourseMarkSheetSnapshotEntry> iterator = getEntrySet().iterator();
        while (iterator.hasNext()) {
            final CompetenceCourseMarkSheetSnapshotEntry entry = iterator.next();
            iterator.remove();
            entry.delete();
        }

        super.setStateChange(null);
        deleteDomainObject();
    }

    public void addEntry(final Integer studentNumber, final String studentName, final Grade grade, final String degreeCode,
            final LocalizedString degreeName, final String shifts) {
        getEntrySet().add(CompetenceCourseMarkSheetSnapshotEntry.create(this, studentNumber, studentName, grade, degreeCode,
                degreeName, shifts));
    }

    @Override
    public void finalize() {
        checkRules();

        if (getEntrySet().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshot.entry.required");
        }

        updateCheckSum();
    }

    private void updateCheckSum() {

        final StringBuilder content = new StringBuilder();
        content.append(getCompetenceCourseName().toString());
        content.append(getExecutionSemester());
        content.append(getEvaluationSeason().toString());
        content.append(getCertifier());
        // WARNING legidio, must not change this now, otherwise existing checksums will be modified!! 
        content.append(getStateChange().getCompetenceCourseMarkSheet().hasCourseEvaluationDate() ? getEvaluationDateTime()
                .toString("yyyy/MM/dd HH:mm") : getEvaluationDate().toString("yyyy/MM/dd"));
        content.append(getStateChange().getDate().toString("yyyy/MM/dd"));

        for (final CompetenceCourseMarkSheetSnapshotEntry entry : getSortedEntries()) {
            content.append(entry.getStudentNumber().toString());
            content.append(entry.getStudentName());
            content.append(entry.getGrade().getValue());
        }

        setCheckSum(FenixDigestUtils.createDigest(content.toString()));

    }

    public SortedSet<CompetenceCourseMarkSheetSnapshotEntry> getSortedEntries() {

        final Comparator<CompetenceCourseMarkSheetSnapshotEntry> byStudentName =
                (x, y) -> CompetenceCourseMarkSheet.COMPARATOR_FOR_STUDENT_NAME.compare(x.getStudentName(), y.getStudentName());

        final SortedSet<CompetenceCourseMarkSheetSnapshotEntry> result =
                Sets.newTreeSet(byStudentName.thenComparing(DomainObjectUtil.COMPARATOR_BY_ID));

        result.addAll(getEntrySet());

        return result;

    }

    public static CompetenceCourseMarkSheetSnapshot create(final CompetenceCourseMarkSheetStateChange stateChange,
            final String competenceCourseCode, final LocalizedString competenceCourseName, final String executionSemester,
            final LocalizedString evaluationSeason, final String certifier, final LocalDate evaluationDate,
            final DateTime evaluationDateTime) {

        final CompetenceCourseMarkSheetSnapshot result = new CompetenceCourseMarkSheetSnapshot();
        result.init(stateChange, competenceCourseCode, competenceCourseName, executionSemester, evaluationSeason, certifier,
                evaluationDate, evaluationDateTime);

        return result;
    }

    public String getFormattedCheckSum() {

        if (StringUtils.isEmpty(getCheckSum())) {
            return null;
        }

        final StringBuilder result = new StringBuilder();
        int counter = 0;
        for (final Character c : getCheckSum().toCharArray()) {
            result.append(c);
            if (++counter % 2 == 0) {
                result.append(" ");
            }
        }

        if (result.toString().endsWith(" ")) {
            result.delete(result.length() - 1, result.length());
        }

        return result.toString().toUpperCase();
    }

    public boolean isLastSnapshot() {
        return getStateChange().getCompetenceCourseMarkSheet().getLastSnapshot().get() == this;
    }

    public String getEvaluationDatePresentation() {
        final DateTime dateTime = CompetenceCourseMarkSheet.getEvaluationDateTime(getEvaluationDateTime(), getEvaluationDate());
        return CompetenceCourseMarkSheet.getEvaluationDatePresentation(dateTime);
    }

    public List<MarkBean> getDifferencesToNextGradeValues() {
        final CompetenceCourseMarkSheetSnapshot next = getNextSnapshot();
        return next != null ? compare(next) : compare(getMarkSheet());
    }

    private CompetenceCourseMarkSheetSnapshot getNextSnapshot() {
        final List<CompetenceCourseMarkSheetSnapshot> snapshots = getMarkSheet().getSnapshots();
        final int thisIndex = snapshots.indexOf(this);
        return thisIndex < 0 || thisIndex == snapshots.size() - 1 ? null : snapshots.get(thisIndex + 1);
    }

    private CompetenceCourseMarkSheet getMarkSheet() {
        return getStateChange().getCompetenceCourseMarkSheet();
    }

    private List<MarkBean> compare(final CompetenceCourseMarkSheetSnapshot other) {
        final List<MarkBean> result = Lists.newArrayList();

        final Set<CompetenceCourseMarkSheetSnapshotEntry> thisMarks = Sets.newHashSet(getEntrySet());
        final Set<CompetenceCourseMarkSheetSnapshotEntry> otherMarks = Sets.newHashSet(other.getEntrySet());

        for (final Iterator<CompetenceCourseMarkSheetSnapshotEntry> thisIterator = thisMarks.iterator(); thisIterator
                .hasNext();) {
            final CompetenceCourseMarkSheetSnapshotEntry thisMark = thisIterator.next();

            for (final Iterator<CompetenceCourseMarkSheetSnapshotEntry> otherIterator = otherMarks.iterator(); otherIterator
                    .hasNext();) {
                final CompetenceCourseMarkSheetSnapshotEntry otherMark = otherIterator.next();

                if (thisMark.getStudentNumber().intValue() == otherMark.getStudentNumber().intValue()) {
                    thisIterator.remove();
                    otherIterator.remove();

                    if (!otherMark.getGrade().equals(thisMark.getGrade())) {
                        final MarkBean bean = new MarkBean();
                        bean.setStudentNumber(otherMark.getStudentNumber());
                        bean.setStudentName(otherMark.getStudentName());
                        bean.setGradeValue(otherMark.getGrade().getValue());
                        bean.setInfoMessage("Nota anterior: " + thisMark.getGrade());
                        result.add(bean);
                    }
                }
            }
        }

        for (final CompetenceCourseMarkSheetSnapshotEntry thisMark : thisMarks) {
            final MarkBean bean = new MarkBean();
            bean.setStudentNumber(thisMark.getStudentNumber());
            bean.setStudentName(thisMark.getStudentName());
            bean.setInfoMessage("Nota removida: " + thisMark.getGrade());
            result.add(bean);
        }

        for (final CompetenceCourseMarkSheetSnapshotEntry otherMark : otherMarks) {
            final MarkBean bean = new MarkBean();
            bean.setStudentNumber(otherMark.getStudentNumber());
            bean.setStudentName(otherMark.getStudentName());
            bean.setGradeValue(otherMark.getGrade().getValue());
            bean.setInfoMessage("Nota adicionada");
            result.add(bean);
        }

        return result;
    }

    private List<MarkBean> compare(final CompetenceCourseMarkSheet other) {
        final List<MarkBean> result = Lists.newArrayList();

        final Set<CompetenceCourseMarkSheetSnapshotEntry> thisMarks = Sets.newHashSet(getEntrySet());
        final Set<EnrolmentEvaluation> otherMarks = Sets.newHashSet(other.getEnrolmentEvaluationSet());

        for (final Iterator<CompetenceCourseMarkSheetSnapshotEntry> thisIterator = thisMarks.iterator(); thisIterator
                .hasNext();) {
            final CompetenceCourseMarkSheetSnapshotEntry thisMark = thisIterator.next();

            for (final Iterator<EnrolmentEvaluation> otherIterator = otherMarks.iterator(); otherIterator.hasNext();) {
                final EnrolmentEvaluation otherMark = otherIterator.next();

                final Registration registration = otherMark.getRegistration();
                if (thisMark.getStudentNumber().intValue() == registration.getNumber().intValue()) {
                    thisIterator.remove();
                    otherIterator.remove();

                    if (!otherMark.getGrade().equals(thisMark.getGrade())) {
                        final MarkBean bean = new MarkBean();
                        bean.setStudentNumber(registration.getNumber());
                        bean.setStudentName(registration.getName());
                        bean.setGradeValue(otherMark.getGrade().getValue());
                        bean.setInfoMessage("Nota anterior: " + thisMark.getGrade());
                        result.add(bean);
                    }
                }
            }
        }

        for (final CompetenceCourseMarkSheetSnapshotEntry thisMark : thisMarks) {
            final MarkBean bean = new MarkBean();
            bean.setStudentNumber(thisMark.getStudentNumber());
            bean.setStudentName(thisMark.getStudentName());
            bean.setInfoMessage("Nota removida: " + thisMark.getGrade());
            result.add(bean);
        }

        for (final EnrolmentEvaluation otherMark : otherMarks) {
            final Registration registration = otherMark.getRegistration();

            final MarkBean bean = new MarkBean();
            bean.setStudentNumber(registration.getNumber());
            bean.setStudentName(registration.getName());
            bean.setGradeValue(otherMark.getGrade().getValue());
            bean.setInfoMessage("Nota adicionada");
            result.add(bean);
        }

        return result;
    }

}
