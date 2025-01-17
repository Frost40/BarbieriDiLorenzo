package polimi.SE2.SC.beans;

public abstract class Communication {
    private int id;
    private String text;
    private boolean read;
    private int studentHasInternshipId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public int getStudentHasInternshipId() {
        return studentHasInternshipId;
    }

    public void setStudentHasInternshipId(int studentHasInternshipId) {
        this.studentHasInternshipId = studentHasInternshipId;
    }
}
