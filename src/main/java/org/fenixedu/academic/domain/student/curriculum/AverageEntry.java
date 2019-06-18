/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 *
 * 
 * This file is part of FenixEdu fenixedu-ulisboa-specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Specifications.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;

import com.google.common.collect.Lists;

public class AverageEntry implements Comparable<AverageEntry> {

    static final public String ENTRY_INFO_EMPTY = "-";
    // TODO translate
    static final public String ENTRY_INFO_EQUALS = "Idem";

    private ICurriculumEntry entry;
    private StudentCurricularPlan studentCurricularPlan;
    private String approvalTypeDescription;
    private Integer curricularYear;
    private Integer curricularSemester;
    private String entryInfo;
    private String targetCurriculumLinesInfo;
    private YearMonthDay conclusionDateOnTarget;

    public AverageEntry(final ICurriculumEntry entry, final StudentCurricularPlan studentCurricularPlan) {
        this.entry = entry;
        this.studentCurricularPlan = studentCurricularPlan;
        this.approvalTypeDescription = getApprovalTypeDescription(entry, studentCurricularPlan);
        this.curricularYear = getCurricularYear(entry);
        this.curricularSemester = getCurricularSemester(entry);
        this.entryInfo = getEntryInfo(entry);
        this.targetCurriculumLinesInfo = getTargetCurriculumLinesInfo(entry, studentCurricularPlan);
        this.conclusionDateOnTarget = getConclusionDateOnTarget(entry, studentCurricularPlan);
    }

    public ICurriculumEntry getEntry() {
        return entry;
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return studentCurricularPlan;
    }

    public ExecutionYear getExecutionYear() {
        return getEntry().getExecutionYear();
    }

    public BigDecimal getEcts() {
        return getEntry().getEctsCreditsForCurriculum();
    }

    public BigDecimal getGradeValue() {
        return getEntry().getGrade().getNumericValue();
    }

    public String getApprovalTypeDescription() {
        return approvalTypeDescription;
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public Integer getCurricularSemester() {
        return curricularSemester;
    }

    public String getEntryInfo() {
        return entryInfo;
    }

    public String getTargetCurriculumLinesInfo() {
        return targetCurriculumLinesInfo;
    }

    public YearMonthDay getConclusionDateOnTarget() {
        return conclusionDateOnTarget;
    }

    @Override
    public int compareTo(final AverageEntry o) {
        int result = getEntryInfo().compareTo(o.getEntryInfo());

        if (result == 0) {
            if (getCurricularYear() != null && o.getCurricularYear() != null) {
                result = 0;
            } else {
                result = getCurricularYear() != null ? -1 : 1;
            }
        }

        if (result == 0) {
            result = getPresentationNameFor(getEntry()).compareTo(getPresentationNameFor(o.getEntry()));
        }

        return result;
    }

    static private String getPresentationNameFor(final ICurriculumEntry entry) {
        final String code = !StringUtils.isEmpty(entry.getCode()) ? entry.getCode() + " - " : "";

        if (entry instanceof OptionalEnrolment) {
            final OptionalEnrolment optionalEnrolment = (OptionalEnrolment) entry;
            return code + optionalEnrolment.getCurricularCourse().getNameI18N(entry.getExecutionPeriod()).getContent();
        } else {
            return code + entry.getName().getContent();
        }
    }

    static public List<AverageEntry> getAverageEntries(final Curriculum curriculum) {
        final List<AverageEntry> result = Lists.newLinkedList();
        final Predicate<AverageEntry> predicate = i -> i.isAccountable();

        curriculum.getEnrolmentRelatedEntries().stream().map(i -> new AverageEntry(i, curriculum.getStudentCurricularPlan()))
                .filter(predicate).collect(Collectors.toCollection(() -> result));

        curriculum.getDismissalRelatedEntries().stream().map(i -> new AverageEntry(i, curriculum.getStudentCurricularPlan()))
                .filter(predicate).collect(Collectors.toCollection(() -> result));

        Collections.sort(result);
        return result;
    }

    private boolean isAccountable() {
        return getEntry().getCurriculumLinesForCurriculum(studentCurricularPlan).stream().filter(i -> i.isDismissal())
                .map(Dismissal.class::cast).map(i -> i.getCredits()).filter(i -> i != null).map(i -> i.getReason())
                .noneMatch(i -> i != null && !i.getAverageEntry());
    }

    static private String getApprovalTypeDescription(final ICurriculumEntry entry,
            final StudentCurricularPlan studentCurricularPlan) {
        LocalizedString result = CurriculumLineServices.getCurriculumEntryDescription(entry, studentCurricularPlan, true, true);

        // here we want some info, even if we were given null
        if (result == null || result.isEmpty()) {
            result = new LocalizedString();

            for (final Locale locale : CoreConfiguration.supportedLocales()) {
                result = result.with(locale, ENTRY_INFO_EMPTY);
            }
        }

        return result.getContent();
    }

    static private Integer getCurricularYear(final ICurriculumEntry entry) {
        Integer result = null;

        if (entry instanceof CurriculumLine) {
            result = CurricularPeriodServices.getCurricularYear((CurriculumLine) entry);
        }

        return result;
    }

    static private Integer getCurricularSemester(final ICurriculumEntry entry) {
        Integer result = null;

        if (entry instanceof CurriculumLine) {
            result = CurricularPeriodServices.getCurricularSemester((CurriculumLine) entry);
        }

        return result;
    }

    static private String getTargetCurriculumLinesInfo(final ICurriculumEntry entry,
            final StudentCurricularPlan studentCurricularPlan) {
        final Set<CurriculumLine> lines = entry.getCurriculumLinesForCurriculum(studentCurricularPlan);

        if (lines.isEmpty()) {
            return ENTRY_INFO_EMPTY;
        }

        if (lines.size() == 1 && lines.contains(entry)) {
            return ENTRY_INFO_EQUALS;
        }

        return lines.stream().map(line -> getEntryInfo((ICurriculumEntry) line)).distinct().sorted()
                .collect(Collectors.joining(" "));
    }

    static private String getEntryInfo(final ICurriculumEntry entry) {
        String curricularYear = "";
        String curricularSemester = "";
        if (entry instanceof CurriculumLine) {
            curricularYear = String.valueOf(CurricularPeriodServices.getCurricularYear((CurriculumLine) entry)) + " "
                    + BundleUtil.getString(Bundle.APPLICATION, "label.curricular.year") + ", ";
            curricularSemester = String.valueOf(CurricularPeriodServices.getCurricularSemester((CurriculumLine) entry)) + " "
                    + BundleUtil.getString(Bundle.APPLICATION, "label.semester.short");
        }
        final String executionYear = entry.getExecutionYear() == null ? "" : " " + entry.getExecutionYear().getQualifiedName();
        return curricularYear + curricularSemester + executionYear;
    }

    static public YearMonthDay getConclusionDateOnTarget(final ICurriculumEntry entry,
            final StudentCurricularPlan studentCurricularPlan) {
        final Set<CurriculumLine> lines = entry.getCurriculumLinesForCurriculum(studentCurricularPlan);

        return lines.isEmpty() ? entry.getApprovementDate() : lines.stream().filter(i -> i.getApprovementDate() != null)
                .map(i -> i.getApprovementDate()).max(YearMonthDay::compareTo).orElse(null);

    }

}
