package cat.tecnocampus.securityjwt.persistence;

import cat.tecnocampus.securityjwt.domain.UserLab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLabRepository extends JpaRepository<UserLab, Long> {
    Optional<UserLab> findByUsername(String email);
}
