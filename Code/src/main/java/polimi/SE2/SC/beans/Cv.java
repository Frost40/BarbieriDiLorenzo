package polimi.SE2.SC.beans;

import java.util.ArrayList;
import java.util.Base64;

public class Cv {
    private int id;
    private String file;
    private ArrayList<String> experiences;
    private ArrayList<String> programmingSkills;
    private ArrayList<String> otherSkills;
    private int studentId;

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

    public ArrayList<String> getExperiences() {
        return experiences;
    }

    public void setExperiences(ArrayList<String> experiences) {
        this.experiences = experiences;
    }

    public ArrayList<String> getProgrammingSkills() {
        return programmingSkills;
    }

    public void setProgrammingSkills(ArrayList<String> programmingSkills) {
        this.programmingSkills = programmingSkills;
    }

    public ArrayList<String> getOtherSkills() {
        return otherSkills;
    }

    public void setOtherSkills(ArrayList<String> otherSkills) {
        this.otherSkills = otherSkills;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }
}
