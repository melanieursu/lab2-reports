package ru.itmo.TolstovaUrsu.domain;

import java.time.Instant;

public final class Report {

    private long id;
    private String name;
    private long sampleId;
    private long experimentId;
    private ReportStatus status;
    private String ownerUsername;
    private String signedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Report(long id, String name, long sampleId, long experimentId,
                  String ownerUsername) {
        this.id = id;
        this.name = name;
        this.sampleId = sampleId;
        this.experimentId = experimentId;
        this.status = ReportStatus.DRAFT;
        this.ownerUsername = ownerUsername;
        this.signedBy = null;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }

    public long getSampleId() { return sampleId; }
    public void setSampleId(long sampleId) { this.sampleId = sampleId; this.updatedAt = Instant.now(); }

    public long getExperimentId() { return experimentId; }
    public void setExperimentId(long experimentId) { this.experimentId = experimentId; this.updatedAt = Instant.now(); }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; this.updatedAt = Instant.now(); }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; this.updatedAt = Instant.now(); }

    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Report{id=" + id + ", name='" + name + "', status=" + status + "}";
    }
}
