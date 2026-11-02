package ma.projet.service;

import ma.projet.classes.Tache;
import ma.projet.util.DatesProjet;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

public class TacheService extends AbstractService<Tache> {
    public TacheService() { super(Tache.class); }

    public List<Tache> findPrixSuperieur1000() {
        return inTransaction(session -> session.createNamedQuery("Tache.prixSuperieur1000", Tache.class)
                .getResultList());
    }

    /** Realisations entierement comprises dans la periode, bornes inclusives, sans doublons. */
    public List<Tache> findRealiseesEntreDates(Date debut, Date fin) {
        DatesProjet.verifierPlanification(debut, fin);
        return inTransaction(session -> session.createQuery("""
                select distinct t from EmployeTache et
                join et.tache t join fetch t.projet p join fetch p.chefProjet
                where et.dateDebutReelle >= :debut and et.dateFinReelle <= :fin
                order by t.id
                """, Tache.class)
                .setParameter("debut", debut, TemporalType.DATE)
                .setParameter("fin", fin, TemporalType.DATE).getResultList());
    }
}
