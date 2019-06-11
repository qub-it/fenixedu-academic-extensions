package org.fenixedu.academic.domain.researchPublication;

import java.util.Collection;
import java.util.Optional;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.base.Strings;

/**
 * 
 * @author shezad
 *
 */
public class ResearchPublicationType extends ResearchPublicationType_Base {

    public ResearchPublicationType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getResearchPublicationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ResearchPublicationType.delete.publicationsNotEmpty"));
        }
    }

    public void delete() {
        super.setRoot(null);
        super.deleteDomainObject();
    }

    public static Optional<ResearchPublicationType> findByCode(String code) {
        if (Strings.isNullOrEmpty(code)) {
            return Optional.empty();
        }
        return Bennu.getInstance().getResearchPublicationTypesSet().stream().filter(c -> code.equals(c.getCode())).findAny();
    }

}
