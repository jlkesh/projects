package uz.jl.todoapp;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@SpringBootApplication
@RequiredArgsConstructor
public class TodoAppApplication implements CommandLineRunner {
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(TodoAppApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        userRepository.save(Users.builder()
//                .username("jl")
//                .password(passwordEncoder.encode("123"))
//                .role("USER")
//                .build());
    }
}


// -------------------------------------------------------------------------
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private boolean completed;

    @Builder.Default
    private Priority priority = Priority.LOW;


    @CreatedDate
    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "timestamp default now()")
    private LocalDateTime createdAt;


    @Getter
    @AllArgsConstructor
    public enum Priority {
        DEFAULT("Default"),
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High");

        private final String value;
    }

}

@Controller
@RequestMapping("/")
@Slf4j
@CrossOrigin("*")
record TodoController(TodoService todoService) {
    @GetMapping
    public String todoListPage(Model model,
                               @RequestParam(required = false, name = "page", defaultValue = "0") int page,
                               @RequestParam(required = false, name = "size", defaultValue = "2") int size) {
        Page<Todo> pageable = todoService.findPageable(page, size);
        model.addAttribute("page", pageable);
        return "todo/todo_list";
    }

    @GetMapping("/add")
    public String addTodoPage(Model model) {
        model.addAttribute("priorities", Todo.Priority.values());
        return "todo/todo_add";
    }

    @PostMapping("/add")
    public String addTodo(@ModelAttribute Todo todo) {
        todoService.create(todo);
        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String todoDeletePage(Model model, @PathVariable Integer id) {
        Todo todo = todoService.get(id);
        model.addAttribute("id", todo.getId());
        model.addAttribute("title", todo.getTitle());
        return "todo/todo_delete";
    }

    @PostMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Integer id) {
        todoService.delete(id);
        return "redirect:/";
    }

    @GetMapping("/update/{id}")
    public String todoUpdatePage(Model model, @PathVariable Integer id) {
        Todo todo = todoService.get(id);
        model.addAttribute("priorities", Todo.Priority.values());
        model.addAttribute("todo", todo);

        return "todo/todo_update";
    }

    @PostMapping("/update/{id}")
    public String updateTodo(@ModelAttribute Todo todo, @PathVariable Integer id) {
        todo.setId(id);
        todoService.create(todo);
        return "redirect:/";
    }

    @PostMapping("/update-completed/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTodo(@PathVariable Integer id) {
        Todo todo = todoService.get(id);
        todo.setCompleted(!todo.isCompleted());
        todoService.create(todo);
    }
}

@Service
record TodoService(TodoRepository todoRepository) {
    public Todo create(@NonNull Todo todo) {
        return todoRepository.save(todo);
    }

    public List<Todo> getAll() {
        return todoRepository.findAll();
    }

    public Todo get(@NonNull Integer id) {
        Supplier<RuntimeException> supplier = () -> new RuntimeException("Todo not found");
        return todoRepository.findById(id).orElseThrow(supplier);
    }

    public void delete(@NonNull Integer id) {
        todoRepository.deleteById(id);
    }

    public List<Todo> getAll(String searchFilter) {
        if (Objects.nonNull(searchFilter))
            return todoRepository.findAllByTitleContains(searchFilter);
        return todoRepository.findAll();
    }

    public Page<Todo> findPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return todoRepository.findAll(pageable);
    }
}

interface TodoRepository extends JpaRepository<Todo, Integer> {

    @Query("from Todo t where t.title like %:filter%")
    List<Todo> findAllByTitleContains(@Param("filter") String filter);


    @Query("from Todo t  where t.title like %:filter%")
    List<Todo> findPageable(@Param("filter") String filter, Pageable pageable);

}

// -------------------------------------------------------------------------

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
class ViewPage {
    private int pageCount;
    private List<Todo> todos;
    private boolean hasPrevious;
    private boolean hasNext;
    private int current;
}


// ------------------------SECURITY------------------------------

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
class SecurityConfigurer {


    public static final String[] WHITE_LIST = {"/auth/login", "/auth/register"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(WHITE_LIST).permitAll()
                .anyRequest()
                .authenticated();

        http.formLogin()
                .defaultSuccessUrl("/", false)
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login");

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
// ----------------------------------------------------------------------------------------------------------------------------------------

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
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthority())
                .build();
    }

    public void createUser(Users users) {
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        userRepository.save(users);
    }
}


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role;

}

interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByUsername(String username);
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
    public String register(@ModelAttribute Users users) {
        users.setRole("USER");
        authService.createUser(users);
        return "redirect:/auth/login";
    }
}
