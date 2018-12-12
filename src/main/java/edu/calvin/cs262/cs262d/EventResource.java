package edu.calvin.cs262.cs262d;

import com.google.api.server.spi.config.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.google.api.server.spi.config.ApiMethod.HttpMethod.*;

/**
 * This Java annotation specifies the general configuration of the Google Cloud endpoint API.
 * The name and version are used in the URL: https://calvincs262-fall2018-cs262d.appspot.com/eventconnect/v1/ENDPOINT.
 * The namespace specifies the Java package in which to find the API implementation.
 * The issuers specifies boilerplate security features that we won't address in this course.
 * <p>
 * You should configure the name and namespace appropriately.
 */
@Api(
        name = "eventconnect",
        version = "v1",
        namespace =
        @ApiNamespace(
                ownerDomain = "cs262d.cs262.calvin.edu",
                ownerName = "cs262d.cs262.calvin.edu",
                packagePath = ""
        ),
        issuers = {
                @ApiIssuer(
                        name = "firebase",
                        issuer = "https://securetoken.google.com/YOUR-PROJECT-ID",
                        jwksUri =
                                "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system"
                                        + ".gserviceaccount.com"
                )
        }
)

public class EventResource {

    /**
     * GET
     * This method gets the full list of events from the Event table.
     *
     * @return JSON-formatted list of event records (based on a root JSON tag of "items")
     * @throws SQLException
     */
    @ApiMethod(path = "events", httpMethod = GET)
    public List<Event> getEvents() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<Event> result = new ArrayList<Event>();
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = selectEvents(statement);
            while (resultSet.next()) {
                Event e = new Event(
                        resultSet.getInt(1),        // id
                        resultSet.getInt(2),        // userID
                        resultSet.getString(3),     // title
                        resultSet.getString(4),     // description
                        resultSet.getTimestamp(5),  // time
                        resultSet.getString(6),     // location
                        resultSet.getFloat(7),      // cost
                        resultSet.getInt(8),        // threshold
                        resultSet.getInt(9),        // capacity
                        resultSet.getString(10),    // category
                        resultSet.getInt(11)        // count
                );
                result.add(e);
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return result;
    }

    /**
     * GET
     * This method gets the event from the Event table with the given ID.
     *
     * @param id the ID of the requested event
     * @return if the event exists, a JSON-formatted event record, otherwise an invalid/empty JSON entity
     * @throws SQLException
     */
    @ApiMethod(path = "event/{id}", httpMethod = GET)
    public Event getEvent(@Named("id") int id) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        Event result = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = selectEvent(id, statement);
            if (resultSet.next()) {
                result = new Event(
                        resultSet.getInt(1),        // id
                        resultSet.getInt(2),        // userID
                        resultSet.getString(3),     // title
                        resultSet.getString(4),     // description
                        resultSet.getTimestamp(5),  // time
                        resultSet.getString(6),     // location
                        resultSet.getFloat(7),      // cost
                        resultSet.getInt(8),        // threshold
                        resultSet.getInt(9),        // capacity
                        resultSet.getString(10),    // category
                        resultSet.getInt(11)        // count
                );
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return result;
    }

