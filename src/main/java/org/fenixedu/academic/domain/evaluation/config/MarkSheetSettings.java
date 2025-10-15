package org.fenixedu.academic.domain.evaluation.config;

import java.util.Collections;
import java.util.List;
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

    public static void init() {
        if (findAll().findAny().isEmpty()) {
            MarkSheetSettings.create(UnitUtils.readInstitutionUnit());
        }
    }

    @Deprecated
    public static MarkSheetSettings getInstance() {
        return findAll().findFirst().orElseGet(() -> MarkSheetSettings.create(UnitUtils.readInstitutionUnit()));
    }

    @Atomic
    public static MarkSheetSettings create(Unit unit) {
        MarkSheetSettings markSheetSettings = new MarkSheetSettings();
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

    public boolean isNotAllowedShifts() {
        return getRequiredNumberOfShifts() == 0;
    }

    public boolean isMarkSheetTemplateCodeDefined() {
        return StringUtils.isNotBlank(getMarkSheetTemplateCode());
    }

    public static Stream<MarkSheetSettings> findAll() {
        return Bennu.getInstance().getMarkSheetSettingsSet().stream();
    }

    public static Optional<MarkSheetSettings> findByCompetenceCourse(CompetenceCourse competenceCourse) {
        return Optional.ofNullable(competenceCourse).map(CompetenceCourse::getCompetenceCourseGroupUnit)
                .flatMap(groupUnit -> Optional.ofNullable(groupUnit.getMarkSheetSettings()).or(() -> {
                    List<Unit> parentUnits = groupUnit.getParentUnitsPath();
                    Collections.reverse(parentUnits);
                    return parentUnits.stream().map(Unit::getMarkSheetSettings).filter(Objects::nonNull).findFirst();
                }));
    }
}
