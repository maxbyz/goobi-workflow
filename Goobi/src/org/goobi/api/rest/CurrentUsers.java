package org.goobi.api.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.goobi.api.rest.model.RestUserInfo;

import de.sub.goobi.forms.SessionForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

// Access with http://localhost:8080/goobi/api/currentusers
@Path("/currentusers")
public class CurrentUsers {

    @Inject
    private SessionForm sessionForm;

    @GET
    @Operation(summary = "Returns a list of the current users",
            description = "Returns a list with all users that are currently connected to this server")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Internal error")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RestUserInfo> getCurrentUsers() {
        // Now return the expected list of users / sessions
        // Otherwise sessionForm stills null, this is a warning
        if (this.sessionForm != null) {
            return this.generateUserList();

        } else {
            return new ArrayList<>();
        }
    }

    // Reads the current sessions and generates a user list
    private List<RestUserInfo> generateUserList() {
        // Read the sessions and create the list
        List sessions = this.sessionForm.getAlleSessions();
        int length = sessions.size();
        List<RestUserInfo> ruiList = new ArrayList<>();
        RestUserInfo user;

        // Handle all current users
        for (int x = 0; x < length; x++) {

            // Create the current user's object
            user = new RestUserInfo();
            Map<String, String> userMap = (Map<String, String>) (sessions.get(x));

            // Set the user's values
            user.setUser(userMap.get("user"));

            user.setAddress(userMap.get("address"));

            String browser = userMap.get("browserIcon");
            // Cast the "browser.png" to "Browser" example: "firefox.png" -> "Firefox"
            browser = Character.toString((char) (browser.charAt(0) - 32)) + browser.substring(1, browser.length() - 4);
            user.setBrowser(browser);

            user.setCreated(userMap.get("created"));

            user.setLast(userMap.get("last"));
            ruiList.add(user);
        }
        return (ruiList);
    }

}