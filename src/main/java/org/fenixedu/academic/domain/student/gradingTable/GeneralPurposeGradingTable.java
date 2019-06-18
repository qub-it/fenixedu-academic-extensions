package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;

public class GeneralPurposeGradingTable extends GeneralPurposeGradingTable_Base {

    private List<BigDecimal> sample;

    private GeneralPurposeGradingTable() {
        super();
        setExecutionYear(ExecutionYear.findCurrent(null));
    }

    public GeneralPurposeGradingTable(List<BigDecimal> sample) {
        this();
        this.sample = sample;
        compileData();
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        if (sample != null) {
            GradingTableGenerator.generateTableData(this, sample);
        } else {
            GradingTableGenerator.defaultData(this);
            setCopied(true);
        }
    }
}
