package org.fenixedu.academic.domain.curriculum.grade;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

public class GradeScaleEntry extends GradeScaleEntry_Base {

    protected GradeScaleEntry() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    private void checkRules() {
        if (getGradeScale() == null) {
            throw new AcademicExtensionsDomainException("error.GradeScaleEntry.gradeScale.is.required");
        }

        if (StringUtils.isBlank(getValue())) {
            throw new AcademicExtensionsDomainException("error.GradeScaleEntry.value.is.required");
        }

        if (getDescription() == null || getDescription().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.GradeScaleEntry.description.is.required");
        }
    }

    public void edit(GradeScale gradeScale, String value, LocalizedString description, boolean allowsApproval) {
        super.setGradeScale(gradeScale);
        super.setValue(value);
        super.setDescription(description);
        super.setAllowsApproval(allowsApproval);

        checkRules();

    }

    static public GradeScaleEntry create(final GradeScale gradeScale, final String value, final LocalizedString description,
            final boolean allowsApproval) {
        final GradeScaleEntry result = new GradeScaleEntry();
        result.edit(gradeScale, value, description, allowsApproval);

        return result;
    }

    @Atomic
    public void delete() {
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public static Collection<GradeScaleEntry> findAll() {
        return Bennu.getInstance().getGradeScaleEntriesSet();
    }

    public static Collection<GradeScaleEntry> findBy(GradeScale gradeScale) {
        return findAll().stream().filter(e -> e.getGradeScale() == gradeScale).collect(Collectors.toSet());
    }

}