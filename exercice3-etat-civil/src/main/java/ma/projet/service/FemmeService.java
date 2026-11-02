package ma.projet.service;

import ma.projet.beans.Femme;
import ma.projet.util.Periode;
import java.util.Date;
import java.util.List;

public class FemmeService extends AbstractService<Femme> {
    public FemmeService() { super(Femme.class); }
    public Femme findPlusAgee() {
        return transaction(s -> s.createQuery(
                "from Femme f order by f.dateNaissance, f.id", Femme.class).setMaxResults(1).uniqueResult());
    }
    /** Somme des enfants declares sur les mariages actifs pendant la periode.
     * Le modele ne contient pas les dates de naissance des enfants. */
    public long compterEnfantsEntreDates(int femmeId, Date debut, Date fin) {
        Periode.verifier(debut, fin);
        return transaction(s -> s.createNamedQuery("Femme.nombreEnfantsEntreDates", Long.class)
                .setParameter("femmeId", femmeId)
                .setParameter("debut", debut, javax.persistence.TemporalType.DATE)
                .setParameter("fin", fin, javax.persistence.TemporalType.DATE).getSingleResult());
    }
    public List<Femme> findMarieesAuMoinsDeuxFois() {
        return transaction(s -> s.createNamedQuery("Femme.marieesAuMoinsDeuxFois", Femme.class).getResultList());
    }
}
