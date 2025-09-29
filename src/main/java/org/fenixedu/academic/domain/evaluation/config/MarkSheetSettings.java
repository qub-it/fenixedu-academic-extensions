package org.fenixedu.academic.domain.evaluation.config;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class MarkSheetSettings extends MarkSheetSettings_Base {

    protected MarkSheetSettings() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public static void init() {
        if (findAll().findAny().isEmpty()) {
            MarkSheetSettings markSheetSettings = new MarkSheetSettings();
            markSheetSettings.setUnit(UnitUtils.readInstitutionUnit());
        }
    }

    @Deprecated
    @Atomic
    public static MarkSheetSettings getInstance() {
        return findAll().findFirst().orElseGet(MarkSheetSettings::new);
    }

    public static MarkSheetSettings create(boolean allowTeacherToChooseCertifier, int requiredNumberOfShifts,
            boolean limitCertifierToResponsibleTeacher, boolean limitCreationToResponsibleTeacher, String markSheetTemplateCode,
            Unit unit) {
        MarkSheetSettings markSheetSettings = new MarkSheetSettings();

        markSheetSettings.setAllowTeacherToChooseCertifier(allowTeacherToChooseCertifier);
        markSheetSettings.setRequiredNumberOfShifts(requiredNumberOfShifts);
        markSheetSettings.setLimitCertifierToResponsibleTeacher(limitCertifierToResponsibleTeacher);
        markSheetSettings.setLimitCreationToResponsibleTeacher(limitCreationToResponsibleTeacher);
        markSheetSettings.setMarkSheetTemplateCode(markSheetTemplateCode);
        markSheetSettings.setUnit(unit);

        return markSheetSettings;
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

    @Override
    public void setUnit(final Unit unit) {
        if (unit == null) {
            throw new AcademicExtensionsDomainException("error.MarkSheetSettings.unit.required");
        }

        if (unit.getMarkSheetSettings() != null && unit.getMarkSheetSettings() != this) {
            throw new AcademicExtensionsDomainException("error.MarkSheetSettings.unit.already.has.markSheetSettings");
        }

        super.setUnit(unit);
    }

    public void delete() {
        if (getTemplateFile() != null) {
            getTemplateFile().delete();
        }

        super.setUnit(null);
        super.setRoot(null);
        super.deleteDomainObject();
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

        if (!isRequiredAtLeastOneShift() && getRequiredNumberOfShifts() != input) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheet.shifts.required",
                    String.valueOf(getRequiredNumberOfShifts()));
        }

        return true;
    }

    public boolean isUnspecifiedNumberOfShifts() {
        return getRequiredNumberOfShifts() < 0;
    }

    public boolean isNotAllowedShifts() {
        return getRequiredNumberOfShifts() == 0;
    }

    public boolean isRequiredAtLeastOneShift() {
        return getRequiredNumberOfShifts() >= 10;
    }

    public boolean isMarkSheetTemplateCodeDefined() {
        return StringUtils.isNotBlank(getMarkSheetTemplateCode());
    }

    public static Stream<MarkSheetSettings> findAll() {
        return Bennu.getInstance().getMarkSheetSettingsSet().stream();
    }

    public static Optional<MarkSheetSettings> findByCompetenceCourse(CompetenceCourse competenceCourse) {
        if (competenceCourse == null) {
            return Optional.empty();
        }

        Unit groupUnit = competenceCourse.getCompetenceCourseGroupUnit();

        if (groupUnit == null) {
            return Optional.empty();
        }

        MarkSheetSettings settings = groupUnit.getMarkSheetSettings();

        return settings != null ? Optional.of(settings) : groupUnit.getParentUnitsPath().stream().map(Unit::getMarkSheetSettings)
                .filter(Objects::nonNull).findFirst();
    }
}
