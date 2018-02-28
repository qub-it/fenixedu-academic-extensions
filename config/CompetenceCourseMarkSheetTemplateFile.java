package org.fenixedu.academic.domain.evaluation.config;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;

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
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheetTemplateFile.markSheetSettings.required");
        }

        if (getFilename() == null) {
            throw new AcademicExtensionsDomainException("error.temporaryFile.filename.required");
        }

    }

    @Atomic
    public void delete() {
        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

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
