package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.StudentCurricularPlan;

public class CurriculumGradeCalculator
        implements org.fenixedu.academic.domain.student.curriculum.Curriculum.CurriculumGradeCalculator {

    static final public RoundingMode ROUNDING_DEFAULT = RoundingMode.HALF_UP;

    static final public RoundingMode ROUNDING_TRUNCATE = RoundingMode.DOWN;

    static final private int FULL_SCALE = 2 * 2 + 1;

    static final public int RAW_SCALE = 2;

    private Curriculum curriculum;

    private BigDecimal sumPiCi;

    private BigDecimal sumPi;

    private Grade rawGrade;

    private Grade finalGrade;

    private void doCalculus(final Curriculum curriculum) {
        this.curriculum = curriculum;
        this.sumPiCi = BigDecimal.ZERO;
        this.sumPi = BigDecimal.ZERO;
        countAverage(curriculum.getEnrolmentRelatedEntries());
        countAverage(curriculum.getDismissalRelatedEntries());
        final BigDecimal avg = calculateAverage();

        // qubExtension, bug fix on finalGrade calculation, must use rawAvg
        final BigDecimal rawAvg = avg.setScale(RAW_SCALE,
                curriculum.getStudentCurricularPlan() == null ? ROUNDING_DEFAULT : getRawGradeRoundingMode(
                        curriculum.getStudentCurricularPlan().getDegree()));
        this.rawGrade = Grade.createGrade(rawAvg.toString(), GradeScale.TYPE20);
        this.finalGrade = Grade.createGrade(rawAvg.setScale(0, ROUNDING_DEFAULT).toString(), GradeScale.TYPE20);
    }

    protected RoundingMode getRawGradeRoundingMode(final Degree degree) {
        return ROUNDING_DEFAULT;
    }

    private void countAverage(final Set<ICurriculumEntry> entries) {
        for (final ICurriculumEntry entry : entries) {
            if (entry.getGrade().isNumeric()) {
                final BigDecimal weigth = entry.getWeigthForCurriculum();

                sumPi = sumPi.add(weigth);
                sumPiCi = sumPiCi.add(entry.getWeigthForCurriculum().multiply(entry.getGrade().getNumericValue()));
            }
        }
    }

    protected BigDecimal calculateAverage() {
        return sumPi.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : sumPiCi.divide(sumPi, FULL_SCALE, ROUNDING_DEFAULT);
    }

    public BigDecimal calculateAverage(Curriculum curriculum) {
        if (sumPiCi == null) {
            doCalculus(curriculum);
        }

        return calculateAverage();
    }

    @Override
    public Grade rawGrade(Curriculum curriculum) {
        if (rawGrade == null) {
            doCalculus(curriculum);
        }
        return rawGrade;
    }

    @Override
    public Grade finalGrade(Curriculum curriculum) {
        if (finalGrade == null) {
            doCalculus(curriculum);
        }
        return finalGrade;
    }

    @Override
    public BigDecimal weigthedGradeSum(Curriculum curriculum) {
        if (sumPiCi == null) {
            doCalculus(curriculum);
        }
        return sumPiCi;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public BigDecimal getSumPiCi() {
        return sumPiCi;
    }

    public BigDecimal getSumPi() {
        return sumPi;
    }

    public boolean isCalculatorForDegree() {
        return true;
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return getCurriculum() == null ? null : getCurriculum().getStudentCurricularPlan();
    }

}
