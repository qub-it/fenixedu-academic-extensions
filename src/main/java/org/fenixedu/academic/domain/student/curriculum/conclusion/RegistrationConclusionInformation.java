package org.fenixedu.academic.domain.student.curriculum.conclusion;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.joda.time.LocalDate;

public class RegistrationConclusionInformation {

    private RegistrationConclusionBean registrationConclusionBean;

    public RegistrationConclusionInformation(final RegistrationConclusionBean registrationConclusionBean) {
        this.registrationConclusionBean = registrationConclusionBean;
    }
    
    public RegistrationConclusionBean getRegistrationConclusionBean() {
        return registrationConclusionBean;
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return registrationConclusionBean.getStudentCurricularPlan();
    }

    public ProgramConclusion getProgramConclusion() {
        return registrationConclusionBean.getProgramConclusion();
    }

    public CurriculumGroup getCurriculumGroup() {
        return registrationConclusionBean.getCurriculumGroup();
    }
    
    public LocalDate getConclusionDate() {
        if(registrationConclusionBean.getConclusionDate() == null) {
            return null;
        }
        
        return registrationConclusionBean.getConclusionDate().toLocalDate();
    }

    public ExecutionYear getConclusionYear() {
        if(registrationConclusionBean.isConclusionProcessed()) {
            return registrationConclusionBean.getConclusionYear();
        }
        
        if(registrationConclusionBean.getConclusionYear() != null) {
            return registrationConclusionBean.getConclusionYear();
        }
        
        return ExecutionYear.readByDateTime(registrationConclusionBean.getConclusionDate().toLocalDate());
    }

    public boolean isConcluded() {
        return registrationConclusionBean.isConcluded();
    }

    public boolean isScholarPart() {
        return !getProgramConclusion().isTerminal();
    }

    public boolean isIntegratedMasterFirstCycle() {
        return getStudentCurricularPlan().getDegree().getDegreeType().isIntegratedMasterDegree()
                && getCurriculumGroup().isCycleCurriculumGroup() && ((CycleCurriculumGroup) getCurriculumGroup()).isFirstCycle();
    }

}
