package railway.management;

import common.exceptions.DatabaseException;
import common.utilities.ConsoleUtils;
import datastructures.linkedlist.CustomLinkedList;
import datastructures.queue.CustomQueue;
import datastructures.hashmap.CustomHashMap;
import railway.dao.BookingDAO;
import railway.dao.WaitingListDAO;
import railway.models.RACEntry;
import railway.models.WaitingListEntry;

/**
 * WaitingListManager — manages in-memory WL and RAC queues.
 *
 * Mirrors the DB state in CustomQueue structures for fast
 * in-session operations.
 *
 * Key rules:
 *   - Confirmed seats fill first.
 *   - When confirmed full → RAC (side-lower berths, 2 per berth).
 *   - When RAC full       → Waiting List (up to MAX_WL_PER_COACH).
 *   - On cancellation: RAC → Confirmed, WL1 → RAC, rest shift up.
 */
public class WaitingListManager {

    // Per-coach WL queue: key = "trainId_coachId_date"
    private final CustomHashMap<String, CustomQueue<WaitingListEntry>> wlQueues;
    // Per-coach RAC queue
    private final CustomHashMap<String, CustomQueue<RACEntry>>         racQueues;

    private final WaitingListDAO wlDAO;
    private final BookingDAO     bookingDAO;

    public static final int MAX_WL  = 10;
    public static final int MAX_RAC = 8;  // 4 berths × 2 passengers

