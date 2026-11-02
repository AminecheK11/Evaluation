package ma.projet.util;

import java.util.Date;

public final class Periode {
    private Periode() { }
    public static void verifier(Date debut, Date fin) {
        if (debut == null || fin == null || debut.after(fin))
            throw new IllegalArgumentException("Periode invalide : debut <= fin, dates obligatoires.");
    }
}
