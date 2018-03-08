package org.fenixedu.academicextensions.util;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class AcademicExtensionsUtil {

    public static final String BUNDLE = "resources.FenixeduAcademicExtensionsResources";

    public static String bundle(final String key, final String... args) {
        return BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, key, args);
    }

    public static LocalizedString bundleI18N(final String key, final String... args) {
        return BundleUtil.getLocalizedString(AcademicExtensionsUtil.BUNDLE, key, args);
    }

}
