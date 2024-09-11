# RFC: HATEOAS Integration in RESTful APIs

## Abstract
This RFC presents an approach to integrating HATEOAS (Hypermedia as the Engine of Application State) into RESTful APIs, comparing naive and production-ready implementations using models and `ModelAssembler`. It includes a breakdown of how to progressively enhance your API from a basic naive solution to an advanced, production-ready one. Additionally, it covers extending HATEOAS to work with API keys and user authentication, with examples and guidelines.

## Naive vs. Production-Ready Solutions

### Naive HATEOAS Solution Overview
In a naive HATEOAS implementation, links are manually added to the API response. This can work for simple APIs but lacks scalability, making it error-prone and hard to maintain.

**Example**: A naive approach to HATEOAS might look like this:

```java
public class Project {
    private Long id;
    private String name;
    private Map<String, String> links = new HashMap<>();

    public Project(Long id, String name) {
        this.id = id;
        this.name = name;
        this.links.put("self", "/api/projects/" + id);
        this.links.put("tasks", "/api/projects/" + id + "/tasks");
    }

    // getters and setters...
}
```

Here, the hypermedia links are manually added within the `Project` entity, making it difficult to scale as the number of links grows.

### Production-Ready HATEOAS Solution Using Model and ModelAssembler
In a production-ready solution, we abstract link generation into a `ModelAssembler`, leveraging libraries such as Spring HATEOAS. This improves maintainability, scalability, and consistency across API responses.

**Example using `ModelAssembler`**:
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectModelAssembler projectModelAssembler;

    public ProjectController(ProjectService projectService, ProjectModelAssembler projectModelAssembler) {
        this.projectService = projectService;
        this.projectModelAssembler = projectModelAssembler;
    }

    @GetMapping("/{id}")
    public EntityModel<Project> getProject(@PathVariable Long id) {
        Project project = projectService.findById(id);
        return projectModelAssembler.toModel(project);
    }
}

@Component
public class ProjectModelAssembler implements RepresentationModelAssembler<Project, EntityModel<Project>> {

    @Override
    public EntityModel<Project> toModel(Project project) {
        return EntityModel.of(project,
            linkTo(methodOn(ProjectController.class).getProject(project.getId())).withSelfRel(),
            linkTo(methodOn(TaskController.class).getTasksByProjectId(project.getId())).withRel("tasks"));
    }
}
```
This approach moves the link generation out of the entity class and into the `ModelAssembler`, providing a cleaner, more scalable solution.

## Expansion to Support API Keys/User Authentication
To support API keys and user authentication in HATEOAS-driven APIs, you can leverage Spring Security alongside the HATEOAS libraries. Based on the authenticated user's role, you can filter which links are visible.

### Steps to Expand HATEOAS for API Key/User:

1. **Add Role-Based Links**: The `ModelAssembler` can dynamically add links based on the user’s roles.
2. **Security Configuration**: Use Spring Security for API key validation and user authentication.
3. **Token Validation**: Ensure tokens are valid before exposing HATEOAS links.
4. **Link Filtering**: Filter out links for unauthorized users.

**Example with Role-Based Link Generation**:
```java
@Override
public EntityModel<Project> toModel(Project project) {
    EntityModel<Project> model = EntityModel.of(project,
        linkTo(methodOn(ProjectController.class).getProject(project.getId())).withSelfRel());

    if (userHasAdminRole()) {
        model.add(linkTo(methodOn(ProjectController.class).deleteProject(project.getId())).withRel("delete"));
    }
    return model;
}

