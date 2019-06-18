package org.fenixedu.academic.domain.student.gradingTable;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionInformation;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.academic.domain.student.gradingTable.GradingTableData.GradeConversion;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InstitutionGradingTable extends InstitutionGradingTable_Base {

    private InstitutionGradingTable() {
        super();
    }

    public static Stream<InstitutionGradingTable> findAll() {
        return Bennu.getInstance().getGradingTablesSet().stream().filter(InstitutionGradingTable.class::isInstance)
                .map(InstitutionGradingTable.class::cast);
    }

    public static InstitutionGradingTable find(final ExecutionYear ey) {
        return ey.getGradingTablesSet().stream().filter(InstitutionGradingTable.class::isInstance)
                .map(InstitutionGradingTable.class::cast).findAny().orElse(null);
    }

    public static InstitutionGradingTable generate(final ExecutionYear executionYear) {
        if (find(executionYear) != null) {
            return null;
        }
        InstitutionGradingTable table = new InstitutionGradingTable();
        table.setExecutionYear(executionYear);
        table.compileData();
        return table;
    }

    public static void copyData(GradingTable table) {
        InstitutionGradingTable institutionTable = find(table.getExecutionYear());
        if (institutionTable != null) {
            for (GradeConversion gc : institutionTable.getData().getTable()) {
                table.addMark(gc.getMark(), gc.getEctsGrade());
            }
        } else {
            GradingTableGenerator.defaultData(table);
        }
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        List<BigDecimal> sample = harvestSample();
        if (sample != null) {
            GradingTableGenerator.generateTableData(this, sample);
        } else {
            GradingTableGenerator.defaultData(this);
            setCopied(true);
        }
        
        checkUniquenessOfTable();
    }

    private void checkUniquenessOfTable() {
        final InstitutionGradingTable existing = InstitutionGradingTable.find(getExecutionYear());
        if (existing != null && existing != this) {
            throw new AcademicExtensionsDomainException("error.InstitutionGradingTable.already.exists",
                    getExecutionYear().getQualifiedName());
        }
    }

    private List<BigDecimal> harvestSample() {
        List<BigDecimal> sample = new ArrayList<BigDecimal>();
        int coveredYears = 0;
        boolean sampleOK = false;
        final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap = collectConclusions();
        for (ExecutionYear year = getExecutionYear().getPreviousExecutionYear(); year != null; year =
                year.getPreviousExecutionYear()) {

            if (conclusionsMap.get(year) != null) {
                for (RegistrationConclusionBean bean : conclusionsMap.get(year)) {
                    Integer finalAverage = bean.getFinalGrade().getNumericValue() != null ? bean.getFinalGrade().getNumericValue()
                            .setScale(0, RoundingMode.HALF_UP).intValue() : 0;
                    if (finalAverage == 0) {
                        continue;
                    }
                    sample.add(new BigDecimal(finalAverage));
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
        return sampleOK ? sample : null;
    }

    private Map<ExecutionYear, Set<RegistrationConclusionBean>> collectConclusions() {
        final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap =
                new LinkedHashMap<ExecutionYear, Set<RegistrationConclusionBean>>();
        final Set<Registration> batch = new HashSet<Registration>();

        for (final Registration registration : Bennu.getInstance().getRegistrationsSet()) {
            if (registration.getStudentCurricularPlansSet().isEmpty()) {
                continue;
            }
            if (!GradingTableSettings.getApplicableDegreeTypes().contains(registration.getDegreeType())) {
                continue;
            }
            batch.add(registration);
            if (batch.size() < 500) {
                continue;
            }
            processBatch(batch, conclusionsMap);
            batch.clear();
            if (isDenseEnough(conclusionsMap)) {
                break;
            }
        }
        if (!batch.isEmpty()) {
            processBatch(batch, conclusionsMap);
        }
        return conclusionsMap;
    }

    private void processBatch(final Set<Registration> batch,
            final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap) {
        Callable<Set<RegistrationConclusionInformation>> workerLogic = new Callable<Set<RegistrationConclusionInformation>>() {
            @Override
            public Set<RegistrationConclusionInformation> call() throws Exception {
                final Set<RegistrationConclusionInformation> conclusions = new HashSet<RegistrationConclusionInformation>();
                for (Registration reg : batch) {
                    for (RegistrationConclusionInformation info : RegistrationConclusionServices.inferConclusion(reg)) {
                        if (info.isConcluded()) {
                            conclusions.add(info);
                        }
                    }
                }
                return conclusions;
            }
        };
        HarvesterWorker worker = new HarvesterWorker(workerLogic);
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (RegistrationConclusionInformation conclusion : worker.getConclusions()) {
            final ExecutionYear conclusionYear = conclusion.getRegistrationConclusionBean().getConclusionYear();
            if (!conclusionsMap.containsKey(conclusionYear)) {
                conclusionsMap.put(conclusionYear, new HashSet<RegistrationConclusionBean>());
            }

            conclusionsMap.get(conclusionYear).add(conclusion.getRegistrationConclusionBean());
        }
    }

    private boolean isDenseEnough(final Map<ExecutionYear, Set<RegistrationConclusionBean>> conclusionsMap) {
        int yearsSweeped = 0;
        ExecutionYear year = getExecutionYear().getPreviousExecutionYear();
        while (year != null) {
            if (conclusionsMap.get(year) != null
                    && conclusionsMap.get(year).size() < (2 * GradingTableSettings.getMinimumSampleSize())) {
                return false;
            }
            if (++yearsSweeped >= (2 * GradingTableSettings.getMinimumPastYears())) {
                return true;
            }
            year = year.getPreviousExecutionYear();
        }
        return false;
    }

    private class HarvesterWorker extends Thread {

        private Callable<Set<RegistrationConclusionInformation>> logic;
        private Set<RegistrationConclusionInformation> conclusions;

        public HarvesterWorker(Callable<Set<RegistrationConclusionInformation>> logic) {
            this.logic = logic;
        }

        @Override
        public void run() {
            try {
                conclusions = FenixFramework.getTransactionManager().withTransaction(logic, new Atomic() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public boolean flattenNested() {
                        return true;
                    }

                    @Override
                    public TxMode mode() {
                        return TxMode.READ;
                    }
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Set<RegistrationConclusionInformation> getConclusions() {
            return conclusions;
        }
    }
}
