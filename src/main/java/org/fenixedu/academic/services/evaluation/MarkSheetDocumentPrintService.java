package org.fenixedu.academic.services.evaluation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.evaluation.config.CompetenceCourseMarkSheetTemplateFile;
import org.fenixedu.academic.domain.evaluation.config.MarkSheetSettings;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheet;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetSnapshot;
import org.fenixedu.academic.domain.evaluation.markSheet.CompetenceCourseMarkSheetSnapshotEntry;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.qubdocs.util.reports.helpers.DateHelper;
import org.fenixedu.qubdocs.util.reports.helpers.EnumerationHelper;
import org.fenixedu.qubdocs.util.reports.helpers.LanguageHelper;
import org.fenixedu.qubdocs.util.reports.helpers.MoneyHelper;
import org.fenixedu.qubdocs.util.reports.helpers.NumbersHelper;
import org.fenixedu.qubdocs.util.reports.helpers.StringsHelper;
import org.joda.time.DateTime;

import com.qubit.terra.docs.core.DocumentGenerator;
import com.qubit.terra.docs.util.IDocumentFieldsData;
import com.qubit.terra.docs.util.IReportDataProvider;

public class MarkSheetDocumentPrintService {

    public static class CompetenceCourseMarkSheetDataProvider implements IReportDataProvider {

        private static final String MARK_SHEET_KEY = "markSheet";

        private static final String INSTITUTION_NAME_KEY = "institutionName";

        private static final String MARK_SHEET_EVALUATIONS_KEY = "markSheetEvaluations";

        private static final String CURRENT_DATE_TIME_KEY = "currentDateTime";

        private CompetenceCourseMarkSheetSnapshot competenceCourseMarkSheet;

        private List<EvaluationLine> evaluations = new ArrayList<>();

        public static class EvaluationLine {

            private Integer studentNumber;

            private String studentName;

            private String grade;

            private EvaluationLine(Integer studentNumber, String studentName, String grade) {
                super();
                this.studentNumber = studentNumber;
                this.studentName = studentName;
                this.grade = grade;
            }

            public Integer getStudentNumber() {
                return studentNumber;
            }

            public String getStudentName() {
                return studentName;
            }

            public String getGrade() {
                return grade;
            }

            public void setStudentNumber(Integer studentNumber) {
                this.studentNumber = studentNumber;
            }

            public void setStudentName(String studentName) {
                this.studentName = studentName;
            }

            public void setGrade(String grade) {
                this.grade = grade;
            }

        }

        public CompetenceCourseMarkSheetDataProvider(CompetenceCourseMarkSheetSnapshot snapshot) {
            this.competenceCourseMarkSheet = snapshot;

            for (final CompetenceCourseMarkSheetSnapshotEntry entry : snapshot.getSortedEntries()) {
                this.evaluations
                        .add(new EvaluationLine(entry.getStudentNumber(), entry.getStudentName(), entry.getGrade().getValue()));
            }

        }

        @Override
        public boolean handleKey(String key) {
            return key.equals(MARK_SHEET_KEY) || key.equals(INSTITUTION_NAME_KEY) || key.equals(MARK_SHEET_EVALUATIONS_KEY)
                    || key.equals(CURRENT_DATE_TIME_KEY);
        }

        @Override
        public Object valueForKey(String key) {

            if (key.equals(MARK_SHEET_KEY)) {
                return competenceCourseMarkSheet;
            }

            if (key.equals(INSTITUTION_NAME_KEY)) {
                return Bennu.getInstance().getInstitutionUnit().getNameI18n();
            }

            if (key.equals(MARK_SHEET_EVALUATIONS_KEY)) {
                return evaluations;

            }

            if (key.equals(CURRENT_DATE_TIME_KEY)) {
                return new DateTime();
            }

            return null;

        }

        @Override
        public void registerFieldsAndImages(IDocumentFieldsData arg0) {
            arg0.registerCollectionAsField(MARK_SHEET_EVALUATIONS_KEY);
        }

    }

    public static final String PDF = DocumentGenerator.PDF;

    private static void registerHelpers(DocumentGenerator generator) {
        generator.registerHelper("dates", new DateHelper());
        generator.registerHelper("lang", new LanguageHelper());
        generator.registerHelper("numbers", new NumbersHelper());
        generator.registerHelper("enumeration", new EnumerationHelper());
        generator.registerHelper("strings", new StringsHelper());
        generator.registerHelper("money", new MoneyHelper());
    }

    public static byte[] print(CompetenceCourseMarkSheet markSheet) {
        markSheet.markAsPrinted();
        return print(markSheet.getLastSnapshot().get());
    }

    public static byte[] print(CompetenceCourseMarkSheetSnapshot snapshot) {
        final CompetenceCourseMarkSheetTemplateFile templateFile = MarkSheetSettings.getInstance().getTemplateFile();
        final DocumentGenerator generator =
                DocumentGenerator.create(new ByteArrayInputStream(templateFile.getContent()), DocumentGenerator.PDF);

        registerHelpers(generator);
        generator.registerDataProvider(new CompetenceCourseMarkSheetDataProvider(snapshot));

        return generator.generateReportCached(templateFile.getExternalId());
    }

}
