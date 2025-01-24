package polimi.SE2.SC.beans;

public class Feedback extends Communication{
    private boolean read;
    private int rating;

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
