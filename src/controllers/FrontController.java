package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import scan.ControllerScan;
import scan.MethodScan;
import scan.SessionScan;
import util.CustomSession;
import util.Mapping;
import util.ModelAndView;
import annotation.RestApi;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;


@MultipartConfig
@WebServlet(urlPatterns = "/")
public class FrontController extends HttpServlet {
    private List<String> controllerList = new ArrayList<>();
    private Map<String, Mapping> urlMappings = new HashMap<>();
    private Map<String, String> handleError = new HashMap<>();
    private boolean initialized = false;
    private Gson gson=new Gson();
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        

        synchronized (this) {
            if (!initialized) {
                try {
                    ControllerScan controllerScan=new ControllerScan(urlMappings, controllerList, getServletContext(), request);
                    controllerScan.initControllers();
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
                out.println("L'URL demandée est introuvable.");
                // response.sendError(HttpServletResponse.SC_NOT_FOUND, "L'URL demandée est introuvable.");
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
                        MethodScan methodScan=new MethodScan(handleError,method, request);

                        Object[] methodParam=methodScan.getMethodParameters();
                        Object result = method.invoke(controllerInstance,methodParam );

                        if(!handleError.isEmpty()){
                            String referer = request.getHeader("Referer");
                            if(referer!=null){
                                request.setAttribute("error",handleError);
                                request.getRequestDispatcher(referer).forward(request, response);
                            }
                        }


                        for(Object param: methodParam){
                            if(param instanceof CustomSession){
                                SessionScan.synchronizeSession(request.getSession(),(CustomSession) param);
                            }
                        }
                        response.setContentType("text/html;charset=UTF-8");
                        if (result instanceof String) {
                            out.println("<br>Résultat de l'invocation de méthode : " + result);
                        } else if (result instanceof ModelAndView) {
                            
                            
                            ModelAndView modelAndView = (ModelAndView) result;
                            if(method.isAnnotationPresent(RestApi.class)){
                                response.setContentType("application/json;charset=UTF-8");
                                PrintWriter out1=response.getWriter();
                                out1.println(gson.toJson(modelAndView.getData()));
                                out1.close();
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
