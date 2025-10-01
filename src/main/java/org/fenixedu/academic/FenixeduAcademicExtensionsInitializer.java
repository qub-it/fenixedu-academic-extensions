package org.fenixedu.academic;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.DegreeInfo;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationConfiguration;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Qualification;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.AnyCurricularCourseExceptionsInitializer;
import org.fenixedu.academic.domain.curricularRules.EnrolmentPeriodRestrictionsInitializer;
import org.fenixedu.academic.domain.curricularRules.StudentScheduleListeners;
import org.fenixedu.academic.domain.curricularRules.UnavailableForEnrolmentRule;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleConfigurationInitializer;
import org.fenixedu.academic.domain.degree.ExtendedDegreeInfo;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.EnrolmentServices;
import org.fenixedu.academic.domain.evaluation.EnrolmentEvaluationExtendedInformation;
import org.fenixedu.academic.domain.evaluation.EvaluationComparator;
import org.fenixedu.academic.domain.evaluation.config.MarkSheetSettings;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYearExtendedInformation;
import org.fenixedu.academic.domain.student.RegistrationExtendedInformation;
import org.fenixedu.academic.domain.student.RegistrationObservations;
import org.fenixedu.academic.domain.student.RegistrationRegimeVerifierInitializer;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineExtendedInformation;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.EctsAndWeightProviders;
import org.fenixedu.academic.domain.student.curriculum.conclusion.ConclusionProcessListenersInitializer;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DeletionListener;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

