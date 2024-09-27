package cat.tecnocampus.securityjwt.security.authentication;

import cat.tecnocampus.securityjwt.domain.UserLab;
import cat.tecnocampus.securityjwt.persistence.UserLabRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserLabDetailsService implements UserDetailsService {
    private UserLabRepository userLabRepository;

    public UserLabDetailsService(UserLabRepository userLabRepository) {
        this.userLabRepository = userLabRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserLab user = userLabRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return new UserLabDetails(user);
    }

}