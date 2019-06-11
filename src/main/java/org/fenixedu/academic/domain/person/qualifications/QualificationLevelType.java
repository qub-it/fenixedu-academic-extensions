package org.fenixedu.academic.domain.person.qualifications;

import java.util.Collection;
import java.util.Comparator;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * 
 * @author shezad
 *
 */
public class QualificationLevelType extends QualificationLevelType_Base implements Comparable<QualificationLevelType> {

    public QualificationLevelType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("error.QualificationLevelType.name.cannotBeEmpty");
        }
        super.setName(name);
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getQualificationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.QualificationLevelType.delete.qualificationsNotEmpty"));
        }
        if (!getLevelsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.QualificationLevelType.delete.levelsNotEmpty"));
        }
    }

    public void delete() {
        super.setRoot(null);
        super.setFamily(null);
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(QualificationLevelType otherType) {
        if (getFamily() != null && otherType.getFamily() != null) {
            int familyCompare = getFamily().compareTo(otherType.getFamily());
            if (familyCompare != 0) {
                return familyCompare;
            }
        }

        final Comparator<String> nullSafeStringComparator = Comparator.nullsLast(String::compareToIgnoreCase);

        final Comparator<QualificationLevelType> levelComparator =
                Comparator.comparing(QualificationLevelType::getCode, nullSafeStringComparator)
                        .thenComparing(QualificationLevelType::getName).thenComparing(QualificationLevelType::getExternalId);

        return levelComparator.compare(this, otherType);
    }
}
