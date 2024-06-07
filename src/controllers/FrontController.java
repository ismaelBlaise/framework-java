package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Mapping;
import util.ModelAndView;
import annotation.AnnotationController;
import annotation.Get;

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

        try (PrintWriter out = response.getWriter()) {
            if (!initialized) {
                try {
                    initControllers(request);
                } catch (Exception e) {
                    out.println("Initialization error: " + e.getMessage());
                   
                    return;
                }
            }

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");

            String requestURL = request.getRequestURL().toString();
            out.println("<p><b>URL:</b> " + requestURL + "</p>");

            if (!controllerList.isEmpty()) {
                for (String controller : controllerList) {
                    out.println("<p>Controller: " + controller + "</p>");
                }
            }

            if (urlMappings.containsKey(requestURL)) {
                Mapping map = urlMappings.get(requestURL);
                out.println("<b>Controller Class:</b> " + map.getControlleur() + "<br>");
                out.println("<b>Associated Method:</b> " + map.getMethode() + "<br>");

                try {
                    Class<?> clazz = Class.forName(map.getControlleur());
                    Method method = clazz.getDeclaredMethod(map.getMethode());
                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                    Object result = method.invoke(controllerInstance);

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
                if (directory.exists() && directory.isDirectory()) {
                    packageEmpty = packageEmpty && directory.list().length == 0;
                    scanControllers(request, directory, packageName);
                }
            }
        }

        if (packageEmpty) {
            throw new Exception("The package " + packageName + " is empty.");
        }

        if (controllerList.isEmpty()) {
            throw new Exception("No classes annotated with @AnnotationController found in package " + packageName);
        }

        initialized = true;
    }

    private void scanControllers(HttpServletRequest request, File directory, String packageName) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(request, file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Get.class)) {
                                validateAndRegisterMethod(request, clazz, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception("Class not found: " + className, e);
                }
            }
        }
    }

    private void validateAndRegisterMethod(HttpServletRequest request, Class<?> clazz, Method method) throws Exception {
        if (method.getReturnType().equals(String.class) || method.getReturnType().equals(ModelAndView.class)) {
            Get getAnnotation = method.getAnnotation(Get.class);
            String urlName = request.getRequestURL().toString() + getAnnotation.url();
            if (urlMappings.containsKey(urlName)) {
                throw new Exception("URL " + urlName + " is already defined.");
            } else {
                urlMappings.put(urlName, new Mapping(clazz.getName(), method.getName()));
            }
        } else {
            throw new Exception("Method return type must be String or ModelAndView.");
        }
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
