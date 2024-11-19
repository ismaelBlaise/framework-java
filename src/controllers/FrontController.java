package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
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
    private Gson gson = new Gson();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        synchronized (this) {
            if (!initialized) {
                try {
                    ControllerScan controllerScan = new ControllerScan(urlMappings, controllerList, getServletContext(), request);
                    controllerScan.initControllers();
                    initialized = true;
                } catch (Exception e) {
                    response.setContentType("text/plain;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.println("Erreur d'initialisation : " + e.getMessage());
                    }
                    return;
                }
            }
        }

        String requestURL = request.getRequestURL().toString();
        String baseUrl = getBaseUrl(request);
        String mappedURL = requestURL.replace(baseUrl, "");

        if (!urlMappings.containsKey(mappedURL)) {
            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("L'URL demandée est introuvable.");
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            Mapping map = urlMappings.get(mappedURL);
            if (!map.getVerbAction().testVerbAction(request.getMethod(), mappedURL)) {
                throw new Exception("Requête HTTP non autorisée : méthode " + request.getMethod() +
                        " utilisée au lieu de " + map.getVerbAction().getVerb());
            }

            Class<?> clazz = Class.forName(map.getControlleur());
            Method method = null;

            for (Method method1 : clazz.getDeclaredMethods()) {
                if (method1.getName().equals(map.getMethode())) {
                    method = method1;
                    break;
                }
            }

            if (method == null) {
                throw new Exception("Méthode non trouvée : " + map.getMethode());
            }

            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
            injectCustomSession(request, controllerInstance);

            MethodScan methodScan = new MethodScan(handleError, method, request);
            Object[] methodParam = methodScan.getMethodParameters();
            Object result = method.invoke(controllerInstance, methodParam);

            synchronizeSessionAttributes(request, methodParam);

            if (result instanceof String) {
                response.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println(result);
                }
            } else if (result instanceof ModelAndView) {
                handleModelAndViewResponse((ModelAndView) result, method, request, response);
            } else {
                if (method.isAnnotationPresent(RestApi.class)) {
                    response.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.println(gson.toJson(result));
                    }
                }
            }
        } catch (Exception e) {
            handleInvocationError(e, response);
        }
    }

    private void injectCustomSession(HttpServletRequest request, Object controllerInstance) throws IllegalAccessException {
        Field[] fields = controllerInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == CustomSession.class) {
                field.setAccessible(true);
                field.set(controllerInstance, new CustomSession());
            }
        }
    }

    private void synchronizeSessionAttributes(HttpServletRequest request, Object[] methodParam) {
        for (Object param : methodParam) {
            if (param instanceof CustomSession) {
                SessionScan.synchronizeSession(request.getSession(), (CustomSession) param);
            }
        }
    }

    private void handleModelAndViewResponse(ModelAndView modelAndView, Method method, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (method.isAnnotationPresent(RestApi.class)) {
            response.setContentType("application/json;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println(gson.toJson(modelAndView.getData()));
            }
        } else {
            for (String key : modelAndView.getData().keySet()) {
                request.setAttribute(key, modelAndView.getData().get(key));
            }
            request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
        }
    }

    private void handleInvocationError(Exception e, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<div class='error-message'>Erreur lors de l'invocation : " + e.getMessage() + "</div>");
            out.println("<div class='stack-trace'>Trace de la pile :");
            for (StackTraceElement element : e.getStackTrace()) {
                out.println("<div>" + element.toString() + "</div>");
            }
            out.println("</div>");
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String requestURI = request.getRequestURI();
        return requestURL.substring(0, requestURL.length() - requestURI.length()) + request.getContextPath() + "/";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
