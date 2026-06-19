package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByFullName(String fullName);

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmailToken(String emailToken);

    Optional<User> findByForgotPasswordToken(String token);

    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :currentUserId")
    List<User> findFollowingUsersByCurrentId(@Param("currentUserId") Long currentUserId);

    @Query(value = "SELECT * FROM users u " +
            "WHERE u.id != :currentUserId " +
            "AND u.id NOT IN (SELECT f.following_id FROM follows f WHERE f.follower_id = :currentUserId) " +
            "ORDER BY RAND()",
            countQuery = "SELECT COUNT(*) FROM users u " +
                    "WHERE u.id != :currentUserId " +
                    "AND u.id NOT IN (SELECT f.following_id FROM follows f WHERE f.follower_id = :currentUserId)",
            nativeQuery = true)
    List<User> findRandomUsers(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id != :currentId AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("currentId") Long currentId, Pageable pageable);
}