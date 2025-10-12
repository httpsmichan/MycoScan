package com.example.myapplication;

public class Report {
    private String postId;
    private String reportedBy;
    private long timestamp;
    private String reason;

    public Report() {}

    public Report(String postId, String reportedBy, long timestamp, String reason) {
        this.postId = postId;
        this.reportedBy = reportedBy;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public String getPostId() { return postId; }
    public String getReportedBy() { return reportedBy; }
    public long getTimestamp() { return timestamp; }
    public String getReason() { return reason; }

    public void setPostId(String postId) { this.postId = postId; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setReason(String reason) { this.reason = reason; }
}
