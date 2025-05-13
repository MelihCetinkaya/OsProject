public class Toll {

    private final int tollId;
    private final TollSide tollSide;
    private TollStatus tollStatus; // maybe boolean


    public Toll(int tollId, TollSide tollSide, TollStatus tollStatus) {
        this.tollId = tollId;
        this.tollSide = tollSide;
        this.tollStatus = tollStatus;
    }



    public TollSide getTollSide() {
        return tollSide;
    }

    public TollStatus getTollStatus() {
        return tollStatus;
    }

    public void setTollStatus(TollStatus tollStatus) {
        this.tollStatus = tollStatus;
    }


    public enum TollSide{
        leftSide,rightSide
    }

    public enum TollStatus{
        free,full
    }

}
