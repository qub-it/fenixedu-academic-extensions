package org.fenixedu.academic.domain.person.qualifications;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;

/**
 * 
 * @author shezad
 *
 */
public class QualificationLevel extends QualificationLevel_Base implements Comparable<QualificationLevel> {

    public QualificationLevel() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("error.QualificationLevel.name.cannotBeEmpty");
        }
        super.setName(name);
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getQualificationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.QualificationLevel.delete.qualificationsNotEmpty"));
        }
    }

    public void delete() {
        super.setRoot(null);
        getTypesSet().clear();
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(QualificationLevel otherLevel) {
        final Comparator<String> nullSafeStringComparator = Comparator.nullsLast(String::compareToIgnoreCase);

        final Comparator<QualificationLevel> levelComparator =
                Comparator.comparing(QualificationLevel::getCode, nullSafeStringComparator)
                        .thenComparing(QualificationLevel::getName).thenComparing(QualificationLevel::getExternalId);

        return levelComparator.compare(this, otherLevel);
    }

    public static Optional<QualificationLevel> findByCode(String code) {
        if (Strings.isNullOrEmpty(code)) {
            return Optional.empty();
        }
        return Bennu.getInstance().getQualificationLevelsSet().stream().filter(c -> code.equals(c.getCode())).findAny();
    }

}
