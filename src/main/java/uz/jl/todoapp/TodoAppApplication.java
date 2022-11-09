package uz.jl.todoapp;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.annotation.Priority;
import javax.persistence.*;
import javax.swing.text.View;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

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
                               @RequestParam(required = false, name = "filter") String searchFilter,
                               @RequestParam(required = false, name = "page", defaultValue = "0") int page,
                               @RequestParam(required = false, name = "size", defaultValue = "2") int size) {
        log.info(searchFilter);
        model.addAttribute("page", todoService.findPageable(page, size, searchFilter));
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

    public ViewPage findPageable(int page, int size, String searchFilter) {
        Pageable pageable = PageRequest.of(page, size);
        List<Todo> todos = todoRepository.findPageable(searchFilter, pageable);
        int pageCount = todoRepository.findAll().size();
        System.out.println(pageCount);
        pageCount = pageCount / size + (pageCount % size == 0 ? -1 : 0);
        System.out.println(pageCount);
        return ViewPage.builder()
                .todos(todos)
                .pageCount(pageCount)
                .hasPrevious(page != 0)
                .hasNext(page != pageCount)
                .current(page)
                .build();
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