package ma.projet.util;

import java.util.Date;

/** Validation et copies defensives des dates du modele de gestion de projets. */
public final class DatesProjet {
    private DatesProjet() { }
    public static Date copie(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
    public static void verifierPlanification(Date debut, Date fin) {
        if (debut == null || fin == null || debut.after(fin)) {
            throw new IllegalArgumentException("Dates planifiees obligatoires, debut <= fin.");
        }
    }
    public static void verifierRealisation(Date debut, Date fin) {
        if (fin != null && (debut == null || debut.after(fin))) {
            throw new IllegalArgumentException("Une fin reelle exige un debut reel anterieur ou egal.");
        }
    }
}
