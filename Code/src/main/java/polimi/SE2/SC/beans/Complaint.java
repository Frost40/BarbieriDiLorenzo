package polimi.SE2.SC.beans;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Complaint complaint = (Complaint) o;
        return readByStudent == complaint.readByStudent &&
                readByInternship == complaint.readByInternship &&
                closedByStudent == complaint.closedByStudent &&
                closedByCompany == complaint.closedByCompany;
    }

    @Override
    public int hashCode() {
        return Objects.hash(readByStudent, readByInternship, closedByStudent, closedByCompany);
    }
}
