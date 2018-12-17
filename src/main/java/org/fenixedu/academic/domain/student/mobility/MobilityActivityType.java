package org.fenixedu.academic.domain.student.mobility;

import java.util.Set;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class MobilityActivityType extends MobilityActivityType_Base {

    protected MobilityActivityType() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected MobilityActivityType(final String code, final LocalizedString name, final boolean active) {
        this();

        this.setCode(code);
        this.setName(name);
        this.setActive(active);
    }

    @Override
    public void setCode(String code) {
        if (Strings.isNullOrEmpty(code)) {
            throw new AcademicExtensionsDomainException("error.MobilityActivityType.code.required");
        }

        final MobilityActivityType foundByCode = findByCode(code);
        if (foundByCode != null && foundByCode != this) {
            throw new AcademicExtensionsDomainException("error.MobilityActivityType.code.duplicated");
        }

        super.setCode(code);
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || Strings.isNullOrEmpty(name.getContent())) {
            throw new AcademicExtensionsDomainException("error.MobilityActivityType.name.required");
        }
        super.setName(name);
    }

    public boolean isActive() {
        return getActive();
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean active) {
        setCode(code);
        setName(name);
        setActive(active);
    }

    @Atomic
    public static final MobilityActivityType create(final String code, final LocalizedString name, final boolean active) {
        return new MobilityActivityType(code, name, active);
    }

    public static Set<MobilityActivityType> findAll() {
        return Bennu.getInstance().getMobilityActivityTypesSet();
    }

    public static Set<MobilityActivityType> findAllActive() {
        return Sets.filter(findAll(), new Predicate<MobilityActivityType>() {
            public boolean apply(final MobilityActivityType arg) {
                return arg.isActive();
            }
        });
    }

    public static final MobilityActivityType findByCode(final String code) {
        MobilityActivityType result = null;

        Set<MobilityActivityType> readAll = findAll();

        for (final MobilityActivityType mobilityActivityType : readAll) {
            if (code.equals(mobilityActivityType.getCode()) && result != null) {
                throw new AcademicExtensionsDomainException("error.MobilityActivityType.code.duplicated");
            }

            if (code.equals(mobilityActivityType.getCode())) {
                result = mobilityActivityType;
            }
        }

        return result;
    }

    public boolean isDeletable() {
        return getMobilityRegistrationInformationsSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicExtensionsDomainException("error.MobilityActivityType.cannot.delete");
        }

        setBennu(null);
        deleteDomainObject();
    }

}
