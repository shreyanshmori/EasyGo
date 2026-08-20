package bus.services;

public class BusSeatManager {

    private final bus.dao.BusDAO busDAO;

    public BusSeatManager() { this.busDAO = new bus.dao.BusDAO(); }

    // ── Available seat count ──────────────────────────────────────────────────
    public int getAvailableCount(int busId, String date,
                                 int totalSeats) throws common.exceptions.DatabaseException {
        int booked = busDAO.countBookedSeats(busId, date);
        return Math.max(0, totalSeats - booked);
    }

    // ── Assign next available seat ────────────────────────────────────────────
    public String assignNextSeat(int busId, String date) throws common.exceptions.DatabaseException {
        datastructures.linkedlist.CustomLinkedList<String> booked =
                busDAO.getBookedSeats(busId, date);
        datastructures.hashmap.CustomHashMap<String, Boolean> bookedMap =
                new datastructures.hashmap.CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        datastructures.linkedlist.CustomLinkedList<bus.models.BusSeat> allSeats =
                busDAO.getSeats(busId);
        for (int i = 0; i < allSeats.size(); i++) {
            bus.models.BusSeat s = allSeats.get(i);
            if (!bookedMap.containsKey(s.getSeatNumber())) return s.getSeatNumber();
        }
        return null;
    }

    // ── Assign specific preferred seat type ───────────────────────────────────
    public String assignPreferredSeat(int busId, String date,
                                      String preferredType) throws common.exceptions.DatabaseException {
        datastructures.linkedlist.CustomLinkedList<String> booked =
                busDAO.getBookedSeats(busId, date);
        datastructures.hashmap.CustomHashMap<String, Boolean> bookedMap =
                new datastructures.hashmap.CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        datastructures.linkedlist.CustomLinkedList<bus.models.BusSeat> allSeats =
                busDAO.getSeats(busId);

        // First pass: preferred type
        for (int i = 0; i < allSeats.size(); i++) {
            bus.models.BusSeat s = allSeats.get(i);
            if (!bookedMap.containsKey(s.getSeatNumber()) &&
                s.getSeatType().equalsIgnoreCase(preferredType))
                return s.getSeatNumber();
        }
        // Second pass: any available
        return assignNextSeat(busId, date);
    }

    // ── Print seat layout ─────────────────────────────────────────────────────
    public void printLayout(int busId, String busType, String date)
            throws common.exceptions.DatabaseException {
        datastructures.linkedlist.CustomLinkedList<String> booked =
                busDAO.getBookedSeats(busId, date);
        datastructures.hashmap.CustomHashMap<String, Boolean> bookedMap =
                new datastructures.hashmap.CustomHashMap<>();
        for (int i = 0; i < booked.size(); i++) bookedMap.put(booked.get(i), true);

        datastructures.linkedlist.CustomLinkedList<bus.models.BusSeat> seats =
                busDAO.getSeats(busId);

        common.utilities.ConsoleUtils.printHeader("BUS SEAT LAYOUT | Date: " + date);
        System.out.println("  [A]=Available  [X]=Booked  | L=Lower U=Upper W=Window I=Aisle");
        common.utilities.ConsoleUtils.printLine('-');

        boolean isSleeper = busType != null && busType.toUpperCase().contains("SLEEPER");
        if (isSleeper) printSleeperLayout(seats, bookedMap);
        else           printSeaterLayout(seats, bookedMap);

        common.utilities.ConsoleUtils.printLine('-');
        System.out.println();
    }

    private void printSleeperLayout(
            datastructures.linkedlist.CustomLinkedList<bus.models.BusSeat> seats,
            datastructures.hashmap.CustomHashMap<String, Boolean> bookedMap) {

        System.out.println("         LOWER BERTHS        UPPER BERTHS");
        System.out.println("  Bay    Left    Right    Left    Right");
        common.utilities.ConsoleUtils.printLine('.');

        // Group by bay number
        datastructures.hashmap.CustomHashMap<Integer, String[]> bays =
                new datastructures.hashmap.CustomHashMap<>();

        for (int i = 0; i < seats.size(); i++) {
            bus.models.BusSeat s = seats.get(i);
            String num = s.getSeatNumber();          // e.g. L3A, U3B
            if (num.length() < 3) continue;
            char tier  = num.charAt(0);              // L or U
            int  bay   = Integer.parseInt(num.substring(1, num.length()-1));
            char side  = num.charAt(num.length()-1); // A or B

            String[] arr = bays.getOrDefault(bay, new String[]{"?","?","?","?"});
            String   slot = bookedMap.containsKey(num) ? "[X]" : "[A]";
            if (tier=='L' && side=='A') arr[0] = slot;
            else if (tier=='L' && side=='B') arr[1] = slot;
            else if (tier=='U' && side=='A') arr[2] = slot;
            else if (tier=='U' && side=='B') arr[3] = slot;
            bays.put(bay, arr);
        }

        Object[] keys = bays.keys();
        // Simple insertion sort on keys
        for (int i = 1; i < keys.length; i++) {
            Object key = keys[i]; int j = i - 1;
            while (j >= 0 && (Integer)keys[j] > (Integer)key) { keys[j+1]=keys[j]; j--; }
            keys[j+1] = key;
        }
        for (Object k : keys) {
            String[] arr = bays.get((Integer)k);
            System.out.printf("  %-6d %-7s %-8s %-7s %-7s%n",
                k, arr[0], arr[1], arr[2], arr[3]);
        }
    }

    private void printSeaterLayout(
            datastructures.linkedlist.CustomLinkedList<bus.models.BusSeat> seats,
            datastructures.hashmap.CustomHashMap<String, Boolean> bookedMap) {

        System.out.println("  Row   WIN   AISLE   AISLE   WIN");
        common.utilities.ConsoleUtils.printLine('.');

        // Group by row
        datastructures.hashmap.CustomHashMap<Integer, String[]> rows =
                new datastructures.hashmap.CustomHashMap<>();

        for (int i = 0; i < seats.size(); i++) {
            bus.models.BusSeat s = seats.get(i);
            String num = s.getSeatNumber(); // e.g. R5C
            if (num.length() < 3) continue;
            int  row  = Integer.parseInt(num.substring(1, num.length()-1));
            char col  = num.charAt(num.length()-1);
            String slot = bookedMap.containsKey(num) ? "[X]" : "[A]";
            String[] arr = rows.getOrDefault(row, new String[]{"?","?","?","?"});
            int idx = col - 'A';
            if (idx >= 0 && idx < 4) arr[idx] = slot;
            rows.put(row, arr);
        }

        Object[] keys = rows.keys();
        for (int i = 1; i < keys.length; i++) {
            Object key = keys[i]; int j = i - 1;
            while (j >= 0 && (Integer)keys[j] > (Integer)key) { keys[j+1]=keys[j]; j--; }
            keys[j+1] = key;
        }
        for (Object k : keys) {
            String[] arr = rows.get((Integer)k);
            System.out.printf("  %-5d %-6s %-7s %-7s %-6s%n",
                k, arr[0], arr[1], arr[2], arr[3]);
        }
    }
}

