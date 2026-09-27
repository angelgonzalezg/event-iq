# EventIQ - View Conventions

How to add a page to EventIQ. Every page reuses the shared layout in `src/main/resources/templates/fragments/layout.html`, so the menu, alerts and styles stay consistent and nobody needs to edit shared files to add a view. The list of pages is in the README, under [Views](../README.md#views).

---

## Routes and controllers

Routes are in English, lowercase and plural, with hyphens between words (`/events`, `/payments`). A module's pages follow this pattern:

| Action | Request | Template or response |
|---|---|---|
| List | `GET /events` | `events/list.html` |
| Detail | `GET /events/{id}` | `events/detail.html` |
| Create form | `GET /events/new` | `events/form.html` |
| Create | `POST /events` | redirect to `/events/{id}` |
| Edit form | `GET /events/{id}/edit` | `events/form.html` |
| Update | `POST /events/{id}` | redirect to `/events/{id}` |
| Delete | `POST /events/{id}/delete` | redirect to `/events` |

- HTML forms only send `GET` and `POST`, so views update and delete with `POST`. `PUT` and `DELETE` belong to the REST API under `/api/**`.
- After a successful `POST`, redirect instead of rendering a template (Post/Redirect/Get), so reloading the page does not submit the form twice. When validation fails, render the form again with the errors.
- Views and the REST API are separate controllers in the module's package, and both call the same service: `EventController` (`@Controller`, returns templates) and `EventApiController` (`@RestController`, under `/api/events`).

---

## Templates

| Folder | Contents |
|---|---|
| `templates/fragments/` | Shared layout |
| `templates/events/` | Events & Attendees pages, including registrations and the assistant |
| `templates/payments/` | Payments & Reports pages, including the analytics dashboard |
| `templates/` | `home.html` and `error.html` |

Name templates after what they show: `list.html`, `detail.html`, `form.html`, or a descriptive name for single pages (`events/assistant.html`, `payments/analytics.html`).

### Using the layout

A page replaces its own `<html>` with the layout and passes three parameters:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(title=#{events.list.title}, section='events', content=~{::main})}">
<body>
<main>
  <div class="page-header">
    <div>
      <h1 th:text="#{events.list.title}">Events</h1>
      <p class="page-subtitle" th:text="#{events.list.subtitle}">Upcoming and past events</p>
    </div>
    <div class="page-actions">
      <a class="btn btn-primary" th:href="@{/events/new}">
        <i class="bi bi-plus-lg" aria-hidden="true"></i><span th:text="#{events.new}">New event</span>
      </a>
    </div>
  </div>

  <!-- page content -->
</main>
</body>
</html>
```

| Parameter | Value |
|---|---|
| `title` | Text for the browser tab, shown as `<title> · EventIQ`. Usually a message key: `#{events.list.title}`. |
| `section` | Menu item to highlight: `home`, `events`, `new-registration`, `assistant`, `payments`, `new-payment` or `analytics`. Pages without their own menu item, such as a detail page or an edit form, use the item of their list. Use `''` for none. |
| `content` | Always `~{::main}`: the page's `<main>` element. |

Everything outside `<main>` is discarded, so pages do not need a `<head>`. The layout already loads Bootstrap, Bootstrap Icons and `app.css`.

**Page-specific scripts** go at the end of `<main>`. Load libraries from jsDelivr with the version in the URL and an `integrity` hash, like the layout does. For the analytics dashboard:

```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.5.1/dist/chart.umd.min.js"
        integrity="sha384-jb8JQMbMoBUzgWatfe6COACi2ljcDdZQ2OxczGA3bGNeWe+6DChMTBJemed7ZnvJ" crossorigin="anonymous"></script>
<script th:inline="javascript">
  const revenueByEvent = /*[[${revenueByEvent}]]*/ [];
  const primary = getComputedStyle(document.documentElement).getPropertyValue('--eiq-primary').trim();
  // new Chart(...)
</script>
```

Scripts inside `<main>` run before Bootstrap's JavaScript, which loads at the end of the page. If a script needs `bootstrap.*`, run it on `DOMContentLoaded`.

---

## Texts

Visible text never goes directly in a template: it goes in a message file and the template refers to it with `#{key}`. The text inside the tag (`<h1 th:text="#{events.list.title}">Events</h1>`) is only a placeholder that Thymeleaf replaces.

| File | Keys | Owner |
|---|---|---|
| `i18n/messages.properties` | Shared: `app.*`, `nav.*`, `alert.*`, `home.*`, `error.*` | Tech Lead |
| `i18n/events.properties` | `events.*`, `registrations.*`, `assistant.*` | Events & AI |
| `i18n/payments.properties` | `payments.*`, `analytics.*` | Payments & Analytics |

- Keys are lowercase and grouped by page: `events.list.title`, `events.form.name`, `payments.status.pending`.
- Parameters use `{0}`, `{1}`: `events.capacity=Capacity: {0}` and `th:text="#{events.capacity(${event.capacity})}"`. In a message with parameters, write an apostrophe as `''`.
- Bean Validation messages can use keys too: `@NotBlank(message = "{events.form.name.required}")`.
- The UI is in English. Spanish can be added later with `messages_es.properties`, `events_es.properties` and `payments_es.properties`, without changing templates.
- `spring.messages.basename` in `application.yml` lists the three files as a comma-separated string. Written as a YAML list, Spring Boot does not detect them and every text shows as `??key_en_US??`.

---

## Alerts

The layout shows these model attributes above the page content. Their value is a message key (preferred) or ready-to-show text:

| Attribute | Style | Use it when |
|---|---|---|
| `successMessage` | Green, dismissible | An action finished. Usually a flash attribute before a redirect. |
| `errorMessage` | Red, dismissible | An action failed for a reason that is not a form field. Field errors go next to the field. |
| `serviceWarning` | Amber | Gemini or the analytics service did not respond. The rest of the page still works. |

```java
@PostMapping
String create(@Valid @ModelAttribute("event") EventForm form, BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) {
        return "events/form";
    }
    Event event = eventService.create(form);
    redirect.addFlashAttribute("successMessage", "events.created");
    return "redirect:/events/" + event.getId();
}
```

```java
@GetMapping
String dashboard(Model model) {
    try {
        model.addAttribute("revenueByEvent", analyticsClient.revenueByEvent());
    } catch (RestClientException e) {
        model.addAttribute("serviceWarning", "analytics.unavailable");
    }
    return "payments/analytics";
}
```

---

## Styling

Use [Bootstrap 5.3](https://getbootstrap.com/docs/5.3/) components and utilities, and [Bootstrap Icons](https://icons.getbootstrap.com/) with `aria-hidden="true"` when the icon sits next to text. Colors come from `static/css/app.css` (`--eiq-primary` and the other tokens at the top of the file): use Bootstrap classes such as `btn-primary` or `text-body-secondary` instead of hex colors in templates, and propose new shared styles in a pull request to `app.css`.

Patterns to reuse:

| Need | Markup |
|---|---|
| Page title and actions | `.page-header` with `<h1>`, `.page-subtitle` and `.page-actions` (see the template above) |
| Content block | `.card` with `.card-body` |
| Table | `.card` > `.table-responsive` > `table.table.table-hover.align-middle` |
| Status | `badge text-bg-success` (confirmed, paid), `text-bg-warning` (pending), `text-bg-secondary` (cancelled, refunded) |
| Nothing to show yet | `.app-empty-state` with a heading, a short text and the main action |

A form field with its validation error:

```html
<div class="mb-3">
  <label class="form-label" for="name" th:text="#{events.form.name}">Name</label>
  <input class="form-control" id="name" th:field="*{name}" th:errorclass="is-invalid">
  <div class="invalid-feedback" th:errors="*{name}">Name is required.</div>
</div>
```

---

## Static files and security

- Files in `src/main/resources/static/` are served from the site root: `static/css/app.css` is `/css/app.css`. Link them with `th:href="@{/css/app.css}"` so the path stays right if the app ever runs under a context path.
- Third-party CSS and JavaScript load from jsDelivr, with the version pinned and an `integrity` hash. To upgrade, change both together; see [Dependency updates](ways-of-working.md#dependency-updates).
- When the security story (EIQ-29) replaces `DevSecurityConfig`, it must allow `/css/**`, `/img/**` and `/error` without signing in. Otherwise the login page loads without styles and error pages redirect to the login page.

---

## Testing views

Test view controllers with `@WebMvcTest`, which starts only the web layer. `@WithMockUser` signs in a test user, so the test does not depend on the security configuration; replace services with `@MockitoBean`. `config/HomeControllerTests` is the reference:

```java
@WebMvcTest(EventController.class)
@WithMockUser
class EventControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void listShowsTheEventsPage() throws Exception {
        this.mvc.perform(get("/events"))
            .andExpect(status().isOk())
            .andExpect(view().name("events/list"));
    }
}
```
