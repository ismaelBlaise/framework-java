package util;

public class Mapping {
    String controlleur;
    String methode;
    
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
}
