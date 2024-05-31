package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Mapping;
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
    List<String> controllerList;
    Map<String,Mapping> url=new HashMap<>();
    boolean initialized = false;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        response.setContentType("text/html;charset=UTF-8");

        if (!initialized) {
            init(request,response);
        }
        getServletContext().getContextPath();
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");
           
            out.println("<br><p><b>URL</b>: " + request.getRequestURL() + "</p>");
            if (controllerList.size() != 0) {
                for (String controller : controllerList) {
                    out.println("<p> Controller: " + controller + "</p>");
                }
            }
            
            if(url.containsKey(request.getRequestURL().toString())){
                

                out.println("<br><b>Class controller</b> :"+url.get(request.getRequestURL().toString()).getControlleur()+"<br>");
                out.println("<b>Methode associer </b>:"+url.get(request.getRequestURL().toString()).getMethode()+"<br>");
                
                Mapping map=url.get(request.getRequestURL().toString());
                Class<?> clazz = Class.forName(map.getControlleur());
                Method meth=clazz.getDeclaredMethod(map.getMethode(),null);
                Object obj=clazz.getDeclaredConstructor().newInstance();
                out.println("<br>Invocation Method :"+meth.invoke(obj,null));

            }
            else{
                out.println("<p>Il n'y a pas de methode associer</p>");
            }
            
            out.println("</body>");
            out.println("</html>");
        }
    }

    public void init(HttpServletRequest request,HttpServletResponse response) {
        controllerList = new ArrayList<>();
        try {
            ServletContext context = getServletContext();
            String packageName = context.getInitParameter("package-to-scan");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(request, file, packageName);
                }
            }

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanControllers(HttpServletRequest request,File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }

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
                        Method[] methods=clazz.getDeclaredMethods();
                        for (Method method :methods) {
                            if(method.isAnnotationPresent(Get.class)){
                                Get getAnnotation=method.getAnnotation(Get.class);
                                String urlName=request.getRequestURL().toString()+""+getAnnotation.url();
                                url.put(urlName,new Mapping(className,method.getName()));
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

   

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | ServletException
                | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | ServletException
                | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
