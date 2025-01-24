package polimi.SE2.SC.beans;

public class Complaint extends Communication{
    private boolean readByStudent;
    private boolean readByInternship;
    private boolean closedByStudent;
    private boolean closedByCompany;

    public boolean isReadByStudent() {
        return readByStudent;
    }

    public void setReadByStudent(boolean readByStudent) {
        this.readByStudent = readByStudent;
    }

    public boolean isReadByInternship() {
        return readByInternship;
    }

    public void setReadByInternship(boolean readByInternship) {
        this.readByInternship = readByInternship;
    }

    public boolean isClosedByStudent() {
        return closedByStudent;
    }

    public void setClosedByStudent(boolean closedByStudent) {
        this.closedByStudent = closedByStudent;
    }

    public boolean isClosedByCompany() {
        return closedByCompany;
    }

    public void setClosedByCompany(boolean closedByCompany) {
        this.closedByCompany = closedByCompany;
    }
}
