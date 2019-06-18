package org.fenixedu.academic.domain.student.gradingTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.academic.domain.student.gradingTable.GradingTableData.GradeConversion;

import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

abstract public class GradingTable extends GradingTable_Base {

    public GradingTable() {
        super();
        setBennu(Bennu.getInstance());
        setCopied(false);
    }

    public void delete() {
        setExecutionYear(null);
        setBennu(null);
        deleteDomainObject();
    }

    //Private methods
    private GradeConversion find(final String mark) {
        return getData().getTable().stream().filter(gc -> {
            try {
                // If marks are numerical, BigDecimal is the canonical representation.
                BigDecimal tableMark = new BigDecimal(gc.getMark());
                BigDecimal testingMark = new BigDecimal(mark);
                return tableMark.compareTo(testingMark) == 0;
            } catch (NumberFormatException nfe) {
                return gc.getMark().equals(mark);
            }
        }).findFirst().orElse(null);
    }

    // Public API - getEctsGrade
    public String getEctsGrade(final String mark) {
        GradeConversion gc = find(mark);
        return gc != null ? gc.getEctsGrade() : null;
    }

    public String getEctsGrade(final int mark) {
        return getEctsGrade(String.valueOf(mark));
    }

    public String getEctsGrade(final BigDecimal mark) {
        return getEctsGrade(mark.toString());
    }

    // Public API - setGrade
    public void setEctsGrade(final String mark, final String ectsGrade) {
        GradeConversion gc = find(mark);
        if (gc != null) {
            gc.setEctsGrade(ectsGrade);
        }
    }

    public void setGrade(final int mark, final String ectsGrade) {
        setEctsGrade(String.valueOf(mark), ectsGrade);
    }

    public void setGrade(final BigDecimal mark, final String ectsGrade) {
        setEctsGrade(mark.toString(), ectsGrade);
    }

    // Public API - hasMark
    public boolean hasMark(final String mark) {
        return find(mark) != null;
    }

    public boolean hasMark(final int mark) {
        return hasMark(String.valueOf(mark));
    }

    public boolean hasMark(final BigDecimal mark) {
        return hasMark(mark.toString());
    }

    // Public API - addMark
    public void addMark(final String mark, final String ectsGrade) {
        GradeConversion gc = new GradeConversion(mark, ectsGrade);
        if (hasMark(mark)) {
            setEctsGrade(mark, ectsGrade);
        } else {
            getData().getTable().add(gc);
        }
    }

    public void addMark(final int mark, final String ectsGrade) {
        addMark(String.valueOf(mark), ectsGrade);
    }

    public void addMark(final BigDecimal mark, final String ectsGrade) {
        addMark(mark.toString(), ectsGrade);
    }

    // Public API - getEctsGrades
    public List<GradeConversion> getEctsGrades() {
        return new ArrayList<GradeConversion>(getData().getTable());
    }

    public String printScale() {
        String scale = "";
        for (GradeConversion gc : getEctsGrades()) {
            scale += gc.getEctsGrade();
        }
        return scale;
    }

    abstract public void compileData();

    protected static class GeneratorWorker<T extends GradingTable> extends Thread {

        private T table;
        private CallableWithoutException<T> logic;

        public GeneratorWorker(CallableWithoutException<T> logic) {
            this.logic = logic;
        }

        @Override
        public void run() {
            table = FenixFramework.getTransactionManager().withTransaction(logic);
        }

        T getTable() {
            return table;
        }
    }

}
