package org.fenixedu.academic.domain.dml;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainObject;

public class DynamicField extends DynamicField_Base {

    protected DynamicField() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    static public Set<DynamicField> findOrCreateFields(final DomainObject domainObject) {
        final Set<DynamicField> result = new LinkedHashSet<>();

        DynamicFieldDescriptor.find(domainObject).stream().sorted((x, y) -> Integer.compare(x.getOrder(), y.getOrder()))
                .forEach(descriptor -> {

                    DynamicField field = descriptor.findField(domainObject);
                    if (field == null) {
                        field = descriptor.createField(domainObject);
                    }

                    result.add(field);
                });

        return result;
    }

    static public DynamicField findField(final DomainObject domainObject, final String code) {
        return findOrCreateFields(domainObject).stream().filter(i -> StringUtils.equals(i.getCode(), code)).findFirst()
                .orElse(null);
    }

    private String getCode() {
        return getDescriptor().getCode();
    }

    private boolean isRequired() {
        return getDescriptor().getRequired();
    }

    public DynamicField edit(final Object value) {
        return edit(DynamicFieldValueConverter.serialize(getDescriptor().getFieldValueClass(), value));
    }

    @Atomic
    private DynamicField edit(final String value) {
        setValue(value);

        // checkRules
        if (isRequired() && Strings.isNullOrEmpty(getValue())) {
            throw new IllegalArgumentException("error.DynamicField.value.required");
        }

        return this;
    }

    public <T> T getValue(Class<T> type) {
        return (T) DynamicFieldValueConverter.deserialize(type, getValue());
    }

    public void delete() {
        super.setDescriptor(null);
        super.setRoot(null);
        super.deleteDomainObject();
    }

}
