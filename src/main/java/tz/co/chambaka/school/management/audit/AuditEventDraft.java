package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;

public class AuditEventDraft {

    private AuditScope scope;
    private AuditAction action;
    private Long schoolId;
    private Long actorUserId;
    private String actorEmail;
    private String actorRole;
    private String resourceType;
    private String resourceId;
    private String summary;
    private String details;
    private String httpMethod;
    private String httpPath;
    private Integer statusCode;

    public AuditScope getScope() {
        return scope;
    }

    public AuditEventDraft scope(AuditScope scope) {
        this.scope = scope;
        return this;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditEventDraft action(AuditAction action) {
        this.action = action;
        return this;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public AuditEventDraft schoolId(Long schoolId) {
        this.schoolId = schoolId;
        return this;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public AuditEventDraft actorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
        return this;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public AuditEventDraft actorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
        return this;
    }

    public String getActorRole() {
        return actorRole;
    }

    public AuditEventDraft actorRole(String actorRole) {
        this.actorRole = actorRole;
        return this;
    }

    public String getResourceType() {
        return resourceType;
    }

    public AuditEventDraft resourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public String getResourceId() {
        return resourceId;
    }

    public AuditEventDraft resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public AuditEventDraft summary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public AuditEventDraft details(String details) {
        this.details = details;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public AuditEventDraft httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public String getHttpPath() {
        return httpPath;
    }

    public AuditEventDraft httpPath(String httpPath) {
        this.httpPath = httpPath;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public AuditEventDraft statusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }
}
