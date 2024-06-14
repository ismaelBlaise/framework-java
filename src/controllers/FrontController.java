package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Mapping;
import util.ModelAndView;
import annotation.Controller;
import annotation.Get;
import annotation.Post;
import annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;

public class FrontController extends HttpServlet {
    private List<String> controllerList = new ArrayList<>();
    private Map<String, Mapping> urlMappings = new HashMap<>();
    private boolean initialized = false;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

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
            out.println("</head>");
            out.println("<body>");
            out.println("<p><b>URL:</b> " + requestURL + "</p>");
            out.println("<p><b>Method:</b> " + requestMethod + "</p>");
            
            out.println("<p><b>Available Controllers:</b></p>");
            out.println("<ul>");
            for (String controller : controllerList) {
                out.println("<li>" + controller + "</li>");
            }
            out.println("</ul>");

            String mappedURL = requestURL.replace(baseUrl, "");
            if (urlMappings.containsKey(mappedURL)) {
                Mapping map = urlMappings.get(mappedURL);
                out.println("<b>Controller Class:</b> " + map.getControlleur() + "<br>");
                out.println("<b>Associated Method:</b> " + map.getMethode() + "<br>");

                try {
                    Class<?> clazz = Class.forName(map.getControlleur());
                    Method[] meth = clazz.getDeclaredMethods();
                    Method method=null;

                    for (Method method1 : meth) {
                        if(method1.getName().compareTo(map.getMethode())==0){
                            method=method1;
                        }
                    }
                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                    Object result = method.invoke(controllerInstance, getMethodParameters(method, request));

                    if (result instanceof String) {
                        out.println("<br>Method Invocation Result: " + result);
                    } else if (result instanceof ModelAndView) {
                        ModelAndView modelAndView = (ModelAndView) result;
                        for (String key : modelAndView.getData().keySet()) {
                            request.setAttribute(key, modelAndView.getData().get(key));
                        }
                        request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
                        return;
                    }
                } catch (Exception e) {
                    out.println("Error invoking method: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                out.println("<p>No associated method found for this URL.</p>");
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

    private void initControllers(HttpServletRequest request) throws Exception {
        ServletContext context = getServletContext();
        String packageName = context.getInitParameter("package-to-scan");

        if (packageName == null) {
            throw new Exception("No package-to-scan parameter found in context.");
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
            throw new Exception("The package " + packageName + " is empty.");
        }

        if (controllerList.isEmpty()) {
            throw new Exception("No classes annotated with @AnnotationController found in package " + packageName);
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
                            if (method.isAnnotationPresent(Get.class) || method.isAnnotationPresent(Post.class)) {
                                validateAndRegisterMethod(clazz, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception("Class not found: " + className, e);
                }
            }
        }
    }

    private void validateAndRegisterMethod(Class<?> clazz, Method method) throws Exception {
        if (method.getReturnType().equals(String.class) || method.getReturnType().equals(ModelAndView.class)) {
            String urlName = null;
            if (method.isAnnotationPresent(Get.class)) {
                Get getAnnotation = method.getAnnotation(Get.class);
                urlName = getAnnotation.url();
            } else if (method.isAnnotationPresent(Post.class)) {
                Post postAnnotation = method.getAnnotation(Post.class);
                urlName = postAnnotation.url();
            }

            if (urlName != null && urlMappings.containsKey(urlName)) {
                throw new Exception("URL " + urlName + " is already defined.");
            } else if (urlName != null) {
                urlMappings.put(urlName, new Mapping(clazz.getName(), method.getName()));
            }
        } else {
            throw new Exception("Method return type must be String or ModelAndView.");
        }
    }
    

    private Object[] getMethodParameters(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value();
                String paramValue = request.getParameter(paramName);
                paramValues[i] = convertParameterType(paramValue, parameters[i].getType());
            }
        }

        return paramValues;
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
