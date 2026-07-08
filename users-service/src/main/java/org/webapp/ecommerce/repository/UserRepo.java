package org.webapp.ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.webapp.ecommerce.entity.Users;
import java.util.List;

public interface UserRepo extends JpaRepository<Users, Long> {

    boolean existsByUserName(String userName);

    boolean existsByEmailId(String emailId);

    boolean existsByContactNo(String contactNo);

    Users findByUserName(String userName);

    Users findByEmailId(String emailId);

    Users findByContactNo(String contactNo);

    @Query("""
    SELECT u.userName, u.emailId
    FROM Users u
    """)
    Page<Object[]> findAllUsernamesAndEmails(Pageable pageable);

}