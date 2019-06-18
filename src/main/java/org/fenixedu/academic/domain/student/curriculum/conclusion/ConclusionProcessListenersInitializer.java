package org.fenixedu.academic.domain.student.curriculum.conclusion;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcessVersion;
import org.fenixedu.academic.domain.student.curriculum.ProgramConclusionProcess;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public abstract class ConclusionProcessListenersInitializer {

    static public void init() {

        ConclusionProcess.getRelationConclusionProcessConclusionProcessVersion()
                .addListener(new RelationAdapter<ConclusionProcessVersion, ConclusionProcess>() {

                    @Override
                    public void beforeAdd(ConclusionProcessVersion o1, ConclusionProcess o2) {

                        if (o1 != null && o2 != null && o2 instanceof ProgramConclusionProcess
                                && RegistrationConclusionServices.hasProcessedProgramConclusionInOtherPlan(
                                        o2.getGroup().getStudentCurricularPlan(),
                                        o2.getGroup().getDegreeModule().getProgramConclusion())) {
                            throw new DomainException(
                                    "error.ConclusionProcess.already.processed.program.conclusion.in.other.plan.of.this.registration");
                        }
                    }

                });

    }

}
