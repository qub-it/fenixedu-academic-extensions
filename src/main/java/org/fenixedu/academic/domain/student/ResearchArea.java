package org.fenixedu.academic.domain.student;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class ResearchArea extends ResearchArea_Base {

    public static final Comparator<ResearchArea> COMPARATOR_BY_CODE = new Comparator<ResearchArea>() {

        @Override
        public int compare(final ResearchArea o1, final ResearchArea o2) {
            int c = Comparator.<String> naturalOrder().compare(o1.getCode(), o2.getCode());
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected ResearchArea() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected ResearchArea(final String code, final LocalizedString name) {
        this();
        setCode(code);
        setName(name);

        checkRules();
    }

    private void checkRules() {

        if (findAll().stream().anyMatch(r -> r != this && Objects.equals(r.getCode(), getCode()))) {
            throw new AcademicExtensionsDomainException("error.ResearchArea.already.exists.with.same.code");
        }

    }

    public static final ResearchArea create(final String code, final LocalizedString name) {
        return new ResearchArea(code, name);
    }

    public static ResearchArea findByCode(String code) {
        return findAll().stream().filter(fos -> code.equalsIgnoreCase(fos.getCode())).findFirst().orElse(null);
    }

    public static Collection<ResearchArea> findAll() {
        return Bennu.getInstance().getResearchAreasSet();
    }

}
