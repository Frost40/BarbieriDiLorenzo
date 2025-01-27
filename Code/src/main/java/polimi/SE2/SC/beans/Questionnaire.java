package polimi.SE2.SC.beans;

import java.util.Base64;

public class Questionnaire {
    private int id;
    private String file;
    private boolean evaluation;
    private int studentHasInternshipId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file != null ? Base64.getEncoder().encodeToString(file) : null;
    }

    public boolean isEvaluation() {
        return evaluation;
    }

    public void setEvaluation(boolean evaluation) {
        this.evaluation = evaluation;
    }

    public int getStudentHasInternshipId() {
        return studentHasInternshipId;
    }

    public void setStudentHasInternshipId(int studentHasInternshipId) {
        this.studentHasInternshipId = studentHasInternshipId;
    }
}
