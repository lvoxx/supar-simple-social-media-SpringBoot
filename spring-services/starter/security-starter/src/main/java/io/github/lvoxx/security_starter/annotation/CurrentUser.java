package io.github.lvoxx.security_starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method parameter annotation for injecting the currently authenticated
 * {@link io.github.lvoxx.starter.security.model.UserPrincipal} into a handler
 * method.
 *
 * <p>
 * Resolved by
 * {@link io.github.lvoxx.starter.security.config.CurrentUserArgumentResolver}
 * which reads the principal from the Reactor Context.
 * </p>
 *
 * <h3>Usage:</h3>
 * 
 * <pre>
 * {@code
 * &#64;GetMapping("/me")
 * public Mono<ApiResponse<UserResponse>> getMe(@CurrentUser UserPrincipal user) {
 *     return userService.findById(user.userId());
 * }
 *
 * &#64;PostMapping("/posts")
 * public Mono<ApiResponse<PostResponse>> createPost(
 *         &#64;CurrentUser UserPrincipal user,
 *         @RequestBody @Valid CreatePostRequest request) {
 *     return postService.create(user.userId(), request);
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * Whether the user must be authenticated for this endpoint.
     * If {@code true} (default) and no authenticated user is present,
     * a 401 response is returned.
     */
    boolean required() default true;
}