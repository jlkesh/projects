package uz.jl.todoapp.user;


import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role;

}

record UserRegisterDTO(String username, String password, String confirmPassword) {
}


interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByUsername(String username);
}


@Service
class AuthService implements UserDetailsService {
    final PasswordEncoder passwordEncoder;
    final UserRepository userRepository;

    AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    public Optional<Users> findUserByUsername(@NonNull String username) {
        return userRepository.findByUsername(username);
    }

    public void createUser(@NonNull UserRegisterDTO dto) {
        Users user = Users.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .role("USER")
                .build();
        userRepository.save(user);
    }
}


@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "/auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "/auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute UserRegisterDTO dto, RedirectAttributes attributes) {
        Map<String, Object> errors = new HashMap<>();

        if (!StringUtils.hasText(dto.username()))
            errors.put("error_message_for_username", "Username can not be blank");

        if (authService.findUserByUsername(dto.username()).isPresent())
            errors.put("error_message_for_username", "Username already taken");

        if (authService.findUserByUsername(dto.username()).isPresent())
            errors.put("error_message_for_username", "Username already taken");

        if (!StringUtils.hasText(dto.password()))
            errors.put("error_message_for_password", "Password can not be blank");

        if (!Objects.equals(dto.password(), dto.confirmPassword()))
            errors.put("error_message_for_password", "Password did not match");

        if (errors.isEmpty()) {
            authService.createUser(dto);
            return "redirect:/auth/login";
        }
        errors.forEach(attributes::addFlashAttribute);
        attributes.addFlashAttribute("username", dto.username());
        attributes.addFlashAttribute("password", dto.password());
        attributes.addFlashAttribute("confirmPassword", dto.confirmPassword());

        return "redirect:/auth/register";
    }

}

