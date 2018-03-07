package org.fenixedu.academic.domain.evaluation.markSheet;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.person.services.PersonServices;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import pt.ist.fenixframework.Atomic;

public class CompetenceCourseMarkSheetChangeRequest extends CompetenceCourseMarkSheetChangeRequest_Base {

    static public Comparator<CompetenceCourseMarkSheetChangeRequest> COMPARATOR_BY_REQUEST_DATE =
            Comparator.comparing(CompetenceCourseMarkSheetChangeRequest::getRequestDate)
                    .thenComparing(CompetenceCourseMarkSheetChangeRequest::getComments,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CompetenceCourseMarkSheetChangeRequest::getExternalId);

    protected CompetenceCourseMarkSheetChangeRequest() {
        super();
        setState(CompetenceCourseMarkSheetChangeRequestStateEnum.PENDING);
        setRequestDate(new DateTime());
    }

    protected void init(final CompetenceCourseMarkSheet markSheet, final Person requester, final String reason) {

        super.setCompetenceCourseMarkSheet(markSheet);
        super.setRequester(requester);
        super.setReason(reason);

        checkRules();
    }

    public boolean isPending() {
        return CompetenceCourseMarkSheetChangeRequestStateEnum.findPending() == getState();
    }

    public boolean isAuthorized() {
        return CompetenceCourseMarkSheetChangeRequestStateEnum.findAuthorized() == getState();
    }

    public boolean isClosed() {
        return CompetenceCourseMarkSheetChangeRequestStateEnum.findClosed() == getState();
    }

    private void checkRules() {

        if (getState() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetChangeRequest.state.required");
        }

        if (getRequestDate() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetChangeRequest.requestDate.required");
        }

        if (getCompetenceCourseMarkSheet() == null) {
            throw new AcademicExtensionsDomainException(
                    "error.CompetenceCourseMarkSheetChangeRequest.competenceCourseMarkSheet.required");
        }

        if (getRequester() == null) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetChangeRequest.requester.required");
        }

        if (StringUtils.isBlank(getReason())) {
            throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetChangeRequest.reason.required");
        }

        if (!isPending()) {

            if (getResponder() == null) {
                throw new AcademicExtensionsDomainException("error.CompetenceCourseMarkSheetChangeRequest.responder.required");
            }

            if (getResponseDate() == null) {
                throw new AcademicExtensionsDomainException(
                        "error.CompetenceCourseMarkSheetChangeRequest.responseDate.required");
            }
        }

    }

    @Atomic
    static public CompetenceCourseMarkSheetChangeRequest create(final CompetenceCourseMarkSheet markSheet, final Person requester,
            final String reason) {
        final CompetenceCourseMarkSheetChangeRequest result = new CompetenceCourseMarkSheetChangeRequest();
        result.init(markSheet, requester, reason);

        return result;
    }

    @Atomic
    public void authorize(final Person responder, final String comments, final LocalDate markSheetExpireDate) {
        edit(responder, comments, new DateTime(), CompetenceCourseMarkSheetChangeRequestStateEnum.findAuthorized());
        getCompetenceCourseMarkSheet().editExpireDate(markSheetExpireDate);
        getCompetenceCourseMarkSheet().revertToEdition(false, comments);
        closeCascade();
    }

    @Atomic
    public void close(final Person responder, final String comments) {
        closeSelf(responder, comments);
        closeCascade();
    }

    private void closeSelf(final Person responder, final String comments) {
        edit(responder, comments, new DateTime(), CompetenceCourseMarkSheetChangeRequestStateEnum.findClosed());
    }

    @Atomic
    public void closeCascade() {
        if (!isPending()) {
            final String comments = getCommentForCascade();
            getCompetenceCourseMarkSheet().getPendingChangeRequests().forEach(i -> i.closeSelf(getResponder(), comments));
        }
    }

    private String getCommentForCascade() {
        String result = "";

        String key = "";
        if (isAuthorized()) {
            key = "info.CompetenceCourseMarkSheetChangeRequest.cascade.authorize";
        } else if (isClosed()) {
            key = "info.CompetenceCourseMarkSheetChangeRequest.cascade.close";
        }

        if (!key.isEmpty()) {
            final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            result = AcademicExtensionsUtil.bundle(key, getRequestDate().toString(formatter),
                    PersonServices.getDisplayName(getResponder()), getResponseDate().toString(formatter));
        }

        return result;
    }

    protected void edit(final Person responder, final String comments, final DateTime respondeDate,
            CompetenceCourseMarkSheetChangeRequestStateEnum state) {
        super.setResponder(responder);
        super.setComments(comments);
        super.setResponseDate(respondeDate);
        super.setState(state);

        checkRules();
    }

    public void delete() {

        super.setCompetenceCourseMarkSheet(null);
        super.setRequester(null);
        super.setResponder(null);

        super.deleteDomainObject();

    }

}
