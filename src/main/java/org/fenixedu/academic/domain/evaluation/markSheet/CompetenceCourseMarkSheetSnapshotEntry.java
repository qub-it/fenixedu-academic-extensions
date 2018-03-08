package org.fenixedu.academic.domain.evaluation.markSheet;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;

import pt.ist.fenixframework.Atomic;

public class CompetenceCourseMarkSheetSnapshotEntry extends CompetenceCourseMarkSheetSnapshotEntry_Base {

    protected CompetenceCourseMarkSheetSnapshotEntry() {
        super();
    }

    protected void init(final CompetenceCourseMarkSheetSnapshot snapshot, final Integer studentNumber, final String studentName,
            final Grade grade, final String degreeCode, final LocalizedString degreeName, final String shifts) {

        setSnapshot(snapshot);
        setStudentNumber(studentNumber);
        setStudentName(studentName);
        setGrade(grade);
        setDegreeCode(degreeCode);
        setDegreeName(degreeName);
        setShifts(shifts);

        checkRules();
    }

    private void checkRules() {

        if (getSnapshot() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshotEntry.snapshot.required");
        }

        if (getStudentNumber() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshotEntry.studentNumber.required");
        }

        if (StringUtils.isEmpty(getStudentName())) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshotEntry.studentName.required");
        }

        if (getGrade() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshotEntry.grade.required");
        }

        if (getDegreeName() == null || getDegreeName().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetSnapshotEntry.degreeName.required");
        }

    }

    @Atomic
    public void delete() {
        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        super.setSnapshot(null);

        deleteDomainObject();
    }

    @Atomic
    static CompetenceCourseMarkSheetSnapshotEntry create(final CompetenceCourseMarkSheetSnapshot snapshot,
            final Integer studentNumber, final String studentName, final Grade grade, final String degreeCode,
            final LocalizedString degreeName, final String shifts) {
        final CompetenceCourseMarkSheetSnapshotEntry result = new CompetenceCourseMarkSheetSnapshotEntry();
        result.init(snapshot, studentNumber, studentName, grade, degreeCode, degreeName, shifts);

        return result;
    }

}
