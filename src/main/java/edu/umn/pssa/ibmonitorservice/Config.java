package edu.umn.pssa.ibmonitorservice;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class Config {

    private static Unmarshaller u;
    static {
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
            Config.u = jc.createUnmarshaller();
        } catch (JAXBException jaxbException) {
            throw new RuntimeException(jaxbException);
        }
    }

    private ConfigType config;

    public Config(String filename) {
        JAXBElement<ConfigType> configElement;
        try {
            configElement = u.unmarshal(new StreamSource(new File(filename)), ConfigType.class);
        } catch (JAXBException jaxbException) {
            System.err.println("FATAL: could not load configuration file");
            throw new RuntimeException("could not load configuration file", jaxbException);
        }
        this.config = configElement.getValue();
    }

    public String getOnCallFile() {
        return config.getOnCallFile();
    }

    public String getEmailReplyTo() {
        return config.getEmailReplyTo();
    }

    public String getEmailUser() {
        return config.getEmailUser();
    }

    public String getEmailPassword() {
        return config.getEmailPassword();
    }

    public String getEmailHost() {
        return config.getEmailHost();
    }

    public Integer getEmailPort() {
        return config.getEmailPort();
    }

    public String getDebugMode() {
        return config.getDebugMode();
    }

    public List<DatabaseType> getDatabase() {
        return config.getDatabase();
    }

}
