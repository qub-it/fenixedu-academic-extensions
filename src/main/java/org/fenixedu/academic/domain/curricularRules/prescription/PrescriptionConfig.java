package org.fenixedu.academic.domain.curricularRules.prescription;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class PrescriptionConfig extends PrescriptionConfig_Base {

    protected PrescriptionConfig() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    protected void init(String name, BigDecimal partialRegimeBonus, boolean reingressionRestartsYearCount,
            ExecutionYear beginExecutionYear) {
        super.setName(name);
        super.setPartialRegimeBonus(partialRegimeBonus);
        super.setReingressionRestartsYearCount(reingressionRestartsYearCount);
        super.setBeginExecutionYear(beginExecutionYear);

        checkRules();

    }

    @Atomic
    public void edit(String name, BigDecimal bonus, boolean reingressionRestartsYearCount, ExecutionYear beginExecutionYear) {
        super.setName(name);
        super.setPartialRegimeBonus(bonus);
        super.setReingressionRestartsYearCount(reingressionRestartsYearCount);
        super.setBeginExecutionYear(beginExecutionYear);

        checkRules();
    }

    @Override
    public void removeDegreeCurricularPlans(DegreeCurricularPlan degreeCurricularPlans) {
        super.removeDegreeCurricularPlans(degreeCurricularPlans);

        checkRules();
    }

    @Override
    public void addDegreeCurricularPlans(DegreeCurricularPlan degreeCurricularPlans) {
        super.addDegreeCurricularPlans(degreeCurricularPlans);

        checkRules();
    }

    protected void checkRules() {

        if (getName() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionConfig.name.cannot.be.null");
        }

        if (getPartialRegimeBonus() == null) {
            throw new AcademicExtensionsDomainException("error.PrescriptionConfig.partialRegimeBonus.cannot.be.null");
        }

        if (PrescriptionConfig.findAll().stream().anyMatch(pc -> pc != this && pc.getName().equals(getName()))) {
            throw new AcademicExtensionsDomainException("error.PrescriptionConfig.already.exists.other.configuration.with.same.name");
        }

        if (PrescriptionConfig.findAll().stream()
                .anyMatch(pc -> pc != this && getDegreeCurricularPlansSet().stream().anyMatch(dcp -> pc.appliesTo(dcp)))) {
            throw new AcademicExtensionsDomainException(
                    "error.PrescriptionConfig.degree.curricular.plan.can.only.belong.to.single.configuration");
        }

    }

    public Collection<ExecutionYear> filterExecutionYears(Registration registration, Collection<ExecutionYear> executionYears) {

        final ExecutionYear lastReingressionYear =
                getReingressionRestartsYearCount() ? RegistrationServices.getLastReingressionYear(registration) : null;
        final ExecutionYear minExecutionYear = getBeginExecutionYear();

        return executionYears.stream()
                .filter(ey -> (minExecutionYear == null || ey.isAfterOrEquals(minExecutionYear))
                        && (lastReingressionYear == null || ey.isAfterOrEquals(lastReingressionYear)))
                .collect(Collectors.toSet());

    }

    public BigDecimal getBonification(Collection<StatuteType> statuteTypes, boolean partialRegime) {
        return getBonificationStatutesSet().stream().filter(b -> statuteTypes.contains(b.getStatuteType())).map(b -> b.getBonus())
                .max((x, y) -> x.compareTo(y)).orElse(BigDecimal.ZERO)
                .max(partialRegime ? getPartialRegimeBonus() : BigDecimal.ZERO);
    }

    public boolean appliesTo(DegreeCurricularPlan degreeCurricularPlan) {
        return getDegreeCurricularPlansSet().contains(degreeCurricularPlan);
    }

    @Atomic
    public void delete() {

        while (!getBonificationStatutesSet().isEmpty()) {
            getBonificationStatutesSet().iterator().next().delete();
        }

        while (!getPrescriptionEntriesSet().isEmpty()) {
            getPrescriptionEntriesSet().iterator().next().delete();
        }

        getDegreeCurricularPlansSet().clear();
        super.setBeginExecutionYear(null);
        super.setBennu(null);
        super.deleteDomainObject();
    }

    @Atomic
    static public PrescriptionConfig create(String name, BigDecimal partialRegimeBonus, boolean reingressionRestartsYearCount,
            ExecutionYear beginExecutionYear) {
        final PrescriptionConfig result = new PrescriptionConfig();
        result.init(name, partialRegimeBonus, reingressionRestartsYearCount, beginExecutionYear);

        return result;
    }

    public static Collection<PrescriptionConfig> findAll() {
        return Bennu.getInstance().getPrescriptionConfigsSet();
    }

    public static PrescriptionConfig findBy(final DegreeCurricularPlan degreeCurricularPlan) {
        return findAll().stream().filter(pc -> pc.appliesTo(degreeCurricularPlan)).findFirst().orElse(null);
    }

}
