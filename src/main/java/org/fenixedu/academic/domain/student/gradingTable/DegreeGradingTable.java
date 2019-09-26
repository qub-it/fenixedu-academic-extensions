package org.fenixedu.academic.domain.student.gradingTable;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionInformation;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.CallableWithoutException;

public class DegreeGradingTable extends DegreeGradingTable_Base {

    // TODO: Remove this workaround.
    // When generating tables with more than one curricular plan of the same
    // degree, it will generate more than one table for the same degree.
    public static class DataTuple {
        private Degree degree;
        private ExecutionYear executionYear;
        private ProgramConclusion programConclusion;

        public DataTuple(final Degree degree, final ExecutionYear executionYear, final ProgramConclusion programConclusion) {
            this.setDegree(degree);
            this.setExecutionYear(executionYear);
            this.setProgramConclusion(programConclusion);
        }

        public Degree getDegree() {
            return degree;
        }

        public void setDegree(final Degree degree) {
            this.degree = degree;
        }

        public ExecutionYear getExecutionYear() {
            return executionYear;
        }

        public void setExecutionYear(final ExecutionYear executionYear) {
            this.executionYear = executionYear;
        }

        public ProgramConclusion getProgramConclusion() {
            return programConclusion;
        }

        public void setProgramConclusion(final ProgramConclusion programConclusion) {
            this.programConclusion = programConclusion;
        }

    }

    public DegreeGradingTable() {
        super();
    }

    @Override
    public void delete() {
        setDegree(null);
        setProgramConclusion(null);
        setRegistration(null);
        super.delete();
    }

    public static Stream<DegreeGradingTable> findAll() {
        return Bennu.getInstance().getGradingTablesSet().stream().filter(DegreeGradingTable.class::isInstance)
                .map(DegreeGradingTable.class::cast);
    }

    public static Set<DegreeGradingTable> find(final ExecutionYear ey) {
        return find(ey, false);
    }

    public static Set<DegreeGradingTable> find(final ExecutionYear ey, final boolean includeLegacy) {
        return ey.getGradingTablesSet().stream().filter(DegreeGradingTable.class::isInstance).map(DegreeGradingTable.class::cast)
                .filter(dgt -> (includeLegacy || dgt.getRegistration() == null)).collect(Collectors.toSet());
    }

    public static DegreeGradingTable find(final ExecutionYear ey, final ProgramConclusion pc, final Degree d) {
        return d.getDegreeGradingTablesSet().stream().filter(dgt -> (dgt.getRegistration() == null))
                .filter(dgt -> dgt.getProgramConclusion() == pc).filter(dgt -> dgt.getExecutionYear() == ey).findAny()
                .orElse(null);
    }

    public static DegreeGradingTable find(final ExecutionYear ey, final ProgramConclusion pc, final Registration reg) {
        return reg.getDegreeGradingTablesSet().stream().filter(dgt -> dgt.getExecutionYear() == ey)
                .filter(dgt -> dgt.getProgramConclusion() == pc).findFirst().orElse(find(ey, pc, reg.getDegree()));
    }

    public static String getEctsGrade(final RegistrationConclusionBean registrationConclusionBean) {
        if (registrationConclusionBean != null && registrationConclusionBean.getFinalGrade() != null
                && registrationConclusionBean.getFinalGrade().getValue() != null) {
            DegreeGradingTable table = DegreeGradingTable.find(registrationConclusionBean.getConclusionYear(),
                    registrationConclusionBean.getProgramConclusion(), registrationConclusionBean.getRegistration());
            if (table != null) {
                return table.getEctsGrade(registrationConclusionBean.getFinalGrade().getValue());
            }
        }
        return "-";
    }

    public static Set<DegreeGradingTable> generate(final ExecutionYear executionYear) {
        Set<DegreeGradingTable> allTables = new HashSet<>();
        Set<DataTuple> allTablesMetaData = new HashSet<>();
        final Set<DegreeCurricularPlan> dcps = executionYear.getExecutionDegreesSet().stream()
                .map(ed -> ed.getDegreeCurricularPlan()).collect(Collectors.toSet());
        for (DegreeCurricularPlan dcp : dcps) {
            Degree degree = dcp.getDegree();

            if (!GradingTableSettings.getApplicableDegreeTypes().contains(degree.getDegreeType())) {
                continue;
            }

            programConclusionLoop: for (ProgramConclusion programConclusion : ProgramConclusion.conclusionsFor(dcp)
                    .collect(Collectors.toSet())) {
                DegreeGradingTable table = find(executionYear, programConclusion, degree);
                if (table == null) {
                    for (DataTuple dataTuple : allTablesMetaData) {
                        if (dataTuple.getExecutionYear() == executionYear && dataTuple.getProgramConclusion() == programConclusion
                                && dataTuple.getDegree() == degree) {
                            //This table will be created by a new thread at the end of this atomic transaction
                            continue programConclusionLoop;
                        }
                    }
                }
                if (table == null) {
                    allTablesMetaData.add(new DataTuple(degree, executionYear, programConclusion));
                    CallableWithoutException<DegreeGradingTable> workerLogic =
                            new CallableWithoutException<DegreeGradingTable>() {
                                @Override
                                public DegreeGradingTable call() {
                                    DegreeGradingTable table = new DegreeGradingTable();
                                    table.setExecutionYear(executionYear);
                                    table.setProgramConclusion(programConclusion);
                                    table.setDegree(degree);
                                    table.compileData();
                                    return table;
                                }
                            };
                    GeneratorWorker<DegreeGradingTable> worker = new GeneratorWorker<>(workerLogic);
                    worker.start();
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    allTables.add(worker.getTable());
                } else {
                    allTables.add(table);
                }
            }
        }
        return allTables;
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        List<RegistrationConclusionBean> harvestRegistrationConclusionBeansUsedInSample = harvestRegistrationConclusionBeansUsedInSample();
        
        List<BigDecimal> sample = harvestRegistrationConclusionBeansUsedInSample.stream().map(r -> r.getFinalGrade())
                .filter(e -> isNumeric(e))
                .map(e -> e.getNumericValue().setScale(0, RoundingMode.HALF_UP))
                .collect(Collectors.toList());

        final String harvestRegistrationConclusionBeansUsedInSampleData = harvestRegistrationConclusionBeansUsedInSample.stream()
                .map(r -> conclusionBeanStringData(r))
                .reduce((a, c) -> a + "\n" + c).orElseGet(() -> "");
        setStudentSampleData(harvestRegistrationConclusionBeansUsedInSampleData);

        if (!sample.isEmpty()) {
            GradingTableGenerator.generateTableDataImprovement(this, sample);
        } else {
            InstitutionGradingTable.copyData(this);
            setCopied(true);
        }

        checkUniquenessOfTable();
    }

