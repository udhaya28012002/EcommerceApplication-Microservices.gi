package org.webapp.ecommerce.auth.refreshtoken.repository;

import org.webapp.ecommerce.auth.refreshtoken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

}

