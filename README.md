# Spring Security

This example introduces the basic concepts of Spring Security. It uses JSON Web Tokens (JWT), you can read about this
sort of token [here](https://jwt.io/introduction/). Since REST APIs are stateless, we need to use
a mechanism to authenticate and authorize the user in each request. JWT is an encrypted token that contains the user's information 
(for example, name and permissions). We will need to send this token in the header of each HTTP request to the server. So, the server will
validate the token and allow the user to access the resource if the token is valid and the user has the necessary permissions.
For the token to be valid, it must be signed by the server. This way, the server can check if the token is valid by checking the signature. 
In this example we are using the HS256 algorithm to sign the token with a single secret key (the signature). There are other algorithms
that use public and private keys to sign the token.

We can divide security process into two main parts authentication and authorization. 
* **Authentication** is the process of verifying the identity of a user. Thai is, to check if the user is who he says he is,
and is usually done using a username and password. In our example, when the user logs in, the server will check the username
and password and, if they are correct, will return a JWT token with his name and credentials. See also that we need to 
store the registered users with their passwords encrypted in the database. The encryption is only one way, so we can't decrypt the password.
In order to compare the password that the user sends with the password stored in the database, we need to encrypt the password that the user sends
and compare it with the encrypted password stored in the database.
* **Authorization** is the process of verifying what the user has access to. That is, to check if the user has the necessary
permissions to access the requested resources. When the user makes a request to the server, the client will send the JWT token
in the header of the request. The server will check the token and, if it is valid, will allow the user to access the resource.

In order to use Spring Security, we need to add the following dependency to the `pom.xml` file:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
```
Note that we are using a part of the OAuth2 protocol that uses JWT tokens instead of the `spring-boot-starter-security` dependency.
Once we have added the dependency, we need to configure the security, that you can find in the security package. The authentication is defined
in the authentication package, and contains the following classes:
* **'AuthenticationRequest'** is a DTO that represents the request that the client sends to the server when the user logs in. It contains
the username and password.
* **'AuthenticationResponse'** is a DTO that represents the response that the AuthenticatioService returns when the user logs in. It contains
the JWT token created by the service.
* **'AuthenticationController'** is a REST controller that receives the login request, calls the AuthenticationService, and returns the
JWT token to the client.
* **'AuthenticationService'** is a service that receives the login request, checks the username and password, creates the JWT token,
and returns it within an AuthenticationResponse object when authentication is correct. Otherwise, it throws an exception. This class uses the 'UserDetailsService' (defined in its own file),
JwtEncoder and a AuthenticationManager (defined in the SecurityConfigurationBeans file).
* **'JwtEncoder'** and **'JwtDecoder'** are two beans defined in the SecurityConfigurationBeans file that are used to encode and decode the JWT token. The encoder 
is used by the AuthenticationService to create the token when the user logs in, while the decoder is used by the AuthenticationFilter to check the token
when the user makes a request to the server and includes the token in to the HTTP headers. Recall that the token contains the user's name and permissions,
and so, if the token is valid, the system extracts the user information from it without going to the database.
* **'AuthenticationManager'** is an interface that defines the method authenticate, that receives the login and password and returns the authenticated user
with his permissions. To do so, it needs to find the user from the database (using the UserLabDetailsService) and encrypt the password (using the 
PasswordEncoder defined in the SecurityConfigurationBeans) to compare it against the one in the database.
* **'UserLabDetailsService'** implements the UserDetailsService and is in charge of getting the user from the database and returning a UserDetails object.
* **'UserLabDetails'** implements the UserDetails interface and represents the user for the security module in Spring. It contains the user's name, password, 
permissions and other information such as whether the account is active.

The authorization is defined in the authorization package, and only contains the class **'SecurityConfigurationAuthorization'** where a bean of
type SecurityFilterChain is defined. As you can see in the next section, security in Spring is implemented as a chain of filters that intercepts the requests
to the server and carries all the security checks before the request reaches the Rest controller.

This filter configures other security characteristics apart from the authorization rights for each endpoint. Namely, it defines:
* CORS (Cross-Origin Resource Sharing) configuration, that allows the server to receive requests from other domains. In a real application,
it may limit the domains that can access the server to the ones where the JavaScript code of the Web HTML pages is hosted.
* CSRF (Cross-Site Request Forgery) is an attack that tricks the user into executing unwanted actions on a web application in which they are authenticated.
In our case this protection is disabled because REST APIs are stateless, so there is no session to protect.
* In the headers, we allow the client to use HTML frames when they all come from the same origin. Allowing frames from different origins can be a 
security risk making easier to perform a CSRF attack.
* The session management is set to stateless, because REST APIs are stateless. This means that the server does not store the user's session, so
the servers doesn't send a cookie to the client.
* The resource server is configured to use the JWT decoder we defined in the SecurityConfigurationBeans file. Recall
that we are using a part of the OAuth2 protocol and our application plays the role of the resource server. The Oauth2 protocol is a story
for another day, but you can read about it [here](https://auth0.com/intro-to-iam/what-is-oauth-2).
* The HTTP basic is a security scheme that allows the client to authenticate with the server using a username and password. In our case, 
we are not using it.
* And finally, we define the authorization rights for each endpoint. In the **authorizeHttpRequests** we add a RequestMatcher for each endpoint or 
set of endpoints that we want to protect. Let me explain the matcher. The matcher receives a path or 

## Spring Security Architecture
Security is a cross-cutting concern, so it is implemented as a filter that intercepts the requests to the server. You can see the 
official documentation [here]https://docs.spring.io/spring-security/reference/servlet/architecture.html