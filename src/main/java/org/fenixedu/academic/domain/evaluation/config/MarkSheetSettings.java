package org.fenixedu.academic.domain.evaluation.config;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class MarkSheetSettings extends MarkSheetSettings_Base {

    protected MarkSheetSettings() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public static MarkSheetSettings getInstance() {
        return findAll().findFirst().orElseGet(() -> new MarkSheetSettings());
    }

    @Atomic
    public void edit(final boolean allowTeacherToChooseCertifier, final int requiredNumberOfShifts,
            final boolean limitCertifierToResponsibleTeacher, final boolean limitCreationToResponsibleTeacher) {
        super.setAllowTeacherToChooseCertifier(allowTeacherToChooseCertifier);
        super.setRequiredNumberOfShifts(requiredNumberOfShifts);
        super.setLimitCertifierToResponsibleTeacher(limitCertifierToResponsibleTeacher);
        super.setLimitCreationToResponsibleTeacher(limitCreationToResponsibleTeacher);
    }

    @Atomic
    public void editTemplateFile(final String filename, final byte[] content) {

        if (getTemplateFile() != null) {
            getTemplateFile().delete();
        }

        CompetenceCourseMarkSheetTemplateFile.create(filename, content, this);
    }

    public boolean isRequiredNumberOfShifts(final int input) {
        if (isUnspecifiedNumberOfShifts()) {
            return true;
        }

        if (isNotAllowedShifts() && input != 0) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.shifts.not.allowed");
        }

        if (isRequiredAtLeastOneShift() && input <= 0) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.shift.required");
        }

        if (!isRequiredAtLeastOneShift() && getInstance().getRequiredNumberOfShifts() != input) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.shifts.required",
                    String.valueOf(getInstance().getRequiredNumberOfShifts()));
        }

        return true;
    }

    public boolean isUnspecifiedNumberOfShifts() {
        return getInstance().getRequiredNumberOfShifts() < 0;
    }

    public boolean isNotAllowedShifts() {
        return getInstance().getRequiredNumberOfShifts() == 0;
    }

    public boolean isRequiredAtLeastOneShift() {
        return getInstance().getRequiredNumberOfShifts() >= 10;
    }

    public boolean isMarkSheetTemplateCodeDefined() {
        return StringUtils.isNotBlank(getInstance().getMarkSheetTemplateCode());
    }

    public static Stream<MarkSheetSettings> findAll() {
        return Bennu.getInstance().getMarkSheetSettingsSet().stream();
    }
}
