package org.fenixedu.academicextensions.domain.person.dataShare;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

public class DataShareAuthorizationChoice extends DataShareAuthorizationChoice_Base {

    protected DataShareAuthorizationChoice() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public void delete() {
        setRoot(null);

        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        if (!getAuthorizationSet().isEmpty()) {
            blockers.add(AcademicExtensionsUtil.bundle("error.DataShareAuthorizationChoice.has.Authorizations"));
        }

        if (!getTypeSet().isEmpty()) {
            blockers.add(AcademicExtensionsUtil.bundle("error.DataShareAuthorizationChoice.has.Types"));
        }
    }

    protected void init(final String code, final LocalizedString description, final boolean allow,
            final boolean allowComercialUse, final boolean allowProfessionalUse) {

        setCode(code);
        setDescription(description);
        setAllow(allow);
        setAllowComercialUse(allowComercialUse);
        setAllowProfessionalUse(allowProfessionalUse);

        checkRules();
    }

    public void checkRules() {

        if (StringUtils.isBlank(getCode())) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationChoice.code.required");
        }

        if (getDescription() == null || getDescription().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationChoice.description.required");
        }

        findUnique(getCode());
    }

    @Atomic
    static public DataShareAuthorizationChoice create(final String code, final LocalizedString description, final boolean allow,
            final boolean allowComercialUse, final boolean allowProfessionalUse) {

        final DataShareAuthorizationChoice result = new DataShareAuthorizationChoice();
        result.init(code, description, allow, allowComercialUse, allowProfessionalUse);
        return result;
    }

    @Atomic
    public DataShareAuthorizationChoice edit(final String code, final LocalizedString description, final boolean allow,
            final boolean allowComercialUse, final boolean allowProfessionalUse) {

        this.init(code, description, allow, allowComercialUse, allowProfessionalUse);
        return this;
    }

    static public Set<DataShareAuthorizationChoice> find(final String code, final String description) {

        final Stream<DataShareAuthorizationChoice> universe = Bennu.getInstance().getDataShareAuthorizationChoiceSet().stream();
        return universe

                .filter(i -> StringUtils.isBlank(code) || StringUtils.equalsIgnoreCase(i.getCode(), code))

                .filter(i -> StringUtils.isBlank(description) || i.getDescription().anyMatch(c -> c.contains(description)))

                .collect(Collectors.toSet());
    }

    static public DataShareAuthorizationChoice findUnique(final String code) {
        final Set<DataShareAuthorizationChoice> found = find(code, (String) null);
        if (found.size() > 1) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationChoice.duplicated");
        }

        return found.size() == 1 ? found.iterator().next() : null;
    }

    public boolean isAllow() {
        return super.getAllow();
    }

    public boolean isAllowComercialUse() {
        return super.getAllowComercialUse();
    }

    public boolean isAllowProfessionalUse() {
        return super.getAllowProfessionalUse();
    }

    public LocalizedString getFullDescription() {
        LocalizedString result = getDescription();

        if (isAllow() || !isAllowComercialUse() || !isAllowProfessionalUse()) {
            result = result.append(" [");

            if (isAllow()) {
                result.append(AcademicExtensionsUtil.bundleI18N("label.DataShareAuthorizationChoice.allow")).append("; ");
            }

            if (!isAllowComercialUse()) {
                result.append(AcademicExtensionsUtil.bundleI18N("label.DataShareAuthorizationChoice.allowComercialUse"))
                        .append(": ").append(AcademicExtensionsUtil.bundleI18N("label.no")).append("; ");
            }

            if (!isAllowProfessionalUse()) {
                result.append(AcademicExtensionsUtil.bundleI18N("label.DataShareAuthorizationChoice.allowProfessionalUse"))
                        .append(": ").append(AcademicExtensionsUtil.bundleI18N("label.no")).append("; ");
            }

            result = result.append("]");
        }

        return result;
    }

}
