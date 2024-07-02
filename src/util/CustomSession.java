package util;

import java.util.HashMap;

public class CustomSession {

    private HashMap<String,Object> values=new HashMap<>();

    public CustomSession() {
    }

    public void add(String key,Object value)  throws Exception{
        if(values.containsKey(key)){
            throw new Exception("La cle existe deja");
        }
        values.putIfAbsent(key, value);
    }

    public Object get(String key) throws Exception{
        if(!values.containsKey(key)){
            throw new Exception("La cle n'existe pas");
        }
        return values.get(key);
    }

    public void update(String key,Object value) throws Exception{
        if(!values.containsKey(key)){
            throw new Exception("La cle n'existe pas");
        }
        values.replace(key, value);
    }
    
    public void delete(String key) throws Exception{
        if(!values.containsKey(key)){
            throw new Exception("La cle n'existe pas");
        }
        this.values.remove(key);
    }

    public HashMap<String, Object> getValues() {
        return values;
    }

    public void setValues(HashMap<String, Object> values) {
        this.values = values;
    }
}
