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

public class UserResource {

    /**
     * GET
     * This method gets the full list of users from the Users table.
     *
     * @return JSON-formatted list of user records (based on a root JSON tag of "items")
     * @throws SQLException
     */
    @ApiMethod(path = "users", httpMethod = GET)
    public List<User> getUsers() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<User> result = new ArrayList<User>();
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = selectUsers(statement);
            while (resultSet.next()) {
                User u = new User(
                        resultSet.getInt(1),        // id
                        resultSet.getString(2),     // username
                        "[hidden]"     // password
                );
                result.add(u);
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
     * This method gets the user from the Users table with the given ID.
     *
     * @param id the ID of the requested user
     * @return if the player exists, a JSON-formatted user record, otherwise an invalid/empty JSON entity
     * @throws SQLException
     */
    @ApiMethod(path = "user/{id}", httpMethod = GET)
    public User getUser(@Named("id") int id) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        User result = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = selectUser(id, statement);
            if (resultSet.next()) {
                result = new User(
                        resultSet.getInt(1),        // id
                        resultSet.getString(2),     // username
                        "[hidden]"      // password
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
     * GET
     * This method gets the user from the Users table with the given ID.
     *
     * @param token username:password base64 encoded
     * @return if the player exists, a JSON-formatted user record, otherwise an invalid/empty JSON entity
     * @throws SQLException
     */
    @ApiMethod(path = "user/events/{token}", httpMethod = GET)
    public List<Event> getUserEvents(@Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        // get the userID from the token
        String username = decodeBase64(token).split(":")[0];
        int userID = getUserId(username);
        List<Event> result = new ArrayList<Event>();
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = selectUserEvents(userID, statement);
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
     * POST
     * This method creates an instance of user with a new, unique ID
     * number. We do this because POST is not idempotent, meaning that running
     * the same POST several times creates multiple objects with unique IDs but
     * otherwise having the same field values.
     *
     * The method creates a new, unique ID by querying the Users table for the
     * largest ID and adding 1 to that. Using a DB sequence would be a better solution.
     * This method creates an instance of user with a new, unique ID.
     *
     * @param user a JSON representation of the user to be created
     * @return new user entity with a system-generated ID
     * @throws SQLException
     */
    @ApiMethod(path="user", httpMethod=POST)
    public User postUser(User user) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int userID;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT MAX(ID) FROM Users");
            if (resultSet.next()) {
                userID = resultSet.getInt(1) + 1;
                user.setId(userID);
            } else {
                throw new RuntimeException("failed to find unique ID...");
            }
            insertUser(user, statement);
        } catch (SQLException e) {
            throw (e);
        } finally {
            if (resultSet != null) { resultSet.close(); }
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return getUser(userID);
    }

    /**
     * PUT
     * This method creates/updates an instance of user with a given ID.
     * If the user doesn't exist, create a new user using the given field values.
     * If the user already exists, update the fields using the new user field values.
     * We do this because PUT is idempotent, meaning that running the same PUT several
     * times is the same as running it exactly once.
     * Any user ID value set in the passed user data is ignored.
     *
     * @param userID the ID for the user, assumed to be unique
     * @param user a JSON representation of the user; The id parameter overrides any id specified here.
     * @return new/updated user entity
     * @throws SQLException
     */
    @ApiMethod(path = "user/{id}/{token}", httpMethod = PUT)
    public User putUser(User user, @Named("id") int userID, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            user.setId(userID);
            resultSet = selectUser(userID, statement);
            if (resultSet.next() && isAuthorized(token)) {
                updateUser(user, statement);
            } else {
                insertUser(user, statement);
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
        return getUser(userID);
    }

    /**
     * DELETE
     * This method deletes the instance of user with a given ID, if it exists.
     * If the user with the given ID doesn't exist, SQL won't delete anything.
     * This makes DELETE idempotent.
     *
     * @param userID the ID for the player, assumed to be unique
     * @param token username:password base64 encoded
     * @throws SQLException
     */
    @ApiMethod(path = "user/{id}/{token}", httpMethod = DELETE)
    public void deleteUser(User user, @Named("id") int userID, @Named("token") String token) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(System.getProperty("cloudsql"));
            statement = connection.createStatement();
            if(isAuthorized(token)) {
                deleteUser(userID, statement);
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
     * This function gets all users in the Users table
     */
    private ResultSet selectUsers(Statement statement) throws SQLException {
        return statement.executeQuery(
                "SELECT ID, Username FROM Users;"
        );
    }

    /*
     * This function gets the user with the given ID from the Users table
     */
    private ResultSet selectUser(int id, Statement statement) throws SQLException {
        return statement.executeQuery(
                String.format(
                        "SELECT ID, Username FROM Users WHERE ID=%s", id
                )
        );
    }

    /*
     * This function inserts the given user using the given JDBC statement.
     */
    private void insertUser(User user, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("INSERT INTO Users " +
                                "VALUES (%d, %s, %s);",
                        user.getId(),
                        getValueStringOrNull(user.getUsername()),
                        getValueStringOrNull(user.getPassword())
                )
        );
    }

    /*
     * This function modifies the given user using the given JDBC statement.
     */
    private void updateUser(User user, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("UPDATE Users " +
                                "SET Username=COALESCE(%s, Users.Username), " +
                                "Password=COALESCE(%s, Users.Password), " +
                                "WHERE id=%d;",
                        getValueStringOrNull(user.getUsername()),
                        getValueStringOrNull(user.getPassword()),
                        user.getId()
                )
        );
    }

    /*
     * This function deletes the User with the given id using the given JDBC statement.
     */
    private void deleteUser(int id, Statement statement) throws SQLException {
        statement.executeUpdate(
                String.format("DELETE FROM Users WHERE ID=%d", id)
        );
    }

    /*
     * This function will check if a user is authorized to change a user
     * given the username:password encoded in base64
     */
    private boolean isAuthorized(String base64UsernamePassword) {
        //TODO: check if user is authorized
        // https://blog.restcase.com/restful-api-authentication-basics/
        // Basic Authentication section
        // Anyone is allowed to access anything
        return true;
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

    /*
     * This function gets all events in the Events table
     */
    private ResultSet selectUserEvents(int UserID, Statement statement) throws SQLException {
        return statement.executeQuery(
                String.format("SELECT Events.*, " +
                        "COUNT (JoinedEvents.EventID) " +
                        "FROM Events " +
                        "LEFT JOIN JoinedEvents " +
                        "ON JoinedEvents.EventID=Events.ID " +
                        "WHERE JoinedEvents.UserID=%s" +
                        "GROUP BY Events.ID " +
                        "ORDER BY Events.Time;"
                        , UserID)
        );
    }

}
