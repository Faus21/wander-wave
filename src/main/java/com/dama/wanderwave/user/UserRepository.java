package com.dama.wanderwave.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

	Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String username);

    boolean existsByEmail(String email);

    Optional<User> findByNicknameOrEmail(String username, String email);

    @Query("select u from User u where u.nickname = :userData or u.email = :userData")
    Optional<User> loadByNicknameOrEmail(@Param("userData") String userData);

    @Query("select u.subscribers from User u where u.id = :userId")
    Page<String> findSubscribersIdsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("select u.subscriptions from User u where u.id = :userId")
    Page<String> findSubscriptionsIdsByUserId(@Param("userId") String userId, Pageable pageable);

    List<User> findAllByIdIn(List<String> ids);

    @Query("SELECT u.subscriptions FROM User u WHERE u.id = :id")
    List<String> findByIdAndFetchSubscriptions(@Param("id") String id);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM subscribers WHERE follower_id = :authenticatedUserId AND followed_id = :targetUserId)", nativeQuery = true)
    boolean isSubscribed(@Param("authenticatedUserId") String authenticatedUserId,
                         @Param("targetUserId") String targetUserId);

    Page<User> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);
}
