package org.fenixedu.academic.dto.student;

import java.io.Serializable;
import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer.CurricularYearResult;
import org.fenixedu.academic.domain.student.curriculum.CurriculumModuleServices;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateTypeEnum;
import org.fenixedu.academic.domain.studentCurriculum.RootCurriculumGroup;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

@SuppressWarnings("serial")
public class RegistrationDataBean implements Serializable {

    private RegistrationDataByExecutionYear data;

    private LocalDate enrolmentDate;

    private StudentCurricularPlan studentCurricularPlan;

    private SchoolClass schoolClass;

    private Boolean notApproved;

    private Integer enrolmentsCount;

    private Double creditsConcluded;

    private BigDecimal enroledEcts;

    private RegistrationStateTypeEnum lastRegistrationStateType;

    private YearMonthDay lastAcademicActDate;

    public RegistrationDataBean(final RegistrationDataByExecutionYear data) {
        this.setData(data);
    }

    public RegistrationDataByExecutionYear getData() {
        return data;
    }

    public void setData(RegistrationDataByExecutionYear data) {
        this.data = data;
    }

    public String getExternalId() {
        return getData() == null ? null : getData().getExternalId();
    }

    public Registration getRegistration() {
        return getData() == null ? null : getData().getRegistration();
    }

    public ExecutionYear getExecutionYear() {
        return getData() == null ? null : getData().getExecutionYear();
    }

    public LocalDate getEnrolmentDate() {
        if (this.enrolmentDate == null) {
            this.enrolmentDate = getData() == null ? null : getData().getEnrolmentDate();
        }

        return this.enrolmentDate;
    }

    public void setEnrolmentDate(LocalDate enrolmentDate) {
        this.enrolmentDate = enrolmentDate;
    }

    public boolean isReingression() {
        return getData() == null ? false : getData().getReingression();
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        if (this.studentCurricularPlan == null) {
            final Registration registration = getRegistration();
            final StudentCurricularPlan plan =
                    registration == null ? null : registration.getStudentCurricularPlan(getExecutionYear());

            // better to return last than have a NPE
            this.studentCurricularPlan = plan == null ? registration.getLastStudentCurricularPlan() : plan;
        }

        return this.studentCurricularPlan;
    }

    private SchoolClass getSchoolClass() {
        if (this.schoolClass == null) {
            final Registration registration = getRegistration();
            final ExecutionInterval executionInterval = getExecutionYear().isCurrent() ? ExecutionInterval
                    .findFirstCurrentChild(registration.getDegree().getCalendar()) : getExecutionYear().getFirstExecutionPeriod();

            this.schoolClass = RegistrationServices.getSchoolClassBy(registration, executionInterval).orElse(null);
        }

        return this.schoolClass;
    }

    public String getSchoolClassPresentation() {
        final String result = "";

        SchoolClass schoolClass = getSchoolClass();
        if (schoolClass != null && schoolClass.getEditablePartOfName() != null) {
            return schoolClass.getEditablePartOfName().toString();
        }

        return result;
    }

    public CurricularYearResult getCurricularYearResult() {
        return RegistrationServices.getCurricularYear(getData().getRegistration(), getData().getExecutionYear());
    }

    public String getCurricularYearPresentation() {
        return String.valueOf(getCurricularYearResult().getResult());
    }

    public String getCurricularYearJustificationPresentation() {
        return getCurricularYearResult().getJustificationPresentation();
    }

    public Boolean getNotApproved() {
        if (this.notApproved == null) {
            final Registration registration = getRegistration();
            final ExecutionYear executionYear = getExecutionYear();
            this.notApproved = RegistrationServices.isFlunkedUsingCurricularYear(registration, executionYear);
        }

        return this.notApproved;
    }

    public Integer getEnrolmentsCount() {
        if (this.enrolmentsCount == null) {
            final StudentCurricularPlan plan = getStudentCurricularPlan();
            this.enrolmentsCount = plan == null ? null : Long
                    .valueOf(plan.getEnrolmentsByExecutionYear(getExecutionYear()).stream().filter(i -> !i.isAnnulled()).count())
                    .intValue();
        }

        return this.enrolmentsCount;
    }

    /**
     * @deprecated This method is misleading, it should be named getApprovedEnrolmentsCredits
     */
    @Deprecated(forRemoval = true)
    public Double getCreditsConcluded() {
        if (this.creditsConcluded == null) {
            final BigDecimal approvedEnrolments = getStudentCurricularPlan().getEnrolmentsByExecutionYear(getExecutionYear())
                    .stream().filter(e -> e.isApproved()).map(e -> e.getEctsCreditsForCurriculum())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.creditsConcluded = approvedEnrolments.doubleValue();
        }

        return this.creditsConcluded;
    }

    public BigDecimal getEnroledEcts() {
        if (this.enroledEcts == null) {
            BigDecimal result = BigDecimal.ZERO;

            final ExecutionYear year = getExecutionYear();

            if (RegistrationServices.isCurriculumAccumulated(getRegistration())) {
                for (final StudentCurricularPlan iter : getRegistration().getStudentCurricularPlansSet()) {
                    final RootCurriculumGroup curriculumGroup = iter.getRoot();
                    result = result.add(CurriculumModuleServices.getEnroledAndNotApprovedEctsCreditsFor(curriculumGroup, year));
                }

            } else {
                final StudentCurricularPlan plan = getStudentCurricularPlan();
                final RootCurriculumGroup curriculumGroup = plan.getRoot();
                result = result.add(CurriculumModuleServices.getEnroledAndNotApprovedEctsCreditsFor(curriculumGroup, year));
            }

            this.enroledEcts = result;
        }

        return this.enroledEcts;
    }

    public String getRegimePresentation() {
        return getRegistration().getRegimeType(getExecutionYear()).getLocalizedName();
    }

    public String getLastRegistrationStatePresentation() {
        if (this.lastRegistrationStateType == null) {
            final RegistrationStateBean bean =
                    RegistrationServices.getLastRegistrationState(getRegistration(), getExecutionYear());
            this.lastRegistrationStateType = bean == null ? null : bean.getStateTypeEnum();
        }

        return this.lastRegistrationStateType == null ? "-" : lastRegistrationStateType.getDescription();
    }

    public YearMonthDay getLastAcademicActDate() {
        if (this.lastAcademicActDate == null) {
            final StudentCurricularPlan plan = getStudentCurricularPlan();
            this.lastAcademicActDate = plan == null ? null : CurriculumModuleServices.calculateLastAcademicActDate(plan.getRoot(),
                    getExecutionYear(), false);
        }

        return this.lastAcademicActDate;
    }

}
