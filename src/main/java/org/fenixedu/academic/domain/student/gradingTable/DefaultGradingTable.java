package org.fenixedu.academic.domain.student.gradingTable;

import org.fenixedu.bennu.core.domain.Bennu;
import pt.ist.fenixframework.Atomic;

public class DefaultGradingTable extends DefaultGradingTable_Base {

    private DefaultGradingTable() {
        super();
        setDefaultBennu(Bennu.getInstance());
        compileData();
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        GradingTableGenerator.defaultData(this);
    }

    @Atomic
    public static DefaultGradingTable getDefaultGradingTable() {
        DefaultGradingTable defaultTable = Bennu.getInstance().getDefaultGradingTable();
        if (defaultTable == null) {
            defaultTable = new DefaultGradingTable();
        }
        return defaultTable;
    }

}
