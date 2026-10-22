# Spring Security with Json Web Tokens (JWT)
## Credits
This example is based on [Dan Vega's tutorial](https://www.danvega.dev/blog/spring-security-jwt). He introduces security with JWT and using the Spring Security OAUTH2 Resource
Server instead of programming a Security Filter for the JWT from scratch as many tutorials (incorrectly) do. As in his tutorial, the client obtains the token
by calling `POST /token` with HTTP Basic credentials.

This example has two main differences with the Dan Vega's tutorial:
* Here, we store the users in the **database** instead of using an *InMemoryUserDetailsManager* which stores users in memory. 
* We use the **HS512** algorithm to sign the JWT token instead of the *RS256* algorithm. The HS512 algorithm uses a single secret key both to sign and to verify the token, 
while the RS256 algorithm uses a pair of keys: a private key to sign and a public key to verify. Both are secure with a strong key. The advantage of RS256 is architectural:
whoever can verify an HS512 token can also create one, while with RS256 the services that only verify tokens never hold the private key. 
The HS512 algorithm is simpler and is enough for this example.

Versions: Spring Boot 4.1.1 (Spring Security 7.1.1), Java 17 or later.

## Running the example
The secret key used to sign the tokens is **not** stored in the repository: anyone who knows it can create valid tokens for any user and role.
It is read from the environment variable `JWT_SECRET`, which must contain at least 64 random bytes encoded in Base64
(HS512 needs a key of at least 512 bits). 64 bytes in Base64 is a string of **88 characters** without line breaks.
The application does not start if the variable is missing (`Could not resolve placeholder 'JWT_SECRET'`) or if the key is shorter than
64 bytes (`HS512 requires a key of at least 512 bits (64 bytes)`).

Each student must generate their own secret. Do not copy a secret from a tutorial or from a classmate.

### Linux and macOS (bash or zsh)
Generate the secret and set it for the current terminal:
```bash
export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
echo ${#JWT_SECRET}          # must print 88
./mvnw spring-boot:run
```
The `tr -d '\n'` is necessary: `openssl` splits its output in lines of 64 characters, and a line break is not a valid Base64 character
(the application would fail with `Illegal base64 character a`). If `openssl` is not installed, use
`head -c 64 /dev/urandom | base64 | tr -d '\n'` instead.

To keep the same secret in every new terminal, generate it once and add it to your shell configuration file
(`~/.bashrc` for bash, `~/.zshrc` for zsh):
```bash
echo "export JWT_SECRET='$(openssl rand -base64 64 | tr -d '\n')'" >> ~/.bashrc
source ~/.bashrc
```

### Windows (PowerShell)
Generate the secret and set it for the current PowerShell window:
```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:JWT_SECRET.Length       # must print 88
.\mvnw.cmd spring-boot:run
```
This works in Windows PowerShell 5.1 and in PowerShell 7. `ToBase64String` does not insert line breaks.

To keep the same secret for your user (it is stored in the registry), run after the commands above:
```powershell
setx JWT_SECRET $env:JWT_SECRET
```
`setx` only affects windows and programs started **afterwards**: open a new terminal and restart IntelliJ.

In the classic command prompt (`cmd.exe`), generate the value with PowerShell as above and set it with
`set JWT_SECRET=<value>` (no quotes, no spaces around `=`), then run `mvnw.cmd spring-boot:run`.

### IntelliJ IDEA
IntelliJ does not read your shell configuration files when it is started from the desktop. Add the variable to the run configuration:
*Run → Edit Configurations… → SecurityJwtApplication → Environment variables* → `JWT_SECRET=<value>`.

### Notes
* Changing the secret invalidates all the tokens issued before: their signature no longer verifies, so clients must log in again.
* The tests use their own key (see `SecurityJwtApplicationTests`), so `./mvnw test` (or `.\mvnw.cmd test`) works without the variable.

All the users in `data.sql` have the password `password123`. Use `src/main/resources/calls.http` to log in and call the endpoints.

## The Example
This example introduces the basic concepts of Spring Security. It uses JSON Web Tokens (JWT), and you can [read about this
sort of token](https://jwt.io/introduction/). Since REST APIs are stateless, we need to use
a mechanism to authenticate and authorize the user in each request. A JWT is a **signed** (not encrypted) token containing the user's information
(name and permissions). Its header and payload are only Base64url encoded, so anyone can read them (paste a token in [jwt.io](https://jwt.io) to see it).
The signature guarantees that nobody without the secret key can create or modify a token. 
We must send this token to the server in the header of each HTTP request (`Authorization: Bearer <token>`). So, the server will
validate the token and allow the user to access the resource if the token is valid and the user has the necessary permissions.
In this example, we use the HS512 algorithm to sign the token with a single secret key. There are other algorithms
that use public and private keys to sign the token.

We can divide the security process into two main parts: authentication and authorization.
* **Authentication** is the process of verifying the identity of a user. That is, to check if the user is who they say they are,
  and the process usually uses a username and password. In our example, when the user logs in, the server will check the username
  and password and, if they are correct, will return a JWT token with their name and roles. We also need to
  store the registered users' passwords **hashed** in the database (with bcrypt). A hash is one way, so we cannot recover the password from it.
  In order to compare the password that the user sends with the one stored in the database, we hash the password sent
  and compare it against the hash stored in the database.
* **Authorization** is the process of verifying what the user has access to. That is, to check if the user has the necessary
  permissions to access the requested resources. When the user makes a request to the server, the client will send the JWT token
  in the header of the request. The server will check the token; if it is valid and the user has permission, it will allow the user to access the resource.
  Spring Security will also add the user's information to the SecurityContext, so we can access it in the Rest controller.

In order to use Spring Security, we need to add the following dependency to the `pom.xml` file:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
        </dependency>
```
Note that this starter adds the part of the OAuth2 protocol that validates JWT tokens (the *resource server*) and already includes the
`spring-boot-starter-security` dependency, so we do not need to add it.
Once we have added the dependency, we need to configure the security. The configuration is placed in the security package. The authentication is defined
in the authentication package, and contains the following classes:
* **'AuthenticationController'** is a REST controller with the endpoint `POST /token`. The client sends the username and password in the
  header `Authorization: Basic base64(username:password)`. Spring Security checks them **before** the controller runs, so the controller
  never sees the password: it receives the already authenticated user (an `Authentication` object) and asks the AuthenticationService for a token.
  If the credentials are wrong, Spring Security answers 401 and the controller is not called.
* **'AuthenticationResponse'** is a DTO with the response returned to the client: `access_token` (the JWT), `token_type` ("Bearer") and
  `expires_in` (seconds). This is the format of the OAuth 2.0 token response.
* **'AuthenticationService'** creates the JWT token for the authenticated user. The token contains the issuer (`iss`), the audience (`aud`),
  the username (`sub`), the issue and expiration times (`iat`, `exp`), a unique id (`jti`) and the user's roles (`roles`). The expiration is set in
  `application.properties` (`application.security.jwt.expiration`, 15 minutes).
* **'JwtEncoder'** and **'JwtDecoder'** are two beans defined in the SecurityConfigurationBeans file used to encode and decode the JWT token. The AuthenticationService uses the encoder to create the token when the user logs in. At the same time, the `BearerTokenAuthenticationFilter` uses the decoder to check the token
  when the user requests the server and includes the token in the HTTP headers. The decoder checks the signature, the algorithm (only HS512 is accepted),
  the expiration, the issuer and the audience. Recall that the token contains the user's name and permissions,
  and so, if the token is valid, the system extracts the user information from it without going to the database.
* **'JwtAuthenticationConverter'** (bean in SecurityConfigurationBeans) converts the `roles` claim of the token into the authorities of the user (`ROLE_USER`, `ROLE_ADMIN`...).
* **'AuthenticationManager'** is an interface that defines the method authenticate, which receives the login and password and returns the authenticated user
  with their permissions. To do so, it needs to find the user from the database (using the UserLabDetailsService) and hash the password (using the
  PasswordEncoder defined in the SecurityConfigurationBeans) to compare it against the one in the database. The HTTP Basic filter of the `/token` chain uses it.
* **'UserLabDetailsService'** implements the UserDetailsService interface and is in charge of getting the user from the database and returning a UserDetails object.
* **'UserLabDetails'** implements the UserDetails interface and represents the user for the security module in Spring. It contains the user's name, password,
  permissions (the roles with the prefix `ROLE_`), and other information such as whether the account is active.

The **authorization** is defined in the authorization package, and only contains the class **'SecurityConfigurationAuthorization'** where two beans of
type SecurityFilterChain are defined. As you can see in the next section, security in Spring contains a chain of filters that intercepts the requests
to the server and carries all the security checks before the request reaches the Rest controller. A request is handled by the first chain (lowest `@Order`) whose `securityMatcher` matches it:
* **tokenFilterChain** (`@Order(1)`) only handles `/token`. It accepts HTTP Basic credentials. 
* **filterChain** (`@Order(2)`) handles all the other requests. It only accepts JWT tokens (`Authorization: Bearer ...`); HTTP Basic is not accepted here.

The second filter configures other security characteristics apart from the authorization rights for each endpoint. Namely, it defines:
* CORS (Cross-Origin Resource Sharing). Browsers block JavaScript from calling a server in another origin unless the server allows it with CORS headers.
  The `CorsConfigurationSource` bean lists the origins allowed (`http://localhost:5173`, a typical front-end development server). An actual application
  lists the domains that host the JavaScript code of its Web HTML pages.
* CSRF (Cross-Site Request Forgery) is an attack that tricks the user's browser into sending unwanted requests to a web application in which they are
  authenticated. It works because browsers attach cookies automatically. This protection is disabled because our token travels in the `Authorization` header,
  which the browser never adds by itself. If the token were stored in a cookie, CSRF protection would be needed again.
* In the headers, we allow HTML frames only when they come from the same origin (the h2-console uses frames). Allowing frames from different origins
  makes *clickjacking* attacks possible.
* The session management is set to stateless because REST APIs are stateless. This means the server does not store the user's session, so
  it does not send a cookie to the client.
* The resource server is configured using the JWT decoder defined in the SecurityConfigurationBeans file. We are using a part of the OAuth2 protocol, and our application plays the role of the resource server. The Oauth2 protocol is a story
  for another day, but you can [read about it](https://auth0.com/intro-to-iam/what-is-oauth-2).
* And finally, we define the authorization rights for each endpoint. In the **authorizeHttpRequests**, we add a RequestMatcher for each endpoint or
  set of endpoints that we want to protect. The matcher receives a list of path templates (endpoints) that will be compared
  with the path of the request and the permissions the user needs to access the resource. When the request path matches one of the templates,
  the server will check if the user has the necessary permissions. It is important to note that when a request matches more than one RequestMatcher,
  the server will use the first added to the SecurityFilterChain. So, the order of the RequestMatchers is important.

Path templates are strings that can contain wildcards. The wildcards are '*' and '**'. The '*' wildcard matches any character except the path separator
while the '**' wildcard matches any character including the path separator. The path separator is the character '/' that separates the directories in the path.
In Spring Security 7 (Spring Framework 7), '**' can only be used at the start or at the end of a template ("/hello/**/bye" is not valid).
Request template examples:
* requestMatchers("/hello") will match the path "/hello"
* requestMatchers("/hello/*") will match the path "/hello/anything" but not "/hello/anything/anythingElse"
* requestMatchers("/hello/**") will match the path "/hello/anything/anythingElse/.../anything"
* requestMatchers("/hello/*/bye") will match the path "/hello/anything/bye" but not "/hello/anything/anything/bye"

The permissions are strings that represent the user's roles. We will give permissions depending on the roles. For example:
* requestMatchers("/hello").permitAll() will allow all users to access the resource '/hello'
* requestMatchers("/hello").authenticated() will allow only authenticated users to access the resource '/hello' independently of their roles
* requestMatchers("/hello").hasRole("USER") will allow only users with the role "USER" (authority `ROLE_USER`) to access the resource '/hello'
* requestMatchers("/hello").hasAnyRole("USER", "ADMIN") will allow only users with the roles "USER" or "ADMIN" to access the resource '/hello'
Note that a user may have more than one role.

The importance of the order of the RequestMatchers. If we have the following code,
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/**").permitAll()
    .requestMatchers("/helloUser").hasRole("USER"))
```
All requests will be allowed because the first RequestMatcher matches all paths.

## Spring Security Architecture
Security is a cross-cutting concern, and it uses a filter that intercepts the requests to the server. You can see the
official documentation [here](https://docs.spring.io/spring-security/reference/servlet/architecture.html).
