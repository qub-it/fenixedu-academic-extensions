package org.fenixedu.academic.domain.evaluation.season.rule;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.ShiftTypes;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

import pt.ist.fenixframework.Atomic;

public class EvaluationSeasonShiftType extends EvaluationSeasonShiftType_Base {

    public EvaluationSeasonShiftType() {
        super();
    }

    @Atomic
    static public EvaluationSeasonRule create(final EvaluationSeason season, final List<ShiftType> shiftTypes) {
        final EvaluationSeasonShiftType result = new EvaluationSeasonShiftType();
        result.init(season, shiftTypes);
        return result;
    }

    private void init(final EvaluationSeason season, final List<ShiftType> shiftTypes) {
        super.init(season);
        setShiftTypes(new ShiftTypes(shiftTypes));

        checkRules();
    }

    private void checkRules() {
        if (getShiftTypes() == null || getShiftTypes().getTypes().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.EvaluationSeasonShiftType.shiftTypes.required");
        }
    }

    @Atomic
    public void edit(final List<ShiftType> shiftTypes) {
        init(getSeason(), shiftTypes);
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public LocalizedString getDescriptionI18N() {
        final Builder builder = AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName()).builder();
        builder.append(getShiftTypes().getTypes().stream()
                .map(i -> String.format("%s [%s]", i.getFullNameTipoAula(), i.getSiglaTipoAula()))
                .collect(Collectors.joining("; ")), ": ");
        return builder.build();
    }

}
