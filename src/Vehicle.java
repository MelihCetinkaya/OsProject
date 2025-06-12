import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Vehicle implements Runnable {

    private final int id;
    private int vehicleCapacity;
    private final VehicleType vehicleType;
    private StartingSide startingSide;
    private Square square = Square.idle;
    private int visitingCount = 0;
    private Boolean isLoaded = false;
    private Boolean isChanged = false;

    public static final Object ferryLock = new Object();
    public static final Object leftTollLock1 = new Object();
    public static final Object rightTollLock1 = new Object();
    public static final Object leftTollLock2 = new Object();
    public static final Object rightTollLock2 = new Object();
    public static final Object moveLock = new Object();

    private static Toll leftToll1 = new Toll(1, Toll.TollSide.leftSide, Toll.TollStatus.free);
    private static Toll leftToll2 = new Toll(2, Toll.TollSide.leftSide, Toll.TollStatus.free);
    private static Toll rightToll1 = new Toll(3, Toll.TollSide.rightSide, Toll.TollStatus.free);
    private static Toll rightToll2 = new Toll(4, Toll.TollSide.rightSide, Toll.TollStatus.free);

    private static ConcurrentHashMap<Integer, Vehicle> vehicleMap = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<Integer, Vehicle> rightVehicle = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Vehicle> leftVehicle = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Vehicle> waitingRight = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Vehicle> waitingLeft = new ConcurrentHashMap<>();

    private static final int ferryIndex = new Random().nextInt(Vehicle.StartingSide.values().length-1);
    private static final Vehicle.StartingSide randomFerrySide = Vehicle.StartingSide.values()[ferryIndex];
    private static volatile Vehicle ferry = new Vehicle(100,randomFerrySide, Vehicle.VehicleType.ferry);

    public Vehicle(int id, StartingSide startingSide, VehicleType vehicleType) {
        this.id = id;
        this.startingSide = startingSide;
        this.vehicleType = vehicleType;
        switch (vehicleType) {
            case car:
                this.vehicleCapacity = 1;
                break;
            case minibus:
                this.vehicleCapacity = 2;
                break;
            case truck:
                this.vehicleCapacity = 3;
                break;
            case ferry:
                this.vehicleCapacity = 0;
                break;
        }
    }

    public enum VehicleType{
        car,minibus,truck,ferry
    }

    public enum Square{
        rightSquare,leftSquare,idle
    }

    public enum StartingSide{
        leftSide, rightSide,idle
    }

    public void setStartingSide(StartingSide startingSide) {
        this.startingSide = startingSide;
    }

    @Override
    public void run() {

        if(startingSide == StartingSide.leftSide) {
            leftVehicle.put(id,Main.vehicleList.get(id));
        }
        else {
            rightVehicle.put(id,Main.vehicleList.get(id));
        }

        while (visitingCount < 2) {
            isChanged = false;

            if (checkTolls()) {
                System.out.println( id + " " + vehicleType + " passed through " + startingSide + " toll waiting for ferry");

                while (isChanged == false) {

                    try {
                        if(loadToFerry()==false){
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public Boolean checkTolls() {

            if (this.startingSide == Vehicle.StartingSide.leftSide && leftToll1.getTollStatus() == Toll.TollStatus.free
                    || leftToll2.getTollStatus() == Toll.TollStatus.free) {

                synchronized (leftTollLock1) {
                    if (leftToll1.getTollStatus() == Toll.TollStatus.free) {
                        leftToll1.setTollStatus(Toll.TollStatus.free);
                  /* try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }*/
                        leftToll1.setTollStatus(Toll.TollStatus.free);
                        square = Square.leftSquare;
                    }}synchronized (leftTollLock2) {
                    if (leftToll2.getTollStatus() == Toll.TollStatus.free) {
                        leftToll2.setTollStatus(Toll.TollStatus.full);
                 /*  try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }*/
                        leftToll2.setTollStatus(Toll.TollStatus.free);
                        square = Square.leftSquare;
                    }
                }
                    return true;

            }

          else  if (startingSide == Vehicle.StartingSide.rightSide && rightToll1.getTollStatus() == Toll.TollStatus.free
                    || rightToll2.getTollStatus() == Toll.TollStatus.free) {

              synchronized (rightTollLock1){ if (rightToll1.getTollStatus() == Toll.TollStatus.free) {
                    rightToll1.setTollStatus(Toll.TollStatus.full);
                    /*try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }*/
                    rightToll1.setTollStatus(Toll.TollStatus.free);
                    square = Square.rightSquare;
                } }
               synchronized (rightTollLock2){ if(rightToll2.getTollStatus() == Toll.TollStatus.free) {
                    rightToll2.setTollStatus(Toll.TollStatus.full);
                    /*try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }*/
                    rightToll2.setTollStatus(Toll.TollStatus.free);
                    square = Square.rightSquare;
                }
            }
                return true;
          }else {
                return false;
            }
        }

    public Boolean loadToFerry() throws InterruptedException {

        synchronized (ferryLock) {
            if (isLoaded == true) {
                //  System.out.println(id + " already loaded to ferry");
                moveFerry();
                return true;

            } else if (ferry.startingSide == this.startingSide && ferry.vehicleCapacity + vehicleCapacity <= 20
                    && visitingCount < 2 ) {

                ferry.vehicleCapacity += vehicleCapacity;
                if (startingSide == StartingSide.leftSide) {

                    leftVehicle.remove(id);
                } else {

                    rightVehicle.remove(id);
                }

                vehicleMap.put(id, Main.vehicleList.get(id));
                isLoaded = true;
                square = Square.idle;
                System.out.println("vehicle " + id + " " + vehicleType + " loaded to ferry , now ferry capacity " + ferry.vehicleCapacity);

                moveFerry();
                return true;
            }

            else if (ferry.startingSide == this.startingSide && ferry.vehicleCapacity + vehicleCapacity > 20
                    && visitingCount < 2 ) {
                // System.out.println("ferry capacity not enough for " + id);

                if (startingSide == StartingSide.leftSide) {

                    leftVehicle.remove(id);
                    waitingLeft.put(id, Main.vehicleList.get(id));

                } else {

                    rightVehicle.remove(id);
                    waitingRight.put(id, Main.vehicleList.get(id));
                }

                return true;
            }

            else if (visitingCount < 2) {
                //System.out.println("ferry is on the other side for " + id);
                moveFerry();
                return true;
            } else {
                // System.out.println("vehicle " + id + " already finished tours");

                return false;
            }
        }
    }

    public void moveFerry() throws InterruptedException {

        synchronized (moveLock) {  if(ferry.vehicleCapacity == 20 || (ferry.startingSide == StartingSide.rightSide && rightVehicle.isEmpty() ) ||
                ferry.startingSide == StartingSide.leftSide && leftVehicle.isEmpty() ) {

            if(ferry.startingSide == StartingSide.leftSide){

                ferry.startingSide = StartingSide.idle;
                ferry.vehicleCapacity = 0;
                System.out.println("the ferry has reached the right side.Waiting for vehicles to get off the ferry (2 seconds)");
                Thread.sleep(2000);
                for (Map.Entry<Integer, Vehicle> entry : vehicleMap.entrySet()) {
                    Vehicle v = entry.getValue();
                    v.visitingCount++;
                    v.isChanged = true;
                    v.setStartingSide(Vehicle.StartingSide.rightSide);
                    v.isLoaded = false;
                    System.out.println(v.id + " used " + v.visitingCount + " times ferry type :  "+ v.vehicleType);

                    if (v.visitingCount == 2) {
                        System.out.println(v.id +  " finished visiting type : " + v.vehicleType);
                    } else {
                        rightVehicle.put(v.id, v);
                    }
                }

                vehicleMap.clear();
                ferry.startingSide = StartingSide.rightSide;

                leftVehicle.putAll(waitingLeft);
                waitingLeft.clear();
            }
            else {

                ferry.startingSide = StartingSide.idle;
                ferry.vehicleCapacity = 0;
                System.out.println("the ferry has reached the left side.Waiting for vehicles to get off the ferry (2 seconds)");
                Thread.sleep(2000);

                for (Map.Entry<Integer, Vehicle> entry : vehicleMap.entrySet()) {
                    Vehicle v = entry.getValue();
                    v.visitingCount++;
                    v.isChanged = true;
                    v.setStartingSide(Vehicle.StartingSide.leftSide);
                    v.isLoaded = false;
                    System.out.println(v.id +" used " +v.visitingCount +" times ferry type : "+ v.vehicleType );

                    if(v.visitingCount == 2){
                        System.out.println( v.id +" finished visiting, type : " + v.vehicleType);
                    }
                    else{
                        leftVehicle.put(v.id,v);}
                }

                vehicleMap.clear();
                ferry.startingSide = StartingSide.leftSide;

                rightVehicle.putAll(waitingRight);
                waitingRight.clear();
            }

            System.out.println("All vehicles got off the ferry.");

        }
        else {
            // System.out.println("ferry not moving for " + id);
        }
        }
    }
}