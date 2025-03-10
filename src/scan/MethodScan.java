package scan;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import annotation.Authentification;
import annotation.DateFormat;
import annotation.FieldAnnotation;
import annotation.Numeric;
import annotation.Param;
import annotation.ParamObject;
import annotation.Public;
import annotation.Range;
import annotation.Required;
import annotation.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import util.CustomPart;
import util.CustomSession;

public class MethodScan {
    private Object objet;
    private Map<String, String> handleError;
    private Method method;
    private HttpServletRequest request;

    // Constructor to initialize the required attributes
    public MethodScan(Map<String, String> handleError, Method method,Object object, HttpServletRequest request) {
        this.handleError = handleError;
        this.objet=object;
        this.method = method;
        this.request = request;
    }

    public void authentification() throws Exception {
        HttpSession session = request.getSession(false);
        if(method.isAnnotationPresent(Authentification.class)==false){
            return;
        }
    
        String rolesInSession = (String) session.getAttribute("role");
        
    
        List<String> userRoles = Arrays.asList(rolesInSession.split(","));
    
        if (this.objet.getClass().isAnnotationPresent(Authentification.class)) {
            if (session == null || Boolean.FALSE.equals(session.getAttribute("authenticated"))) {
                throw new Exception("Accès interdit : l'utilisateur n'est pas authentifié.");
            }
            if (rolesInSession == null || rolesInSession.isBlank()) {
                throw new Exception("Accès interdit : aucun rôle défini dans la session.");
            }
            Authentification classAuth = this.objet.getClass().getAnnotation(Authentification.class);
            if (method.isAnnotationPresent(Public.class)) {
                return;
            }
            if (!classAuth.name().isBlank()) {
                String[] authorizedRoles = classAuth.name().split(",");
                boolean isAuthorized = Arrays.stream(authorizedRoles)
                                             .map(String::trim)
                                             .anyMatch(userRoles::contains);
                if (!isAuthorized) {
                    throw new Exception("Accès interdit : rôle insuffisant pour accéder à cette classe.");
                }
            }
        }
    
        if (method.isAnnotationPresent(Authentification.class)) {
            if (session == null || Boolean.FALSE.equals(session.getAttribute("authenticated"))) {
                throw new Exception("Accès interdit : l'utilisateur n'est pas authentifié.");
            }
            if (rolesInSession == null || rolesInSession.isBlank()) {
                throw new Exception("Accès interdit : aucun rôle défini dans la session.");
            }
            Authentification methodAuth = method.getAnnotation(Authentification.class);
            if (!methodAuth.name().isBlank()) {
                String[] authorizedRoles = methodAuth.name().split(",");
                boolean isAuthorized = Arrays.stream(authorizedRoles)
                                             .map(String::trim)
                                             .anyMatch(userRoles::contains);
                if (!isAuthorized) {
                    throw new Exception("Accès interdit : rôle insuffisant pour accéder à cette méthode.");
                }
            }
        }
    }
    



