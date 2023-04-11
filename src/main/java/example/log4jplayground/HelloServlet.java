package example.log4jplayground;

import de.bund.bka.log4jplayground.init.*;
import example.log4jplayground.init.ServiceEndpointInitializer;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.*;

import javax.servlet.http.*;
import java.io.*;


public class HelloServlet  extends HttpServlet {
    private String message;
    private static Logger logger = LogManager.getLogger(HelloServlet.class);


    public void init() {
        try {
            ServiceEndpointInitializer initializer = new ServiceEndpointInitializer(getServletContext());
            LoggerContext lc = LoggerContext.getContext(false);
            logger.info("HelloServlet.init...!");
            logger.info("ConfigurationName:{}, Location:{}\n", lc.getConfiguration().getName(), lc.getConfiguration().getConfigurationSource().getLocation());
        }
        catch(Exception ex) {
            logger.fatal("Exception in ServiceEndpointSOAP.init ", ex);
            throw new RuntimeException(ex);
        }
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        logger.info("Hello Servlet, being called!");
        message = "Hello";
        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
    }


    public void destroy() {
    }
}
