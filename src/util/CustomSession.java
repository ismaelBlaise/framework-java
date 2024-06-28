package util;

import java.util.HashMap;

public class CustomSession {
    private HashMap<String,Object> values=new HashMap<>();

    public CustomSession() {
    }

    public CustomSession(HashMap<String, Object> values) {
        this.values = values;
    }

    public HashMap<String, Object> getValues() {
        return values;
    }

    public void setValues(HashMap<String, Object> values) {
        this.values = values;
    }

    public void add(String key,Object value) throws Exception{
        if(values.containsKey(key)){
            throw new Exception("La cle existe deja");
        }
        this.values.put(key, value);
    }


    public Object get(String key) throws Exception{
        if(!values.containsKey(key)){
            throw new Exception("La cle n'existe pas");
        }
        return this.values.get(key);
    }

    public void remove(String key) throws Exception{
        if(values.containsKey(key)){
            throw new Exception("La cle existe deja");
        }
        this.values.remove(key);
    }

    public void update(String key,Object value) throws Exception{
        if(!values.containsKey(key)){
            throw new Exception("La cle n'existe pas");
        }
        this.values.replace(key, value);
    }

    
}
