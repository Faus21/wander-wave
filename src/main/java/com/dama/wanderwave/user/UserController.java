package com.dama.wanderwave.user;


import com.azure.core.annotation.Get;
import com.dama.wanderwave.azure.AzureService;
import com.dama.wanderwave.user.request.BlockRequest;
import com.dama.wanderwave.user.request.SubscribeRequest;
import com.dama.wanderwave.user.response.ShortUserResponse;
import com.dama.wanderwave.user.response.UserResponse;
import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "User", description = "Endpoints for retrieving user data.")
@Validated
public class UserController {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserService userService;
    private final AzureService azureService;

    @GetMapping("/profile/id/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get user profile by ID", description = "Retrieves profile details for a specific user by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserProfileById(@PathVariable String id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), user));
    }

    @GetMapping("/profile/nickname/{nickname}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get user profile by nickname", description = "Retrieves profile details for a specific user by nickname.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserProfileByNickname(@PathVariable String nickname) {
        UserResponse user = userService.getUserByNickname(nickname);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), user));
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Subscribe a user", description = "Allows a user to subscribe on other user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully subscribed", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid subscription request", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> subscribe(@RequestBody SubscribeRequest request) {
        String res = userService.updateSubscription(request, true);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unsubscribe a user", description = "Allows a user to unsubscribe from another user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully unsubscribed", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid unsubscribe request", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> unsubscribe(@RequestBody SubscribeRequest request) {
        String res = userService.updateSubscription(request, false);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @PostMapping("/block")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Block a user",
            description = "Blocks a user by adding the blocked user's ID to the blocker's blacklist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully blocked", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> blockUser(@RequestBody String blockedId) {
        String res = userService.updateBlacklist(blockedId, true);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @PostMapping("/unblock")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Unblock a user",
            description = "Removes the blocked user's ID from the blocker's blacklist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully unblocked", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> unblockUser(@RequestBody String blockedId) {
        String res = userService.updateBlacklist(blockedId, false);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @PostMapping("/ban/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Ban a user", description = "Bans a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully banned", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> banUser(@PathVariable String id) {
        String res = userService.updateBan(id, true);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @PostMapping("/unban/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unban a user", description = "Unbans a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully unbanned", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> unbanUser(@PathVariable String id) {
        String res = userService.updateBan(id, false);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @GetMapping("/subscriptions/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get subscriptions", description = "Fetch subscriptions for a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions successfully fetched", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserSubscriptions(
            @PathVariable String userId,
            @RequestParam @Max(MAX_PAGE_SIZE) Integer page,
            @RequestParam int size) {

        List<UserResponse> subscriptionsPage = userService.getUserSubscriptions(userId, page, size);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), subscriptionsPage));
    }

    @GetMapping("/subscribers/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get subscribers", description = "Fetch subscribers for a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscribers successfully fetched", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserSubscribers(
            @PathVariable String userId,
            @RequestParam @Max(MAX_PAGE_SIZE) Integer page,
            @RequestParam int size) {

        List<UserResponse> subscribersPage = userService.getUserSubscribers(userId, page, size);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), subscribersPage));
    }

    @GetMapping("/subscribers/{userId}/status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Check subscription status", description = "Check if a user is subscribed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription status successfully fetched", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> isUserSubscribed(@PathVariable String userId) {

        boolean isSubscribed = userService.isSubscribed(userId);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), isSubscribed));
    }

    @PostMapping("/upload-avatar")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Upload user avatar", description = "Upload a user avatar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid file format", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Invalid file size/Internal server error", content = @Content()),
    })
    public ResponseEntity<ResponseRecord> uploadImage(@RequestPart("file") MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        String url = azureService.uploadAvatar(
                "avatars",
                fileName,
                file.getBytes(),
                file.getContentType(),
                file.getSize()
        );
        userService.changeAvatar(url);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), url));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get friendship recommendations", description = "Retrieve list of friendship recommendations for user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserFriendshipRecommendations() {
        List<UserResponse> res = userService.getUserFriendshipRecommendations();
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), res));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all users", description = "Retrieves a paginated list of all users, optionally filtered by nickname.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getAllUsers(
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") @Max(MAX_PAGE_SIZE) int size
    ) {
        Page<UserResponse> usersPage = userService.getAllUsers(nickname, page, size);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), usersPage));
    }

    @PatchMapping("/change-username")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Change username", description = "Updates the username for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Username updated successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid username or username already taken", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> changeUsername(
            @RequestParam String username) {
        userService.changeUsername(username);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/change-description")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Change description", description = "Updates the description for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Description updated successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid description", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> changeDescription(
            @RequestParam String description) {
        userService.changeDescription(description);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/blacklist")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get user blacklist", description = "Retrieves the list of users blocked by the specified user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blacklist retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserBlacklist() {
        List<ShortUserResponse> blacklist = userService.getUserBlacklist();
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), blacklist));
    }

}
