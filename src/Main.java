import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static Map<Integer, Vehicle> vehicleList = new ConcurrentHashMap<>();

    public static void main(String[] args)  {

        for(int i = 0; i <30; i++){

            int typeIndex = new Random().nextInt(Vehicle.StartingSide.values().length-1);
            Vehicle.StartingSide randomVehicleSide = Vehicle.StartingSide.values()[typeIndex];

            if(i<12){

                Vehicle vehicle = new Vehicle(i,randomVehicleSide, Vehicle.VehicleType.car);
                vehicleList.put(i,vehicle);
                Thread t1 = new Thread(vehicle);

                t1.start();

            } else if (i<22) {

               Vehicle vehicle = new Vehicle(i,randomVehicleSide, Vehicle.VehicleType.minibus);
                vehicleList.put(i,vehicle);
                Thread t1 = new Thread(vehicle);

                t1.start();
            }
           else{
              Vehicle vehicle = new Vehicle(i,randomVehicleSide, Vehicle.VehicleType.truck);
                vehicleList.put(i,vehicle);
                Thread t1 = new Thread(vehicle);

                t1.start();
            }
        }
    }
}