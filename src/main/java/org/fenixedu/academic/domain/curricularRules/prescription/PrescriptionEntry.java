package org.fenixedu.academic.domain.curricularRules.prescription;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;

import pt.ist.fenixframework.Atomic;

public class PrescriptionEntry extends PrescriptionEntry_Base {

    public PrescriptionEntry() {
        super();
    }

    protected void init(BigDecimal enrolmentYears, BigDecimal minEctsApproved, PrescriptionConfig prescriptionConfig) {
        super.setEnrolmentYears(enrolmentYears);
        super.setMinEctsApproved(minEctsApproved);
        super.setPrescriptionConfig(prescriptionConfig);

        checkRules();

    }

    @Atomic
    public void edit(BigDecimal enrolmentYears, BigDecimal minEctsApproved) {
        super.setEnrolmentYears(enrolmentYears);
        super.setMinEctsApproved(minEctsApproved);

        checkRules();
    }

    private void checkRules() {

        if (getEnrolmentYears() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionEntry.enrolmentYears.cannot.be.null");
        }

        if (getMinEctsApproved() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionEntry.minEctsApproved.cannot.be.null");
        }


        if (getPrescriptionConfig().getPrescriptionEntriesSet().stream()
                .anyMatch(b -> b != this && b.getEnrolmentYears().intValue() == getEnrolmentYears().intValue())) {
            throw new AcademicExtensionsDomainException("error.PrescriptionEntry.enrolmentYears.already.exists");
        }
    }
    
    @Atomic
    static public PrescriptionEntry create(BigDecimal enrolmentYears, BigDecimal minEctsApproved,
            PrescriptionConfig prescriptionConfig) {
        final PrescriptionEntry result = new PrescriptionEntry();
        result.init(enrolmentYears, minEctsApproved, prescriptionConfig);

        return result;
    }

    @Atomic
    public void delete() {
        setPrescriptionConfig(null);
        super.deleteDomainObject();
    }

}
