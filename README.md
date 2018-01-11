# builder-generator [![](https://jitpack.io/v/WellingtonCosta/builder-generator.svg)](https://jitpack.io/#WellingtonCosta/builder-generator)


This project is a simple example of code generation at compile-time using Java Annotation Processing.

Example:

__Step 1 - Annotate the class with__ ```@Builder```

```java
@Builder
public class Contact {

    public final String name;
    public final String email;
    public final String phone;

    @AllArgsConstructor
    public Contact(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
    
}
```

or

```java
@Builder
public class User {

    private String name;
    private String email;
    private String phone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
```

*Note: The ```@BuilderConstructorWithAllArgs``` annotation sinalize the compiler that the class have a constructor with all arguments.*

__Step 2 - Compile the project__

__Step 3 - Use the builder class generated automatically__

```java
public class App {

    public static void main(String[] args) {
        Contact contact = new ContactBuilder()
                .name("Wellington")
                .email("wellington@email.com")
                .phone("9999999999")
                .build();

        System.out.println("contact name: " + contact.name);
        System.out.println("contact email: " + contact.email);
        System.out.println("contact phone: " + contact.phone);
    }

}
```
