package org.fenixedu.academic.domain.evaluation.season.rule;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

import pt.ist.fenixframework.Atomic;

public class EvaluationSeasonStatuteType extends EvaluationSeasonStatuteType_Base {

    public EvaluationSeasonStatuteType() {
        super();
    }

    @Atomic
    static public EvaluationSeasonRule create(final EvaluationSeason season, final List<StatuteType> statuteTypes) {
        final EvaluationSeasonStatuteType result = new EvaluationSeasonStatuteType();
        result.init(season, statuteTypes);
        return result;
    }

    private void init(final EvaluationSeason season, final List<StatuteType> statuteTypes) {
        super.init(season);
        getStatuteTypesSet().addAll(statuteTypes);

        checkRules();
    }

    private void checkRules() {
        if (getStatuteTypesSet() == null || getStatuteTypesSet().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.EvaluationSeasonStatuteType.statuteType.required");
        }
    }

    @Atomic
    public void edit(final List<StatuteType> statuteTypes) {
        init(getSeason(), statuteTypes);
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public LocalizedString getDescriptionI18N() {
        final Builder builder = AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName()).builder();
        builder.append(getStatuteTypesSet().stream().map(i -> String.format("%s [%s]", i.getName().getContent(), i.getCode()))
                .collect(Collectors.joining("; ")), ": ");
        return builder.build();
    }

}
