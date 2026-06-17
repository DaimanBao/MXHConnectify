package com.example.mxhconnectify.dao;

import com.example.mxhconnectify.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByFullName(String fullName);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmailToken(String emailToken);
    Optional<User> findByForgotPasswordToken(String token);
    Optional<List<User>> getAllUsers();
    User save(User user); // Dùng cho cả thêm mới và cập nhật
}