package polimi.SE2.SC.beans;

public class Complaint extends Communication{
    private boolean closedByStudent;
    private boolean closedByCompany;

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
