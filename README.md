# Spring Security with Json Web Tokens (JWT)
## Credits
This example is based on [Dan Vega's tutorial](https://www.danvega.dev/blog/spring-security-jwt). He introduces security with JWT and using the Spring Security OAUTH2 Resource
Server instead of programming a Security Filter for the JWT from scratch as many tutorials (incorrectly) do. 

This example has two main differences with the Dan Vega's tutorial:
* Here, we store the users in the **database** instead of using an *InMemoryUserDetailsManager* which stores users in memory. 
* We use the **HS512** algorithm to sign the JWT token instead of the *RS256* algorithm. The HS512 algorithm uses a single secret key to sign the token, 
while the RS256 algorithm uses a pair of public and private keys. The RS256 algorithm is more secure because the private key is never shared, but it is more complex to implement. 
The HS512 algorithm is simpler and is enough for this example.

## The Example
This example introduces the basic concepts of Spring Security. It uses JSON Web Tokens (JWT), and you can [read about this
sort of token](https://jwt.io/introduction/). Since REST APIs are stateless, we need to use
a mechanism to authenticate and authorize the user in each request. JWT is an encrypted token containing the user's information
(name and permissions). We must send this token to the server in the header of each HTTP request. So, the server will
validate the token and allow the user to access the resource if the token is valid and the user has the necessary permissions.
The server must sign the token for it to be valid. The server can then check the signature to see if the token is valid.
In this example, we use the HS256 algorithm to sign the token with a single secret key (the signature). There are other algorithms
that use public and private keys to sign the token.

We can divide the security process into two main parts: authentication and authorization.
* **Authentication** is the process of verifying the identity of a user. That is, to check if the user is who he says he is,
  and the process usually uses a username and password. In our example, when the user logs in, the server will check the username
  and password and, if they are correct, will return a JWT token with his name and credentials. We also need to
  store the registered users' passwords encrypted in the database. The encryption is only one way, so we cannot decrypt the password.
  In order to compare the password that the user sends with the password stored in the database, we need to encrypt the password sent
  and compare it against the encrypted one stored in the database.
* **Authorization** is the process of verifying what the user has access to. That is, to check if the user has the necessary
  permissions to access the requested resources. When the user makes a request to the server, the client will send the JWT token
  in the header of the request. The server will check the token; if it is valid and the user has permission, it will allow the user to access the resource.
  Spring Security will also add the user's information to the SecurityContext, so we can access it in the Rest controller.

In order to use Spring Security, we need to add the following dependency to the `pom.xml` file:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
```
Note that we use a part of the OAuth2 protocol that uses JWT tokens instead of the `spring-boot-starter-security` dependency.
Once we have added the dependency, we need to configure the security. The configuration is placed in the security package. The authentication is defined
in the authentication package, and contains the following classes:
* **'AuthenticationRequest'** is a DTO that represents the request that the client sends to the server when the user logs in. It contains
  the username and password.
* **'AuthenticationResponse'** is a DTO that represents the response that the AuthenticatioService returns when the user logs in. It contains
  the JWT token created by the service.
* **'AuthenticationController'** is a REST controller that receives the login request, calls the AuthenticationService and returns the
  JWT token to the client.
* **'AuthenticationService'** is a service that receives the login request, checks the username and password, creates the JWT token,
  and returns it within an AuthenticationResponse object when authentication is correct. Otherwise, it throws an exception. This class uses the 'UserDetailsService' (defined in its file),
  JwtEncoder and an AuthenticationManager (defined in the SecurityConfigurationBeans file).
* **'JwtEncoder'** and **'JwtDecoder'** are two beans defined in the SecurityConfigurationBeans file used to encode and decode the JWT token. The AuthenticationService uses the encoder to create the token when the user logs in. At the same time, the AuthenticationFilter uses the decoder to check the token
  when the user requests the server and includes the token in to the HTTP headers. Recall that the token contains the user's name and permissions,
  and so, if the token is valid, the system extracts the user information from it without going to the database.
* **'AuthenticationManager'** is an interface that defines the method authenticate, which receives the login and password and returns the authenticated user
  with his permissions. To do so, it needs to find the user from the database (using the UserLabDetailsService) and encrypt the password (using the
  PasswordEncoder defined in the SecurityConfigurationBeans) to compare it against the one in the database.
* **'UserLabDetailsService'** implements the UserDetailsService interface and is in charge of getting the user from the database and returning a UserDetails object.
* **'UserLabDetails'** implements the UserDetails interface and represents the user for the security module in Spring. It contains the user's name, password,
  permissions, and other information such as whether the account is active.

The **authorization** is defined in the authorization package, and only contains the class **'SecurityConfigurationAuthorization'** where a bean of
type SecurityFilterChain is defined. As you can see in the next section, security in Spring contains a chain of filters that intercepts the requests
to the server and carries all the security checks before the request reaches the Rest controller.

This filter configures other security characteristics apart from the authorization rights for each endpoint. Namely, it defines:
* CORS (Cross-Origin Resource Sharing) configuration allows the server to receive requests from other domains. An actual application may limit the domains that can access the server to the ones that host the JavaScript code of the Web HTML pages.
* CSRF (Cross-Site Request Forgery) is an attack that tricks the user into executing unwanted actions on a web application in which they are
  authenticated and a session is active. This protection is disabled because REST APIs are stateless, so there is no session to protect.
* In the headers, we allow the client to use HTML frames when they all come from the exact origin. Allowing frames from different origins can be a
  security risk, making it easier to perform a CSRF attack.
* The session management is set to stateless because REST APIs are stateless. This means the server does not store the user's session, so
  it does not send a cookie to the client.
* The resource server is configured using the JWT decoder defined in the SecurityConfigurationBeans file. We are using a part of the OAuth2 protocol, and our application plays the role of the resource server. The Oauth2 protocol is a story
  for another day, but you can [read about it](https://auth0.com/intro-to-iam/what-is-oauth-2).
* The HTTP basic is a security scheme that allows the client to authenticate with the server using a username and password. In our case,
  we are not using it.
* And finally, we define the authorization rights for each endpoint. In the **authorizeHttpRequests**, we add a RequestMatcher for each endpoint or
  set of endpoints that we want to protect. The matcher receives a list of path templates (endpoints) that will be compared
  with the path of the request and the permissions the user needs to access the resource. When the request path matches one of the templates,
  the server will check if the user has the necessary permissions. It is important to note that when a request matches more than one RequestMatcher,
  the server will use the first added to the SecurityFilterChain. So, the order of the RequestMatchers is important.

Path templates are strings that can contain wildcards. The wildcards are '*' and '**'. The '*' wildcard matches any character except the path separator
while the '**' wildcard matches any character including the path separator. The path separator is the character '/' that separates the directories in the path.
Request template examples:
* requestMatchers("/hello") will match the path "/hello"
* requestMatchers("/hello/*") will match the path "/hello/anything" but not "/hello/anything/anythingElse"
* requestMatchers("/hello/**") will match the path "/hello/anything/anythingElse/.../anything"
* requestMatchers("/hello/*/bye") will match the path "/hello/anything/bye" but not "/hello/anything/anything/bye"

The permissions are strings that represent the user's roles. We will give permissions depending on the roles. For example:
* requestMatchers("/hello").permitAll() will allow all users to access the resource '/hello'
* requestMatchers("/hello").authenticated() will allow only authenticated users to access the resource '/hello' independently of their roles
* requestMatchers("/hello").access(hasScope("USER") will allow only users with the role "USER" to access the resource '/hello'
* requestMatchers("/hello").access(hasAnyScope("USER", "ADMIN") will allow only users with the roles "USER" or "ADMIN" to access the resource '/hello'
Note that a user may have more than one role.

The importance of the order of the RequestMatchers. If we have the following code,
```java
.authorizeHttpRequests(auth -> {
    auth.requestMatchers("/**").permitAll();
    auth.requestMatchers("/helloUser").access(hasScope("USER"));
})
```
All requests will be allowed because the first RequestMatcher matches all paths.

## Spring Security Architecture
Security is a cross-cutting concern, and it uses a filter that intercepts the requests to the server. You can see the
official documentation [here]https://docs.spring.io/spring-security/reference/servlet/architecture.html