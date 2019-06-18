package org.fenixedu.academic.domain.student;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.FenixFramework;

//TODO: Merge with registration data by execution interval
@Deprecated
public class RegistrationDataByExecutionYearExtendedInformation extends RegistrationDataByExecutionYearExtendedInformation_Base {

    protected RegistrationDataByExecutionYearExtendedInformation() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    protected void init(final RegistrationDataByExecutionYear dataByExecutionYear) {
        super.setDataByExecutionYear(dataByExecutionYear);
        checkRules();

    }

    private void checkRules() {

        if (getDataByExecutionYear() == null) {
            throw new AcademicExtensionsDomainException(
                    "error.RegistrationDataByExecutionYearExtendedInformation.dataByExecutionYear.cannot.be.null");
        }

    }

    static public void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(RegistrationDataByExecutionYear.class, x -> {
            if (x.getExtendedInformation() != null) {
                x.getExtendedInformation().delete();
            }
        });
    }

    private void delete() {

        super.setDataByExecutionYear(null);
        super.setBennu(null);
        super.deleteDomainObject();

    }

    static public RegistrationDataByExecutionYearExtendedInformation findOrCreate(
            RegistrationDataByExecutionYear dataByExecutionYear) {
        return dataByExecutionYear.getExtendedInformation() != null ? dataByExecutionYear
                .getExtendedInformation() : create(dataByExecutionYear);
    }

    static public RegistrationDataByExecutionYearExtendedInformation create(RegistrationDataByExecutionYear dataByExecutionYear) {
        final RegistrationDataByExecutionYearExtendedInformation result =
                new RegistrationDataByExecutionYearExtendedInformation();
        result.init(dataByExecutionYear);

        return result;
    }

}
