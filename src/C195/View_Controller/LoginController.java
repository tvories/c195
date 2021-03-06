/*
 * Author: Taylor Vories
 * WGU C195 Project
 * Handles the logging in of the application.
 */

package C195.View_Controller;

import C195.C195;
import C195.Model.User;
import C195.Model.Validation;
import com.sun.jmx.remote.util.ClassLogger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    private C195 main;
    private final String requiredPassword = "test";
    @FXML private TextField textFieldUsername, textFieldPassword;
    @FXML private Label labelUsername, labelPassword, labelInstructions;
    @FXML private Button buttonLogin;
    private final DateTimeFormatter loggingDTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss(z)");

    public LoginController(C195 main) {
        this.main = main;
    }

    /**
     * Handles the submit button or the enter button.
     */
    @SuppressWarnings("Duplicates")
    @FXML public void onEnter() throws IOException {
        boolean usernameIsValid = true;
        boolean passwordIsValid = true;
        String inputUsername = textFieldUsername.getText();
        String inputPassword = textFieldPassword.getText();
        StringBuilder errors = new StringBuilder();
        User validUser;

        // Validate username
        String usernameValidation = Validation.validateName(inputUsername);
        if(!usernameValidation.isEmpty()) {
            errors.append(usernameValidation);
            errors.append("\n");
            textFieldUsername.setStyle("-fx-border-color: #ba171c;");
            usernameIsValid = false;
        } else {
            textFieldUsername.setStyle(null);
        }
        // Validate password
        String passwordValidation = Validation.validatePassword(inputPassword);
        if(!passwordValidation.isEmpty()) {
            errors.append(passwordValidation);
            errors.append("\n");
            textFieldPassword.setStyle("-fx-border-color: #ba171c;");
            passwordIsValid = false;
        } else {
            textFieldPassword.setStyle(null);
        }

        // If both username and password is valid
        if(usernameIsValid && passwordIsValid) {
            User inputUser = new User(inputUsername,inputPassword);
            validUser = tryLogin(inputUser);
            if(validUser == null) { // login was incorrect or user not found
                logLogin(inputUser.getUsername(), false);
                Alerts.warningAlert(main.rb.getString("login_error_badlogin"));
            } else { // login was valid
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(main.rb.getString("login_success_title"));
                alert.setHeaderText(main.rb.getString("login_success_header"));
                alert.setContentText(main.rb.getString("login_success_content"));
                alert.showAndWait();
                // Sets current user for the current session
                main.currentUser = validUser;
                logLogin(inputUser.getUsername(), true);
                main.rootLayoutController.setLoggedInUser(validUser.getUsername());
                main.showAppointmentsScreen();
            }
        } else {
            Alerts.warningAlert(errors.toString());
        }
    }

    /**
     * Logs user activity to the logfile.
     * @param username Username to be logged.
     * @param wasSuccessful Indicates whether the login attempt was successful or unsuccessful.
     */
    private void logLogin(String username, boolean wasSuccessful) throws IOException{
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        if(wasSuccessful) {
            C195.bufferedWriter.write(now.format(loggingDTF) + " SUCCESSFUL login for: " + username);
            C195.bufferedWriter.newLine();
        } else {
            C195.bufferedWriter.write(now.format(loggingDTF) + " FAILED login for: " + username);
            C195.bufferedWriter.newLine();
        }
    }

    /**
     * Checks database to validate password and username were valid.
     * @param loginAttempt User making the login attempt.
     * @return Returns a user from the database if login was successful.
     */
    private User tryLogin(User loginAttempt) {
        User user = new User();
        try {
            PreparedStatement stmt = C195.dbConnection.prepareStatement("SELECT * FROM user WHERE userName=? AND password=?");
            stmt.setString(1,loginAttempt.getUsername());
            stmt.setString(2,loginAttempt.getPassword());
            ResultSet results = stmt.executeQuery();
            if (results.next()) { // user was found
                user.setUserID(results.getInt("userId"));
                user.setUsername(results.getString("userName"));
                user.setPassword(results.getString("password"));
            } else { // user not found
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set text based on locale
        labelInstructions.setText(main.rb.getString("instructions"));
        labelUsername.setText(main.rb.getString("username"));
        labelPassword.setText(main.rb.getString("password"));
        buttonLogin.setText(main.rb.getString("login_button"));
    }
}