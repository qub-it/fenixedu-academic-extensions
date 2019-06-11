package org.fenixedu.academic.domain.person.qualifications;

import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * 
 * @author shezad
 *
 */
public class QualificationLevelTypeFamily extends QualificationLevelTypeFamily_Base
        implements Comparable<QualificationLevelTypeFamily> {

    public QualificationLevelTypeFamily() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("error.QualificationLevelTypeFamily.name.cannotBeEmpty");
        }
        super.setName(name);
    }

    @Override
    public void setCode(String code) {
        if (StringUtils.isNotBlank(code) && Bennu.getInstance().getQualificationLevelTypeFamiliesSet().stream()
                .anyMatch(f -> code.equals(f.getCode()) && f != this)) {
            throw new IllegalArgumentException("error.QualificationLevelTypeFamily.code.alreadyExists");
        }
        super.setCode(code);
    }

    public static QualificationLevelTypeFamily findByCode(final String code) {
        return StringUtils.isNotBlank(code) ? Bennu.getInstance().getQualificationLevelTypeFamiliesSet().stream()
                .filter(f -> code.equals(f.getCode())).findFirst().orElse(null) : null;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getTypesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.QualificationLevelTypeFamily.delete.typesNotEmpty"));
        }
    }

    @Override
    public int compareTo(QualificationLevelTypeFamily otherFamily) {
        final Comparator<String> nullSafeStringComparator = Comparator.nullsLast(String::compareToIgnoreCase);

        final Comparator<QualificationLevelTypeFamily> familyComparator = Comparator
                .comparing(QualificationLevelTypeFamily::getCode, nullSafeStringComparator)
                .thenComparing(QualificationLevelTypeFamily::getName).thenComparing(QualificationLevelTypeFamily::getExternalId);

        return familyComparator.compare(this, otherFamily);
    }

    public void delete() {
        super.setRoot(null);
        getTypesSet().forEach(t -> t.delete());
        super.deleteDomainObject();
    }
}