    /**
     * POST
     * This method creates an instance of Event with a new, unique ID
     * number. We do this because POST is not idempotent, meaning that running
     * the same POST several times creates multiple objects with unique IDs but
     * otherwise having the same field values.
     *
     * The method creates a new, unique ID by querying the Event table for the
     * largest ID and adding 1 to that. Using a DB sequence would be a better solution.
     * This method creates an instance of Event with a new, unique ID.
     *
     * @param event a JSON representation of the event to be created
     * @param token username:password encoded in base64
     * @return new event entity with a system-generated ID
     * @throws SQLException
     */
    @ApiMethod(path="event/{token}", httpMethod=POST)
    public Event postEvent(Event event, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int eventID;
        // get the userID from the token
        String username = decodeBase64(token).split(":")[0];
        int userID = getUserId(username);
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT MAX(ID) FROM Events");
            if (resultSet.next()) {
                eventID = resultSet.getInt(1) + 1;
                event.setId(eventID);
                event.setUserId(userID);
            } else {
                throw new RuntimeException("failed to find unique ID...");
            }
            insertEvent(event, statement);
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) { resultSet.close(); }
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return getEvent(eventID);
    }

    /**
     * PUT
     * This method joins a user from the Users table with an event in the Events table
     * to indicate the user has joined the event
     * @param eventID the ID of the event to join
     * @param token username:password encoded in base64
     * @return event in JSON format with updated count
     * @throws SQLException
     */
    @ApiMethod(path="event/{eventID}/join/{token}", httpMethod=PUT)
    public Event joinEvent(@Named("eventID") int eventID, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        // get the userID from the token
        String username = decodeBase64(token).split(":")[0];
        int userID = getUserId(username);
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = statement.executeQuery(
                    String.format("SELECT * FROM JoinedEvents " +
                            "WHERE EventID=%d AND UserID=%d;",
                            eventID, userID)
            );
            if (!resultSet.next()) {
                statement.executeUpdate(
                        String.format("INSERT INTO JoinedEvents VALUES (%d,%d);",
                                eventID, userID)
                );
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) { resultSet.close(); }
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return getEvent(eventID);
    }

    /**
     * PUT
     * This method creates/updates an instance of Event with a given ID.
     * If the Event doesn't exist, create a new Event using the given field values.
     * If the Event already exists, update the fields using the new Event field values.
     * We do this because PUT is idempotent, meaning that running the same PUT several
     * times is the same as running it exactly once.
     * Any Event ID value set in the passed Event data is ignored.
     *
     * @param eventID    the ID for the Event, assumed to be unique
     * @param event a JSON representation of the Event; The id parameter overrides any id specified here.
     * @return new/updated Event entity
     * @throws SQLException
     */
    @ApiMethod(path = "event/{id}/{token}", httpMethod = PUT)
    public Event putEvent(Event event, @Named("id") int eventID, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            event.setId(eventID);
            resultSet = selectEvent(eventID, statement);
            if (resultSet.next() && isAuthorized(token, eventID)) {
                updateEvent(event, statement);
            } else {
                insertEvent(event, statement);
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return getEvent(eventID);
    }

    /**
     * DELETE
     * This method deletes the instance of Event with a given ID, if it exists.
     * If the event with the given ID doesn't exist, SQL won't delete anything.
     * This makes DELETE idempotent.
     *
     * @param eventID the ID for the event, assumed to be unique
     * @param token username:password base64 encoded
     * @throws SQLException
     */
    @ApiMethod(path = "event/{id}/{token}", httpMethod = DELETE)
    public void deleteEvent(Event event, @Named("id") int eventID, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            if(isAuthorized(token, eventID)) {
                deleteEvent(eventID, statement);
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
    }
    /**
     * SQL Utility Functions
     *********************************************/

    /*
     * This function gets all events in the Events table
     */
    private ResultSet selectEvents(Statement statement) throws SQLException {
        return statement.executeQuery(
                "SELECT Events.*, " +
                        "COUNT (JoinedEvents.EventID) " +
                        "FROM Events " +
                        "LEFT JOIN JoinedEvents " +
                        "ON JoinedEvents.EventID=Events.ID " +
                        "GROUP BY Events.ID " +
                        "ORDER BY Events.Time;"
        );
    }

    /*
     * This function gets the event with the given ID from the Events table
     */
    private ResultSet selectEvent(int id, Statement statement) throws SQLException {
        return statement.executeQuery(
                String.format(
                        "SELECT Events.*, " +
                                "COUNT (JoinedEvents.EventID) " +
                                "FROM Events " +
                                "LEFT JOIN JoinedEvents " +
                                "ON JoinedEvents.EventID=Events.ID " +
                                "WHERE Events.ID=%d" +
                                "GROUP BY Events.ID " +
                                "ORDER BY Events.Time;", id
                )
        );
    }

    /*
     * This function inserts the given event using the given JDBC statement.
     */
    private void insertEvent(Event event, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("INSERT INTO Events " +
                                "VALUES (%d, %d, %s, %s, %s, %s, %s, %s, %s, %s);",
                        event.getId(),
                        event.getUserId(),
                        getValueStringOrNull(event.getTitle()),
                        getValueStringOrNull(event.getDescription()),
                        (event.getTime() == null) ? "NULL" : getValueStringOrNull(event.getTime().toString()),
                        getValueStringOrNull(event.getLocation()),
                        (event.getCost() == 0) ? "NULL" : Float.toString(Float.max(event.getCost(), 0)),
                        (event.getThreshold() == 0) ? "NULL" : Integer.toString(event.getThreshold()),
                        (event.getCapacity() == 0) ? "NULL" : Integer.toString(event.getCapacity()),
                        getValueStringOrNull(event.getCategory())
                )
        );
    }

    /*
     * This function modifies the given event using the given JDBC statement.
     */
    private void updateEvent(Event event, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("UPDATE Events " +
                                "SET Title=COALESCE(%s, Events.Title), " +
                                "Description=COALESCE(%s, Events.Description), " +
                                "Time=COALESCE(%s, Events.Time), " +
                                "Location=COALESCE(%s, Events.Location), " +
                                "Cost=COALESCE(%s, Events.Cost), " +
                                "Threshold=COALESCE(%s, Events.Threshold), " +
                                "Capacity=COALESCE(%s, Events.Capacity), " +
                                "Category=COALESCE(%s, Events.Category) " +
                                "WHERE id=%d;",
                        getValueStringOrNull(event.getTitle()),
                        getValueStringOrNull(event.getDescription()),
                        (event.getTime() == null) ? "NULL" : getValueStringOrNull(event.getTime().toString()),
                        getValueStringOrNull(event.getLocation()),
                        (event.getCost() == 0) ? "NULL" : Float.toString(Float.max(event.getCost(), 0)),
                        (event.getThreshold() == 0) ? "NULL" : Integer.toString(event.getThreshold()),
                        (event.getCapacity() == 0) ? "NULL" : Integer.toString(event.getCapacity()),
                        getValueStringOrNull(event.getCategory()),
                        event.getId()
                )
        );
    }

    /*
     * This function deletes the event with the given id using the given JDBC statement.
     */
    private void deleteEvent(int id, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("DELETE FROM Events WHERE ID=%d", id)
        );
    }

    /*
     * This function returns a value literal suitable for an SQL INSERT/UPDATE command.
     * If the value is NULL, it returns an unquoted NULL, otherwise it returns the value with or without quotes
     * depending on the value of "quoted".
     */
    private String getValueStringOrNull(String value) {
        if (value == null) {
            return "NULL";
        } else {
            return "'" + value + "'";
        }
    }

    /*
     * This function will decode a base64 encoded string
     */
    private String decodeBase64(String encodedString) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes);
        return decodedString;
    }

    /*
     * This function will encode a string to base64
     */
    private String encodeBase64(String decodedString) {
        String encodedString = Base64.getEncoder().encodeToString(decodedString.getBytes());
        return encodedString;
    }


    /*
     * This function will check if a user is authorized to edit an event
     * given the username:password encoded in base64 and the event ID
     */
    private boolean isAuthorized(String base64UsernamePassword, int eventID) {
        //TODO: check if user is authorized
        // https://blog.restcase.com/restful-api-authentication-basics/
        // Basic Authentication section
        // Anyone is allowed to access anything
        return true;
    }

    /*
     * This function returns the userID from the Users table given a username
     */
    private int getUserId(String username) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int userID;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = statement.executeQuery(
                    String.format("SELECT ID FROM Users WHERE Username='%s'", username)
            );
            if (resultSet.next()) {
                userID = resultSet.getInt(1);
            } else {
                throw new RuntimeException("failed to find user with the given username");
            }
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) { resultSet.close(); }
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return userID;
    }


}
