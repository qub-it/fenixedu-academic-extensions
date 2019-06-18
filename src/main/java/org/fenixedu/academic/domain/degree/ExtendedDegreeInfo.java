package org.fenixedu.academic.domain.degree;

import java.util.Objects;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeInfo;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ExtendedDegreeInfo extends ExtendedDegreeInfo_Base {

    public static void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(DegreeInfo.class, degreeInfo -> {
            ExtendedDegreeInfo edi = degreeInfo.getExtendedDegreeInfo();
            degreeInfo.setExtendedDegreeInfo(null);
            if (edi != null) {
                edi.delete();
            }
        });
    }

    public static void setupCreationListener() {
        Signal.register(DegreeInfo.DEGREE_INFO_CREATION_EVENT, (final DomainObjectEvent<DegreeInfo> event) -> {
            DegreeInfo degreeInfo = event.getInstance();
            if (degreeInfo.getExtendedDegreeInfo() != null) {
                return; // @diogo-simoes 22MAR2016 // Only apply for new DegreeInfos created outside the ExtendedDegreeInfo scope
            }
            final ExtendedDegreeInfo mostRecent = findMostRecent(degreeInfo.getExecutionYear(), degreeInfo.getDegree());
            if (mostRecent != null) {
                new ExtendedDegreeInfo(degreeInfo, mostRecent);
            } else {
                new ExtendedDegreeInfo(degreeInfo);
            }
        });
    }

    public ExtendedDegreeInfo() {
        super();
        setBennu(Bennu.getInstance());
    }

    public ExtendedDegreeInfo(final DegreeInfo degreeInfo) {
        this();
        setDegreeInfo(degreeInfo);
    }

    public ExtendedDegreeInfo(final DegreeInfo degreeInfo, final ExtendedDegreeInfo olderEdi) {
        this(degreeInfo);

        setScientificAreas(olderEdi.getScientificAreas());
        setStudyRegime(olderEdi.getStudyRegime());
        setStudyProgrammeDuration(olderEdi.getStudyProgrammeDuration());
        setStudyProgrammeRequirements(olderEdi.getStudyProgrammeRequirements());
        setHigherEducationAccess(olderEdi.getHigherEducationAccess());
        setProfessionalStatus(olderEdi.getProfessionalStatus());
        setSupplementExtraInformation(olderEdi.getSupplementExtraInformation());
        setSupplementOtherSources(olderEdi.getSupplementOtherSources());
    }

    public void delete() {
        setDegreeInfo(null);
        setBennu(null);
        deleteDomainObject();
    }

    /**
     * @return The ExtendedDegreeInfo instance associated with the MOST recent DegreeInfo.
     */
    @Atomic
    public static ExtendedDegreeInfo getMostRecent(final ExecutionYear executionYear, final Degree degree) {
        DegreeInfo di = degree.getMostRecentDegreeInfo(executionYear);
        if (di.getExtendedDegreeInfo() == null) {
            final ExtendedDegreeInfo mostRecent = findMostRecent(executionYear, degree);
            return mostRecent != null ? new ExtendedDegreeInfo(di, mostRecent) : new ExtendedDegreeInfo(di);
        }
        return di.getExtendedDegreeInfo();
    }

    /**
     * @return The ExtendedDegreeInfo instance associated with a DegreeInfo instance specific to the specified ExecutionYear. If
     *         no such DegreeInfo exists, a new one is created by cloning the most recent DegreeInfo. The same process is applied
     *         to the ExtendedDegreeInfo.
     */
    @Atomic
    public static ExtendedDegreeInfo getOrCreate(final ExecutionYear executionYear, final Degree degree) {
        DegreeInfo di = degree.getDegreeInfoFor(executionYear);
        if (di == null) {
            DegreeInfo mrdi = degree.getMostRecentDegreeInfo(executionYear);
            di = mrdi != null ? new DegreeInfo(mrdi, executionYear) : new DegreeInfo(degree, executionYear);
        }
        if (di.getExtendedDegreeInfo() == null) {
            final ExtendedDegreeInfo mostRecent = findMostRecent(executionYear, degree);
            return mostRecent != null ? new ExtendedDegreeInfo(di, mostRecent) : new ExtendedDegreeInfo(di);
        }
        return di.getExtendedDegreeInfo();
    }

    public static ExtendedDegreeInfo findMostRecent(final ExecutionYear executionYear, final Degree degree) {
        return degree.getDegreeInfosSet().stream().filter(di -> di.getExecutionYear().isBeforeOrEquals(executionYear))
                .sorted((di1, di2) -> ExecutionYear.REVERSE_COMPARATOR_BY_YEAR.compare(di1.getExecutionYear(),
                        di2.getExecutionYear()))
                .map(di -> di.getExtendedDegreeInfo()).filter(Objects::nonNull).findFirst().orElse(null);
    }

}
