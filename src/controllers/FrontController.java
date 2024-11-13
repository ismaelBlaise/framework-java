package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.CustomSession;
import util.Mapping;
import util.ModelAndView;
import util.VerbAction;
import annotation.Controller;
import annotation.FieldAnnotation;
import annotation.Numeric;
import annotation.ParamObject;
import annotation.Post;
import annotation.Required;
import annotation.RestApi;
import annotation.Url;
import annotation.Param;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import java.util.ArrayList;
import jakarta.servlet.ServletContext;

import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;



public class FrontController extends HttpServlet {
    private List<String> controllerList = new ArrayList<>();
    private Map<String, Mapping> urlMappings = new HashMap<>();
    private boolean initialized = false;
    private Gson gson=new Gson();
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        

        synchronized (this) {
            if (!initialized) {
                try {
                    initControllers(request);
                    initialized = true;
                } catch (Exception e) {
                    try (PrintWriter out = response.getWriter()) {
                        out.println("Initialization error: " + e.getMessage());
                    }
                    return;
                }
            }
        }

        try (PrintWriter out = response.getWriter()) {
            String requestURL = request.getRequestURL().toString();
            String requestMethod = request.getMethod();
            String baseUrl = getBaseUrl(request);

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("<style>.error-message {\n" + //
                    "    color: red;\n" + //
                    "    font-weight: bold;\n" + //
                    "}\n" + //
                    "\n" + //
                    ".stack-trace {\n" + //
                    "    font-family: monospace;\n" + //
                    "    margin-top: 10px;\n" + //
                    "    padding: 10px;\n" + //
                    "    background-color: #f8f8f8;\n" + //
                    "    border: 1px solid #ccc;\n" + //
                    "}\n" + //
                    "</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p><b>URL:</b> " + requestURL + "</p>");
            out.println("<p><b>Méthode HTTP:</b> " + requestMethod + "</p>");

            out.println("<p><b>Contrôleurs Disponibles:</b></p>");
            out.println("<ul>");
            for (String controller : controllerList) {
                out.println("<li>" + controller + "</li>");
            }
            out.println("</ul>");

            String mappedURL = requestURL.replace(baseUrl, "");
            if (!urlMappings.containsKey(mappedURL)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "L'URL demandée est introuvable.");
                return;
            }
            
