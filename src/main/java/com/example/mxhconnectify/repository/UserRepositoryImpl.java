package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.dao.UserDAO;
import com.example.mxhconnectify.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository // Đánh dấu đây là tầng Repository thực hiện thao tác DB
public class UserRepositoryImpl implements UserDAO {

    @PersistenceContext
    private EntityManager entityManager; // Sử dụng EntityManager của JPA để code chi tiết

    @Override
    public Optional<User> findById(Long id) {
        User user = entityManager.find(User.class, id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByForgotPasswordToken(String token) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.forgotPasswordToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    @Override
    public Optional<User> findByEmail(String email) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByFullName(String fullName) {
        try {
            User user = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.full_name = :fullName", User.class)
                    .setParameter("full_name", fullName)
                    .getSingleResult();
            return Optional.of(user);
        }catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String username, String email) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username OR u.email = :email", User.class)
                    .setParameter("username", username)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<User> findByEmailToken(String emailToken) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.emailToken = :emailToken", User.class)
                    .setParameter("emailToken", emailToken)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user); // Thêm mới nếu chưa có ID
            return user;
        } else {
            return entityManager.merge(user); // Cập nhật nếu đã có ID
        }
    }

    @Override
    public Optional<List<User>> getAllUsers() {
        // 1. Thực hiện query và nhận kết quả là một List
        List<User> allUser = entityManager.createQuery("SELECT u FROM User u", User.class)
                .getResultList();

        // 2. Trả về Optional chứa danh sách đó (nếu list rỗng thì nó vẫn là một list rỗng)
        return Optional.ofNullable(allUser);
    }
}