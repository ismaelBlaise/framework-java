package controller;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontController extends HttpServlet{


    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        
        PrintWriter printWriter=response.getWriter();
        printWriter.println(request.getRequestURL().toString());

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
                
        processRequest(request, response);

    }

    

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
}