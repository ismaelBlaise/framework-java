package util;
import java.lang.annotation.Annotation;

public class Mapping {
    private String controlleur;
    private String methode;
    private Class<? extends Annotation> verb;
    
     
    public Mapping(String controlleur, String methode, Class<? extends Annotation> verb) {
        this.controlleur = controlleur;
        this.methode = methode;
        this.verb = verb;
    }

     
    public Mapping(String controlleur, String methode) {
        this.controlleur = controlleur;
        this.methode = methode;
    }

     
    public String getControlleur() {
        return controlleur;
    }

    public void setControlleur(String controlleur) {
        this.controlleur = controlleur;
    }

    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    public Class<? extends Annotation> getVerb() {
        return verb;
    }

    public void setVerb(Class<? extends Annotation> verb) {
        this.verb = verb;
    }
}
