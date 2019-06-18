package org.fenixedu.academic.domain.degreeStructure;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodRule;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.RuleEnrolment;
import org.fenixedu.academic.domain.enrolment.EnroledEnrolmentWrapper;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.CurricularPeriodInfoDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CurricularPeriodServices {

    static private final Logger logger = LoggerFactory.getLogger(CurricularPeriodServices.class);

    static public CurricularPeriod getCurricularPeriod(final DegreeCurricularPlan dcp, final int year, final Integer semester) {
        final CurricularPeriod result;

        if (semester == null) {
            result = getCurricularPeriod(dcp, year);

        } else {
            result = dcp.getCurricularPeriodFor(year, semester);

            if (result == null) {
                logger.info("Unsupported Curricular Period [Y{},S{}], DCP [{}] ", year, semester, dcp.getPresentationName());
            }
        }

        return result;
    }

    static public CurricularPeriod getCurricularPeriod(final DegreeCurricularPlan dcp, final int year) {
        CurricularPeriod result = null;

        final CurricularPeriodInfoDTO dto = new CurricularPeriodInfoDTO(year, AcademicPeriod.YEAR);

        final CurricularPeriod degreeStructure = dcp.getDegreeStructure();
        if (degreeStructure != null) {

            // test for self, bug fix on CurricularPeriod.getCurricularPeriod(CurricularPeriodInfoDTO...)
            if (degreeStructure.getAbsoluteOrderOfChild() == year
                    && degreeStructure.getAcademicPeriod().equals(AcademicPeriod.YEAR)) {
                result = degreeStructure;
            } else {
                result = degreeStructure.getCurricularPeriod(dto);
            }
        }

        if (result == null) {
            logger.info("Unsupported Curricular Period [{},{}], DCP [{}]", dto.getPeriodType().getName(), dto.getOrder(),
                    dcp.getPresentationName());
        }

        return result;
    }

    static public CurricularPeriodConfiguration getCurricularPeriodConfiguration(final DegreeCurricularPlan dcp, final int year) {
        final CurricularPeriod curricularPeriod = getCurricularPeriod(dcp, year);
        return curricularPeriod == null ? null : curricularPeriod.getConfiguration();
    }

    private static int getCurricularYear(final IDegreeModuleToEvaluate input) {
        return input instanceof EnroledEnrolmentWrapper ? getCurricularYear(
                ((EnroledEnrolmentWrapper) input).getCurriculumModule()) : input.getContext().getCurricularYear();
    }

    //TODO: move to CurriculumLineServices
    static public int getCurricularYear(final CurriculumLine input) {

        // no course group placeholder takes precedence over everything else
        final String report = input.print(StringUtils.EMPTY).toString();
        if (input.getCurriculumGroup().isNoCourseGroupCurriculumGroup()) {
            logger.debug("NoCourseGroupCurriculumGroup as parent for [{}], returning 1", report);
            return 1;
        }

        final DegreeModule degreeModule = input instanceof OptionalEnrolment ? ((OptionalEnrolment) input)
                .getOptionalCurricularCourse() : input.getDegreeModule();
        final ExecutionYear executionYear = input.getExecutionYear();
        final Set<Context> contexts = input.getCurriculumGroup().getDegreeModule().getChildContextsSet();

        final Integer calculated = getCurricularYearCalculated(report, degreeModule, executionYear, contexts);
        if (calculated != null) {
            return calculated;

        } else {

            // migrated overriden value must only be used as a last resort
            final Integer overridenCurricularYear = CurriculumLineServices.getCurricularYear(input);
            if (overridenCurricularYear == null) {
                logger.warn("Unable to guess curricular year for [{}], returning 1", report.toString().replace("\n", ""));
                return 1;

            } else {
                return overridenCurricularYear.intValue();
            }
        }
    }

    static final private Cache<String, Integer> CACHE_DEGREE_MODULE_CURRICULAR_YEAR =
            CacheBuilder.newBuilder().concurrencyLevel(8).maximumSize(3000).expireAfterWrite(1, TimeUnit.DAYS).build();

    /**
     * Assume lowest curricular year of the degree module's contexts on the parent group
     */
    static private Integer getCurricularYearCalculated(final String report, final DegreeModule degreeModule,
            final ExecutionYear executionYear, final Set<Context> contexts) {

        final String key = String.format("%s#%s#%s", degreeModule == null ? "null" : degreeModule.getExternalId(),
                executionYear == null ? "null" : executionYear.getExternalId(),
                contexts.stream().map(i -> i.getExternalId()).collect(Collectors.joining(";")));

        try {
            return CACHE_DEGREE_MODULE_CURRICULAR_YEAR.get(key, new Callable<Integer>() {
                @Override
                public Integer call() {
                    logger.debug(String.format("Miss on DegreeModule CurricularYear cache [%s %s]", new DateTime(), key));
                    return loadCurricularYearCalculated(report, degreeModule, executionYear, contexts).orElse(null);
                }
            });

        } catch (final Throwable t) {
            logger.debug(String.format("Unable to get DegreeModule CurricularYear [%s %s %s]", new DateTime(), key,
                    t.getLocalizedMessage()));
            return null;
        }
    }

    static private Optional<Integer> loadCurricularYearCalculated(final String report, final DegreeModule degreeModule,
            final ExecutionYear executionYear, final Set<Context> contexts) {

        // best scenario, we want to assert execution year on context
        final List<Integer> curricularYearsValids = Lists.newArrayList();

        // last resort, we'll forget execution year on checking contexts
        final List<Integer> curricularYears = Lists.newArrayList();

        for (final Context context : contexts) {

            if (!context.getChildDegreeModule().isLeaf()) {
                continue;
            }

            final Integer curricularYear = context.getCurricularYear();

            if (degreeModule == null || context.getChildDegreeModule() == degreeModule) {
                curricularYears.add(curricularYear);

                if (context.isValid(executionYear)) {
                    curricularYearsValids.add(curricularYear);
                }
            }
        }

        if (!curricularYearsValids.isEmpty()) {
            return Optional.of(Collections.min(curricularYearsValids));

        } else if (!curricularYears.isEmpty()) {
            return Optional.of(Collections.min(curricularYears));

        } else {
            return Optional.empty();
        }
    }

    static public Map<CurricularPeriod, BigDecimal> mapYearCredits(final ICurriculum curriculum) {
        return mapYearCredits(curriculum, (Boolean) null);
    }

    static public Map<CurricularPeriod, BigDecimal> mapYearCredits(final ICurriculum curriculum, final Boolean applyToOptionals) {

        final Map<CurricularPeriod, BigDecimal> result = Maps.newHashMap();

        final DegreeCurricularPlan dcp = curriculum.getStudentCurricularPlan().getDegreeCurricularPlan();

        for (final ICurriculumEntry iter : curriculum.getCurricularYearEntries()) {

            final CurriculumLine curriculumLine = (CurriculumLine) iter;
            if (applyToOptionals != null) {
                final boolean isOptionalByGroup = CurriculumLineServices.isOptionalByGroup(curriculumLine);
                if ((applyToOptionals && !isOptionalByGroup) || (!applyToOptionals && isOptionalByGroup)) {
                    continue;
                }
            }

            final int year = getCurricularYear(curriculumLine);
            final CurricularPeriod curricularPeriod = getCurricularPeriod(dcp, year);

            if (curricularPeriod != null) {

                final BigDecimal credits = iter.getEctsCreditsForCurriculum();

                addYearCredits(result, curricularPeriod, credits, iter.getCode());
            }
        }

        mapYearCreditsLogger(result);
        return result;
    }

    static public Map<CurricularPeriod, BigDecimal> mapYearCredits(final EnrolmentContext enrolmentContext,
            final Boolean applyToOptionals, final ExecutionSemester semester) {

        final Map<CurricularPeriod, BigDecimal> result = Maps.newHashMap();

        final DegreeCurricularPlan dcp = enrolmentContext.getStudentCurricularPlan().getDegreeCurricularPlan();

        for (final IDegreeModuleToEvaluate iter : RuleEnrolment.getEnroledAndEnroling(enrolmentContext)) {

            final DegreeModule degreeModule = iter.getDegreeModule();
            if (applyToOptionals != null) {
                final boolean isOptionalByGroup = iter.getCurriculumGroup().getDegreeModule().isOptionalCourseGroup();
                if ((applyToOptionals && !isOptionalByGroup) || (!applyToOptionals && isOptionalByGroup)) {
                    continue;
                }
            }

            final int year = getCurricularYear(iter);
            final CurricularPeriod curricularPeriod =
                    getCurricularPeriod(dcp, year, semester == null ? null : semester.getSemester());

            if (curricularPeriod != null) {

                final BigDecimal credits =
                        BigDecimal.valueOf(semester != null ? iter.getAccumulatedEctsCredits(semester) : iter.getEctsCredits());

                final String code = degreeModule == null ? "Opt" : degreeModule.getCode();
                addYearCredits(result, curricularPeriod, credits, code);
            }
        }

        mapYearCreditsLogger(result);
        return result;
    }

    static public void mapYearCreditsLogger(final Map<CurricularPeriod, BigDecimal> input) {
        for (final Map.Entry<CurricularPeriod, BigDecimal> entry : input.entrySet()) {
            CurricularPeriodRule.logger.debug("{}#{} ECTS", entry.getKey().getFullLabel(), entry.getValue().toPlainString());
        }
    }

    static public void addYearCredits(final Map<CurricularPeriod, BigDecimal> result, final CurricularPeriod curricularPeriod,
            final BigDecimal credits, final String code) {

        final BigDecimal creditsYear = result.get(curricularPeriod);
        result.put(curricularPeriod, creditsYear != null ? creditsYear.add(credits) : credits);
        CurricularPeriodRule.logger.debug("{}#UC {}#{} ECTS", code, curricularPeriod.getFullLabel(), credits.toPlainString());
    }

    //TODO: move to CurriculumLineServices
    static public int getCurricularSemester(final CurriculumLine curriculumLine) {

        if (curriculumLine.isEnrolment()) {
            return curriculumLine.getExecutionPeriod().getSemester();
        }

        final Collection<Context> contexts = CurriculumLineServices.getParentContexts(curriculumLine);
        return contexts.size() == 1 ? contexts.iterator().next().getCurricularPeriod().getChildOrder() : curriculumLine
                .getExecutionPeriod().getSemester();

    }

}
