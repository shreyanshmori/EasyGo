package railway.services;

import common.exceptions.DatabaseException;
import common.exceptions.TrainNotFoundException;
import common.utilities.ConsoleUtils;
import common.utilities.DateUtils;
import datastructures.linkedlist.DoublyLinkedList;
import datastructures.linkedlist.CustomLinkedList;
import datastructures.trees.BinarySearchTree;
import railway.dao.TrainDAO;
import railway.management.SeatManager;
import railway.models.Coach;
import railway.models.Route;
import railway.models.Train;

/**
 * TrainSearchService — handles all train discovery operations.
 *
 * Features:
 *   - Search by origin/destination
 *   - Search by train number / name
 *   - Date navigation using DoublyLinkedList (prev/next date)
 *   - BST-indexed train lookup by number for O(log n) search
 *   - Route listing
 *   - Availability summary per date
 */
public class TrainSearchService {

    private final TrainDAO    trainDAO;
    private final SeatManager seatManager;

    // BST for fast train-number lookup (populated on first full load)
    private final BinarySearchTree<String> trainNumberIndex;
    // Map trainNumber → Train (cached)
    private final datastructures.hashmap.CustomHashMap<String, Train> trainCache;

    // DoublyLinkedList of 30-day window around a base date for navigation
    private DoublyLinkedList<String>          dateDLL;
    private DoublyLinkedList.Node<String>     currentDateNode;

