package org.fenixedu.academic.domain.evaluation.season.rule;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.ShiftTypes;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;

public class EvaluationSeasonShiftType extends EvaluationSeasonShiftType_Base {

    static private final Logger logger = LoggerFactory.getLogger(EvaluationSeasonShiftType.class);

    public EvaluationSeasonShiftType() {
        super();
    }

    static public EvaluationSeasonRule create(final EvaluationSeason season, final Collection<CourseLoadType> courseLoadTypes) {
        final EvaluationSeasonShiftType result = new EvaluationSeasonShiftType();
        result.init(season);
        result.getCourseLoadTypesSet().addAll(courseLoadTypes);
        return result;
    }

    @Deprecated
    @Atomic
    static public EvaluationSeasonRule create(final EvaluationSeason season, final List<ShiftType> shiftTypes) {
//        final EvaluationSeasonShiftType result = new EvaluationSeasonShiftType();
//        result.init(season, shiftTypes);
//        return result;
        throw new UnsupportedOperationException();
    }

//    @Deprecated
//    private void init(final EvaluationSeason season, final List<ShiftType> shiftTypes) {
//        super.init(season);
//        setShiftTypes(new ShiftTypes(shiftTypes));
//
//        checkRules();
//    }

//    @Deprecated
//    private void checkRules() {
//        if (getShiftTypes() == null || getShiftTypes().getTypes().isEmpty()) {
//            throw new AcademicExtensionsDomainException("error.EvaluationSeasonShiftType.shiftTypes.required");
//        }
//    }

    @Deprecated
    @Atomic
    public void edit(final List<ShiftType> shiftTypes) {
//        init(getSeason(), shiftTypes);
        throw new UnsupportedOperationException();
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

    public static void migrateCourseLoadTypes() {
        final Set<EvaluationSeasonShiftType> allShiftTypeRules =
                EvaluationSeason.all().flatMap(s -> s.getRulesSet().stream()).filter(EvaluationSeasonShiftType.class::isInstance)
                        .map(EvaluationSeasonShiftType.class::cast).collect(Collectors.toSet());

        if (!allShiftTypeRules.isEmpty()) {
            logger.info("EvaluationSeasonShiftType - starting migration from ShiftType to CourseLoadType ...");

            final AtomicInteger counter = new AtomicInteger();
            for (EvaluationSeasonShiftType rule : allShiftTypeRules) {
                if (rule.getCourseLoadTypesSet().isEmpty()) {
                    final Set<CourseLoadType> courseLoadTypes =
                            rule.getShiftTypes().getTypes().stream().map(st -> st.toCourseLoadType()).collect(Collectors.toSet());
                    rule.getCourseLoadTypesSet().addAll(courseLoadTypes);
                    counter.incrementAndGet();
                }
            }

            logger.info("EvaluationSeasonShiftType - migration ended. ({}/{})", counter.get(), allShiftTypeRules.size());
        }
    }

}
