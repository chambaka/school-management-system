package tz.co.chambaka.school.management.push;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/public-key")
    @PreAuthorize(Access.SCHOOL_USER)
    public Map<String, String> publicKey() {
        return Map.of("publicKey", pushService.publicKey() == null ? "" : pushService.publicKey());
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.SCHOOL_USER)
    public void subscribe(@CurrentUser UserPrincipal principal, @Valid @RequestBody PushSubscribeRequest request) {
        pushService.subscribe(principal.getId(), request);
    }

    @DeleteMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.SCHOOL_USER)
    public void unsubscribe(@CurrentUser UserPrincipal principal, @RequestParam String endpoint) {
        pushService.unsubscribe(principal.getId(), endpoint);
    }
}