private boolean userHasAdminRole() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
}
```
In this example, only users with the `ROLE_ADMIN` role can see the "delete" link.

## Decision Matrix: Naive vs. Advanced

| Criteria                     | Naive HATEOAS              | Advanced HATEOAS (Spring HATEOAS with ModelAssembler) |
|------------------------------|----------------------------|-------------------------------------------------------|
| **Ease of Development**       | High                       | Medium                                                 |
| **Maintainability**           | Low                        | High                                                   |
| **Scalability**               | Low                        | High                                                   |
| **Security Integration**      | Manual                     | Integrated with Spring Security                        |
| **Link Consistency**          | Prone to errors            | Consistent across endpoints                            |
| **Error Handling**            | Custom                     | Standardized                                           |
| **Pagination, Sorting**       | Manual                     | Automatic                                              |
| **Complex Workflows**         | Hard to implement          | Easy to extend and maintain                            |
| **Performance**               | Inefficient                | Optimized                                              |

## Theory Behind HATEOAS

HATEOAS enables the client to dynamically interact with a RESTful API based on the hypermedia links provided by the server. This decouples the client from the API structure, allowing it to evolve without breaking the client.

When a client requests a resource, the server responds with data and links to related actions or resources. These links serve as guides to what the client can do next, fostering discoverability and navigation.

### Example:
For a `Project` entity:

```json
{
  "id": 1,
  "name": "Project Alpha",
  "_links": {
    "self": "/api/projects/1",
    "tasks": "/api/projects/1/tasks",
    "delete": "/api/projects/1/delete"
  }
}
```

The client doesn't need to hard-code endpoints; instead, it relies on the links returned by the server.

## 15 Points to Make the Solution Production-Ready

1. **Use `ModelAssembler`**: Always use `ModelAssembler` for consistent link generation.
2. **Role-Based Link Generation**: Dynamically expose links based on user roles.
3. **Security Integration**: Integrate Spring Security for API key and user authentication.
4. **Paginated Responses**: Automatically generate pagination links using Spring HATEOAS.
5. **Sorting and Filtering**: Add support for sorting/filtering with corresponding HATEOAS links.
6. **Error Handling**: Implement robust error handling for missing or invalid links.
7. **Versioning**: Include API versioning in links for backward compatibility.
8. **Internationalization**: Enable multi-language support for links and hypermedia responses.
9. **Caching**: Use Redis to cache frequently used hypermedia links.
10. **Documentation**: Automatically document HATEOAS responses using Swagger/OpenAPI.
11. **ETag and Conditional Requests**: Use ETag headers to ensure link freshness.
12. **Testing**: Implement unit and integration tests for HATEOAS links.
13. **Custom Media Types**: Support HAL or JSON:API formats for hypermedia responses.
14. **Audit Logs**: Track the generation and usage of HATEOAS links for security auditing.
15. **Performance Optimization**: Ensure that link generation is optimized for high-traffic environments.

## Checklist of Activities

### Starter:
1. Implement a naive HATEOAS approach by manually adding hypermedia links.
2. Create simple APIs that return data with static links.
3. Add basic pagination links without leveraging a library.

**Example Naive Implementation**:
```java
public class ProjectController {
    @GetMapping("/projects/{id}")
    public Project getProject(@PathVariable Long id) {
        Project project = projectService.findById(id);
        project.addLink("self", "/api/projects/" + id);
        return project;
    }
}
```

### Intermediate:
1. Introduce `ModelAssembler` to automate link generation.
2. Use Spring HATEOAS to create consistent hypermedia links.
3. Add role-based link generation for authenticated users.
4. Implement pagination and sorting links using Spring HATEOAS.

**Intermediate Example**:
```java
@Component
public class ProjectModelAssembler implements RepresentationModelAssembler<Project, EntityModel<Project>> {

    @Override
    public EntityModel<Project> toModel(Project project) {
        return EntityModel.of(project,
            linkTo(methodOn(ProjectController.class).getProject(project.getId())).withSelfRel(),
            linkTo(methodOn(TaskController.class).getTasksByProjectId(project.getId())).withRel("tasks"));
    }
}
```

### Advanced:
1. Integrate rate limiting and throttling for HATEOAS links based on API keys or user roles.
2. Enable caching with Redis to optimize frequently used links.
3. Implement ETag for caching control in responses.
4. Add support for HAL or JSON:API to your responses.
5. Perform load testing to ensure the solution can scale under high traffic.
6. Document the hypermedia-driven API using Swagger/OpenAPI.

**Advanced Role-Based Example**:
```java
@Override
public EntityModel<Project> toModel(Project project) {
    EntityModel<Project> model = EntityModel.of(project,
        linkTo(methodOn(ProjectController.class).getProject(project.getId())).withSelfRel());

    if (hasAdminRole()) {
        model.add(linkTo(methodOn(ProjectController.class).deleteProject(project.getId())).withRel("delete"));
    }
    return model;
}

private boolean hasAdminRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority

().equals("ROLE_ADMIN"));
}
```

## Conclusion
By adopting a progressive approach, starting from naive implementations and evolving toward a production-ready solution using Spring HATEOAS and `ModelAssembler`, APIs can leverage HATEOAS to create more robust, scalable, and secure systems. With careful planning and adherence to best practices, this solution can be extended to support advanced features like role-based link generation, API key authentication, and seamless integration with modern API documentation tools like Swagger.