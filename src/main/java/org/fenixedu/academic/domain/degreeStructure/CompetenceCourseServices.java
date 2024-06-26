/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 *
 * 
 * This file is part of FenixEdu fenixedu-ulisboa-specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Specifications.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.degreeStructure;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

abstract public class CompetenceCourseServices {

    static final private Logger logger = LoggerFactory.getLogger(CompetenceCourseServices.class);

    static final private int CACHE_APPROVALS_MAX_SIZE = 200_000;
    static final private int CACHE_APPROVALS_EXPIRE_MIN = 10;

    static final private Map<StudentCurricularPlan, Cache<String, Boolean>> CACHE_APPROVALS =
            new ConcurrentHashMap<StudentCurricularPlan, Cache<String, Boolean>>();

    static final private Map<StudentCurricularPlan, Cache<Registration, Set<StudentCurricularPlan>>> CACHE_SCPS =
            new ConcurrentHashMap<StudentCurricularPlan, Cache<Registration, Set<StudentCurricularPlan>>>();

    static public boolean isCompetenceCourseApproved(final StudentCurricularPlan plan, final CurricularCourse course,
            final ExecutionInterval Interval) {

        final Registration registration = plan.getRegistration();
        final CompetenceCourse competence = course.getCompetenceCourse();

        // optional curricular course
        if (competence == null) {
            return plan.isApproved(course, Interval);
        }

        Cache<Registration, Set<StudentCurricularPlan>> cache = CACHE_SCPS.computeIfAbsent(plan,
                p -> CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(CACHE_APPROVALS_MAX_SIZE)
                        .expireAfterWrite(CACHE_APPROVALS_EXPIRE_MIN, TimeUnit.MINUTES).build());
        try {
            Set<StudentCurricularPlan> set = cache.get(registration, () -> getScpsToCheck(registration));
            return set.stream().anyMatch(i -> isApproved(i, competence, Interval));
        } catch (ExecutionException e) {
            logger.error(String.format("Unable to get Approvals [%s %s %s]", new DateTime(), registration.getExternalId(),
                    e.getLocalizedMessage()));
            return false;
        }
    }

    static public int countEnrolmentsUntil(final StudentCurricularPlan plan, final CurricularCourse curricularCourse,
            final ExecutionYear executionYear) {

        final Registration registration = plan.getRegistration();
        final CompetenceCourse competence = curricularCourse.getCompetenceCourse();

        final Predicate<Enrolment> validEnrolment =
                (e) -> !e.isAnnulled() && e.getExecutionYear().isBeforeOrEquals(executionYear);

        // optional curricular course
        if (competence == null) {
            return (int) plan.getEnrolments(curricularCourse).stream().filter(validEnrolment).count();
        }

        final Set<CurricularCourse> expandedCourses = getExpandedCurricularCourses(competence.getCode());
        int total = 0;
        for (final StudentCurricularPlan scpToCheck : getScpsToCheck(registration)) {
            for (final CurricularCourse expandedCourse : expandedCourses) {
                total += scpToCheck.getEnrolments(expandedCourse).stream().filter(validEnrolment).count();
            }
        }

        return total;
    }

    static private Set<StudentCurricularPlan> getScpsToCheck(final Registration registration) {
        final Set<StudentCurricularPlan> result = Sets.newHashSet();

        if (FenixEduAcademicExtensionsConfiguration.getConfiguration()
                .getCurricularRulesApprovalsAwareOfCompetenceCourseAtStudentScope()) {
            registration.getStudent().getRegistrationsSet().stream().flatMap(r -> r.getStudentCurricularPlansSet().stream())
                    .sequential().collect(Collectors.toCollection(() -> result));

        } else {
            result.addAll(registration.getStudentCurricularPlansSet());

        }

        return result;
    }

    static private boolean isApproved(final StudentCurricularPlan plan, final CompetenceCourse competence,
            final ExecutionInterval interval) {

        Cache<String, Boolean> scpCache = getApprovalCacheForSCP(plan);

        final String key = buildCourseApprovalCacheKey(competence, interval);

        try {
            return scpCache.get(key, new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    logger.debug(
                            String.format("Miss on Approvals cache [%s %s]", new DateTime(), plan.getExternalId() + "#" + key));
                    final Set<CurricularCourse> curriculars = getExpandedCurricularCourses(competence.getCode());
                    return curriculars == null ? false : curriculars.stream()
                            .anyMatch(curricular -> plan.isApproved(curricular, interval));
                }
            });

        } catch (final Throwable t) {
            logger.error(String.format("Unable to get Approvals [%s %s %s]", new DateTime(), plan.getExternalId() + "#" + key,
                    t.getLocalizedMessage()));
            return false;
        }
    }

    private static Cache<String, Boolean> getApprovalCacheForSCP(final StudentCurricularPlan plan) {
        Cache<String, Boolean> scpCache = CACHE_APPROVALS.computeIfAbsent(plan, p -> CacheBuilder.newBuilder().concurrencyLevel(4)
                .maximumSize(CACHE_APPROVALS_MAX_SIZE).expireAfterWrite(CACHE_APPROVALS_EXPIRE_MIN, TimeUnit.MINUTES).build());
        return scpCache;
    }

    private static String buildCourseApprovalCacheKey(final CompetenceCourse competence, final ExecutionInterval interval) {
        return competence.getExternalId() + "#" + (interval == null ? "null" : interval.getExternalId());
    }

    private static Map<String, Set<CurricularCourse>> CACHE_COMPETENCE_CURRICULARS = new ConcurrentHashMap<>();

    static public Set<CurricularCourse> getExpandedCurricularCourses(final String code) {
        final String key = filterCode(code);
        try {
            return CACHE_COMPETENCE_CURRICULARS.computeIfAbsent(key, k -> loadExpandedCurricularCourses(k));
        } catch (final Throwable t) {
            logger.error(String.format("Unable to get CompetenceCourse CurricularCourses [%s %s %s]", new DateTime(), key,
                    t.getLocalizedMessage()));
            return null;
        }
    }

    static private Set<CurricularCourse> loadExpandedCurricularCourses(final String key) {
        Set<CurricularCourse> result = Sets.newHashSet();

        final String code = filterCode(key);
        if (!Strings.isNullOrEmpty(code)) {

            final CompetenceCourse competence = CompetenceCourse.find(code);
            if (competence != null) {
                result.addAll(competence.getAssociatedCurricularCoursesSet());
            }

            for (final CompetenceCourse iter : Bennu.getInstance().getCompetenceCoursesSet()) {
                if (iter != competence && code.equals(filterCode(iter.getCode()))) {
                    result.addAll(iter.getAssociatedCurricularCoursesSet());
                }
            }
        }

        return result;
    }

    static private boolean isExpandedCode(final String input) {
        return !Strings.isNullOrEmpty(input) && input.endsWith("ects") && input.contains("_");
    }

    static private String filterCode(final String input) {
        return !isExpandedCode(input) ? input : input.substring(0, input.lastIndexOf("_"));
    }

    public static void invalidateCourseApprovalCache(final StudentCurricularPlan plan, final CurricularCourse curricularCourse,
            final ExecutionInterval interval) {
        if (curricularCourse == null || curricularCourse.getCompetenceCourse() == null) {
            return;
        }

        Cache<String, Boolean> approvalCacheForSCP = getApprovalCacheForSCP(plan);
        approvalCacheForSCP.invalidate(buildCourseApprovalCacheKey(curricularCourse.getCompetenceCourse(), interval));
    }

}
