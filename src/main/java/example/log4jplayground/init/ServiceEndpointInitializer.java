package example.log4jplayground.init;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.xml.*;

import javax.servlet.*;
import java.io.*;
import java.nio.file.*;


public class ServiceEndpointInitializer {
    private static Logger logger = LogManager.getLogger(ServiceEndpointInitializer.class);
    private ServletContext sc = null;
    public static Thread logFileWatcher;


    public ServiceEndpointInitializer(ServletContext sc) throws Exception {
        this.sc = sc;
        loadLoggingConfig();
        logger.info("ServiceEndpointInitializer");
    }


    private void loadLoggingConfig() throws Exception {
        FileInputStream fisLog = null;
        String path = sc.getInitParameter("config.dir") + "/log4j2_externalConfig.xml";
        logger.info("load Log4j Config from {}", path);
        try {
            fisLog = new FileInputStream(new File(path));
        }
        catch(Throwable nfex1) {
            throw new Exception();
        }
        finally {
            try {
                if(fisLog != null) {

                    LoggerContext lc = LoggerContext.getContext(false);
                    logger.info("Before reconfiguration!");
                    logger.info("ConfigurationName:{}, Location:{}\n", lc.getConfiguration().getName(), lc.getConfiguration().getConfigurationSource().getLocation());

                    ConfigurationFactory factory = XmlConfigurationFactory.getInstance();
                    ConfigurationSource configurationSource = new ConfigurationSource(fisLog);
                    Configuration configuration = factory.getConfiguration(LoggerContext.getContext(false), configurationSource);
                    lc.setConfiguration(configuration);

                    // ConfigLocation wird nicht gesetzt. In der WLS12 ist sie nach setConfiguration gesetzt.
                    lc.setConfigLocation(new File(path).toURI()); //

                    lc.start();
                    logger.info("After reconfiguration!");
                    logger.info("ConfigurationName:{}, Location:{}\n", lc.getConfiguration().getName(), lc.getConfiguration().getConfigurationSource().getLocation());

                    // close FileInputStream
                    fisLog.close();
                }
            }
            catch(IOException ioex) {
                throw new Exception(" Log4j-Property-Datei " + sc.getInitParameter("config.dir") + "/log4j2_config.xml konnte nicht geladen werden: " + ioex);
            }
        }
    }


    class LogFileWatcher implements Runnable {

        private boolean isRun = true;

        private Path toWatch;

        private String fileName;


        public LogFileWatcher(Path toWatch, String fileName) {
            this.toWatch = toWatch;
            this.fileName = fileName;
            logger.debug("Watching log4j-Config: " + toWatch.toString() + fileName);
        }


        @Override
        public void run() {
            try(final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = toWatch.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while(isRun) {
                    final WatchKey wk = watchService.take();
                    for(WatchEvent<?> event : wk.pollEvents()) {
                        final Path changed = (Path) event.context();
                        if(changed.toString().endsWith(fileName)) {
                            loadLoggingConfig();
                            Level[] levels = new Level[] { Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG };

                            for(Level l : levels) {
                                logger.log(l, "Log4j configuration was updated!");
                            }
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if(!valid) {
                        logger.info("Key has been unregisterede");
                    }
                }
            }
            catch(Exception e) {
                logger.error("Log4jWatcher unexpected exception occurred!", e);
            }
        }


        public void stopRun() {
            isRun = false;
        }
    }
}
