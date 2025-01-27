package polimi.SE2.SC.beans;

import java.util.Date;

public class InterviewSchedule {
    private int id;
    private Date date;
    private boolean evaluation;
    private boolean hasBeenRejected;
    private int studentHasInternshipId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isEvaluation() {
        return evaluation;
    }

    public void setEvaluation(boolean evaluation) {
        this.evaluation = evaluation;
    }

    public boolean isHasBeenRejected() {
        return hasBeenRejected;
    }

    public void setHasBeenRejected(boolean hasBeenRejected) {
        this.hasBeenRejected = hasBeenRejected;
    }

    public int getStudentHasInternshipId() {
        return studentHasInternshipId;
    }

    public void setStudentHasInternshipId(int studentHasInternshipId) {
        this.studentHasInternshipId = studentHasInternshipId;
    }
}
