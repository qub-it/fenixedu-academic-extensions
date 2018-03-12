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

public class DataShareAuthorizationType extends DataShareAuthorizationType_Base {

    protected DataShareAuthorizationType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public void delete() {
        setRoot(null);

        getChoiceSet().clear();

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        if (!getAuthorizationSet().isEmpty()) {
            blockers.add(AcademicExtensionsUtil.bundle("error.DataShareAuthorizationType.has.Authorizations"));
        }
    }

    protected void init(final String code, final LocalizedString name, final String groupExpression, final boolean active,
            final LocalizedString question) {

        setCode(code);
        setName(name);
        setGroupExpression(groupExpression);
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

        if (StringUtils.isBlank(getGroupExpression())) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.groupExpression.required");
        }

        if (getQuestion() == null || getQuestion().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorizationType.question.required");
        }

        findUnique(getCode());
    }

    @Atomic
    static public DataShareAuthorizationType create(final String code, final LocalizedString name, final String groupExpression,
            final boolean active, final LocalizedString question) {
        final DataShareAuthorizationType result = new DataShareAuthorizationType();
        result.init(code, name, groupExpression, active, question);
        return result;
    }

    @Atomic
    public DataShareAuthorizationType edit(final String code, final LocalizedString name, final String groupExpression,
            final boolean active, final LocalizedString question) {

        this.init(code, name, groupExpression, active, question);
        return this;
    }

    static public Set<DataShareAuthorizationType> find(final String code, final String name, final String groupExpression,
            final boolean active, final String question) {

        final Stream<DataShareAuthorizationType> universe = Bennu.getInstance().getDataShareAuthorizationTypeSet().stream();
        return universe

                .filter(i -> StringUtils.isBlank(code) || StringUtils.equalsIgnoreCase(i.getCode(), code))

                .filter(i -> StringUtils.isBlank(name) || i.getName().anyMatch(c -> c.contains(name)))

                .filter(i -> StringUtils.isBlank(groupExpression) || i.getGroupExpression().contains(groupExpression))

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
    
    public boolean isActive() {
        return super.getActive();
    }

}
