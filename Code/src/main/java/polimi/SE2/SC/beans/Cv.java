package polimi.SE2.SC.beans;

import java.util.ArrayList;

public class Cv {
    private int id;
    private String filePath;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
