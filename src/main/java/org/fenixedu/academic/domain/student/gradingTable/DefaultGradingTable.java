package org.fenixedu.academic.domain.student.gradingTable;

import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class DefaultGradingTable extends DefaultGradingTable_Base {

    private DefaultGradingTable() {
        super();
        compileData();
    }

    @Override
    public void compileData() {
        GradingTableData tableData = new GradingTableData();
        setData(tableData);
        GradingTableGenerator.defaultData(this);
    }

    private static DefaultGradingTable findUnique() {
        if (Bennu.getInstance().getGradingTablesSet().stream().filter(DefaultGradingTable.class::isInstance)
                .map(DefaultGradingTable.class::cast).count() > 1) {
            throw new AcademicExtensionsDomainException("error.gradingTables.defaultGradingTable.moreThanOneTableFound");
        } else {
            return Bennu.getInstance().getGradingTablesSet().stream().filter(DefaultGradingTable.class::isInstance)
                    .map(DefaultGradingTable.class::cast).findFirst().orElse(null);
        }
    }

    @Atomic
    public static DefaultGradingTable getDefaultGradingTable() {
        DefaultGradingTable defaultTable = findUnique();
        if (defaultTable == null) {
            defaultTable = new DefaultGradingTable();
        }
        return defaultTable;
    }

}