@WebListener
public class FenixeduAcademicExtensionsInitializer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(FenixeduAcademicExtensionsInitializer.class);

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
    }

    @Atomic(mode = TxMode.SPECULATIVE_READ)
    @Override
    public void contextInitialized(final ServletContextEvent event) {

        CurriculumLineServices.initialize();
        RegistrationServices.initialize();
        EnrolmentServices.initialize();

        CurricularRuleConfigurationInitializer.init();
        AnyCurricularCourseExceptionsInitializer.init();
        ConclusionProcessListenersInitializer.init();
        CurriculumConfigurationInitializer.init();
        RegistrationRegimeVerifierInitializer.init();

        RegistrationExtendedInformation.setupDeleteListener();
        RegistrationDataByExecutionYearExtendedInformation.setupDeleteListener();
        EctsAndWeightProviders.init();

        setupListenersForStudentSchedule();

        CurriculumLineExtendedInformation.setupDeleteListener();

        RegistrationObservations.setupDeleteListener();

        ExtendedDegreeInfo.setupDeleteListener();
        ExtendedDegreeInfo.setupCreationListener();

        EnrolmentEvaluationExtendedInformation.setupDeleteListener();

        MarkSheetSettings.init();

        EnrolmentPeriodRestrictionsInitializer.init();

        EvaluationSeasonServices.initialize();
        EvaluationSeasonServices.setEnrolmentsInEvaluationsDependOnAcademicalActsBlocked(FenixEduAcademicExtensionsConfiguration
                .getConfiguration().getEnrolmentsInEvaluationsDependOnAcademicalActsBlocked());

        EvaluationConfiguration.setEnrolmentEvaluationOrder(new EvaluationComparator());

        setupListenerForCurricularPeriodDelete();
        setupListenerForEnrolmentDelete();
        setupListenerForSchoolClassDelete();
        setupListenerForInvalidEquivalences();

        registerDeletionListenerOnEnrolmentEvaluation();
        registerDeletionListenerOnCurriculumLineForCourseGradingTable();
        registerDeletionListenerOnDegreeModuleForCurriculumLineLogs();

        registerDeletionListenerOnQualification();

        registerDeletionListenerOnUnit();

        registerDeletionListenersForDynamicFields();

        UnavailableForEnrolmentRule.initializeDomainListenersAndExtensions();

        MergeExecutionCourses.registerMergeHandler(FenixeduAcademicExtensionsInitializer::mergeExecutionCoursesMarksheets);
    }

    private void setupListenersForStudentSchedule() {
        Signal.register(Enrolment.SIGNAL_CREATED, StudentScheduleListeners.SHIFTS_ENROLLER);
    }

    private void setupListenerForCurricularPeriodDelete() {
        FenixFramework.getDomainModel().registerDeletionListener(CurricularPeriod.class,
                (final CurricularPeriod curricularPeriod) -> {
                    if (curricularPeriod.getConfiguration() != null) {
                        curricularPeriod.getConfiguration().delete();
                    }
                });
    }

    private void setupListenerForEnrolmentDelete() {
        Attends.getRelationAttendsEnrolment().addListener(new RelationAdapter<Enrolment, Attends>() {
            @Override
            public void beforeRemove(final Enrolment enrolment, final Attends attends) {
                final Registration registration = attends.getRegistration();
                if (registration != null) {
                    registration.getShiftsFor(attends.getExecutionCourse()).forEach(s -> s.unenrol(registration));
                }
            }
        });
    }

    private void setupListenerForSchoolClassDelete() {
        FenixFramework.getDomainModel().registerDeletionListener(SchoolClass.class, new DeletionListener<SchoolClass>() {

            @Override
            public void deleting(final SchoolClass schoolClass) {
                schoolClass.getRegistrationsSet().clear();
                schoolClass.setNextSchoolClass(null);
                schoolClass.getPreviousSchoolClassesSet().clear();
            }
        });
    }

    private void setupListenerForInvalidEquivalences() {
        Dismissal.getRelationCreditsDismissalEquivalence().addListener(new RelationAdapter<Dismissal, Credits>() {
            @Override
            public void beforeAdd(final Dismissal dismissal, final Credits credits) {
                if (credits != null && dismissal != null && (dismissal.isCreditsDismissal() || dismissal.isOptional())
                        && credits.isEquivalence() && FenixEduAcademicExtensionsConfiguration.getConfiguration()
                                .getRestrictEquivalencesToCurricularCourses()) {
                    throw new DomainException("error.Equivalence.can.only.be.applied.to.curricular.courses");

                }
            }
        });
    }

    private void registerDeletionListenerOnEnrolmentEvaluation() {
        FenixFramework.getDomainModel().registerDeletionListener(EnrolmentEvaluation.class, enrolmentEvaluation -> {
            if (enrolmentEvaluation.getCompetenceCourseMarkSheet() != null) {
                throw new RuntimeException(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                        "error.unenrolment.not.possible.student.already.in.marksheet"));
            }
        });
    }

    private void registerDeletionListenerOnCurriculumLineForCourseGradingTable() {
        FenixFramework.getDomainModel().registerDeletionListener(CurriculumLine.class, new DeletionListener<CurriculumLine>() {

            @Override
            public void deleting(final CurriculumLine line) {
                if (line.getCourseGradingTable() != null) {
                    line.getCourseGradingTable().delete();
                }
            }
        });
    }

    private void registerDeletionListenerOnDegreeModuleForCurriculumLineLogs() {
        FenixFramework.getDomainModel().registerDeletionListener(DegreeModule.class, dm -> {

            dm.getCurriculumLineLogsSet().forEach(log -> log.delete());

            if (dm instanceof OptionalCurricularCourse) {
                final OptionalCurricularCourse optionalCurricularCourse = (OptionalCurricularCourse) dm;
                optionalCurricularCourse.getOptionalEnrolmentLogsSet().forEach(log -> log.delete());
            }
        });

    }

    private void registerDeletionListenerOnQualification() {
        FenixFramework.getDomainModel().registerDeletionListener(Qualification.class, q -> {
            q.getAcademicAreasSet().clear();
            q.getQualificationTypesSet().clear();
            q.setDegreeUnit(null);
            q.setInstitutionUnit(null);
            q.setLevel(null);
        });
    }

    private void registerDeletionListenerOnUnit() {
        FenixFramework.getDomainModel().registerDeletionListener(Unit.class, u -> {
            u.getAcademicAreasSet().clear();
            u.setMarkSheetSettings(null);
        });
    }

    private void registerDeletionListenersForDynamicFields() {

        FenixFramework.getDomainModel().registerDeletionListener(CompetenceCourseInformation.class,
                cci -> cci.getDynamicFieldSet().forEach(df -> {
                    df.setCompetenceCourseInformation(null);
                    df.delete();
                }));

        FenixFramework.getDomainModel().registerDeletionListener(DegreeInfo.class, di -> di.getDynamicFieldSet().forEach(df -> {
            df.setDegreeInfo(null);
            df.delete();
        }));

        FenixFramework.getDomainModel().registerDeletionListener(Party.class, p -> p.getDynamicFieldSet().forEach(df -> {
            df.setParty(null);
            df.delete();
        }));

    }

    private static void mergeExecutionCoursesMarksheets(final ExecutionCourse from, final ExecutionCourse to) {
        to.getCompetenceCourseMarkSheetSet().addAll(from.getCompetenceCourseMarkSheetSet());
        from.getCompetenceCourseMarkSheetSet().clear();
    }

    @SuppressWarnings("unchecked")
    static public <T> T loadClass(final String key, final String value) {
        T result = null;

        try {

            if (StringUtils.isNotBlank(value)) {
                result = (T) Class.forName(value).newInstance();
            } else {

                final String message = "Property [" + key + "] must be defined in configuration file";
                if (CoreConfiguration.getConfiguration().developmentMode()) {
                    logger.error("{}. Empty value may lead to wrong system behaviour", message);
                } else {
                    throw new RuntimeException(message);
                }
            }

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("An error occured loading class: " + value, e);
        }

        if (result != null) {
            logger.debug("Using " + result.getClass().getSimpleName());
        }

        return result;
    }

}
