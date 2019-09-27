package org.fenixedu.academic.domain.dml;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainObject;

@SuppressWarnings("rawtypes")
public class DynamicFieldDescriptor extends DynamicFieldDescriptor_Base {

    static final private String DOMAIN_OBJECT_FIELD_NAME = "DynamicField";
    static final private String DOMAIN_OBJECT_METHOD_NAME_ADD = "add" + DOMAIN_OBJECT_FIELD_NAME;
    static final private String DOMAIN_OBJECT_METHOD_NAME_GET = "get" + DOMAIN_OBJECT_FIELD_NAME + "Set";

    protected DynamicFieldDescriptor() {
        super();
        setRoot(Bennu.getInstance());
    }

    protected void init(final Class<? extends DomainObject> domainObjectClass, final String code, final LocalizedString name,
            final Class fieldValueClass, final boolean required, final int order) {

        setDomainObjectClassName(domainObjectClass == null ? null : domainObjectClass.getName());
        setCode(code);
        setName(name);
        setFieldValueClassName(fieldValueClass == null ? null : fieldValueClass.getName());
        setRequired(required);
        updateOrder(order);

        checkRules(fieldValueClass);
    }

    private void checkRules(final Class fieldValueClass) {

        if (Strings.isNullOrEmpty(getDomainObjectClassName())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.domainObjectClassName.required");
        }

        if (Strings.isNullOrEmpty(getCode())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.code.required");
        }

        final Class<? extends DomainObject> domainObjectClass = getDomainObjectClass();
        if (domainObjectClass == null) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.domainObjectClass.required");
        }
        find(domainObjectClass, getCode());

        if (getName() == null || getName().isEmpty()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.name.required");
        }

        if (Strings.isNullOrEmpty(getFieldValueClassName())) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.fieldValueClassName.required");
        }

        if (!DynamicFieldValueConverter.isSupported(fieldValueClass)) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.fieldValueClass.unsupported");
        }

        if (getRequired() && getFieldsSet().stream().anyMatch(i -> Strings.isNullOrEmpty(i.getValue()))) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.value.inconsistent");
        }
    }

    @Atomic
    static public DynamicFieldDescriptor create(final Class<? extends DomainObject> domainObjectClass, final String code,
            final LocalizedString name, final Class fieldValueClass, final boolean required, final int order) {

        final DynamicFieldDescriptor result = new DynamicFieldDescriptor();
        result.init(domainObjectClass, code, name, fieldValueClass, required, order);
        return result;
    }

    @Atomic
    public DynamicFieldDescriptor edit(final Class<? extends DomainObject> domainObjectClass, final String code,
            final LocalizedString name, final Class fieldValueClass, final boolean required, final int order) {

        this.init(domainObjectClass, code, name, fieldValueClass, required, order);
        return this;
    }

    static public DynamicFieldDescriptor find(final Class<? extends DomainObject> domainObjectClass, final String code) {
        DynamicFieldDescriptor result = null;

        if (domainObjectClass != null && !Strings.isNullOrEmpty(code)) {

            for (final DynamicFieldDescriptor iter : Bennu.getInstance().getDynamicFieldDescriptorSet()) {
                if (iter.getDomainObjectClass() == domainObjectClass && StringUtils.equalsIgnoreCase(iter.getCode(), code)) {

                    if (result != null) {
                        throw new IllegalArgumentException("error.DynamicFieldDescriptor.duplicate");
                    }

                    result = iter;
                }
            }
        }

        return result;
    }

    static public Set<DynamicFieldDescriptor> find(final DomainObject domainObject) {
        final Set<DynamicFieldDescriptor> result = new HashSet<>();

        if (domainObject != null) {

            for (final DynamicFieldDescriptor iter : Bennu.getInstance().getDynamicFieldDescriptorSet()) {
                if (iter.isFor(domainObject)) {
                    result.add(iter);
                }
            }
        }

        return result;
    }

    private boolean isFor(final DomainObject domainObject) {
        return domainObject != null && getDomainObjectClass() == domainObject.getClass();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends DomainObject> getDomainObjectClass() {
        Class<?> result = null;

        try {
            final Class<?> domainClass = Class.forName(getDomainObjectClassName());
            result = DomainObject.class.isAssignableFrom(domainClass) ? domainClass : null;
        } catch (final Throwable t) {
        }

        return (Class<? extends DomainObject>) result;
    }

    protected DynamicField createField(final DomainObject domainObject) {
        final DynamicField result = new DynamicField();

        result.setDescriptor(this);
        setField(domainObject, result);

        // checkRules
        findField(domainObject);

        return result;
    }

    @SuppressWarnings("unchecked")
    protected DynamicField findField(final DomainObject domainObject) {
        DynamicField result = null;

        if (isFor(domainObject)) {

            try {
                final Method method = domainObject.getClass().getMethod(DOMAIN_OBJECT_METHOD_NAME_GET);
                final Set<DynamicField> fields = (Set<DynamicField>) method.invoke(domainObject);

                for (final DynamicField iter : fields) {
                    if (iter.getDescriptor() == this) {

                        if (result != null) {
                            throw new IllegalArgumentException("error.DynamicField.duplicate");
                        }

                        result = iter;
                    }
                }

            } catch (final Throwable t) {
            }
        }

        return result;
    }

    protected void setField(final DomainObject domainObject, final DynamicField field) {
        if (isFor(domainObject) && getFieldsSet().contains(field)) {

            try {
                final Method method = domainObject.getClass().getMethod(DOMAIN_OBJECT_METHOD_NAME_ADD, DynamicField.class);
                method.invoke(domainObject, field);
            } catch (final Throwable t) {
            }
        }
    }

    private void updateOrder(final int order) {
        // TODO legidio
        setOrder(order);
    }

    public Class getFieldValueClass() {
        try {
            return Class.forName(getFieldValueClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        if (!getFieldsSet().isEmpty()) {
            throw new IllegalArgumentException("error.DynamicFieldDescriptor.cannot.delete.with.field.instances");
        }

        super.setRoot(null);

        super.deleteDomainObject();
    }

}
