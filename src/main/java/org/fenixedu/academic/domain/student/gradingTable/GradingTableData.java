package org.fenixedu.academic.domain.student.gradingTable;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GradingTableData {

    /**
     * GradingTableData - JSON structure holding a grade conversion table
     * - mark : internal grade representation
     * - ectsGrade : ects grade representation
     */

    private Gson gson = new Gson();
    private Set<GradeConversion> table = new TreeSet<GradeConversion>(GradeConversion.COMPARE_BY_MARK);

    public String toJson() {
        Type type = new TypeToken<Set<GradeConversion>>() {
        }.getType();
        return gson.toJson(table, type);
    }

    public static GradingTableData fromJson(String json) {
        GradingTableData data = new GradingTableData();
        Type type = new TypeToken<Set<GradeConversion>>() {
        }.getType();
        Set<GradeConversion> table = data.gson.fromJson(json, type);
        data.table.addAll(table);
        return data;
    }

    public Set<GradeConversion> getTable() {
        return table;
    }

    public void setTable(Set<GradeConversion> table) {
        this.table = table;
    }

    static public class GradeConversion {
        private String mark;
        private String ectsGrade;

        public static Comparator<GradeConversion> COMPARE_BY_MARK = new Comparator<GradeConversion>() {
            @Override
            public int compare(GradeConversion gc1, GradeConversion gc2) {
                return gc1.getMark().compareTo(gc2.getMark());
            }
        };

        public GradeConversion(String mark, String ectsGrade) {
            this.mark = mark;
            this.ectsGrade = ectsGrade;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(final String mark) {
            this.mark = mark;
        }

        public String getEctsGrade() {
            return ectsGrade;
        }

        public void setEctsGrade(final String ectsGrade) {
            this.ectsGrade = ectsGrade;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof GradeConversion) {
                GradeConversion gc = (GradeConversion) obj;
                return getMark().equals(gc.getMark());
            }
            return super.equals(obj);
        }
    }

}