    public Object[] getMethodParameters() throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];
    
        for (int i = 0; i < parameters.length; i++) {
            Param requestParam = parameters[i].getAnnotation(Param.class);
            ParamObject objectParam = parameters[i].getAnnotation(ParamObject.class);

            if (requestParam != null) {
                String paramName = requestParam.name();
                if (parameters[i].getType() == CustomPart.class) {
                    Part part = request.getPart(paramName);
                    paramValues[i] = new CustomPart(part);
                } else {
                    String paramValue = request.getParameter(paramName);
                    paramValues[i] = convertParameterType(paramValue, parameters[i].getType());
                }
            } else if (objectParam != null) {
                paramValues[i] = handleObjectParam(parameters[i].getType(), objectParam);
            } else if (parameters[i].getType() == CustomSession.class) {
                paramValues[i] = handleCustomSession();
            } 
            else {
                throw new Exception("<b>ETU002391</b>  les parametres doivent etre annoter par @Param ou @ParamObject");
            }
        }
        return paramValues;
    }
    
    
    private Object handleObjectParam(Class<?> paramType, ParamObject objectParam) throws Exception {
        Object paramObject = paramType.getDeclaredConstructor().newInstance();
        Map<String, String[]> parameterMap = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String fullParamName = entry.getKey();
            String[] paramNameParts = fullParamName.split("\\.");
            if (paramNameParts.length < 2) continue;

            String objectName = paramNameParts[0];
            String fieldName = paramNameParts[1];

            if (objectName.equalsIgnoreCase(objectParam.name())) {
                Field[] fields = paramType.getDeclaredFields();
                for (Field field : fields) {
                    String fieldValue = request.getParameter(fullParamName);
                    if (field.isAnnotationPresent(FieldAnnotation.class) && field.getAnnotation(FieldAnnotation.class).name().equalsIgnoreCase(fieldName)) {
                        validateField(field, objectName, fieldName, fieldValue);
                    }

                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        validateField(field, objectName, fieldName, fieldValue);
                        field.setAccessible(true);
                        field.set(paramObject, convertParameterType(fieldValue, field.getType()));
                    }else{
                        validateRole(paramObject, field);
                    }
                }
            }
        }

        return paramObject;
    }

    private CustomSession handleCustomSession() {
        HttpSession session = request.getSession();
        CustomSession customSession = new CustomSession();
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            customSession.add(attributeName, session.getAttribute(attributeName));
        }
        return customSession;
    }

    private void validateField(Field field, String objectName, String fieldName, String value) throws Exception {
    
        
 
        if (field.isAnnotationPresent(Required.class) && (value == null || value.isEmpty())) {
            addError(objectName, fieldName, field.getAnnotation(Required.class).message(), value);
        }

         
        if (field.isAnnotationPresent(Numeric.class)) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                addError(objectName, fieldName, field.getAnnotation(Numeric.class).message(), value);
            }
        }

        
        if (field.isAnnotationPresent(annotation.DateFormat.class)) {
            annotation.DateFormat dateFormat = field.getAnnotation(annotation.DateFormat.class);
            try {
                new SimpleDateFormat(dateFormat.format()).parse(value);
            } catch (ParseException e) {
                addError(objectName, fieldName, field.getAnnotation(DateFormat.class).message(), value);
            }
        }

         
        if (field.isAnnotationPresent(annotation.Range.class)) {
            annotation.Range range = field.getAnnotation(annotation.Range.class);
            try {
                double numericValue = Double.parseDouble(value);
                if (numericValue < range.min() || numericValue > range.max()) {
                    addError(objectName, fieldName, field.getAnnotation(Range.class).message(), value);
                }
            } catch (NumberFormatException e) {
                addError(objectName, fieldName, "La valeur doit être un nombre pour vérifier la plage.", value);
            }
        }

         
        
    }

    public void validateRole(Object object,Field field) throws Exception{
        if (field.isAnnotationPresent(Role.class)) {
            annotation.Role roleAnnotation = field.getAnnotation(annotation.Role.class);
            String roleName = roleAnnotation.name();  
            HttpSession session = request.getSession();  
            if (roleName != null && !roleName.isBlank()) {
                field.setAccessible(true);
                field.set(object,roleName);
                String existingRoles = (String) session.getAttribute("role");
                if (existingRoles == null || existingRoles.isBlank()) {
                    System.out.println("iSMAAAA");
                    session.setAttribute("role", roleName);
                } else {
                    
                    List<String> roleList = new ArrayList<>(Arrays.asList(existingRoles.split(",")));
                    if (!roleList.contains(roleName)) {
                        roleList.add(roleName);
                        session.setAttribute("role", String.join(",", roleList));
                    }
                }
            }

            
            session.setAttribute("authenticated", true);
        }
    }

    

    
    private void addError(String objectName, String fieldName, String message, String value) {
        String key = objectName + "." + fieldName;
        String errorKey = key + ".err";
    
        
        handleError.put(key, value);
    
         
        if (handleError.containsKey(errorKey)) {
            String existingMessage = handleError.get(errorKey);
            
             
            if (!existingMessage.contains(message)) {
                handleError.replace(errorKey, existingMessage + "," + message);
            }
             
        } else {
            
            handleError.put(errorKey, message);
        }
    }
    
    

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object convertParameterType(String paramValue, Class<?> paramType) throws Exception {
        if (paramValue == null) {
            return null;
        }

        if (paramType == String.class) {
            return paramValue;
        } else if (paramType == int.class || paramType == Integer.class) {
            return Integer.parseInt(paramValue);
        } else if (paramType == long.class || paramType == Long.class) {
            return Long.parseLong(paramValue);
        } else if (paramType == double.class || paramType == Double.class) {
            return Double.parseDouble(paramValue);
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            return Boolean.parseBoolean(paramValue);
        } else if (paramType == Date.class) {
            return Date.valueOf(paramValue);
        } else if (paramType.isEnum()) {
            return Enum.valueOf((Class<Enum>) paramType, paramValue);
        } else {
            Constructor<?> constructor = paramType.getConstructor();
            return constructor.newInstance(paramValue);
        }
    }
}
