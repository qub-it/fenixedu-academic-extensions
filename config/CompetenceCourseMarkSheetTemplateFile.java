package org.fenixedu.academic.domain.evaluation.config;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;

import pt.ist.fenixframework.Atomic;

public class CompetenceCourseMarkSheetTemplateFile extends CompetenceCourseMarkSheetTemplateFile_Base {

    protected CompetenceCourseMarkSheetTemplateFile() {
        super();
    }

    protected void init(final String filename, final byte[] content, final MarkSheetSettings settings) {
        super.init(filename, filename, content);
        super.setMarkSheetSettings(settings);

        checkRules();
    }

    private void checkRules() {

        if (getMarkSheetSettings() == null) {
            throw new ULisboaSpecificationsDomainException(
                    "error.CompetenceCourseMarkSheetTemplateFile.markSheetSettings.required");
        }

        if (getFilename() == null) {
            throw new ULisboaSpecificationsDomainException("error.ULisboaSpecificationsTemporaryFile.filename.required");
        }

    }

    @Atomic
    public void delete() {
        ULisboaSpecificationsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        super.setMarkSheetSettings(null);
        super.delete();
    }

    @Override
    public boolean isAccessible(User user) {
        return false;
    }

    static CompetenceCourseMarkSheetTemplateFile create(final String filename, final byte[] content,
            final MarkSheetSettings settings) {
        final CompetenceCourseMarkSheetTemplateFile result = new CompetenceCourseMarkSheetTemplateFile();
        result.init(filename, content, settings);

        return result;
    }

}
