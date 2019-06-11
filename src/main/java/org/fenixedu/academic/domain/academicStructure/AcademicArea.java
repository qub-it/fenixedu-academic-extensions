package org.fenixedu.academic.domain.academicStructure;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * 
 * @author shezad
 *
 */
public class AcademicArea extends AcademicArea_Base implements Comparable<AcademicArea> {

    public AcademicArea() {
        super();
        setRoot(Bennu.getInstance());
    }

    public AcademicArea(final String code, final LocalizedString name, final AcademicAreaType type) {
        this();
        setCode(code);
        setName(name);
        setType(type);
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("error.AcademicArea.name.cannotBeEmpty");
        }
        super.setName(name);
    }

    public static AcademicArea findByCodeAndType(final String code, AcademicAreaType type) {
        return type != null && StringUtils.isNotBlank(code) ? type.getAreasSet().stream().filter(a -> code.equals(a.getCode()))
                .findAny().orElse(null) : null;
    }
    
    public static Set<AcademicArea> findAcademicAreasInUnit(final Unit unit, final AcademicAreaType type) {
        return unit.getAcademicAreasSet().stream().filter(a -> a.getType() == type).collect(Collectors.toSet());
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getQualificationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.AcademicArea.delete.qualificationsNotEmpty"));
        }
    }

    public void delete() {
        super.setRoot(null);
        super.setType(null);
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(AcademicArea otherArea) {
        if (getType() != null && otherArea.getType() != null) {
            int typeCompare = getType().getName().compareTo(otherArea.getType().getName());
            if (typeCompare != 0) {
                return typeCompare;
            }
        }

        final Comparator<String> nullSafeStringComparator = Comparator.nullsLast(String::compareToIgnoreCase);

        final Comparator<AcademicArea> comparator = Comparator.comparing(AcademicArea::getCode, nullSafeStringComparator)
                .thenComparing(AcademicArea::getName).thenComparing(AcademicArea::getExternalId);

        return comparator.compare(this, otherArea);
    }

}
