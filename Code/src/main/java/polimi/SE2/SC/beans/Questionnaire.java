package polimi.SE2.SC.beans;

import java.util.Base64;

public class Questionnaire {
    private int id;
    private String file;
    private String fileType;
    private boolean completed;
    private Boolean evaluation;
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

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Boolean isEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Boolean evaluation) {
        this.evaluation = evaluation;
    }

    public int getStudentHasInternshipId() {
        return studentHasInternshipId;
    }

    public void setStudentHasInternshipId(int studentHasInternshipId) {
        this.studentHasInternshipId = studentHasInternshipId;
    }
}
