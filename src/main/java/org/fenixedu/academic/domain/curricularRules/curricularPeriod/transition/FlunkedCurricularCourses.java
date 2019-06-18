package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class FlunkedCurricularCourses extends FlunkedCurricularCourses_Base {

    static private final Logger logger = LoggerFactory.getLogger(FlunkedCurricularCourses.class);

    private Set<CompetenceCourse> competenceCourses;

    protected FlunkedCurricularCourses() {
        super();
    }

    @Atomic
    static public FlunkedCurricularCourses create(final CurricularPeriodConfiguration configuration, final BigDecimal credits,
            final Integer year, final String codesCSV) {

        final FlunkedCurricularCourses result = new FlunkedCurricularCourses();
        result.init(configuration, credits, year, codesCSV);
        return result;
    }

    private void init(final CurricularPeriodConfiguration configuration, final BigDecimal credits, final Integer year,
            final String codesCSV) {
        super.init(configuration, credits, year, year);
        setCodesCSV(codesCSV);
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        getCompetenceCourses();
    }

    private Set<CompetenceCourse> getCompetenceCourses() {
        if (this.competenceCourses == null) {
            loadCompetenceCourses();
        }

        return this.competenceCourses;
    }

    private synchronized void loadCompetenceCourses() {
        if (this.competenceCourses == null) {

            final Set<CompetenceCourse> loadedCompetenceCourses = Sets.newHashSet();
            if (StringUtils.isNotBlank(getCodesCSV())) {

                for (final String code : getCodesCSV().split(",")) {

                    final CompetenceCourse competenceCourse = CompetenceCourse.find(code);
                    if (competenceCourse == null) {
                        logger.warn("[{}], for DCP [{}]: unable to find Competence Course [{}]", this, getDegreeCurricularPlan()
                                .getPresentationName(), code);
                    } else {
                        loadedCompetenceCourses.add(competenceCourse);
                    }
                }
            }

            this.competenceCourses = loadedCompetenceCourses;
        }

    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getCredits().toString(),
                getYearMin().toString(), getCodesCSV().toString());
    }

    @Override
    public RuleResult execute(final Curriculum curriculum) {
        final Set<CompetenceCourse> toInspect = Sets.newHashSet();
        toInspect.addAll(getCompetenceCourses());

        for (final ICurriculumEntry entry : curriculum.getCurricularYearEntries()) {
            for (Iterator<CompetenceCourse> iterator = toInspect.iterator(); iterator.hasNext();) {
                final CompetenceCourse competenceCourse = iterator.next();
                if (StringUtils.equals(entry.getCode(), competenceCourse.getCode())) {
                    iterator.remove();
                    break;
                }
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (final CompetenceCourse competenceCourse : toInspect) {
            total = total.add(BigDecimal.valueOf(competenceCourse.getEctsCredits()));
        }

        return total.compareTo(getCredits()) <= 0 ? createTrue() : createFalseLabelled(total);
    }

}
