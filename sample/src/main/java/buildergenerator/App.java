package buildergenerator;

public class App {

    public static void main(String[] args) {
        Contact contact = new ContactBuilder()
                .name("Wellington")
                .email("wellington@email.com")
                .phone("85986846409")
                .build();

        System.out.println(contact);

        User user = new UserBuilder()
                .name("Wellington")
                .username("wellington")
                .password("p@ssw0rd")
                .build();

        System.out.println(user);
    }

}