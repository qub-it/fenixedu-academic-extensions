package org.fenixedu.academic.services.evaluation;

import java.util.Optional;
import java.util.function.Function;

import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheet;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetSnapshot;

public class MarkSheetDocumentPrintService {

    private static MarkSheetDocumentPrinterInterface SINGLETON;

    public static void registerPrinterInterface(MarkSheetDocumentPrinterInterface printer) {
        SINGLETON = printer;
    }

    public static final String PDF = "application/pdf";

    private static <T extends Object> T apply(Function<MarkSheetDocumentPrinterInterface, T> function) {
        return Optional.ofNullable(SINGLETON).map(function).orElseThrow(() -> new RuntimeException("Feature not available"));
    }

    public static byte[] print(CompetenceCourseMarkSheet markSheet) {
        return apply(p -> p.print(markSheet));
    }

    public static byte[] print(CompetenceCourseMarkSheetSnapshot snapshot) {
        return apply(p -> p.print(snapshot));
    }

}