            if (urlMappings.containsKey(mappedURL)) {
                try {
                
                    Mapping map = urlMappings.get(mappedURL);
                    if(!map.getVerbAction().testVerbAction(request.getMethod(), mappedURL)){
                        throw new Exception("Vous essayez d'utiliser une requette avec la methode "+request.getMethod()+" au lieu de "+map.getVerbAction().getVerb());
                    }
                    out.println("<b>Classe du Contrôleur:</b> " + map.getControlleur() + "<br>");
                    out.println("<b>Méthode Associée:</b> " + map.getMethode() + "<br>");

                
                    Class<?> clazz = Class.forName(map.getControlleur());
                    Method[] methods = clazz.getDeclaredMethods();
                    Method method = null;
        
                    for (Method method1 : methods) {
                        if (method1.getName().equals(map.getMethode())) {
                            method = method1;
                            break;
                        }
                    }

                    if (method == null) {
                        throw new Exception("Méthode non trouvée : " + map.getMethode());
                    }

                    try{
                        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                        Field[] fields=clazz.getDeclaredFields();
                        for(Field field: fields){
                            if(field.getType()==CustomSession.class){
                                field.setAccessible(true);
                                field.set(controllerInstance, new CustomSession());
                            }
                        }

                        Object[] methodParam=getMethodParameters(method, request);
                        Object result = method.invoke(controllerInstance,methodParam );

                       


                        for(Object param: methodParam){
                            if(param instanceof CustomSession){
                                synchronizeSession(request.getSession(),(CustomSession) param);
                            }
                        }
                        response.setContentType("text/html;charset=UTF-8");
                        if (result instanceof String) {
                            out.println("<br>Résultat de l'invocation de méthode : " + result);
                        } else if (result instanceof ModelAndView) {
                            
                            
                            ModelAndView modelAndView = (ModelAndView) result;
                            if(method.isAnnotationPresent(RestApi.class)){
                                response.setContentType("application/json;charset=UTF-8");
                                
                                out.println(gson.toJson(modelAndView.getData()));
                                
                                return;
                            }
                            for (String key : modelAndView.getData().keySet()) {
                                request.setAttribute(key, modelAndView.getData().get(key));
                            }
                            request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
                            return;
                        }
                        else{
                            if(method.isAnnotationPresent(RestApi.class)){
                                response.setContentType("application/json;charset=UTF-8");
                                out.println(result);
                            }
                        }
                    }catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof Exception) {
                            throw (Exception) cause; 
                        } else {
                            throw new Exception("Erreur lors de l'invocation : " + cause.getMessage());
                        }
                    }
                } catch (Exception e) {
                    String referer = request.getHeader("Referer");
                    if(referer!=null){
                        request.setAttribute("error", e.getMessage());
                        request.getRequestDispatcher(referer).forward(request, response);
                    }
                    out.println("<div class='error-message'>Erreur lors de l'invocation : " + e.getMessage() + "</div>");
                    out.println("<div class='stack-trace'>Trace de la pile :");
                    for (StackTraceElement element : e.getStackTrace()) {
                        out.println("<div>" + element.toString() + "</div>");
                    }
                    out.println("</div>");
                    e.printStackTrace();
                }
            } else {
                out.println("<p class='error-message' >Aucune méthode associée à cet URL.</p>");
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

    private void initControllers(HttpServletRequest request) throws Exception {
        ServletContext context = getServletContext();
        String packageName = context.getInitParameter("package-to-scan");

        if (packageName == null) {
            throw new Exception("Aucune package-to-scan trouver dans le context.");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));
        boolean packageEmpty = true;

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File directory = new File(resource.toURI());
                if (directory.exists() && isDirectoryNotEmpty(directory)) {
                    packageEmpty = false;
                    scanControllers(directory, packageName);
                }
            }
        }

        if (packageEmpty) {
            throw new Exception("Le package " + packageName + " est vide.");
        }

        if (controllerList.isEmpty()) {
            throw new Exception("Aucune class annoter par @Controller trouver dans :" + packageName);
        }
    }

    private boolean isDirectoryNotEmpty(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    private void scanControllers(File directory, String packageName) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Url.class)) {
                                validateAndRegisterMethod(clazz, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception("Class non trouve: " + className, e);
                }
            }
        }
    }

    private void validateAndRegisterMethod(Class<?> clazz, Method method) throws Exception {
        if (method.getReturnType().equals(String.class) || method.getReturnType().equals(ModelAndView.class)) {
            String urlName = null;
            if(method.isAnnotationPresent(Url.class)){
                Url urlAnnotation=method.getAnnotation(Url.class);
                urlName = urlAnnotation.url();
                
            }

            if (urlName != null && urlMappings.containsKey(urlName)) {
                throw new Exception("URL " + urlName + " is already defined.");
            } else if (urlName != null) {
                if(method.isAnnotationPresent(Post.class)){
                    urlMappings.put(urlName, new Mapping(clazz.getName(), method.getName(),new VerbAction("POST", urlName)));
                }
                else{
                    urlMappings.put(urlName, new Mapping(clazz.getName(), method.getName(),new VerbAction("GET",urlName)));
                }
            }
        } else {
            throw new Exception("Les methode doivent retourner soit String soit ModelAndView.");
        }
    }
    

    private Object[] getMethodParameters(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];
    
        for (int i = 0; i < parameters.length; i++) {
            Param requestParam = parameters[i].getAnnotation(Param.class);
            ParamObject objectParam = parameters[i].getAnnotation(ParamObject.class);

            if (requestParam != null) {
                 
                String paramName = requestParam.name();
                String paramValue = request.getParameter(paramName);
                paramValues[i] = convertParameterType(paramValue, parameters[i].getType());
            } else if (objectParam != null) {
                
                Class<?> paramType = parameters[i].getType();
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
                            String fieldValue = null;
                            
                            FieldAnnotation fieldAnnotation = field.getAnnotation(FieldAnnotation.class);
                            if (fieldAnnotation != null && fieldAnnotation.name().equalsIgnoreCase(fieldName)) {
                                

                                
                                fieldValue = request.getParameter(fullParamName);
                                validateField(field, fieldValue);
                            } else if (field.getName().equalsIgnoreCase(fieldName)) {
                                fieldValue = request.getParameter(fullParamName);
                            }

                            if (fieldValue != null) {
                                field.setAccessible(true);
                                field.set(paramObject, convertParameterType(fieldValue, field.getType()));
                            }
                        }
                    }
                }

                paramValues[i] = paramObject;
            }
            else if(parameters[i].getType()== CustomSession.class){
                HttpSession session=request.getSession();
                CustomSession customSession=new CustomSession();
                Enumeration<String> attributeNames=session.getAttributeNames();
                while(attributeNames.hasMoreElements()){
                    String attributeName=attributeNames.nextElement();
                    customSession.add(attributeName, session.getAttribute(attributeName));
                }
                
                paramValues[i]=customSession;
            }
            else if(requestParam == null && objectParam == null && parameters[i].getType() != CustomSession.class){
                throw new Exception("<b>ETU002391</b>  les parametres doivent etre annoter par @Param ou @ParamObject");
            }
            
        }
    
        return paramValues;
    }
    
    private void validateField(Field field, String value) throws Exception {
         
        if (field.isAnnotationPresent(Required.class) && (value == null || value.isEmpty())) {
            throw new Exception(field.getAnnotation(Required.class).message());
        }
        
         
        if (field.isAnnotationPresent(Numeric.class)) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new Exception(field.getAnnotation(Numeric.class).message());
            }
        }
        
         
        if (field.isAnnotationPresent(annotation.DateFormat.class)) {
            annotation.DateFormat dateFormat = field.getAnnotation(annotation.DateFormat.class);
            try {
                new SimpleDateFormat(dateFormat.format()).parse(value);
            } catch (ParseException e) {
                throw new Exception(dateFormat.message());
            }
        }

         if (field.isAnnotationPresent(annotation.Range.class)) {
            annotation.Range range = field.getAnnotation(annotation.Range.class);
            try {
                double numericValue = Double.parseDouble(value);
                if (numericValue < range.min() || numericValue > range.max()) {
                    throw new Exception(range.message());
                }
            } catch (NumberFormatException e) {
                throw new Exception("La valeur doit être un nombre pour verifier la plage.");
            }
        }
    }


    private void synchronizeSession(HttpSession httpSession, CustomSession customSession) {
        Map<String, Object> customSessionValues = customSession.getValues();
        Enumeration<String> httpSessionAttributeNames = httpSession.getAttributeNames();
        List<String> attributesToRemove = new ArrayList<>();
 
        while (httpSessionAttributeNames.hasMoreElements()) {
            String attributeName = httpSessionAttributeNames.nextElement();
            if (!customSessionValues.containsKey(attributeName)) {
                attributesToRemove.add(attributeName);
            }
        }

        
        for (String attributeName : attributesToRemove) {
            httpSession.removeAttribute(attributeName);
        } 
        
        for (Map.Entry<String, Object> entry : customSessionValues.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
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
        }else if (paramType == Date.class || paramType == Date.class) {
            return Date.valueOf(paramValue);
        } else if (paramType.isEnum()) {
            return Enum.valueOf((Class<Enum>) paramType, paramValue);
        } else {
            Constructor<?> constructor = paramType.getConstructor();
            return constructor.newInstance(paramValue);
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String requestURI = request.getRequestURI();
        return requestURL.substring(0, requestURL.length() - requestURI.length()) + request.getContextPath() + "/";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            handleException(e, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            handleException(e, response);
        }
    }

    private void handleException(Exception e, HttpServletResponse response) throws IOException {
        e.printStackTrace();
        try (PrintWriter out = response.getWriter()) {
            out.println("Error: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                out.println(element.toString());
            }
        }
    }
}
