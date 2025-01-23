package polimi.SE2.SC.beans;

import java.util.Date;

public class StudentHasInternship {
    private int id;
    private Date startDate;
    private Date endDate;
    private boolean companyAccepted;
    private String applicationStatus;
    private int studentId;
    private int internshipId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isCompanyAccepted() {
        return companyAccepted;
    }

    public void setCompanyAccepted(boolean companyAccepted) {
        this.companyAccepted = companyAccepted;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getInternshipId() {
        return internshipId;
    }

    public void setInternshipId(int internshipId) {
        this.internshipId = internshipId;
    }
}
