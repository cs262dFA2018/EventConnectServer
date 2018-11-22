package edu.calvin.cs262.cs262d;


import java.sql.Timestamp;

/**
 * This class implements an Event Data-Access Object (DAO) class for the Event relation.
 * This provides an object-oriented way to represent and manipulate Event "objects" from
 * the traditional (non-object-oriented) EventConnect database.
 *
 */
public class Event {

    private int id;
    private int userId;
    private String title;
    private String description;
    private Timestamp time;
    private String location;
    private float cost;
    private int threshold;
    private int capacity;
    private String category;

    public Event() {
        // The JSON marshaller used by Endpoints requires this default constructor.
    }
    public Event(int id, int userId, String title, String description, Timestamp time, String location, float cost,
                 int threshold, int capacity, String category) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.time = time;
        this.location = location;
        this.cost = cost;
        this.threshold = threshold;
        this.capacity = capacity;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
