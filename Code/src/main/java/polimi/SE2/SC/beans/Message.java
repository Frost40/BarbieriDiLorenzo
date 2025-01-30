package polimi.SE2.SC.beans;

public class Message {
    private int id;
    private String message;
    private boolean readByStudent;
    private boolean readByInternship;

    private boolean fromInternship;
    private int senderId;
    private int receiverId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public boolean isFromInternship() {
        return fromInternship;
    }

    public void setFromInternship(boolean fromInternship) {
        this.fromInternship = fromInternship;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
}
