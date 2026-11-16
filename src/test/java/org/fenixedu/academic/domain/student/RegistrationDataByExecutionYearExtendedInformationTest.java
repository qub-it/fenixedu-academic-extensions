package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationDataByExecutionYearExtendedInformationTest {

    private static ExecutionYear executionYear;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            RegistrationTest.init();

            executionYear = ExecutionYear.findCurrent(null);

            return null;
        });
    }

    @Test
    public void testCurricularYear_settersAreSynced() {
        Registration registration = RegistrationTest.createFreshRegistration();
        RegistrationDataByExecutionYear data = RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear);
        RegistrationDataByExecutionYearExtendedInformation extendedInformation = RegistrationDataByExecutionYearExtendedInformation.findOrCreate(data);

        assertNull(data.getCurricularYear());
        assertNull(extendedInformation.getCurricularYear());

        // Call setter in RegistrationDataByExecutionYearExtendedInformation to trigger the sync in RegistrationDataByExecutionYear
        extendedInformation.setCurricularYear(2);

        // Assert both fields have the same value
        assertEquals(Integer.valueOf(2), data.getCurricularYear());
        assertEquals(Integer.valueOf(2), extendedInformation.getCurricularYear());

        // Call setter in RegistrationDataByExecutionYear to trigger the sync in RegistrationDataByExecutionYearExtendedInformation
        data.setCurricularYear(3);

        // After migration, extendedInformation.getCurricularYear reads new field from RegistrationDataByExecutionYear
        assertEquals(Integer.valueOf(3), data.getCurricularYear());
        assertEquals(Integer.valueOf(3), extendedInformation.getCurricularYear());

        extendedInformation.setCurricularYear(null);

        assertNull(data.getCurricularYear());
        assertNull(extendedInformation.getCurricularYear());

        // Set value via RegistrationDataServices
        Authenticate.mock(registration.getPerson().getUser(), "admin");
        RegistrationDataServices.setCurricularYear(data, 1);

        assertEquals(Integer.valueOf(1), data.getCurricularYear());
        assertEquals(Integer.valueOf(1), extendedInformation.getCurricularYear());

        Authenticate.unmock();
    }
}
