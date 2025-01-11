package util;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

public class HandleError {
    private HttpSession session;

    public HandleError(HttpSession session) {
        this.session = session;
    }

    /**
     * Récupère une valeur pour un champ spécifique depuis les erreurs.
     *
     * @param key La clé du champ (par exemple, "user.username").
     * @return La valeur associée ou une chaîne vide si non trouvée.
     */
    public String getFieldValue(String key) {
        Map<String, String> errors = getErrors();
        return errors != null && errors.containsKey(key) ? errors.get(key) : "";
    }

    /**
     * Récupère le message d'erreur pour un champ spécifique.
     *
     * @param key La clé de l'erreur (par exemple, "user.username.err").
     * @return Le message d'erreur ou une chaîne vide si non trouvé.
     */
    public String getErrorMessage(String key) {
        Map<String, String> errors = getErrors();
        return errors != null && errors.containsKey(key) ? errors.get(key) : "";
    }

    /**
     * Efface les erreurs après qu'elles ont été affichées.
     */
    public void clearErrors() {
        session.removeAttribute("error");
    }

    /**
     * Récupère toutes les erreurs stockées dans la session.
     *
     * @return Une Map contenant les erreurs, ou null si aucune erreur n'est stockée.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getErrors() {
        return (Map<String, String>) session.getAttribute("error");
    }
}
