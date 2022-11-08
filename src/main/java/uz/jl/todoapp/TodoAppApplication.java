package uz.jl.todoapp;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.annotation.Priority;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class TodoAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoAppApplication.class, args);
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


    public enum Priority {
        LOW, MEDIUM, HIGH
    }

}

@Controller
@RequestMapping("/")
record TodoController(TodoService todoService) {
    @GetMapping
    public String todoListPage(Model model) {
        model.addAttribute("todos", todoService.getAll());
        return "todo/todo_list";
    }

    @GetMapping("/add")
    public String addTodoPage() {
        return "todo/todo_add";
    }

    @PostMapping("/add")
    public String addTodo(@ModelAttribute Todo todo) {
        todoService.create(todo);
        return "redirect:/";
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
}

interface TodoRepository extends JpaRepository<Todo, Integer> {
}

// -------------------------------------------------------------------------
record MailService() {
}

record TemplateGeneratorService() {
}