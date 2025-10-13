package org.fenixedu.academic.domain.evaluation.config;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class MarkSheetSettingsTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            CompetenceCourseTest.initCompetenceCourse();
            return null;
        });
    }

    @After
    public void tearDown() {
        MarkSheetSettings.findAll().forEach(MarkSheetSettings::delete);
    }

    @Test
    public void testFindByCompetenceCourse_Successful() {
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);
        MarkSheetSettings expected = MarkSheetSettings.create(competenceCourse.getCompetenceCourseGroupUnit());

        assertEquals(expected, MarkSheetSettings.findByCompetenceCourse(competenceCourse).orElse(null));
    }

    @Test
    public void testFindByCompetenceCourse_MultipleMarkSheetSettings_Successful() {
        Unit schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElse(null);
        Unit coursesAgregatorUnit = Unit.findInternalUnitByAcronymPath("QS>Courses").orElse(null);
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        MarkSheetSettings.create(schoolUnit);
        MarkSheetSettings.create(coursesAgregatorUnit);
        MarkSheetSettings expected = MarkSheetSettings.create(competenceCourse.getCompetenceCourseGroupUnit());

        assertEquals(expected, MarkSheetSettings.findByCompetenceCourse(competenceCourse).orElse(null));
    }

    @Test
    public void testFindByCompetenceCourse_GetParentUnitMarkSheetSettings_Successful() {
        Unit schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElse(null);
        Unit coursesAgregatorUnit = Unit.findInternalUnitByAcronymPath("QS>Courses").orElse(null);
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        MarkSheetSettings.create(schoolUnit);
        MarkSheetSettings expected = MarkSheetSettings.create(coursesAgregatorUnit);

        assertEquals(expected, MarkSheetSettings.findByCompetenceCourse(competenceCourse).orElse(null));
    }

    @Test
    public void testFindByCompetenceCourse_GetRootUnitMarkSheetSettings_Successful() {
        Unit schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElse(null);
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        MarkSheetSettings expected = MarkSheetSettings.create(schoolUnit);

        assertEquals(expected, MarkSheetSettings.findByCompetenceCourse(competenceCourse).orElse(null));
    }

    @Test
    public void testFindByCompetenceCourse_NoMarkSheetSettingsInstance_ReturnsEmptyOptional() {
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        assertEquals(Optional.empty(), MarkSheetSettings.findByCompetenceCourse(competenceCourse));
    }

    @Test
    public void testFindByCompetenceCourse_MarkSheetSettingNotFound_ReturnsEmptyOptional() {
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        Unit unrelatedUnit =
                Unit.createNewUnit(PartyType.of(PartyTypeEnum.PLANET), buildLS.apply("Unrelated Unit Name"), "UU", null, null);

        MarkSheetSettings.create(unrelatedUnit);

        assertEquals(Optional.empty(), MarkSheetSettings.findByCompetenceCourse(competenceCourse));
    }

    private final static Function<String, LocalizedString> buildLS =
            s -> new LocalizedString.Builder().with(Locale.getDefault(), s).build();
}
