
package EDPProject;


public class ITRUSTEmployee {
    public String username;
    public String password;
    public String role;
    public String name;
    public String id;
    public boolean isActive;

    public ITRUSTEmployee(String username, String password, String role, String name, String id, boolean isActive) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.id = id;
        this.isActive = isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
