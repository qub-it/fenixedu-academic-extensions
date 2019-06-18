package org.fenixedu.academic.domain.enrolment;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.LocalDate;

public class StudentCurricularPlanImprovementOfApprovedEnrolmentManager
        extends org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanImprovementOfApprovedEnrolmentManager {

    public StudentCurricularPlanImprovementOfApprovedEnrolmentManager(final EnrolmentContext enrolmentContext) {
        super(enrolmentContext);
    }

    @Override
    protected void assertEnrolmentPreConditions() {
        if (isResponsiblePersonManager()) {
            return;
        }

// qubExtension, removed check - should be replaced somehow?
//        if (!hasRegistrationInValidState()) {
//            throw new DomainException("error.StudentCurricularPlan.cannot.enrol.with.registration.inactive");
//        }

        if (!AcademicAuthorizationGroup.get(AcademicOperationType.STUDENT_ENROLMENTS).isMember(Authenticate.getUser())
                && FenixEduAcademicExtensionsConfiguration.getConfiguration()
                        .getEnrolmentsInEvaluationsDependOnAcademicalActsBlocked()
                && TreasuryBridgeAPIFactory.implementation().isAcademicalActsBlocked(getPerson(), new LocalDate())) {
            throw new DomainException("error.StudentCurricularPlan.cannot.enrol.with.debts.for.previous.execution.years");
        }

// qubExtension, removed check 
//        if (areModifiedCyclesConcluded()) {
//            checkUpdateRegistrationAfterConclusion();
//        }
    }

}
