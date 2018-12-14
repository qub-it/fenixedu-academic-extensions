package org.fenixedu.academic.domain.student.mobility;

import java.util.Set;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class MobilityProgramType extends MobilityProgramType_Base {

    protected MobilityProgramType() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected MobilityProgramType(final String code, final LocalizedString name, final boolean active) {
        this();

        this.setCode(code);
        this.setName(name);
        this.setActive(active);

        checkRules();
    }

    protected void checkRules() {

        if (Strings.isNullOrEmpty(getCode())) {
            throw new AcademicExtensionsDomainException("error.MobilityProgramType.code.required");
        }

        if (getName() == null || Strings.isNullOrEmpty(getName().getContent())) {
            throw new AcademicExtensionsDomainException("error.MobilityProgramType.name.required");
        }

        findByCode(getCode());

    }

    public boolean isActive() {
        return getActive();
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean active) {
        setCode(code);
        setName(name);
        setActive(active);

        checkRules();
    }

    @Atomic
    public static final MobilityProgramType create(final String code, final LocalizedString name, final boolean active) {
        return new MobilityProgramType(code, name, active);
    }

    public static Set<MobilityProgramType> findAll() {
        return Bennu.getInstance().getMobilityProgramTypesSet();
    }

    public static Set<MobilityProgramType> findAllActive() {
        return Sets.filter(findAll(), new Predicate<MobilityProgramType>() {
            public boolean apply(final MobilityProgramType arg) {
                return arg.isActive();
            }
        });
    }

    public static final MobilityProgramType findByCode(final String code) {
        MobilityProgramType result = null;

        final Set<MobilityProgramType> readAll = findAll();

        for (final MobilityProgramType MobilityProgramType : readAll) {
            if (code.equals(MobilityProgramType.getCode()) && result != null) {
                throw new AcademicExtensionsDomainException("error.MobilityProgramType.code.duplicated");
            }

            if (code.equals(MobilityProgramType.getCode())) {
                result = MobilityProgramType;
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
            throw new AcademicExtensionsDomainException("error.MobilityProgramType.cannot.delete");
        }

        setBennu(null);
        deleteDomainObject();
    }

}
