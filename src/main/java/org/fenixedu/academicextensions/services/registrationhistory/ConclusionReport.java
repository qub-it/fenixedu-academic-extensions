package org.fenixedu.academicextensions.services.registrationhistory;

import java.math.BigDecimal;
import java.util.Objects;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.joda.time.LocalDate;

public class ConclusionReport {

    protected static class EmptyConclusionReportEntry extends ConclusionReport {

        protected EmptyConclusionReportEntry() {
            super();
        }

        @Override
        public Boolean getConcluded() {
            return null;
        }

        @Override
        public Boolean getConclusionProcessed() {
            return null;
        }

        @Override
        public Grade getRawGrade() {
            return Grade.createEmptyGrade();
        }

        @Override
        public Grade getFinalGrade() {
            return Grade.createEmptyGrade();
        }

        @Override
        public String getDescriptiveGradeExtendedValue() {
            return null;
        }

        @Override
        public LocalDate getConclusionDate() {
            return null;
        }

        @Override
        public ExecutionYear getConclusionYear() {
            return null;
        }

        @Override
        public BigDecimal getConclusionCredits() {
            return null;
        };

    }

    private static ConclusionReport EMPTY_CONCLUSION = new EmptyConclusionReportEntry();

    private RegistrationConclusionBean conclusionBean;

    protected ConclusionReport() {

    }

    public ConclusionReport(RegistrationConclusionBean bean) {
        this.conclusionBean = Objects.requireNonNull(bean);
    }

    public RegistrationConclusionBean getConclusionBean() {
        return this.conclusionBean;
    }

    public Boolean getConcluded() {
        return this.conclusionBean.isConcluded();
    }

    public Boolean getConclusionProcessed() {
        return this.conclusionBean.isConclusionProcessed();
    }

    public Grade getRawGrade() {
        return this.conclusionBean.getRawGrade();
    }

    public Grade getFinalGrade() {
        return this.conclusionBean.getFinalGrade();
    }

    public String getDescriptiveGradeExtendedValue() {
        return this.conclusionBean.getDescriptiveGradeExtendedValue();
    }

    public LocalDate getConclusionDate() {
        return this.conclusionBean.isConcluded() ? this.conclusionBean.getConclusionDate().toLocalDate() : null;
    }

    public ExecutionYear getConclusionYear() {
        return this.conclusionBean.isConcluded() ? this.conclusionBean.getConclusionYear() : null;
    }

    public BigDecimal getConclusionCredits() {
        return BigDecimal.valueOf(this.conclusionBean.getEctsCredits());
    }

    public static ConclusionReport empty() {
        return EMPTY_CONCLUSION;
    }

}