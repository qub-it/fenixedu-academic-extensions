package org.fenixedu.academicextensions.domain.person.dataShare;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.qubit.terra.framework.services.accessControl.Permission;
import com.qubit.terra.framework.services.accessControl.Profile;
import com.qubit.terra.framework.services.accessControl.ProfileBuilder;

import pt.ist.fenixframework.Atomic;

public class DataShareAuthorizationType extends DataShareAuthorizationType_Base {

    protected static final String PERSON_DATA_AUTHORIZATIONS = "PERSON_DATA_AUTHORIZATIONS";

    protected DataShareAuthorizationType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public void delete() {
        setRoot(null);
        setDataShareAuthorizationTypeParent(null);

        for (; !getDataShareAuthorizationTypeChildrenSet().isEmpty(); getDataShareAuthorizationTypeChildrenSet().iterator().next()
                .delete());

        findIdentifierAccessControlProfile().forEach(p -> {
            p.getParents().forEach(pp -> pp.removeChildProfile(p));
            p.delete();
        });

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.deleteDomainObject();
    }

    public boolean isDeletable() {
        return getAuthorizationSet().isEmpty()
                && !getDataShareAuthorizationTypeChildrenSet().stream().filter(x -> !x.isDeletable()).findAny().isPresent();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        if (!getAuthorizationSet().isEmpty()) {
            blockers.add(AcademicExtensionsUtil.bundle("error.DataShareAuthorizationType.has.Authorizations"));
        }
    }

    protected void init(final String code, final LocalizedString name, final boolean active, final LocalizedString question) {

        setCode(code);
        setName(name);
        setActive(active);
        setQuestion(question);

        checkRules();
    }

    public void checkRules() {

        if (StringUtils.isBlank(getCode())) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.code.required");
        }

        if (getName() == null || getName().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.name.required");
        }

        if (getQuestion() == null || getQuestion().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.question.required");
        }

        findUnique(getCode());
    }

    @Atomic
    public static DataShareAuthorizationType create(final String code, final LocalizedString name, final boolean active,
            final LocalizedString question) {
        final DataShareAuthorizationType result = new DataShareAuthorizationType();
        result.init(code, name, active, question);
        result.initAccessControlProfile();
        return result;
    }

    @Atomic
    public DataShareAuthorizationType edit(final String code, final LocalizedString name, final boolean active,
            final LocalizedString question) {

        this.init(code, name, active, question);
        return this;
    }

    static public Set<DataShareAuthorizationType> find(final String code, final String name, final String groupExpression,
            final boolean active, final String question) {

        final Stream<DataShareAuthorizationType> universe = Bennu.getInstance().getDataShareAuthorizationTypeSet().stream();
        return universe

                .filter(i -> i.isActive() == active)

                .filter(i -> StringUtils.isBlank(code) || StringUtils.equalsIgnoreCase(i.getCode(), code))

                .filter(i -> StringUtils.isBlank(name) || i.getName().anyMatch(c -> c.contains(name)))

                .filter(i -> StringUtils.isBlank(question) || i.getQuestion().anyMatch(c -> c.contains(question)))

                .collect(Collectors.toSet());
    }

    static public DataShareAuthorizationType findUnique(final String code) {
        final Set<DataShareAuthorizationType> found = find(code, (String) null, (String) null, true, (String) null);
        if (found.size() > 1) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.duplicated");
        }

        return found.size() == 1 ? found.iterator().next() : null;
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        getDataShareAuthorizationTypeChildrenSet().forEach(c -> c.setActive(active));
    }

    @Override
    public void setDataShareAuthorizationTypeParent(DataShareAuthorizationType dataShareAuthorizationTypeParent) {
        super.setDataShareAuthorizationTypeParent(dataShareAuthorizationTypeParent);
        if (dataShareAuthorizationTypeParent != null) {
            inheritProfilesFromParent(dataShareAuthorizationTypeParent);
        }
    }

    public boolean isActive() {
        return super.getActive();
    }

    public boolean isRoot() {
        return getDataShareAuthorizationTypeParent() == null;
    }

    private void initAccessControlProfile() {
        String profileCode = getCode() + ":" + UUID.randomUUID();
        com.qubit.terra.framework.tools.primitives.LocalizedString profileName =
                new com.qubit.terra.framework.tools.primitives.LocalizedString().with(getCode() + " - " + getName().getContent());

        ProfileBuilder profileBuilder = Profile.builder().code(profileCode).name(profileName)
                .permissions(Permission.findPermissionByCode(PERSON_DATA_AUTHORIZATIONS).orElse(null)).autoGenerated(true)
                .associate(DataShareAuthorizationType.class);

        Profile autoGeneratedProfile = profileBuilder.build();
        autoGeneratedProfile.addObject(this);
    }

    public Stream<Profile> findIdentifierAccessControlProfile() {
        return Permission.findPermissionByCode(PERSON_DATA_AUTHORIZATIONS).stream().flatMap(p -> p.findProfilesContaining(this))
                .filter(Profile::isAutoGenerated);
    }

    private void inheritProfilesFromParent(DataShareAuthorizationType dataShareAuthorizationTypeParent) {
        Set<Profile> thisProfiles = findIdentifierAccessControlProfile().collect(Collectors.toSet());
        dataShareAuthorizationTypeParent.findIdentifierAccessControlProfile().flatMap(p -> p.getParents().stream())
                .forEach(pp -> thisProfiles.forEach(pp::addChildProfile));
    }
}
