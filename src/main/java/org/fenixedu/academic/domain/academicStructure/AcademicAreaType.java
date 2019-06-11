package org.fenixedu.academic.domain.academicStructure;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * 
 * @author shezad
 *
 */
public class AcademicAreaType extends AcademicAreaType_Base {

    public AcademicAreaType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    public void setName(LocalizedString name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("error.AcademicAreaType.name.cannotBeEmpty");
        }
        super.setName(name);
    }

    @Override
    public void setCode(String code) {
        if (StringUtils.isNotBlank(code)
                && Bennu.getInstance().getAcademicAreaTypesSet().stream().anyMatch(t -> code.equals(t.getCode()) && t != this)) {
            throw new IllegalArgumentException("error.AcademicAreaType.code.alreadyExists");
        }
        super.setCode(code);
    }

    public static AcademicAreaType findByCode(final String code) {
        return StringUtils.isNotBlank(code) ? Bennu.getInstance().getAcademicAreaTypesSet().stream()
                .filter(t -> code.equals(t.getCode())).findAny().orElse(null) : null;
    }

    public void delete() {
        super.setRoot(null);
        getAreasSet().forEach(a -> a.delete());
        super.deleteDomainObject();
    }
}
