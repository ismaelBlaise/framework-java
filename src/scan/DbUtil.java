package scan;

import jakarta.servlet.http.HttpServletRequest;

public class DbUtil {
    HttpServletRequest httpServletRequest;

    public DbUtil(HttpServletRequest request){
        this.httpServletRequest=request;
    }
}