    public TrainSearchService() {
        this.trainDAO        = new TrainDAO();
        this.seatManager     = new SeatManager();
        this.trainNumberIndex = new BinarySearchTree<>();
        this.trainCache      = new datastructures.hashmap.CustomHashMap<>();
        buildDateWindow(DateUtils.today());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEARCH BY ROUTE
    // ══════════════════════════════════════════════════════════════════════════

    public CustomLinkedList<Train> searchByRoute(String origin, String destination)
            throws DatabaseException, TrainNotFoundException {
        ConsoleUtils.printInfo("Searching trains: " + origin + " → " + destination);
        CustomLinkedList<Train> results = trainDAO.searchTrains(origin, destination);
        if (results.isEmpty())
            throw new TrainNotFoundException(origin + " → " + destination);
        return results;
    }

    /** Search with availability for a specific date. */
    public void searchAndDisplay(String origin, String destination, String date)
            throws DatabaseException, TrainNotFoundException {

        CustomLinkedList<Train> trains = searchByRoute(origin, destination);

        ConsoleUtils.printHeader("TRAINS: " + origin.toUpperCase()
                + " → " + destination.toUpperCase()
                + " | " + DateUtils.friendly(date)
                + " (" + DateUtils.dayOfWeek(date) + ")");

        String[] headers = {"#", "Number", "Name", "Dep", "Arr", "Avail", "RAC", "WL"};
        int[]    widths  = {3, 8, 28, 7, 7, 6, 5, 5};
        ConsoleUtils.printTableHeader(headers, widths);

        for (int i = 0; i < trains.size(); i++) {
            Train t = trains.get(i);
            // Show availability across all coaches (sum)
            int totalAvail = 0;
            boolean anyRAC = false, anyWL = false;
            try {
                CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(t.getTrainId());
                for (int j = 0; j < coaches.size(); j++) {
                    Coach c = coaches.get(j);
                    totalAvail += seatManager.getAvailableSeats(t.getTrainId(), c.getCoachId(), date);
                    if (!anyRAC) anyRAC = seatManager.isRACAvailable(t.getTrainId(), c.getCoachId(), date);
                    if (!anyWL)  anyWL  = seatManager.isWLAvailable(t.getTrainId(), c.getCoachId(), date);
                }
            } catch (DatabaseException ignored) {}

            ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i + 1),
                t.getTrainNumber(), t.getTrainName(),
                t.getDeparture(),   t.getArrival(),
                String.valueOf(totalAvail),
                anyRAC ? "Y" : "N",
                anyWL  ? "Y" : "N"
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEARCH BY NUMBER / NAME
    // ══════════════════════════════════════════════════════════════════════════

    public Train searchByNumber(String number) throws DatabaseException, TrainNotFoundException {
        // Try BST cache first
        if (trainNumberIndex.search(number)) {
            Train cached = trainCache.get(number);
            if (cached != null) return cached;
        }
        Train t = trainDAO.getTrainByNumber(number);
        if (t == null) throw new TrainNotFoundException("Number: " + number);
        // Index it
        trainNumberIndex.insert(number);
        trainCache.put(number, t);
        return t;
    }

    public CustomLinkedList<Train> searchByName(String keyword)
            throws DatabaseException, TrainNotFoundException {
        CustomLinkedList<Train> all     = trainDAO.getAllTrains();
        CustomLinkedList<Train> results = new CustomLinkedList<>();
        String kw = keyword.toUpperCase();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getTrainName().toUpperCase().contains(kw))
                results.add(all.get(i));
        }
        if (results.isEmpty()) throw new TrainNotFoundException("Name keyword: " + keyword);
        return results;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATE NAVIGATION  (DoublyLinkedList prev/next)
    // ══════════════════════════════════════════════════════════════════════════

    /** Build a 30-day window DLL centred on baseDate. */
    private void buildDateWindow(String baseDate) {
        dateDLL = new DoublyLinkedList<>();
        // 15 days before → 15 days after
        for (int i = -15; i <= 15; i++) {
            dateDLL.addLast(DateUtils.addDays(baseDate, i));
        }
        // Position current node at today (index 15)
        currentDateNode = dateDLL.getHead();
        for (int i = 0; i < 15 && currentDateNode.next != null; i++)
            currentDateNode = currentDateNode.next;
    }

    public String getCurrentDate() {
        return currentDateNode != null ? currentDateNode.data : DateUtils.today();
    }

    public String goNextDate() {
        if (currentDateNode != null && currentDateNode.next != null) {
            currentDateNode = currentDateNode.next;
        } else {
            // Extend window by 1 day
            String next = DateUtils.addDays(dateDLL.getTail().data, 1);
            dateDLL.addLast(next);
            currentDateNode = dateDLL.getTail();
        }
        return currentDateNode.data;
    }

    public String goPrevDate() {
        if (currentDateNode != null && currentDateNode.prev != null) {
            // Don't allow past dates
            if (DateUtils.isBefore(currentDateNode.prev.data, DateUtils.today())) {
                ConsoleUtils.printWarning("Cannot navigate to past dates.");
                return currentDateNode.data;
            }
            currentDateNode = currentDateNode.prev;
        }
        return currentDateNode.data;
    }

    /** Interactive date navigation menu for a route search. */
    public String navigateDateForSearch(String origin, String destination)
            throws DatabaseException, TrainNotFoundException {
        while (true) {
            String date = getCurrentDate();
            searchAndDisplay(origin, destination, date);

            ConsoleUtils.printLine('-');
            System.out.println("  [1] Select this date");
            System.out.println("  [2] Next date  (" + DateUtils.friendly(DateUtils.addDays(date, 1)) + ")");
            System.out.println("  [3] Prev date  (" + DateUtils.friendly(DateUtils.addDays(date, -1)) + ")");
            System.out.println("  [0] Back");
            ConsoleUtils.printLine('-');
            int choice = ConsoleUtils.readIntInRange("Choose", 0, 3);

            switch (choice) {
                case 0 -> { return null; }
                case 1 -> { return date; }
                case 2 -> goNextDate();
                case 3 -> goPrevDate();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ROUTE LISTING
    // ══════════════════════════════════════════════════════════════════════════

    public void listAllRoutes() throws DatabaseException {
        CustomLinkedList<Route> routes = trainDAO.getAllRoutes();
        ConsoleUtils.printHeader("ALL RAILWAY ROUTES");
        if (routes.isEmpty()) { ConsoleUtils.printInfo("No routes found."); return; }

        String[] headers = {"#", "Origin", "Destination", "Distance (km)"};
        int[]    widths  = {4, 20, 20, 15};
        ConsoleUtils.printTableHeader(headers, widths);
        for (int i = 0; i < routes.size(); i++) {
            Route r = routes.get(i);
            ConsoleUtils.printTableRow(new String[]{
                String.valueOf(i + 1), r.getOrigin(),
                r.getDestination(), String.valueOf(r.getDistanceKm())
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FULL TRAIN INFO
    // ══════════════════════════════════════════════════════════════════════════

    public void displayTrainDetails(int trainId, String date) throws DatabaseException {
        Train t = trainDAO.getTrainById(trainId);
        if (t == null) { ConsoleUtils.printError("Train not found."); return; }

        ConsoleUtils.printHeader("TRAIN DETAILS");
        ConsoleUtils.printInfo("Number    : " + t.getTrainNumber());
        ConsoleUtils.printInfo("Name      : " + t.getTrainName());
        ConsoleUtils.printInfo("Route     : " + t.getOrigin() + " → " + t.getDestination());
        ConsoleUtils.printInfo("Distance  : " + t.getDistanceKm() + " km");
        ConsoleUtils.printInfo("Departure : " + t.getDeparture());
        ConsoleUtils.printInfo("Arrival   : " + t.getArrival());
        ConsoleUtils.printInfo("Status    : " + t.getStatus());
        ConsoleUtils.printLine('-');

        CustomLinkedList<Coach> coaches = trainDAO.getCoachesByTrain(trainId);
        ConsoleUtils.printInfo("COACHES & AVAILABILITY on " + DateUtils.friendly(date) + ":");

        String[] headers = {"Coach", "Type", "Total", "Avail", "RAC", "WL", "Fare/km"};
        int[]    widths  = {7, 5, 6, 6, 5, 5, 10};
        ConsoleUtils.printTableHeader(headers, widths);

        for (int i = 0; i < coaches.size(); i++) {
            Coach c = coaches.get(i);
            int avail    = seatManager.getAvailableSeats(trainId, c.getCoachId(), date);
            boolean rac  = seatManager.isRACAvailable(trainId, c.getCoachId(), date);
            boolean wl   = seatManager.isWLAvailable(trainId, c.getCoachId(), date);
            ConsoleUtils.printTableRow(new String[]{
                c.getCoachName(), c.getCoachType(),
                String.valueOf(c.getTotalSeats()),
                String.valueOf(avail),
                rac ? "Y" : "N", wl ? "Y" : "N",
                String.format("%.2f", c.getFarePerKm())
            }, widths);
        }
        ConsoleUtils.printTableSeparator(widths);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BST INDEX REBUILD  (admin utility)
    // ══════════════════════════════════════════════════════════════════════════

    public void rebuildIndex() throws DatabaseException {
        CustomLinkedList<Train> all = trainDAO.getAllTrains();
        for (int i = 0; i < all.size(); i++) {
            Train t = all.get(i);
            trainNumberIndex.insert(t.getTrainNumber());
            trainCache.put(t.getTrainNumber(), t);
        }
        ConsoleUtils.printInfo("Train index rebuilt — " + all.size() + " trains indexed.");
    }
}