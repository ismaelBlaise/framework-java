package util;

import java.util.HashSet;
import java.util.Set;

public class Mapping {
    private String controlleur;        // Nom du contrôleur
    private Set<VerbAction> verbeAction; // Ensemble d'actions de verbes

    // Constructeur
    public Mapping(String controlleur) {
        this.controlleur = controlleur;
        this.verbeAction = new HashSet<>(); // Initialisation de l'ensemble
    }

    // Méthode pour ajouter une action de verbe
    public void addVerbAction(VerbAction verbAction) {
        this.verbeAction.add(verbAction);
    }

    // Getters et Setters
    public String getControlleur() {
        return controlleur;
    }

    public void setControlleur(String controlleur) {
        this.controlleur = controlleur;
    }

    public Set<VerbAction> getVerbeAction() {
        return verbeAction;
    }

    public void setVerbeAction(Set<VerbAction> verbeAction) {
        this.verbeAction = verbeAction;
    }

    // Méthode pour afficher les verbes et leurs actions
    public void afficherMappings() {
        System.out.println("Contrôleur : " + controlleur);
        for (VerbAction va : verbeAction) {
            System.out.println("Verbe : " + va.getVerb() + ", Action : " + va.getAction());
        }
    }
}
