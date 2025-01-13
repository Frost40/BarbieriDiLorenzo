package polimi.SE2.SC.beans;

import java.util.ArrayList;

public class Company extends User{
    private String mainOffice;
    private ArrayList<String> offices;

    public String getMainOffice() {
        return mainOffice;
    }

    public void setMainOffice(String mainOffice) {
        this.mainOffice = mainOffice;
    }

    public ArrayList<String> getOffices() {
        return offices;
    }

    public void setOffices(ArrayList<String> offices) {
        this.offices = offices;
    }
}
