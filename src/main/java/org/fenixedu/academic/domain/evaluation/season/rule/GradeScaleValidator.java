package org.fenixedu.academic.domain.evaluation.season.rule;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class GradeScaleValidator extends GradeScaleValidator_Base {

    public GradeScaleValidator() {
        super();
    }

    @Atomic
    static public EvaluationSeasonRule create(final EvaluationSeason season, final GradeScale gradeScale,
            final String gradeValues, final LocalizedString description, final boolean appliesToCurriculumAggregatorEntry,
            final Collection<DegreeType> degreeTypes, final Collection<Unit> units) {

        final GradeScaleValidator result = new GradeScaleValidator();
        result.init(season, gradeScale, gradeValues, description, appliesToCurriculumAggregatorEntry, degreeTypes, units);
        return result;
    }

    private void init(final EvaluationSeason season, final GradeScale gradeScale, final String gradeValues,
            final LocalizedString description, final boolean appliesToCurriculumAggregatorEntry,
            final Collection<DegreeType> degreeTypes, final Collection<Unit> units) {

        setGradeScale(gradeScale);
        setGradeValues(gradeValues);
        setRuleDescription(description);
        setAppliesToCurriculumAggregatorEntry(appliesToCurriculumAggregatorEntry);
        getDegreeTypeSet().clear();
        getDegreeTypeSet().addAll(degreeTypes);
        getUnitsSet().clear();
        getUnitsSet().addAll(units);

        super.init(season);

        checkRules();
    }

    private void checkRules() {
        if (getGradeScale() == null) {
            throw new AcademicExtensionsDomainException("error.GradeScaleValidator.gradeScale.required");
        }

        if (StringUtils.isBlank(getGradeValues())) {
            throw new AcademicExtensionsDomainException("error.GradeScaleValidator.gradeValues.required");
        }

        if (getRuleDescription() == null || getRuleDescription().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.GradeScaleValidator.description.required");
        }

        if (getDegreeTypeSet().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.GradeScaleValidator.degreeTypes.required");
        }
    }

    @Override
    protected Predicate<? super EvaluationSeasonRule> checkDuplicate() {
        return i -> {

            if (i == this || !(i instanceof GradeScaleValidator)) {
                return false;
            }

            final GradeScaleValidator o = (GradeScaleValidator) i;
            final boolean existsOtherForSameDegreeTypes = !Sets.intersection(o.getDegreeTypeSet(), getDegreeTypeSet()).isEmpty();
            final boolean existsOtherForSameUnits = getUnitsSet().isEmpty() || o.getUnitsSet().isEmpty()
                    || !Sets.intersection(o.getUnitsSet(), getUnitsSet()).isEmpty();
            
            return o.getGradeScale() == getGradeScale()
                    && o.getAppliesToCurriculumAggregatorEntry() == getAppliesToCurriculumAggregatorEntry()
                    && existsOtherForSameDegreeTypes && existsOtherForSameUnits;
        };
    }

    @Atomic
    public void edit(final GradeScale gradeScale, final String gradeValues, final LocalizedString description,
            final boolean appliesToCurriculumAggregatorEntry, final Collection<DegreeType> degreeTypes,
            final Collection<Unit> units) {

        init(getSeason(), gradeScale, gradeValues, description, appliesToCurriculumAggregatorEntry, degreeTypes, units);
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public LocalizedString getDescriptionI18N() {
        final Builder builder = AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName()).builder();
        builder.append(getGradeScale().getName().getContent(), " [");
        builder.append(getGradeValues(), "]: ");
        builder.append(String.valueOf(getDegreeTypeSet().stream().count()), " [");
        builder.append(AcademicExtensionsUtil.bundle("label.Degree.degreeType"), " ");
        builder.append("]");
        builder.append(getUnitsSet().isEmpty() ? "" : " [" + getUnitsSet().stream()
                .map(u -> StringUtils.isNotBlank(u.getAcronym()) ? u.getAcronym() : u.getNameI18n().getContent())
                .collect(Collectors.joining(", ")) + "]");

        return builder.build();
    }

    public boolean isGradeValueAccepted(final String input) {
        if (getGradeScale().belongsTo(input)) {
            for (final String iter : getGradeValues().split(" ")) {

                if (iter.equals(input)) {
                    return true;
                }

                final List<String> limits = Lists.newArrayList(iter.split("-"));
                if (limits.size() == 2) {

                    final String limitMin = limits.get(0);
                    final String limitMax = limits.get(1);
                    final int scale = getScale(limitMin, limitMax);

                    try {
                        final BigDecimal value = new BigDecimal(input);
                        if (value.scale() <= scale) {

                            final BigDecimal min = new BigDecimal(limitMin);
                            final BigDecimal max = new BigDecimal(limitMax);
                            return BigDecimalValidator.getInstance().isInRange(value, min, max);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        return false;
    }

    static private int getScale(final String limitMin, final String limitMax) {
        int scale = 0;

        if (limitMin.contains(".")) {
            final int temp = limitMin.split("[.]")[1].length();
            scale = scale == 0 || temp < scale ? temp : scale;
        }

        if (limitMax.contains(".")) {
            final int temp = limitMax.split("[.]")[1].length();
            scale = scale == 0 || temp < scale ? temp : scale;
        }

        return scale;
    }

    @Override
    @Atomic
    public void delete() {
        super.setGradeScale(null);
        getDegreeTypeSet().clear();
        getUnitsSet().clear();

        super.delete();
    }

}