    private String conclusionBeanStringData(RegistrationConclusionBean bean) {
        Integer studentNumber = bean.getRegistration().getNumber();
        String studentName = bean.getRegistration().getStudent().getName();
        String degreeCode = bean.getRegistration().getDegree().getCode();
        String programConclusionCode = bean.getProgramConclusion().getCode();
        String executionYearName = bean.getConclusionYear().getQualifiedName();
        Integer finalGrade =
                isNumeric(bean.getFinalGrade()) ? bean.getFinalGrade().getNumericValue().setScale(0, RoundingMode.HALF_UP).intValue() : 0;

        return String.format("%s\t%s\t%s\t%s\t%s\t%s", studentNumber, studentName, degreeCode, 
                programConclusionCode, executionYearName, finalGrade);
    }
    
    private boolean isNumeric(final Grade grade) {
        if (grade == null) {
            return false;
        }
        
        if(!grade.isNumeric()) {
            return false;
        }
        
        try {
            Double.parseDouble(grade.getValue());
            if (grade.getNumericValue() != null) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void checkUniquenessOfTable() {
        if (DegreeGradingTable.find(getExecutionYear()).stream()
                .anyMatch(t -> t != this && t.getDegree() == getDegree() && t.getProgramConclusion() == getProgramConclusion())) {
            throw new AcademicExtensionsDomainException("error.DegreeGradingTable.already.exists",
                    getExecutionYear().getQualifiedName(), "[" + getDegree().getCode() + "] " + getDegree().getPresentationName(),
                    getProgramConclusion().getName().getContent());
        }
    }

    private List<RegistrationConclusionBean> harvestRegistrationConclusionBeansUsedInSample() {
        final List<RegistrationConclusionBean> sample = new ArrayList<>();
        
        
        int coveredYears = 0;
        boolean sampleOK = false;
        final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap = collectConclusions();
        for (ExecutionYear year = getExecutionYear().getPreviousExecutionYear(); year != null; year =
                year.getPreviousExecutionYear()) {

            if (conclusionsMap.get(year) != null) {
                for (RegistrationConclusionBean bean : conclusionsMap.get(year)) {
                    final Grade finalGrade = bean.getFinalGrade();
                    Integer finalAverage = finalGrade.isNumeric() && finalGrade.getNumericValue() != null ? finalGrade
                            .getNumericValue().setScale(0, RoundingMode.HALF_UP).intValue() : 0;
                    if (finalAverage == 0) {
                        continue;
                    }
                    sample.add(bean);
                }
            }

            if (++coveredYears >= GradingTableSettings.getMinimumPastYears()
                    && sample.size() >= GradingTableSettings.getMinimumSampleSize()) {
                sampleOK = true;
                break;
            }

            if (coveredYears == GradingTableSettings.getMaximumPastYears()) {
                break;
            }
        }

        return sampleOK ? sample : Collections.emptyList();
    }
    
    private Map<ExecutionYear, Set<RegistrationConclusionBean>> collectConclusions() {
        final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap = new LinkedHashMap<>();

        for (final Registration registration : getDegree().getRegistrationsSet()) {
            if (registration.getStudentCurricularPlansSet().isEmpty()) {
                continue;
            }
            for (RegistrationConclusionInformation info : RegistrationConclusionServices.inferConclusion(registration)) {
                if (info.getCurriculumGroup() == null || !info.isConcluded()) {
                    continue;
                }
                final ExecutionYear conclusionYear = info.getRegistrationConclusionBean().getConclusionYear();
                if (!conclusionsMap.containsKey(conclusionYear)) {
                    conclusionsMap.put(conclusionYear, new HashSet<RegistrationConclusionBean>());
                }

                conclusionsMap.get(conclusionYear).add(info.getRegistrationConclusionBean());
            }
        }
        return conclusionsMap;
    }
}