    public WaitingListManager() {
        this.wlQueues   = new CustomHashMap<>();
        this.racQueues  = new CustomHashMap<>();
        this.wlDAO      = new WaitingListDAO();
        this.bookingDAO = new BookingDAO();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUEUE KEY
    // ══════════════════════════════════════════════════════════════════════════

    private String key(int trainId, int coachId, String date) {
        return trainId + "_" + coachId + "_" + date;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOAD FROM DB  (called on first access for a given train/coach/date)
    // ══════════════════════════════════════════════════════════════════════════

    public void loadFromDB(int trainId, int coachId, String date) throws DatabaseException {
        String k = key(trainId, coachId, date);

        // Load WL
        if (!wlQueues.containsKey(k)) {
            CustomQueue<WaitingListEntry> wlQ = new CustomQueue<>();
            CustomLinkedList<WaitingListEntry> list = wlDAO.getWLList(trainId, coachId, date);
            for (int i = 0; i < list.size(); i++) wlQ.enqueue(list.get(i));
            wlQueues.put(k, wlQ);
        }

        // Load RAC
        if (!racQueues.containsKey(k)) {
            CustomQueue<RACEntry> racQ = new CustomQueue<>();
            // fetch RAC list (reuse WL DAO)
            CustomLinkedList<RACEntry> racList = getRACListFromDB(trainId, coachId, date);
            for (int i = 0; i < racList.size(); i++) racQ.enqueue(racList.get(i));
            racQueues.put(k, racQ);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WL OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isWLFull(int trainId, int coachId, String date) throws DatabaseException {
        int dbCount = wlDAO.getWLCount(trainId, coachId, date);
        return dbCount >= MAX_WL;
    }

    public int getWLCount(int trainId, int coachId, String date) throws DatabaseException {
        return wlDAO.getWLCount(trainId, coachId, date);
    }

    /** Add a new WL entry — returns position assigned. */
    public int enqueueWL(int bookingId, int trainId, int coachId, String date)
            throws DatabaseException {
        int pos = wlDAO.getWLCount(trainId, coachId, date) + 1;
        wlDAO.insertWL(bookingId, trainId, coachId, date, pos);

        String k = key(trainId, coachId, date);
        CustomQueue<WaitingListEntry> q = wlQueues.get(k);
        if (q == null) { q = new CustomQueue<>(); wlQueues.put(k, q); }
        WaitingListEntry entry = new WaitingListEntry(bookingId, trainId, coachId, date, pos);
        q.enqueue(entry);
        return pos;
    }

    /** Dequeue first WL entry (for promotion after cancellation). */
    public WaitingListEntry dequeueWL(int trainId, int coachId, String date)
            throws DatabaseException {
        String k = key(trainId, coachId, date);
        CustomQueue<WaitingListEntry> q = wlQueues.get(k);

        WaitingListEntry entry = null;
        if (q != null && !q.isEmpty()) {
            entry = q.dequeue();
        } else {
            // fall back to DB
            entry = wlDAO.getFirstWL(trainId, coachId, date);
        }

        if (entry != null) {
            wlDAO.deleteWL(entry.getBookingId());
            wlDAO.shiftWLPositions(trainId, coachId, date);
            // Shift in-memory queue positions
            shiftInMemoryWL(k);
        }
        return entry;
    }

    /** Remove a specific booking from WL (for cancellation of WL ticket). */
    public void removeFromWL(int bookingId, int trainId, int coachId, String date)
            throws DatabaseException {
        wlDAO.deleteWL(bookingId);
        wlDAO.shiftWLPositions(trainId, coachId, date);

        // Remove from in-memory queue
        String k = key(trainId, coachId, date);
        CustomQueue<WaitingListEntry> q = wlQueues.get(k);
        if (q != null) {
            CustomQueue<WaitingListEntry> newQ = new CustomQueue<>();
            int pos = 1;
            while (!q.isEmpty()) {
                WaitingListEntry e = q.dequeue();
                if (e.getBookingId() != bookingId) {
                    e.setWlPosition(pos++);
                    newQ.enqueue(e);
                }
            }
            wlQueues.put(k, newQ);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RAC OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isRACFull(int trainId, int coachId, String date) throws DatabaseException {
        return wlDAO.getRACCount(trainId, coachId, date) >= MAX_RAC;
    }

    public int getRACCount(int trainId, int coachId, String date) throws DatabaseException {
        return wlDAO.getRACCount(trainId, coachId, date);
    }

    /** Add a new RAC entry — returns position assigned. */
    public int enqueueRAC(int bookingId, int trainId, int coachId, String date)
            throws DatabaseException {
        int pos     = wlDAO.getRACCount(trainId, coachId, date) + 1;
        String seat = "RAC-" + ((pos + 1) / 2) + (pos % 2 == 1 ? "A" : "B");
        wlDAO.insertRAC(bookingId, trainId, coachId, date, pos, seat);

        String k = key(trainId, coachId, date);
        CustomQueue<RACEntry> q = racQueues.get(k);
        if (q == null) { q = new CustomQueue<>(); racQueues.put(k, q); }
        RACEntry entry = new RACEntry(bookingId, trainId, coachId, date, pos, seat);
        q.enqueue(entry);
        return pos;
    }

    /** Dequeue first RAC entry (for promotion to CONFIRMED). */
    public RACEntry dequeueRAC(int trainId, int coachId, String date)
            throws DatabaseException {
        String k = key(trainId, coachId, date);
        CustomQueue<RACEntry> q = racQueues.get(k);

        RACEntry entry = null;
        if (q != null && !q.isEmpty()) {
            entry = q.dequeue();
        } else {
            entry = wlDAO.getFirstRAC(trainId, coachId, date);
        }

        if (entry != null) wlDAO.deleteRAC(entry.getBookingId());
        return entry;
    }

    /** Remove specific booking from RAC (on RAC cancellation). */
    public void removeFromRAC(int bookingId) throws DatabaseException {
        wlDAO.deleteRAC(bookingId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    public void displayWLStatus(int trainId, int coachId, String date)
            throws DatabaseException {
        ConsoleUtils.printHeader("WAITING LIST — Train:" + trainId
                + " Coach:" + coachId + " Date:" + date);

        int wlCount  = wlDAO.getWLCount(trainId, coachId, date);
        int racCount = wlDAO.getRACCount(trainId, coachId, date);

        ConsoleUtils.printInfo("WL Occupied  : " + wlCount  + " / " + MAX_WL);
        ConsoleUtils.printInfo("RAC Occupied : " + racCount + " / " + MAX_RAC);
        ConsoleUtils.printLine('-');

        CustomLinkedList<WaitingListEntry> wlList = wlDAO.getWLList(trainId, coachId, date);
        if (wlList.isEmpty()) {
            ConsoleUtils.printInfo("Waiting List is empty.");
        } else {
            String[] h = {"WL Pos", "Booking ID", "Passenger", "Booked At"};
            int[]    w = {8, 12, 24, 20};
            ConsoleUtils.printTableHeader(h, w);
            for (int i = 0; i < wlList.size(); i++) {
                WaitingListEntry e = wlList.get(i);
                ConsoleUtils.printTableRow(new String[]{
                    "WL/" + e.getWlPosition(),
                    String.valueOf(e.getBookingId()),
                    e.getPassengerName() != null ? e.getPassengerName() : "-",
                    e.getCreatedAt() != null ? e.getCreatedAt().substring(0, 16) : "-"
                }, w);
            }
            ConsoleUtils.printTableSeparator(w);
        }
    }

    public void displayRACStatus(int trainId, int coachId, String date)
            throws DatabaseException {
        ConsoleUtils.printHeader("RAC LIST — Train:" + trainId
                + " Coach:" + coachId + " Date:" + date);
        int racCount = wlDAO.getRACCount(trainId, coachId, date);
        ConsoleUtils.printInfo("RAC Occupied : " + racCount + " / " + MAX_RAC);
        ConsoleUtils.printLine('-');
        ConsoleUtils.printInfo("RAC entries are displayed on the ticket as RAC/<position>.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void shiftInMemoryWL(String key) {
        CustomQueue<WaitingListEntry> q = wlQueues.get(key);
        if (q == null) return;
        CustomQueue<WaitingListEntry> shifted = new CustomQueue<>();
        int pos = 1;
        while (!q.isEmpty()) {
            WaitingListEntry e = q.dequeue();
            e.setWlPosition(pos++);
            shifted.enqueue(e);
        }
        wlQueues.put(key, shifted);
    }

    private CustomLinkedList<RACEntry> getRACListFromDB(int trainId, int coachId, String date)
            throws DatabaseException {
        // Delegate to WL DAO (it has the RAC queries)
        CustomLinkedList<RACEntry> list = new CustomLinkedList<>();
        // We'll use getFirstRAC iteratively (limited by MAX_RAC)
        // A full list method would be in WaitingListDAO in production
        for (int pos = 1; pos <= MAX_RAC; pos++) {
            RACEntry e = wlDAO.getFirstRAC(trainId, coachId, date);
            if (e == null) break;
            list.add(e);
        }
        return list;
    }
}