package org.fenixedu.academic.domain.curricularRules.prescription;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class PrescriptionBonificationStatute extends PrescriptionBonificationStatute_Base {

    protected PrescriptionBonificationStatute() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    protected void init(PrescriptionConfig configuration, StatuteType statuteType, BigDecimal bonus) {
        super.setConfiguration(configuration);
        super.setStatuteType(statuteType);
        super.setBonus(bonus);

        checkRules();
    }

    private void checkRules() {

        if (getConfiguration() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionBonificationStatute.configuration.cannot.be.null");
        }

        if (getStatuteType() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionBonificationStatute.statuteType.cannot.be.null");
        }

        if (getBonus() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionBonificationStatute.bonus.cannot.be.null");
        }

        if (getConfiguration().getBonificationStatutesSet().stream()
                .anyMatch(b -> b != this && b.getStatuteType() == getStatuteType())) {
            throw new AcademicExtensionsDomainException("error.PrescriptionBonificationStatute.configuration.already.has.statute.type");
        }
    }

    @Atomic
    public void edit(BigDecimal bonus) {
        super.setBonus(bonus);
        checkRules();
    }

    @Atomic
    public void delete() {
        super.setConfiguration(null);
        super.setStatuteType(null);
        super.setBennu(null);
        super.deleteDomainObject();
    }

    @Atomic
    static public PrescriptionBonificationStatute create(PrescriptionConfig configuration, StatuteType statuteType,
            BigDecimal bonus) {
        final PrescriptionBonificationStatute result = new PrescriptionBonificationStatute();
        result.init(configuration, statuteType, bonus);

        return result;
    }

}
