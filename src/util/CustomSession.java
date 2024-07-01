package util;

import jakarta.servlet.http.HttpSession;

public class CustomSession {
    private HttpSession session;

    public CustomSession(HttpSession session) {
        this.session=session;
    }

    public void add(String key,Object value){
        this.session.setAttribute(key,value);
    }


    public Object get(String key){
        return this.session.getAttribute(key);
    }

    public void remove(String key){
        this.session.removeAttribute(key);
    }

    public void update(String key,Object value){
        this.session.setAttribute(key, value);
    }

    
}
