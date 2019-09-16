package org.fenixedu.academic.domain.student.gradingTable;

import java.util.Set;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

public class GradingTableSettings extends GradingTableSettings_Base {

    private static Integer MIN_SAMPLE_SIZE = 30;
    private static Integer MIN_PAST_YEARS = 3;
    private static Integer MAX_PAST_YEARS = 5;

    private GradingTableSettings() {
        super();
        setBennu(Bennu.getInstance());
    }

    private GradingTableSettings(Integer minSampleSize, int minPastYears, int maxPastYears) {
        this();
        setMinSampleSize(minSampleSize);
        setMinPastYears(minPastYears);
        setMaxPastYears(maxPastYears);

        checkRules();
    }

    private void checkRules() {

        if (getMinSampleSize() == null) {
            throw new AcademicExtensionsDomainException("error.GradingTableSettings.minSampleSize.required");
        }

        if (getMinPastYears() == null) {
            throw new AcademicExtensionsDomainException("error.GradingTableSettings.minPastYears.required");
        }

        if (getMaxPastYears() == null) {
            throw new AcademicExtensionsDomainException("error.GradingTableSettings.maxPastYears.required");
        }

    }

    @Atomic
    public static GradingTableSettings getInstance() {
        GradingTableSettings settings = Bennu.getInstance().getGradingTableSettings();
        if (settings == null) {
            settings = new GradingTableSettings(MIN_SAMPLE_SIZE, MIN_PAST_YEARS, MAX_PAST_YEARS);
        }
        return settings;
    }

    public static int getMinimumSampleSize() {
        return getInstance().getMinSampleSize();
    }

    public static int getMinimumPastYears() {
        return getInstance().getMinPastYears();
    }

    public static int getMaximumPastYears() {
        return getInstance().getMaxPastYears();
    }

    public static Set<DegreeType> getApplicableDegreeTypes() {
        return getInstance().getApplicableDegreeTypesSet();
    }
}
