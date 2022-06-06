package org.fenixedu.academic.domain.student.curriculum;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

public class CreditsReasonType extends CreditsReasonType_Base {

    public CreditsReasonType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    private static boolean equalInAnyLanguage(final LocalizedString object, final LocalizedString value) {
        for (Locale objectLocale : object.getLocales()) {
            for (Locale valueLocale : value.getLocales()) {
                if (object.getContent(objectLocale).equals(value.getContent(valueLocale))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Atomic
    static public CreditsReasonType create(final LocalizedString reason, final boolean active, final boolean averageEntry,
            final boolean infoHidden, final LocalizedString infoText, final boolean infoExplained,
            final boolean infoExplainedWithCountry, final boolean infoExplainedWithInstitution,
            final boolean infoExplainedWithEcts) {

        final CreditsReasonType result = new CreditsReasonType();
        result.init(reason, active, averageEntry, infoHidden, infoText, infoExplained, infoExplainedWithCountry,
                infoExplainedWithInstitution, infoExplainedWithEcts);

        return result;
    }

    @Atomic
    public CreditsReasonType edit(final LocalizedString reason, final boolean active, final boolean averageEntry,
            final boolean infoHidden, final LocalizedString infoText, final boolean infoExplained,
            final boolean infoExplainedWithCountry, final boolean infoExplainedWithInstitution,
            final boolean infoExplainedWithEcts) {

        init(reason, active, averageEntry, infoHidden, infoText, infoExplained, infoExplainedWithCountry,
                infoExplainedWithInstitution, infoExplainedWithEcts);

        return this;
    }

    private void init(final LocalizedString reason, final boolean active, final boolean averageEntry, final boolean infoHidden,
            final LocalizedString infoText, final boolean infoExplained, final boolean infoExplainedWithCountry,
            final boolean infoExplainedWithInstitution, final boolean infoExplainedWithEcts) {

        super.setReason(reason);
        super.setActive(active);
        super.setAverageEntry(averageEntry);
        super.setInfoHidden(infoHidden);
        super.setInfoText(infoText);
        super.setInfoExplained(infoExplained);
        super.setInfoExplainedWithCountry(infoExplainedWithCountry);
        super.setInfoExplainedWithInstitution(infoExplainedWithInstitution);
        super.setInfoExplainedWithEcts(infoExplainedWithEcts);

        checkRules();
    }

    private void checkRules() {

        if (getReason() == null || getReason().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.CreditsReasonType.required.Reason");
        }

        for (final CreditsReasonType creditsReasonType : findAll()) {
            if (creditsReasonType != this && equalInAnyLanguage(creditsReasonType.getReason(), getReason())) {
                throw new AcademicExtensionsDomainException("error.CreditsReasonType.reason.must.be.unique");
            }
        }
    }

    public boolean isActive() {
        return super.getActive();
    }

    @Atomic
    public void delete() {
        if (!getCreditsSet().isEmpty()) {
            throw new AcademicExtensionsDomainException(
                    "error.CreditsReasonType.cannot.delete.because.already.has.credits.associated");
        }

        super.setRootDomainObject(null);
        super.deleteDomainObject();
    }

    static public Collection<CreditsReasonType> findActive() {
        final Set<CreditsReasonType> result = new HashSet<>();

        for (final CreditsReasonType reasonType : findAll()) {
            if (reasonType.isActive()) {
                result.add(reasonType);
            }
        }

        return result;
    }

    static public Collection<CreditsReasonType> findAll() {
        return Bennu.getInstance().getCreditsReasonTypesSet();
    }

}
