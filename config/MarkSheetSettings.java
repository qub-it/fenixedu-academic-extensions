package org.fenixedu.academic.domain.evaluation.config;

import org.fenixedu.ulisboa.specifications.domain.ULisboaSpecificationsRoot;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;

import pt.ist.fenixframework.Atomic;

public class MarkSheetSettings extends MarkSheetSettings_Base {

    protected MarkSheetSettings() {
        super();
    }

    public static void init() {
        if (getInstance() == null) {
            makeInstance();
        }
    }

    public static MarkSheetSettings getInstance() {
        return ULisboaSpecificationsRoot.getInstance().getMarkSheetSettings();
    }

    @Atomic
    private static void makeInstance() {
        ULisboaSpecificationsRoot.getInstance().setMarkSheetSettings(new MarkSheetSettings());
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

    static public boolean isRequiredNumberOfShifts(final int input) {
        if (isUnspecifiedNumberOfShifts()) {
            return true;
        }

        if (isNotAllowedShifts() && input != 0) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheet.shifts.not.allowed");
        }

        if (isRequiredAtLeastOneShift() && input <= 0) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheet.shift.required");
        }

        if (!isRequiredAtLeastOneShift() && getInstance().getRequiredNumberOfShifts() != input) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheet.shifts.required",
                    String.valueOf(MarkSheetSettings.getInstance().getRequiredNumberOfShifts()));
        }

        return true;
    }

    static public boolean isUnspecifiedNumberOfShifts() {
        return getInstance().getRequiredNumberOfShifts() < 0;
    }

    static public boolean isNotAllowedShifts() {
        return getInstance().getRequiredNumberOfShifts() == 0;
    }

    static public boolean isRequiredAtLeastOneShift() {
        return getInstance().getRequiredNumberOfShifts() >= 10;
    }

}
