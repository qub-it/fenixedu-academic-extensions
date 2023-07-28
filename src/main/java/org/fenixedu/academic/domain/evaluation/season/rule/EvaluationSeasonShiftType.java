package org.fenixedu.academic.domain.evaluation.season.rule;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;

public class EvaluationSeasonShiftType extends EvaluationSeasonShiftType_Base {

    public EvaluationSeasonShiftType() {
        super();
    }

    static public EvaluationSeasonRule create(final EvaluationSeason season, final Collection<CourseLoadType> courseLoadTypes) {
        final EvaluationSeasonShiftType result = new EvaluationSeasonShiftType();
        result.init(season);
        result.getCourseLoadTypesSet().addAll(courseLoadTypes);
        return result;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public LocalizedString getDescriptionI18N() {
        final Builder builder = AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName()).builder();
        builder.append(getCourseLoadTypesSet().stream()
                .map(i -> String.format("%s [%s]", i.getName().getContent(), i.getInitials().getContent()))
                .collect(Collectors.joining("; ")), ": ");
        return builder.build();
    }

    @Override
    public void delete() {
        getCourseLoadTypesSet().clear();
        super.delete();
    }

}
