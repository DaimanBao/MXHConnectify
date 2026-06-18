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

    // 1. findById(Long id) -> JPA có sẵn hàm findById(ID)

    // 2. findByUsername -> JPA tự sinh query từ tên hàm
    Optional<User> findByUsername(String username);

    // 3. findByEmail -> JPA tự sinh query từ tên hàm
    Optional<User> findByEmail(String email);

    // 4. findByFullName -> JPA tự sinh query từ tên hàm
    Optional<User> findByFullName(String fullName);

    // 5. findByUsernameOrEmail -> Cần dùng @Query
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    // 6. existsByUsername -> JPA tự sinh query
    boolean existsByUsername(String username);

    // 7. existsByEmail -> JPA tự sinh query
    boolean existsByEmail(String email);

    // 8. findByEmailToken -> JPA tự sinh query
    Optional<User> findByEmailToken(String emailToken);

    // 9. findByForgotPasswordToken -> JPA tự sinh query
    Optional<User> findByForgotPasswordToken(String token);

    // 10. getAllUsers() -> JPA có sẵn hàm findAll()
    // Lưu ý: findAll() trả về List<User>, không trả về Optional

    // 11. save(User user) -> JPA có sẵn hàm save()

    // 12. Hàm cho tính năng Search (thêm mới cho trang tìm kiếm)
    @Query(value = "SELECT * FROM users u WHERE u.id != :currentUserId ORDER BY RAND()",
            countQuery = "SELECT COUNT(*) FROM users u WHERE u.id != :currentUserId",
            nativeQuery = true)
    List<User> findRandomUsers(@Param("currentUserId") Long currentUserId, Pageable pageable);

    // Hàm tìm kiếm có keyword (vẫn nên loại trừ chính mình nếu bạn muốn)
    // Đảm bảo truyền đủ currentId và keyword
    @Query("SELECT u FROM User u WHERE u.id != :currentId AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("currentId") Long currentId, Pageable pageable);
}