package org.fenixedu.academic.services.evaluation;

import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheet;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetSnapshot;

public interface MarkSheetDocumentPrinterInterface {

    byte[] print(CompetenceCourseMarkSheet markSheet);
    byte[] print(CompetenceCourseMarkSheetSnapshot snapshot);

}
