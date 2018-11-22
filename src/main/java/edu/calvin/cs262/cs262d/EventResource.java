package edu.calvin.cs262.cs262d;

import com.google.api.server.spi.config.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.google.api.server.spi.config.ApiMethod.HttpMethod.*;

/**
 * This Java annotation specifies the general configuration of the Google Cloud endpoint API.
 * The name and version are used in the URL: https://calvincs262-fall2018-cs262d.appspot.com/eventconnect/v1/ENDPOINT.
 * The namespace specifies the Java package in which to find the API implementation.
 * The issuers specifies boilerplate security features that we won't address in this course.
 *
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
    @ApiMethod(path="events", httpMethod=GET)
    public List<Event> getEvent() throws SQLException {
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
                        resultSet.getString(10)     // category
                );
                result.add(e);
            }
        } catch (SQLException e) {
            throw(e);
        } finally {
            if (resultSet != null) { resultSet.close(); }
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return result;
    }

    /** SQL Utility Functions *********************************************/

    /*
     * This function gets all events in the Events table
     */
    private ResultSet selectEvents(Statement statement) throws SQLException {
        return statement.executeQuery(
                "SELECT * FROM Events"
        );
    }

    /*
     * This function will check if a user is authorized given the username:password encoded in base64
     */
    private boolean isAuthorized(String base64UsernamePassword){
        //TODO: check if user is authorized
        // https://blog.restcase.com/restful-api-authentication-basics/
        // Basic Authentication section
        // Anyone is allowed to access anything
        return true;
    }

}
