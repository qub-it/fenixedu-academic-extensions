package org.fenixedu.academic.domain.curriculum.grade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.exceptions.DomainException;

public class QualitativeGradeComparator implements Comparator<Grade> {

    private List<String> sortedGrades = new ArrayList<String>();

    public QualitativeGradeComparator(List<String> sortedGrades) {
        this.sortedGrades.addAll(sortedGrades);
    }

    @Override
    public int compare(Grade leftGrade, Grade rightGrade) {

        if (rightGrade == null || rightGrade.isEmpty()) {
            return 1;
        }

        if (leftGrade == null || leftGrade.isEmpty()) {
            return -1;
        }

        if (!leftGrade.getGradeScale().equals(rightGrade.getGradeScale())) {
            throw new DomainException("Grade.unsupported.comparassion.of.grades.of.different.scales");
        }

        final boolean isLeftApproved = leftGrade.isApproved();
        final boolean isRightApproved = rightGrade.isApproved();

        if (isLeftApproved && isRightApproved) {
            return getGradeIndex(leftGrade).compareTo(getGradeIndex(rightGrade));
        } else if (isLeftApproved) {
            return 1;
        } else if (isRightApproved) {
            return -1;
        } else {
            return getGradeIndex(leftGrade).compareTo(getGradeIndex(rightGrade));
        }

    }

    private Integer getGradeIndex(Grade value) {
        return sortedGrades.indexOf(value.getValue());
    }

}
