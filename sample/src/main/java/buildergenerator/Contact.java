package buildergenerator;

import buildergenerator.annotation.Builder;
import buildergenerator.annotation.AllArgsConstructor;

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

    @Override
    public String toString() {
        return "name: " + name + ", email: " + email + " phone: " + phone;
    }
}